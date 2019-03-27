package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.gamification.GamificationCache;
import eu.trentorise.smartcampus.mobility.gamification.model.Badge;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeAssignmentDTO;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamification.model.PlayerLevel;
import eu.trentorise.smartcampus.mobility.gamification.statistics.AggregationGranularity;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsBuilder;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsGroup;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgeCollectionConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.OtherPlayer;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerClassification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.UserCheck;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.BannedChecker;
import eu.trentorise.smartcampus.mobility.security.CustomTokenExtractor;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ConfigUtils;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.AccountProfile;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;

@RestController
@EnableScheduling
public class PlayerController {
	
	private static final String NICK_RECOMMANDATION = "nick_recommandation";
	private static final String TIMESTAMP = "timestamp";
	
	private static transient final Logger logger = LoggerFactory.getLogger(PlayerController.class);

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@Autowired
	@Value("${mobilityURL}")
	private String mobilityUrl;	

	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;
	
	@Autowired
	private GamificationCache gamificationCache;	

	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	
	protected BasicProfileService profileService;

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;	
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private StatusUtils statusUtils;

	@Autowired
	private ConfigUtils configUtils;
	
	@Autowired
	private BannedChecker bannedChecker;
	
	@Autowired
	private StatisticsBuilder statisticsBuilder;
	
	@Autowired
	private ChallengesUtils challengeUtils;	
	
	@Autowired
	private RankingManager rankingManager;
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	private CustomTokenExtractor tokenExtractor = new CustomTokenExtractor();
	
	private LoadingCache<String, OtherPlayer> otherPlayers;
	
