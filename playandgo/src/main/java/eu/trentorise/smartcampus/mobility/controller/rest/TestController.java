package eu.trentorise.smartcampus.mobility.controller.rest;

import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

//import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
//import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import eu.trentorise.smartcampus.mobility.gamificationweb.NotificationsManager;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;

@Controller
public class TestController {

	// private static final String NOTIFICATION_APP = "mobility.trentoplaygo.test";

	@Autowired
	private NotificationsManager notificationManager;

	@Autowired
	private NotificationHelper notificatioHelper;

	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;	
	
	@Autowired
	private AppSetup appSetup;

	@Autowired
	private GameSetup gameSetup;		

	private static Log logger = LogFactory.getLog(TestController.class);

	@PostConstruct
	public void init() {
		
		
	}
	
//	@HystrixCommand(commandKey="yesPingTimeout",fallbackMethod = "yesPingTimeout", commandProperties = { 
//			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500"),
//			@HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "2"),
//			@HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000")			
////			@HystrixProperty(name = "execution.isolation.strategy", value = "SEMAPHORE"),
//			})
	@RequestMapping(method = RequestMethod.GET, value = "/test/pingTimeout")
	public @ResponseBody void pingTimeout(HttpServletResponse response) {
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			System.err.println(e.getMessage());
//		}
		System.err.println("START TO");
		
		long start = System.currentTimeMillis();
		do {
		} while (System.currentTimeMillis() < start + 2000);
		
		System.err.println("END TO");
	}

	public void yesPingTimeout(HttpServletResponse response) throws Exception {
		System.err.println("FALLBACK TO");
		response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
	}
	
//	@HystrixCommand(commandKey="ping404",fallbackMethod = "yesPing404", commandProperties = { 
//			@HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "2"),
//			@HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000")
//	})
	@RequestMapping(method = RequestMethod.GET, value = "/test/ping404")
	public @ResponseBody String ping404(HttpServletResponse response) {
		RestTemplate restTemplate = new RestTemplate();
		System.err.println("START 404");
		ResponseEntity<String> res = restTemplate.exchange(gamificationUrl + "gengine/state/5b7a885149c95d50c5f9d442/15", HttpMethod.GET, new HttpEntity<Object>(null, createHeaders("trentinoplaygo2018")),
				String.class);
		String data = res.getBody();
		System.err.println("END 404");
		return data;
	}	
	
	public String yesPing404(HttpServletResponse response) throws Exception {
		System.err.println("FALLBACK 404");
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		return "Error";
	}	
	

	@RequestMapping(method = RequestMethod.GET, value = "/test/notification")
	public @ResponseBody void notification(@RequestParam(required = true) String playerId, @RequestHeader(required = true, value = "appId") String appId) throws Exception {
		Player p = playerRepositoryDao.findByPlayerIdAndGameId(playerId, getGameId(appId));

		notificationManager.sendDirectNotification(appId, p, "INVITATION", null);
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

	// @RequestMapping(method = RequestMethod.GET, value = "/test/notification")
	// public @ResponseBody void notification(@RequestParam(required = true) String id, @RequestParam(required = false) String title, @RequestParam(required = false) String description, @RequestParam(required = false) String type, @RequestParam(required = true) String notificationAppId) throws Exception {
	// Notification notification = notificatioManager.buildSimpleNotification("it", LevelGainedNotification.class.getSimpleName());
	//
	// if (title != null) {
	// notification.setTitle(title);
	// }
	// if (description != null) {
	// notification.setDescription(description);
	// }
	// if (type != null) {
	// Map<String, Object> content = Maps.newTreeMap();
	// content.put("type", type);
	// notification.setContent(content);
	// }
	//
	// notificatioHelper.notify(notification, id, notificationAppId);
	// }

	// @RequestMapping(method = RequestMethod.GET, value = "/test/broadcast")
	// public @ResponseBody void broadcast(@RequestParam(required = false) String title, @RequestParam(required = false) String description) throws Exception {
	// Notification notification = new Notification();
	// if (title != null) {
	// notification.setTitle(title);
	// } else {
	// notification.setTitle("Test broadcast");
	// }
	// if (description != null) {
	// notification.setDescription(description);
	// } else {
	// notification.setDescription("...");
	// }
	//
	// notificatioHelper.notify(notification, NOTIFICATION_APP);
	//
	// }

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

}