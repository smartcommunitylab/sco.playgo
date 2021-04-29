package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import eu.trentorise.smartcampus.mobility.gamification.model.ExecutionDataDTO;
import eu.trentorise.smartcampus.mobility.gamificationweb.WebLinkUtils.PlayerIdentity;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;
import eu.trentorise.smartcampus.mobility.security.BannedChecker;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ConfigUtils;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;

@Controller
@EnableScheduling
public class GamificationWebController {

	private static transient final Logger logger = LoggerFactory.getLogger(GamificationWebController.class);

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;

	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	protected BasicProfileService profileService;

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;	
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private WebLinkUtils linkUtils;
	
	@Autowired
	private ConfigUtils configUtils;
	
	@Autowired
	private BannedChecker bannedChecker;
	
	@RequestMapping(method = RequestMethod.GET, value = {"/gamificationweb","/gamificationweb/"})	///{socialId}
	public 
	ModelAndView web(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang) {
		ModelAndView model = new ModelAndView("redirect:/gamificationweb/rules");
		model.addObject("lang", lang);
		return model;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/cookie_license")	///{socialId}
	public 
	ModelAndView cookieLicense(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang) {
		ModelAndView model = new ModelAndView("web/cookie_license");
		model.addObject("lang", lang);
		return model;
	}
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/cookie_info")	///{socialId}
	public 
	ModelAndView cookieInfo(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang) {
		ModelAndView model = new ModelAndView("web/cookie_info");
		model.addObject("lang", lang);
		return model;
	}

	
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/{page}")	///{socialId}
	public 
	ModelAndView webPage(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false, defaultValue="it") String lang, @PathVariable String page) {
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(lang));

		ModelAndView model = new ModelAndView("web/index");
		model.addObject("lang", lang);
		WeekConfData week = configUtils.getCurrentWeekConf();
		if (week != null) {
			model.addObject("week", week.getWeekNum());
			model.addObject("weeklyPrizes", configUtils.getWeekPrizes(week.getWeekNum(), lang));
		}
		model.addObject("view", page);
		return model;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/survey/{lang}/{survey}/{playerId:.*}")	///{socialId}
	public 
	ModelAndView survey(HttpServletRequest request, HttpServletResponse response, @PathVariable String lang, @PathVariable String survey, @PathVariable String playerId) throws Exception {
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(lang));
		
