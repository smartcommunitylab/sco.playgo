package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamification.GamificationCache;
import eu.trentorise.smartcampus.mobility.gamification.challenges.TargetPrizeChallengesCalculator;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeChoice;
import eu.trentorise.smartcampus.mobility.gamification.model.Inventory;
import eu.trentorise.smartcampus.mobility.gamification.model.Inventory.ItemChoice;
import eu.trentorise.smartcampus.mobility.gamification.model.Inventory.ItemChoice.ChoiceType;
import eu.trentorise.smartcampus.mobility.gamification.model.Invitation;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept.ChallengeDataType;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation.ChallengePlayer;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation.PointConceptRef;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeInvitation.Reward;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerBlackList;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.CustomTokenExtractor;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;

@Controller
public class ChallengeController {

	private enum InvitationStatus {
		accept,refuse,cancel
	}
	
	private static Log logger = LogFactory.getLog(ChallengeController.class);
	
	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;	
	
	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	
	@Autowired
	@Value("${playgoURL}")
	private String playgoURL;		
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	private ChallengesUtils challengeUtils;		
	
	@Autowired
	private StatusUtils statusUtils;	
	
	@Autowired
	private NotificationsManager notificationsManager;
	
	@Autowired
	private GamificationCache gamificationCache;	
	
	@Autowired
	private TargetPrizeChallengesCalculator tpcc;	
	
	private BasicProfileService profileService;
	
	private CustomTokenExtractor tokenExtractor = new CustomTokenExtractor();	
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	private Map<String, Reward> rewards;
	
	@PostConstruct
	public void init() throws Exception {
		profileService = new BasicProfileService(aacURL);
		
		rewards = mapper.readValue(Resources.getResource("challenges/rewards.json"), new TypeReference<Map<String, Reward>>() {
		});
	}
	
	@GetMapping("/gamificationweb/challenge/type/{playerId}")
	public @ResponseBody List<ChallengeChoice> getChallengesStatus(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String playerId, HttpServletResponse response) throws Exception {
		String gameId = getGameId(appId);
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + playerId + "/inventory", HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		
		String res = result.getBody();
		
		Inventory inventory = mapper.readValue(res , Inventory.class);

		return inventory.getChallengeChoices();
	}	
	
	@PutMapping("/gamificationweb/challenge/unlock/{type}")
	public @ResponseBody List<ChallengeChoice> activateChallengeType(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String type, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get status user token " + token);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();		
		String gameId = getGameId(appId);
		
		gamificationCache.invalidatePlayer(userId, appId);
		
		RestTemplate restTemplate = new RestTemplate();
		ItemChoice choice = new ItemChoice(ChoiceType.CHALLENGE_MODEL, type);
		
		try {
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/inventory/activate", HttpMethod.POST, new HttpEntity<Object>(choice, createHeaders(appId)), String.class);
		
		String res = result.getBody();
		
		Inventory inventory = mapper.readValue(res , Inventory.class);

		return inventory.getChallengeChoices();
		} catch (HttpClientErrorException e) {
			response.setStatus(e.getRawStatusCode());
			return null;
		}
	}	
	
	@GetMapping("/gamificationweb/challenges")
	public @ResponseBody eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept getChallenges(@RequestHeader(required = true, value = "appId") String appId, @RequestParam(required=false) ChallengeDataType filter, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get status user token " + token);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
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

		String statusUrl = "state/" + gameId + "/" + userId;
		String allData = getAll(statusUrl, appId);
		
		PlayerStatus ps =  statusUtils.convertPlayerData(allData, userId, gameId, nickName, playgoURL, 1, language);
		if (filter != null) {
			ps.getChallengeConcept().getChallengeData().entrySet().removeIf(x -> !filter.equals(x.getKey()));
		}
		
		return ps.getChallengeConcept();
	}	
	
	@PutMapping("/gamificationweb/challenge/choose/{challengeId}")
	public void chooseChallenge(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String challengeId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		logger.debug("WS-get status user token " + token);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);
		
		gamificationCache.invalidatePlayer(userId, appId);
		
		RestTemplate restTemplate = new RestTemplate();
		String partialUrl = "game/" + gameId + "/player/" + userId + "/challenges/" + challengeId + "/accept";
		ResponseEntity<String> tmp_res = restTemplate.exchange(gamificationUrl + "data/" + partialUrl, HttpMethod.POST, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
		logger.info("Sent player registration to gamification engine(mobile-access) " + tmp_res.getStatusCode());		
	}
	
	@PostMapping("/gamificationweb/invitation")
	public void sendInvitation(@RequestHeader(required = true, value = "appId") String appId, @RequestBody Invitation invitation, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);	
		
