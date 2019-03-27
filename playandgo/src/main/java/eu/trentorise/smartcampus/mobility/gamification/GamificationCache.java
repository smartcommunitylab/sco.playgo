package eu.trentorise.smartcampus.mobility.gamification;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import eu.trentorise.smartcampus.mobility.gamification.model.GameStatistics;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;

@Component
public class GamificationCache {

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;	

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private GameSetup gameSetup;	
	
	private LoadingCache<String, String> playerState;
	private LoadingCache<String, String> playerNotifications;
	private LoadingCache<String, List<GameStatistics>> statistics;
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}		
	
	private static transient final Logger logger = LoggerFactory.getLogger(GamificationCache.class);
	
	@PostConstruct
	public void init() {
		playerState = CacheBuilder.newBuilder().refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
			@Override
			public String load(String id) throws Exception {
				try {
					String[] ids = id.split("@");
					String data = loadPlayerState(ids[0], ids[1]);
					logger.debug("Loaded player state: " + ids[0]);
					return data;
				} catch (Exception e) {
					logger.error("Error populating player state cache: ", e);
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<String> reload(String key, String old) {
				ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
					@Override
					public String call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for player state: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}

		});	
		
		playerNotifications = CacheBuilder.newBuilder().refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
			@Override
			public String load(String id) throws Exception {
				try {
					String[] ids = id.split("@");
					String data = loadNotifications(ids[0], ids[1]);
					logger.debug("Loaded player notifications: " + ids[0]);
					return data;
				} catch (Exception e) {
					logger.error("Error populating player notifications cache:", e);
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<String> reload(String key, String old) {
				ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
					@Override
					public String call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for notifications: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}

		});			
		
		
		statistics = CacheBuilder.newBuilder().refreshAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<String, List<GameStatistics>>() {
			@Override
			public List<GameStatistics> load(String id) throws Exception {
				try {
					List<GameStatistics> data = loadStatistics(id);
					logger.debug("Loaded statistics: " + id);
					return data;
				} catch (Exception e) {
					logger.error("Error populating statistics cache: ", e);
					throw e;
				}
			}
			
			@Override
			public ListenableFuture<List<GameStatistics>> reload(String key, List<GameStatistics> old) {
				ListenableFutureTask<List<GameStatistics>> task = ListenableFutureTask.create(new Callable<List<GameStatistics>>() {
					@Override
					public List<GameStatistics> call() throws Exception {
						try {
							return load(key);
						} catch (Exception e) {
							logger.error("Returning old value for notifications: " + key);
							return old;
						}
					}
				});
				task.run();
				return task;
			}

		});			
		
		
	}	
	
	public String getPlayerState(String playerId, String appId) {
		try {
			return playerState.get(playerId + "@" + appId);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getPlayerNotifications(String playerId, String appId) {
		try {
			return playerNotifications.get(playerId + "@" + appId);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}	
	
	public void invalidatePlayer(String playerId, String appId) {
		playerState.invalidate(playerId + "@" + appId);
		playerNotifications.invalidate(playerId + "@" + appId);
	}
	
	public List<GameStatistics> getStatistics(String appId) {
		try {
			return statistics.get(appId);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}	
	
	private String loadPlayerState(String playerId, String appId) {
		AppInfo appInfo = appSetup.findAppById(appId);
		if (appInfo == null) {
			return null;
		}
		String gameId = appInfo.getGameId();
		if (gameId == null) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();
		String url = gamificationUrl + "gengine/state/" + gameId + "/" + playerId;
		logger.debug("Player state: " + url);
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)),
				String.class);
		String data = res.getBody();		
		
		return data;
	}

	private String loadNotifications(String playerId, String appId) {
		AppInfo appInfo = appSetup.findAppById(appId);
		if (appInfo == null) {
			return null;
		}
		String gameId = appInfo.getGameId();
		if (gameId == null) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();
		String url = gamificationUrl + "/notification/game/" + gameId + "/player/" + playerId + "/grouped?size=10000";
		logger.debug("Notifications: " + url);
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)),
				String.class);
		String data = res.getBody();		
		
		return data;
	}	
	
	private List<GameStatistics> loadStatistics(String appId) throws Exception {
		AppInfo appInfo = appSetup.findAppById(appId);
		if (appInfo == null) {
			return null;
		}
		String gameId = appInfo.getGameId();
		if (gameId == null) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();
		String url = gamificationUrl + "data/game/" + gameId + "/statistics";
		logger.debug("Statistics: " + url);
		ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);		
		
		List<GameStatistics> stats = mapper.readValue(result.getBody(),  new TypeReference<List<GameStatistics>>() {});
		
		return stats;
	}	
	
	
	
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