	@PostConstruct
	public void init() {
		profileService = new BasicProfileService(aacURL);
		
		otherPlayers = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).build(new CacheLoader<String, OtherPlayer>() {
			@Override
			public OtherPlayer load(String id) throws Exception {
				try {
					String[] ids = id.split("@");
					OtherPlayer op = buildOtherPlayer(ids[0], ids[1]);
					if (op != null) {
						op.setUpdated(System.currentTimeMillis());
					}
					return op;
				} catch (Exception e) {
					logger.error("Error populating players cache.", e);
					throw e;
				}
			}
		});			
		
	}		
	
	
	@Scheduled(fixedDelay = 1 * 60 * 1000) 
	public synchronized void checkRecommendations() throws Exception {
		for (AppInfo appInfo : appSetup.getApps()) {
			try {
				checkRecommendations(appInfo.getAppId());
			} catch (Exception e) {
				logger.error("Error checking recommendations for " + appInfo.getAppId() + ": " , e);
				e.printStackTrace();
			}
		}
	}

	
	
	
	@GetMapping("/gamificationweb/status/other/{playerId}")
	public @ResponseBody OtherPlayer getOtherPlayerStatus(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String playerId, HttpServletResponse response) throws Exception {
		String userId = null;
		try {
			userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		} catch (SecurityException e) {
			logger.error("Unauthorized user.", e);
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}	
		
		String gameId = getGameId(appId);
		
		Player p = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		final String lang;
		if (p == null || p.getLanguage() == null || p.getLanguage().isEmpty()) {
			lang = "it";
		} else {
			lang = p.getLanguage();
		}
		OtherPlayer op = null;
		
		try {
			op = otherPlayers.get(playerId + "@" + appId);
			op.getWonChallenges().forEach(x -> {
				x.put("text", x.get(lang));
				x.remove("it");
				x.remove("en");
			});
		} catch (InvalidCacheLoadException e1) {
			logger.error("Player " + playerId + " not found.");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		} catch (Exception e) {
			logger.error("Error getting player " + playerId);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;			
		}
		
//		if (op == null) {
//			logger.error("Player " + playerId + " not found.");
//			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//			return null;			
//		}
		
		return op;
	}	
	
	@GetMapping("/gamificationweb/classification")
	public @ResponseBody PlayerClassification getPlayerClassification(HttpServletRequest request, @RequestParam(required = false) Long timestamp, @RequestParam(required = false) Integer start,
			@RequestParam(required = false) Integer end, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse res) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
	logger.debug("WS-get classification user token " + token);
	
	BasicProfile user = null;
	try {
		user = profileService.getBasicProfile(token);
		if (user == null) {
			res.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
	} catch (Exception e) {
		res.setStatus(HttpStatus.UNAUTHORIZED.value());
		return null;
	}
	String userId = user.getUserId();
	String gameId = getGameId(appId);
	
//		PlayerClassification pc = getPlayerClassification(gameId, userId, timestamp, start, end, appId);
	PlayerClassification pc = getCachedPlayerClassification(userId, appId, timestamp, start, end);
	
	return pc;
}	

	// Method used to get the user status data (by mobile app)
	@GetMapping("/gamificationweb/status")
	public @ResponseBody PlayerStatus getPlayerStatus(HttpServletRequest request, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse res) throws Exception{
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get status user token " + token);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				res.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			res.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);
		
		Player p = null;
		String nickName = "";
		p = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		String language = "it";
		if(p != null){
			nickName = p.getNickname();
			language = (p.getLanguage() != null && !p.getLanguage().isEmpty()) ? p.getLanguage() : "it";
		}

		String data = gamificationCache.getPlayerState(userId, appId);
		
		PlayerStatus ps =  statusUtils.convertPlayerData(data, userId, gameId, nickName, mobilityUrl, 1, language);
		
		return ps;
	}
	
	// Method used to check if a user is registered or not to the system (by
	// mobile app)
	@GetMapping("/gamificationweb/checkuser/{socialId}")
	public @ResponseBody UserCheck getUserData(HttpServletRequest request, @PathVariable String socialId, @RequestHeader(required = true, value = "appId") String appId) {
		logger.debug("WS-get checkuser " + socialId);
		boolean result = false;
		String gameId = getGameId(appId);
		
		Player p = playerRepositoryDao.findByPlayerIdAndGameId(socialId, gameId);
		if (p != null && p.getNickname() != null && !p.getNickname().isEmpty()) {
			logger.debug(String.format("Profile find result %s", p.toJSONString()));
			result = true;
		}
		UserCheck uc = new UserCheck(result);
		logger.debug(String.format("WS-get check if user %s already access app: %s", socialId, result));
		return uc;
	}	
	
	
	// Method for mobile player registration (in mobile app)
	@PostMapping("/gamificationweb/register")
	public @ResponseBody Player registerExternal(@RequestBody Map<String, Object> data, @RequestParam String email,
			@RequestParam(required = false, defaultValue = "it") String language, @RequestParam String nickname, @RequestHeader(required = true, value = "appId") String appId, HttpServletRequest req, HttpServletResponse res) {
		logger.debug("External registration. ");

		BasicProfile user = null;
		AccountProfile account = null;
		String token = tokenExtractor.extractHeaderToken(req);
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				res.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
			if (email == null) {
				account = profileService.getAccountProfile(token);
				for (String aName : account.getAccountNames()) {
					for (String key : account.getAccountAttributes(aName).keySet()) {
						if (key.toLowerCase().contains("email")) {
							email = account.getAccountAttributes(aName).get(key);
							if (email != null)
								break;
						}
					}
					if (email != null)
						break;
				}
			}
		} catch (Exception e) {
			res.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String id = user.getUserId();
		String gameId = getGameId(appId);
		logger.debug("External registration: found user profile with id " + id);
		Player withNick = playerRepositoryDao.findByNicknameIgnoreCaseAndGameId(correctNameForQuery(nickname), gameId);
		if (withNick != null && !withNick.getPlayerId().equals(id)) {
			logger.debug("External registration: nickname conflict with user " + withNick.getPlayerId());
			res.setStatus(HttpStatus.CONFLICT.value());
			return null;
		}
		Player p = playerRepositoryDao.findByPlayerIdAndGameId(id, gameId);
		if (p != null) {
			logger.debug("External registration: user exists");
			res.setStatus(HttpStatus.CONFLICT.value());
			return null;
		} else {
			logger.debug("External registration: new user");
			data.put(TIMESTAMP, System.currentTimeMillis());
			p = new Player(id, gameId, user.getName(), user.getSurname(), nickname, email, language, true, data, null, true); // default sendMail attribute value is true
			if (data.containsKey(NICK_RECOMMANDATION) && !((String) data.get(NICK_RECOMMANDATION)).isEmpty()) {
				Player recommender = playerRepositoryDao.findByNicknameIgnoreCaseAndGameId(correctNameForQuery((String) data.get(NICK_RECOMMANDATION)), gameId);
				if (recommender != null) {
					p.setCheckedRecommendation(true);
				} else {
					p.setCheckedRecommendation(false);
				}

			}
			try {
				logger.info("Creating player");
				createPlayerInGamification(id, gameId, appId);
				if (email != null) {
					logger.info("Added user (mobile registration) " + email);
				}
				
				AppInfo app = appSetup.findAppById(appId);
				GameInfo game = gameSetup.findGameById(app.getGameId());
				if (game.getSend() != null && game.getSend()) {
					logger.info("Assigning survey challenge");
					assignSurveyChallenge(id, gameId, appId);
					logger.info("Assigning initial challenge");
					assignInitialChallenge(id, gameId, appId);
					logger.info("Saving player");
				}
				playerRepositoryDao.save(p);
				return p;
			} catch (Exception e) {
				res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				logger.error("Exception in user registration to gamification " + e.getMessage(), e);
			}
		}
		return null;
	}

	private void assignInitialChallenge(String playerId, String gameId, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bonusPointType", "green leaves");
		data.put("bonusScore", new Double(50.0));
		data.put("target", new Double(1.0));
		data.put("periodName", "weekly");
		data.put("counterName", "ZeroImpact_Trips");
		
		ChallengeAssignmentDTO challenge = new ChallengeAssignmentDTO();
		long now = System.currentTimeMillis();
		LocalDateTime ldt = LocalDateTime.now().plusDays(10).with(ChronoField.DAY_OF_WEEK, 6).truncatedTo(ChronoUnit.DAYS).minusSeconds(1);
		Date end = new Date(ldt.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());		
		
		challenge.setStart(new Date(now));
		challenge.setEnd(end);
//		challenge.setEnd(new Date(now + 2 * 7 * 24 * 60 * 60 * 1000L));

		challenge.setModelName("absoluteIncrement");
		challenge.setInstanceName("'initial_challenge_" + Long.toHexString(now) + "-" + Integer.toHexString((playerId + gameId).hashCode()));
		
		challenge.setData(data);
		
		String partialUrl = "game/" + gameId + "/player/" + playerId + "/challenges";
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "data/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(challenge, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());
	}

	// /data/game/{gameId}/player/{playerId}/challenges
	private void assignSurveyChallenge(String playerId, String gameId, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bonusPointType", "green leaves");
		data.put("bonusScore", new Double(100.0));
		data.put("surveyType", "start");
		data.put("link", ""); // TODO
		
		ChallengeAssignmentDTO challenge = new ChallengeAssignmentDTO();
		long now = System.currentTimeMillis();
		challenge.setStart(new Date(now));
		
//		Date end = new Date(now + 2 * 7 * 24 * 60 * 60 * 1000L);
		LocalDateTime ldt = LocalDateTime.now().plusDays(10).with(ChronoField.DAY_OF_WEEK, 6).truncatedTo(ChronoUnit.DAYS).minusSeconds(1);
		Date end = new Date(ldt.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli());
		
		challenge.setEnd(end);

		challenge.setModelName("survey");
		challenge.setInstanceName("start_survey-" + Long.toHexString(now) + "-" + Integer.toHexString((playerId + gameId).hashCode()));
		
		challenge.setData(data);
		
		String partialUrl = "game/" + gameId + "/player/" + playerId + "/challenges";
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "data/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(challenge, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());
	}	
	
	private List<BadgeCollectionConcept> buildBadgeCollectionConcepts(List<Badge> badges) {
		Multimap<String, BadgeConcept> conceptMaps = ArrayListMultimap.create();
		
		badges.forEach(x -> {
			conceptMaps.put(x.getCollectionName(), new BadgeConcept(x.getBadge(), statusUtils.getUrlFromBadgeName(mobilityUrl, x.getBadge())));
		});
		
		List<BadgeCollectionConcept> result = Lists.newArrayList();
		conceptMaps.keySet().forEach(x -> {
			result.add(new BadgeCollectionConcept(x, Lists.newArrayList(conceptMaps.get(x))));
		});
		
		return result;
	}
	
	private OtherPlayer buildOtherPlayer(String playerId, String appId) throws Exception {
		OtherPlayer op = new OtherPlayer();

		String gameId = appSetup.findAppById(appId).getGameId();

		GameInfo game = gameSetup.findGameById(gameId);

		Player player = playerRepositoryDao.findByPlayerIdAndGameId(playerId, gameId);

		if (player == null) {
			return null;
		}

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> res = null;			
		
		try {
			String data = gamificationCache.getPlayerState(playerId, appId);

			int greenLeaves = getGreenLeavesPoints(data);
			op.setGreenLeaves(greenLeaves);
			String level = getGreenLeavesLevel(data);
			op.setLevel(level);
		} catch (Exception e) {
			logger.error("Error retrieving player state", e);
		}


		try {
			String data = gamificationCache.getPlayerNotifications(player.getPlayerId(), appId);
			Map<String, List> notsMap = mapper.readValue(data, Map.class);
			List<Badge> badges = null;
			if (notsMap.containsKey("BadgeNotification")) {
				badges = mapper.convertValue(notsMap.get("BadgeNotification"), new TypeReference<List<Badge>>() {
				});
			} else {
				badges = Collections.EMPTY_LIST;
			}
			op.setBadgeCollectionConcept(buildBadgeCollectionConcepts(badges));
		} catch (Exception e) {
			logger.error("Error retrieving badges", e);
		}		
		
		long now = System.currentTimeMillis();
		try {
			StatisticsGroup statistics = statisticsBuilder.computeStatistics(playerId, appId, 0, System.currentTimeMillis(), AggregationGranularity.total);
			if (statistics.getStats() != null && !statistics.getStats().isEmpty()) {
				op.setStatistics(statistics.getStats().get(0).getData());
			}
		} catch (Exception e) {
			logger.error("Error computing statistics", e);
		}		

		try {
			StatisticsGroup statistics = statisticsBuilder.computeStatistics(playerId, appId, now - (1000L * 60 * 60 * 24 * 31 * 1), now, AggregationGranularity.month);
			if (statistics.getStats() != null && !statistics.getStats().isEmpty()) {
				op.setLastMonthStatistics(statistics.getStats().get(statistics.getStats().size() - 1).getData());
			}
		} catch (Exception e) {
			logger.error("Error computing statistics", e);
		}		
		

		op.setNickname(player.getNickname());

		String data = gamificationCache.getPlayerState(playerId, appId);

		List<ChallengeConcept> challengeConcepts = challengeUtils.parse(data);
		for (ChallengeConcept challengeConcept: challengeConcepts) {
			
			if (challengeConcept.isCompleted()) {
				String itDescription = challengeUtils.fillDescription(challengeConcept, "it");
				String enDescription = challengeUtils.fillDescription(challengeConcept, "en");
				long time = challengeConcept.getDateCompleted().getTime();
				
				Map<String, Object> challengeMap = Maps.newTreeMap();
				challengeMap.put("it", itDescription);
				challengeMap.put("en", enDescription);
				challengeMap.put("date", time);
				
				op.getWonChallenges().add(challengeMap);
			}
		}		

		return op;
	}
	
	private void checkRecommendations(String appId) throws Exception {
		String gameId = appSetup.findAppById(appId).getGameId();
		Iterable<Player> players = playerRepositoryDao.findAllByCheckedRecommendationAndGameId(true, gameId);
		for (Player player : players) {
			logger.debug("Checking recommendation for " + player.getPlayerId());
			if (player.getPersonalData() != null) {
				String nickname = (String) player.getPersonalData().get(NICK_RECOMMANDATION);
				if (nickname != null && !nickname.isEmpty()) {
					Player recommender = playerRepositoryDao.findByNicknameIgnoreCaseAndGameId(correctNameForQuery(nickname), gameId);
					if (recommender != null) {
						if (bannedChecker.isBanned(recommender.getPlayerId(), gameId)) {
							logger.warn("Not sending for banned player " + recommender.getPlayerId());
							player.setCheckedRecommendation(false);
							playerRepositoryDao.save(player);							
							continue;
						}						
						String data = gamificationCache.getPlayerState(player.getPlayerId(), appId);

						if (getGreenLeavesPoints(data) > 0) {
							logger.info("Sending recommendation to gamification engine: " + player.getPlayerId() + " -> " + recommender.getPlayerId());
							sendRecommendationToGamification(recommender.getPlayerId(), gameId, appId);
							player.setCheckedRecommendation(false);
							playerRepositoryDao.save(player);
						} else {
							logger.debug("Not Sending recommendation for " + player.getPlayerId() + " -> " + recommender.getPlayerId() + ", no points yet.");
						}
					} else {
						logger.debug("Recommender not found for " + player.getPlayerId());
						player.setCheckedRecommendation(false);
						playerRepositoryDao.save(player);
					}
				} else {
					logger.debug("No recommender for " + player.getPlayerId());
					player.setCheckedRecommendation(false);
					playerRepositoryDao.save(player);
				}
			}
		}
	}		
	
	
	
		private String correctNameForQuery(String nickName) {
			return "^" + nickName + "$";
		}		
	
	// Method to force the player creation in gamification engine
	private void createPlayerInGamification(String playerId, String gameId, String appId) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		// data.put("actionId", "app_sent_recommandation");
		data.put("gameId", gameId);
		data.put("playerId", playerId);
		String partialUrl = "game/" + gameId + "/player";

		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "console/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(data, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());
	}
	