		gamificationCache.invalidatePlayer(userId, appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;			
		}
		Player attendee = playerRepositoryDao.findByPlayerIdAndGameId(invitation.getAttendeeId(), gameId);
		if (attendee == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;			
		}		
		
		if (attendee.getId().equals(player.getId())) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;	
		}
		
		ChallengeInvitation ci = new ChallengeInvitation();
		ci.setGameId(gameId);
		ci.setProposer(new ChallengePlayer(userId));
		ci.getGuests().add(new ChallengePlayer(invitation.getAttendeeId()));
		ci.setChallengeModelName(invitation.getChallengeModelName().toString()); // "groupCompetitivePerformance"
		
		LocalDateTime day = LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).truncatedTo(ChronoUnit.DAYS);
		ci.setChallengeStart(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // next saturday
		day = day.plusWeeks(1).minusSeconds(1);
		ci.setChallengeEnd(new Date(day.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli())); // 2 fridays
		
		ci.setChallengePointConcept(new PointConceptRef(invitation.getChallengePointConcept(), "weekly")); // "Walk_Km"
		
		Reward reward = rewards.get(ci.getChallengeModelName());
		
		if (invitation.getChallengeModelName().isCustomPrizes()) {
			Map<String, Double> prizes = tpcc.targetPrizeChallengesCompute(userId, invitation.getAttendeeId(), appId, invitation.getChallengePointConcept(), invitation.getChallengeModelName().toString());
			logger.info("Calculated prize for " + userId + "/" + invitation.getAttendeeId() + ": " + prizes);
			Map<String, Double> bonusScore = Maps.newTreeMap();
			bonusScore.put(userId, prizes.get(TargetPrizeChallengesCalculator.PLAYER1_PRZ));
			bonusScore.put(invitation.getAttendeeId(), prizes.get(TargetPrizeChallengesCalculator.PLAYER2_PRZ));
			reward.setBonusScore(bonusScore);
			ci.setChallengeTarget(prizes.get(TargetPrizeChallengesCalculator.TARGET));
		}

		ci.setReward(reward); // from body
		
		RestTemplate restTemplate = new RestTemplate();

		String url = gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/invitation";
		
		ResponseEntity<String> result = null;
		
		try {
			logger.info("Sending invitation " + mapper.writeValueAsString(ci));
		result = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<Object>(ci, createHeaders(appId)), String.class);
		
		if (result.getStatusCode() == HttpStatus.OK) {
			Map<String, String> extraData = Maps.newTreeMap();
			extraData.put("opponent", player.getNickname());
			notificationsManager.sendDirectNotification(appId, attendee, "INVITATION", extraData);			
		}
		} catch (HttpClientErrorException e) {
			logger.error("GE returned " + e.getRawStatusCode());
			response.sendError(e.getRawStatusCode());
		} catch (Exception e2) {
			logger.error("Error sending invitation", e2);
			response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		
	}

	@PostMapping("/gamificationweb/invitation/preview")
	public @ResponseBody Map<String, String> getGroupChallengePreview(@RequestHeader(required = true, value = "appId") String appId, @RequestBody Invitation invitation, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return null;	
		}
		Player attendee = playerRepositoryDao.findByPlayerIdAndGameId(invitation.getAttendeeId(), gameId);
		if (attendee == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return null;	
		}		
		
		if (attendee.getId().equals(player.getId())) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return null;
		}
		
		Reward reward = rewards.get(invitation.getChallengeModelName().toString());
		
		Map<String, Object> pars = Maps.newTreeMap();
		pars.put("opponent", attendee.getNickname());
		
		if (invitation.getChallengeModelName().isCustomPrizes()) {
			Map<String, Double> prizes = tpcc.targetPrizeChallengesCompute(userId, invitation.getAttendeeId(), appId, invitation.getChallengePointConcept(), invitation.getChallengeModelName().toString());
			logger.info("Calculated prize for preview " + userId + "/" + invitation.getAttendeeId() + ": " + prizes);
			Map<String, Double> bonusScore = Maps.newTreeMap();
			pars.put("rewardBonusScore", prizes.get(TargetPrizeChallengesCalculator.PLAYER1_PRZ));
			pars.put("reward", prizes.get(TargetPrizeChallengesCalculator.PLAYER1_PRZ));
			pars.put("challengerBonusScore", prizes.get(TargetPrizeChallengesCalculator.PLAYER2_PRZ));
			pars.put("challengeTarget", prizes.get(TargetPrizeChallengesCalculator.TARGET));
			pars.put("target", prizes.get(TargetPrizeChallengesCalculator.TARGET));
		} else {
			pars.put("rewardPercentage", reward.getPercentage());
			pars.put("rewardThreshold", reward.getThreshold());
		}
		
		String descr = challengeUtils.fillDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars, player.getLanguage());
		String longDescr = challengeUtils.fillLongDescription(invitation.getChallengeModelName().toString(), invitation.getChallengePointConcept(), pars, player.getLanguage());
		
		Map<String, String> result = Maps.newTreeMap();
		result.put("description", descr);
		result.put("longDescription", longDescr);
		
		return result;
	}	
	
	@PostMapping("/gamificationweb/invitation/status/{challengeName}/{status}")
	public void changeInvitationStatus(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String challengeName, @PathVariable InvitationStatus status, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);		
		
		gamificationCache.invalidatePlayer(userId, appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;			
		}		
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/invitation/" + status + "/" + challengeName, HttpMethod.POST, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
	}	
	
	@GetMapping("/gamificationweb/blacklist")
	public @ResponseBody List<Map<String, String>> getBlackList(@RequestHeader(required = true, value = "appId") String appId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return null;
		}

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/blacklist", HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);		
		
		PlayerBlackList pbl = mapper.readValue(result.getBody(), PlayerBlackList.class);
		
		List<Map<String, String>> res = Lists.newArrayList();
		pbl.getBlockedPlayers().forEach(x -> {
			Player p = playerRepositoryDao.findByPlayerIdAndGameId(x, gameId);
			if (p != null) {
				Map<String, String> pd = Maps.newTreeMap();
				pd.put("id", x);
				pd.put("nickname", p.getNickname());
				res.add(pd);
			}
		});
		
		return res;
	}
	
	@PostMapping("/gamificationweb/blacklist/{otherPlayerId}")
	public @ResponseBody void addToBlackList(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String otherPlayerId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);	
		
		gamificationCache.invalidatePlayer(userId, appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		Player other = playerRepositoryDao.findByPlayerIdAndGameId(otherPlayerId, gameId);
		if (other == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}		
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/block/" + otherPlayerId, HttpMethod.POST, new HttpEntity<Object>(null, createHeaders(appId)), String.class);		
	}	
	
	@DeleteMapping("/gamificationweb/blacklist/{otherPlayerId}")
	public @ResponseBody void deleteFromBlackList(@RequestHeader(required = true, value = "appId") String appId, @PathVariable String otherPlayerId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);	
		
		gamificationCache.invalidatePlayer(userId, appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		Player other = playerRepositoryDao.findByPlayerIdAndGameId(otherPlayerId, gameId);
		if (other == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}			
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/unblock/" + otherPlayerId, HttpMethod.POST, new HttpEntity<Object>(null, createHeaders(appId)), String.class);		
	}		
	
	@GetMapping("/gamificationweb/challengables")
	public @ResponseBody List<Map<String, String>> getChallengables(@RequestHeader(required = true, value = "appId") String appId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String token = tokenExtractor.extractHeaderToken(request);
		BasicProfile user = null;
		try {
			user = profileService.getBasicProfile(token);
			if (user == null) {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				return null;
			}
		} catch (Exception e) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return null;
		}
		String userId = user.getUserId();
		String gameId = getGameId(appId);		
		
		Player player = playerRepositoryDao.findByPlayerIdAndGameId(userId, gameId);
		if (player == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> result = restTemplate.exchange(gamificationUrl + "data/game/" + gameId + "/player/" + userId + "/challengers", HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);		
		
		List<String> ps = mapper.readValue(result.getBody(), List.class);
		
		List<Map<String, String>> res = Lists.newArrayList();
		ps.forEach(x -> {
			Player p = playerRepositoryDao.findByPlayerIdAndGameId(x, gameId);
			if (p != null) {
				Map<String, String> pd = Maps.newTreeMap();
				pd.put("id", x);
				pd.put("nickname", p.getNickname());
				res.add(pd);
			}
		});
		
		Collections.sort(res, new Comparator<Map>() {
			@Override
			public int compare(Map o1, Map o2) {
				return ((String)o1.get("nickname")).compareToIgnoreCase((String)o2.get("nickname"));
			}
			
		});
		
		return res;
	}	
	
	@GetMapping("/gamificationweb/challenges/rewards")
	public @ResponseBody Map<String, Reward> getRewards(@RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws Exception {
		return rewards;
	}	
	
	private String getAll(@RequestParam String urlWS, String appId) {
		RestTemplate restTemplate = new RestTemplate();
		logger.debug("WS-GET. Method " + urlWS);
		String result = "";
		ResponseEntity<String> res = null;
		try {
			res = restTemplate.exchange(gamificationUrl + "gengine/" + urlWS, HttpMethod.GET, new HttpEntity<Object>(createHeaders(appId)), String.class);
		} catch (Exception ex) {
			logger.error(String.format("Exception in proxyController get ws. Method: %s. Details: %s", urlWS, ex.getMessage()));
		}
		if (res != null) {
			result = res.getBody();
		}
		return result;
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