		ModelAndView model = null;
		try {
			PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
			String sId = identity.playerId;
			String gameId = identity.gameId;
//			String sId = "4";
//			String gameId = "5edf5f7d4149dd117cc7f17d";
			if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
				logger.info("Survey data. Found player : " + sId);
				Player p = playerRepositoryDao.findByPlayerIdAndGameId(sId, gameId);
				if (p.getSurveys().containsKey(survey)) {
					model = new ModelAndView("web/survey_complete");
					model.addObject("surveyComplete", true);
					return model;
				}
				GameInfo gameInfo = gameSetup.findGameById(gameId);
				if (gameInfo.getSurveys() != null && gameInfo.getSurveys().containsKey(survey)) {
					String url = gameInfo.getSurveys().get(survey);
					if (!StringUtils.isEmpty(url)) {
						url = url.replace("{playerId}", playerId);
						return new ModelAndView("redirect:"+url);
					}
				}
				model = new ModelAndView("web/survey/"+survey);
				model.addObject("language", lang);
				model.addObject("key", playerId);
				model.addObject("survey", survey);
				return model;
			} else {
				logger.error("Unkonwn user data:" + playerId);
				model = new ModelAndView("web/survey_complete");
				model.addObject("surveyComplete", false);
				return model;
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			model = new ModelAndView("web/survey_complete");
			model.addObject("surveyComplete", false);
			return model;
		}		
	}

	@RequestMapping(method = RequestMethod.POST, value = "/gamificationweb/survey/{lang}/{survey}/{playerId:.*}")	///{socialId}
	public 
	ModelAndView sendSurvey(
			@RequestBody MultiValueMap<String,String> formData, 
			@PathVariable String lang, 
			@PathVariable String survey, 
			@PathVariable String playerId, 
			@RequestParam(required=false, defaultValue = "survey_complete") String completeTemplate,
			@RequestParam(required=false, defaultValue = "false") Boolean multi
			) throws Exception {
		ModelAndView model =  null;
		boolean complete = false;
		try {
			PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
			String sId = identity.playerId;
			String gameId = identity.gameId;
			if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
				logger.info("Survey data. Found player : " + sId);
				Player p = playerRepositoryDao.findByPlayerIdAndGameId(sId, gameId);
				if (!p.getSurveys().containsKey(survey)) {
					Map<String, Object> data = toSurveyData(formData, multi);
					data.remove("multi");
					data.remove("completeTemplate");
					p.addSurvey(survey, data);
					sendSurveyToGamification(sId, gameId, survey);
					playerRepositoryDao.save(p);
					model =  new ModelAndView("web/" + completeTemplate);
					model.addObject("surveyData", data);
				}
				complete = true;
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (model == null) {
			model =  new ModelAndView("web/survey_complete");
		}
		model.addObject("surveyComplete", complete);
		return model;
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/gamificationweb/surveyext/{survey:.*}")
	public @ResponseBody Boolean sendSurveyWebhook(
			@RequestBody Map<String,Object> formData, 
			@PathVariable String survey
			) throws Exception {
		boolean complete = false;
		try {
			String playerId = (String)formData.get("playerId");
			PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
			String sId = identity.playerId;
			String gameId = identity.gameId;
			if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
				logger.info("Survey data. Found player : " + sId);
				Player p = playerRepositoryDao.findByPlayerIdAndGameId(sId, gameId);
				if (!p.getSurveys().containsKey(survey)) {
					Map<String, Object> data = new HashMap<>(formData);
					data.remove("playerId");
					data = correctData(data);
					p.addSurvey(survey, data);
					sendSurveyToGamification(sId, gameId, survey);
					playerRepositoryDao.save(p);
				}
				complete = true;
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return complete;
	}
	
	
	/**
	 * @param data
	 * @return
	 */
	private Map<String, Object> correctData(Map<String, Object> data) {
		Map<String, Object> res = new HashMap<>();
		data.keySet().forEach(k -> {
			String nk = k.replace('.', ' ');
			res.put(nk, data.getOrDefault(k, ""));
		});
		return res;
	}

		//Method used to send the survey call to gamification engine (if user complete the survey the engine need to be updated with this call)
		private void sendSurveyToGamification(String playerId, String gameId, String survey) throws Exception{

			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(gameId);
			ed.setPlayerId(playerId);
			ed.setActionId("survey_complete");
			ed.setData(Collections.singletonMap("surveyType", survey));

			String content = JsonUtils.toJSON(ed);
			GameInfo game = gameSetup.findGameById(gameId);
			
			if (bannedChecker.isBanned(playerId, gameId)) {
				logger.info("Not sending for banned player " + playerId);
				return;
			}		
			
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());		
//			logger.info("Sent app survey data to gamification engine ");
		}	
	
	/**
	 * @param formData
	 * @param multi 
	 * @return
	 */
	private Map<String, Object> toSurveyData(MultiValueMap<String, String> formData, Boolean multi) {
		Map<String, Object> result = new HashMap<>();
		formData.forEach((key, list) -> result.put(key, Boolean.TRUE.equals(multi) ? formData.get(key) : formData.getFirst(key)));
		return result;
	}

	// Method used to unsubscribe user to mailing list
	@RequestMapping(method = RequestMethod.GET, value = "/gamificationweb/unsubscribeMail/{playerId:.*}")	///{socialId}
	public 
	ModelAndView unsubscribeMail(HttpServletRequest request, HttpServletResponse response, @PathVariable String playerId) throws Exception {
		ModelAndView model = new ModelAndView("web/unsubscribe");
		String user_language = "it";
		Player p = null;
		if(!StringUtils.isEmpty(playerId)) { // && playerId.length() >= 16){
			logger.debug("WS-GET. Method unsubscribeMail. Passed data : " + playerId);
			try {
				PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
				String sId = identity.playerId;
				String gameId = identity.gameId;
				if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
					logger.info("WS-GET. Method unsubscribeMail. Found player : " + sId);
					p = playerRepositoryDao.findByPlayerIdAndGameId(sId, gameId);
					user_language = (p.getLanguage() != null && !p.getLanguage().isEmpty()) ? p.getLanguage() : "it";
				}
			} catch (Exception ex){
				logger.error("Error in mail unsubscribtion " + ex.getMessage());
				p = null;
			}
		}
		boolean res = (p != null) ? true : false;
		model.addObject("wsresult", res);
		
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(user_language));
		return model;
	}	
	@RequestMapping(method = RequestMethod.POST, value = "/gamificationweb/unsubscribeMail/{playerId:.*}")	///{socialId}
	public 
	ModelAndView sendUnsubscribeMail(HttpServletRequest request, HttpServletResponse response, @PathVariable String playerId) throws Exception {
		ModelAndView model = new ModelAndView("web/unsubscribesuccess");
		String user_language = "it";
		Player p = null;
		if(!StringUtils.isEmpty(playerId)) { // && playerId.length() >= 16){
			logger.debug("WS-GET. Method sendUnsubscribeMail. Passed data : " + playerId);
			try {
				PlayerIdentity identity = linkUtils.decryptIdentity(playerId);
				String sId = identity.playerId;
				String gameId = identity.gameId;
				if(!StringUtils.isEmpty(sId)){	// case of incorrect encrypted string
					logger.info("WS-GET. Method sendUnsubscribeMail. Found player : " + sId);
					p = playerRepositoryDao.findByPlayerIdAndGameId(sId, gameId);
					p.setSendMail(false);
					playerRepositoryDao.save(p);
					user_language = (p.getLanguage() != null && !p.getLanguage().isEmpty()) ? p.getLanguage() : "it";
				}
			} catch (Exception ex){
				logger.error("Error in mail unsubscribtion " + ex.getMessage());
				p = null;
			}
		}
		boolean res = (p != null) ? true : false;
		model.addObject("wsresult", res);
		
		RequestContextUtils.getLocaleResolver(request).setLocale(request, response, Locale.forLanguageTag(user_language));
		return model;
	}	
	
}