//	private String getAll(@RequestParam String urlWS, String appId) {
//		RestTemplate restTemplate = new RestTemplate();
//		logger.debug("WS-GET. Method " + urlWS);
//		String result = "";
//		ResponseEntity<String> res = null;
//		try {
//			res = restTemplate.exchange(gamificationUrl + "gengine/" + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
//		} catch (Exception ex) {
//			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
//		}
//		if (res != null) {
//			result = res.getBody();
//		}
//		return result;
//	}	
	
	private PlayerClassification getCachedPlayerClassification(String playerId, String appId, Long timestamp, Integer start, Integer end) throws ExecutionException {
		List<ClassificationData> data = null;

		if (timestamp != null) {
			WeekConfData wcd = configUtils.getWeek(timestamp);
			WeekConfData wcdnow = configUtils.getCurrentWeekConf();
			if (wcdnow == null) {
				wcdnow = WeekConfData.buildDummyCurrentWeek();
			}
			if (wcd == null) {
				if (timestamp < System.currentTimeMillis() - 1000 * 60 * 60 * 24) {
					wcd = WeekConfData.buildDummyPrevioustWeek();
				} else {
					wcd = WeekConfData.buildDummyCurrentWeek();
				}
			}

			if (wcd.getWeekNum() == wcdnow.getWeekNum()) {
				data = rankingManager.getCurrentIncClassification().get(appId);
			} else if (wcd.getWeekNum() == wcdnow.getWeekNum() - 1) {
				data = rankingManager.getPreviousIncClassification().get(appId);
			}
		} else {
			data = rankingManager.getGlobalClassification().get(appId);
		}
		
		PlayerClassification pc = new PlayerClassification();
		if (data == null) {
			return pc;
		}

		AppInfo app = appSetup.findAppById(appId);
		Criteria criteria = new Criteria("gameId").is(app.getGameId());
		Query query = new Query(criteria);
		query.fields().include("nickname").include("playerId");

		List<Player> players = template.find(query, Player.class, "player");
		Map<String, String> nicknames = players.stream().collect(Collectors.toMap(Player::getPlayerId, Player::getNickname));

		pc.setClassificationList(data);
		for (ClassificationData cd : data) {
			if (playerId.equals(cd.getPlayerId())) {
				pc.setActualUser(cd);
				break;
			}
		}
		
		int size = 0;
		if (start == null || start < 1) {
			start = 1;
		}
		if (end != null) {
			if (start != null) {
				size = end - start + 1;
			} else {
				size = end;
			}
		}		
		
		data = data.stream().skip(start != null ? (start - 1) : 0).limit(size != 0 ? size : data.size()).collect(Collectors.toList());
		pc.setClassificationList(data);
		
		return pc;
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

	@SuppressWarnings("rawtypes")
	private String getGreenLeavesLevel(String data) throws Exception {
		Map playerMap = mapper.readValue(data, Map.class);
		if (playerMap.containsKey("levels")) {
			List<PlayerLevel> levelsList = mapper.convertValue(playerMap.get("levels"), new TypeReference<List<PlayerLevel>>() {
			});
			Optional<PlayerLevel> greenLeaves = levelsList.stream().filter(x -> "green leaves".equals(x.getPointConcept())).findFirst();
			return greenLeaves.isPresent() ? greenLeaves.get().getLevelValue() : "";
		}
		return "";
	}

	@SuppressWarnings("rawtypes")
	private int getGreenLeavesPoints(String data) throws Exception {
		Map playerMap = mapper.readValue(data, Map.class);
		if (playerMap.containsKey("state")) {
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			if (stateMap.containsKey("PointConcept")) {
				List<PointConcept> conceptList = mapper.convertValue(stateMap.get("PointConcept"), new TypeReference<List<PointConcept>>(){});
				Optional<PointConcept> greenLeaves = conceptList.stream().filter(x -> "green leaves".equals(x.getName())).findFirst();
				return greenLeaves.isPresent() ? (int)greenLeaves.get().getScore() : 0;
			}
		}
		return 0;
	}

	private void sendRecommendationToGamification(String recommenderId, String gameId, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("actionId", "app_sent_recommandation");
		data.put("gameId", gameId);
		data.put("playerId", recommenderId);
		data.put("data", new HashMap<String, Object>());
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "gengine/execute", HttpMethod.POST, new HttpEntity<Object>(data, createHeaders(appId)), String.class);
		logger.info("Sent app recommendation to gamification engine " + tmp_res.getStatusCode());
	};
	

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
				set("Content-Type", "application/json");
			}
		};
	}	
	
}
