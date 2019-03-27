package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.gamification.model.ClassificationBoard;
import eu.trentorise.smartcampus.mobility.gamification.model.ClassificationPosition;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;

@Component
public class RankingManager {

	public enum RankingType {
		NONE, GLOBAL, CURRENT, PREVIOUS  
	}
	
	private static transient final Logger logger = LoggerFactory.getLogger(RankingManager.class);
	
	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;	
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;		

	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	private LoadingCache<String, List<ClassificationData>> currentIncClassification;
	private LoadingCache<String, List<ClassificationData>> previousIncClassification;
	private LoadingCache<String, List<ClassificationData>> globalClassification;	
	
	@PostConstruct
	public void init() {
//		profileService = new BasicProfileService(aacURL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		currentIncClassification = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
				.build(new CacheLoader<String, List<ClassificationData>>() {
					@Override
					public List<ClassificationData> load(String appId) throws Exception {
						String gameId = getGameId(appId);
						if (gameId != null) {
							try {
								return getFullIncClassification(gameId, appId, System.currentTimeMillis());
							} catch (Exception e) {
								logger.error("Error populating current classification cache.", e);
							}
						}
						return Collections.EMPTY_LIST;
					}
				});
		
		previousIncClassification = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
				.build(new CacheLoader<String, List<ClassificationData>>() {
					@Override
					public List<ClassificationData> load(String appId) throws Exception {
						String gameId = getGameId(appId);
						if (gameId != null) {
							try {
								return getFullIncClassification(gameId, appId, System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L);
							} catch (Exception e) {
								logger.error("Error populating previous classification cache.", e);
							}								
						}
						return Collections.EMPTY_LIST;
					}
				});	
		
		globalClassification = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
				.build(new CacheLoader<String, List<ClassificationData>>() {
					@Override
					public List<ClassificationData> load(String appId) throws Exception {
						String gameId = getGameId(appId);
						if (gameId != null) {
							try {
								return getFullClassification(gameId, appId);
							} catch (Exception e) {
								logger.error("Error populating previous classification cache.", e);
							}								
						}
						return Collections.EMPTY_LIST;
					}
				});			
		
	}		

	public LoadingCache<String, List<ClassificationData>> getCurrentIncClassification() {
		return currentIncClassification;
	}

	public LoadingCache<String, List<ClassificationData>> getPreviousIncClassification() {
		return previousIncClassification;
	}

	public LoadingCache<String, List<ClassificationData>> getGlobalClassification() {
		return globalClassification;
	}

	private List<ClassificationData> getFullClassification(String gameId, String appId) throws Exception {
		List<ClassificationData> classificationList = Lists.newArrayList();

		try {
			String url = "game/" + gameId + "/classification/" + URLEncoder.encode("global classification green", "UTF-8");
			ClassificationBoard board = getClassification(url, appId);
			if (board != null) {
				computeRanking(board);

				Criteria criteria = new Criteria("gameId").is(gameId);
				Query query = new Query(criteria);
				query.fields().include("nickname").include("playerId");

				List<Player> players = template.find(query, Player.class, "player");
				Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getPlayerId, Player::getNickname));

				if (board.getBoard() != null) {
					for (ClassificationPosition pos : board.getBoard()) {
						if (nicknames.get(pos.getPlayerId()) != null) {
							ClassificationData cd = new ClassificationData(pos.getPlayerId(), nicknames.get(pos.getPlayerId()), (int) pos.getScore(), pos.getPosition());
							classificationList.add(cd);
						}
					}
				} else {
					logger.error("Empty board");
				}
			}
		} catch (Exception e) {
			logger.error("Error reading classification", e);
		}

		return classificationList;
	}
	
	private List<ClassificationData> getFullIncClassification(String gameId, String appId, Long timestamp) throws Exception {
		List<ClassificationData> classificationList = Lists.newArrayList();

		try {
			String url = "game/" + gameId + "/incclassification/" + URLEncoder.encode("week classification green", "UTF-8") + "?timestamp=" + timestamp;
			ClassificationBoard board = getClassification(url, appId);
			if (board != null) {
				computeRanking(board);

				Criteria criteria = new Criteria("gameId").is(gameId);
				Query query = new Query(criteria);
				query.fields().include("nickname").include("playerId");

				List<Player> players = template.find(query, Player.class, "player");
				Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getPlayerId, Player::getNickname));

				for (ClassificationPosition pos : board.getBoard()) {
					if (nicknames.get(pos.getPlayerId()) != null) {
						ClassificationData cd = new ClassificationData(pos.getPlayerId(), nicknames.get(pos.getPlayerId()), (int) pos.getScore(), pos.getPosition());
						classificationList.add(cd);
					}
				}
			} else {
				logger.error("Empty board");
			}
		} catch (Exception e) {
			logger.error("Error reading incclassification", e);
		}

		return classificationList;
	}
	
	private ClassificationBoard getClassification(@RequestParam String urlWS, String appId) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		logger.debug("WS-GET. Method " + urlWS); // Added for log ws calls info
													// in preliminary phase of
													// portal
		String result = "";
		ResponseEntity<String> tmp_res = null;
		try {
			// result = restTemplate.getForObject(gamificationUrl + urlWS,
			// String.class);
			tmp_res = restTemplate.exchange(gamificationUrl + "data/" + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		} catch (Exception ex) {
			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
		}
		if (tmp_res != null) {
			result = tmp_res.getBody();
		}

		ClassificationBoard board = null;
		if (result != null && !result.isEmpty()) {
			board = mapper.readValue(result, ClassificationBoard.class);
			// Collections.sort(board.getBoard());

			Multimap<Double, ClassificationPosition> ranking = ArrayListMultimap.create();
			board.getBoard().forEach(x -> ranking.put(x.getScore(), x));
			TreeSet<Double> scores = new TreeSet<>(ranking.keySet());

			int position = 1;
			for (Double score : scores.descendingSet()) {
				final int pos = position;
				ranking.get(score).stream().forEach(x -> x.setPosition(pos));
				position++;
			}
			board.setBoard(Lists.newArrayList(ranking.values()));
			Collections.sort(board.getBoard());
		}

		return board;
	}	
	
	private void computeRanking(ClassificationBoard board) {
		Multimap<Double, ClassificationPosition> ranking = ArrayListMultimap.create();
		board.getBoard().forEach(x -> ranking.put(x.getScore(), x));
		TreeSet<Double> scores = new TreeSet<>(ranking.keySet());

		int position = 1;
		for (Double score : scores.descendingSet()) {
			int ex = 0;
			for (ClassificationPosition exaequo : ranking.get(score)) {
				exaequo.setPosition(position);
				ex++;
			}
			position += ex;
		}
		board.setBoard(Lists.newArrayList(ranking.values()));
		Collections.sort(board.getBoard());

		board.setUpdateTime(System.currentTimeMillis());
	}		
	
	private String getGameId(String appId) {
		if (appId != null) {
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				return null;
			}
			String gameId = ai.getGameId();
			return gameId;
		}
		return null;
	}	
	
	@SuppressWarnings("serial")
	HttpHeaders createHeaders(String appId) {
		return new HttpHeaders() {
			{
				AppInfo app = appSetup.findAppById(appId);
				GameInfo game = gameSetup.findGameById(app.getGameId());
				String auth = game.getUser() + ":" + game.getPassword();
				byte[] encodedAuth = Base64.encode(auth.getBytes(Charset.forName("UTF-8")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}	
	
}
