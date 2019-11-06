package eu.trentorise.smartcampus.mobility.controller.rest;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.gamification.GamificationManager;
import eu.trentorise.smartcampus.mobility.gamification.GamificationValidator;
import eu.trentorise.smartcampus.mobility.gamification.GeolocationsProcessor;
import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.ItineraryDescriptor;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance.ScoreStatus;
import eu.trentorise.smartcampus.mobility.gamification.model.TravelDetails;
import eu.trentorise.smartcampus.mobility.gamification.model.UserDescriptor;
import eu.trentorise.smartcampus.mobility.gamification.statistics.AggregationGranularity;
import eu.trentorise.smartcampus.mobility.gamification.statistics.GlobalStatistics;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsBuilder;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsGroup;
import eu.trentorise.smartcampus.mobility.gamificationweb.RankingManager;
import eu.trentorise.smartcampus.mobility.gamificationweb.RankingManager.RankingType;
import eu.trentorise.smartcampus.mobility.gamificationweb.ReportEmailSender;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Event;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.GeolocationsEvent;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.security.AppDetails;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.BannedChecker;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ConfigUtils;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

@RestController
public class GamificationController {

	private static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";

	@Autowired
	private DomainStorage storage;
	@Autowired
	private PlayerRepositoryDao playerRepo;
	
	@Autowired
	@Value("${aacURL}")
	private String aacURL;

	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;
	
	@Autowired
	private StatisticsBuilder statisticsBuilder;

	@Autowired
	private GamificationValidator gamificationValidator;		
	
	@Autowired
	private GamificationManager gamificationManager;	

	@Autowired
	private GeolocationsProcessor geolocationsProcessor;
	
	@Autowired
	private ConfigUtils config;
	
	@Autowired
	private ReportEmailSender emailSender;
	
	@Autowired
	private BannedChecker bannedChecker;
	
	@Autowired
	private RankingManager rankingManager;
	
	private static Log logger = LogFactory.getLog(GamificationController.class);

	private static FastDateFormat shortSdf = FastDateFormat.getInstance("yyyy/MM/dd");
	private static FastDateFormat reverseShortSdf = FastDateFormat.getInstance("dd/MM/yyyy");
	private static FastDateFormat timeSdf = FastDateFormat.getInstance("HH:mm");
	private static FastDateFormat fullSdf = FastDateFormat.getInstance("yyyy/MM/dd HH:mm");

	private ObjectMapper mapper = new ObjectMapper();
	

	@PostMapping("/gamification/geolocations")
	public @ResponseBody String storeGeolocationEvent(@RequestBody(required = false) GeolocationsEvent geolocationsEvent, @RequestHeader(required = true, value = "appId") String appId,
			HttpServletResponse response) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.error("Storing geolocations, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return "";
			}

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.error("Storing geolocations, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return "";
			}
			
			geolocationsProcessor.storeGeolocationEvents(geolocationsEvent, appId, userId, gameId);

		} catch (Exception e) {
			logger.error("Failed storing events: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return "{\"storeResult\":\"FAIL\"}";
		}
		return "{\"storeResult\":\"OK\"}";
	}

	@GetMapping("/gamification/geolocations")
	public @ResponseBody List<Geolocation> searchGeolocationEvent(@RequestParam Map<String, Object> query, HttpServletResponse response) throws Exception {

		Criteria criteria = new Criteria();
		for (String key : query.keySet()) {
			criteria = criteria.and(key).is(query.get(key));
		}

		Query mongoQuery = new Query(criteria).with(new Sort(Sort.Direction.DESC, "created_at"));

		return storage.searchDomainObjects(mongoQuery, Geolocation.class);
	}

	@PutMapping("/gamification/freetracking/{transport}/{itineraryId}")
	public @ResponseBody void startFreeTracking(@PathVariable String transport, @PathVariable String itineraryId,
			@RequestHeader(required = true, value = "appId") String appId, @RequestHeader(required = false, value = "device") String device, HttpServletResponse response) throws Exception {
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.error("Start freetracking, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.error("Start freetracking, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			logger.info("Starting free tracking for user " + userId + ", transport " + transport + ", itineraryId " + itineraryId + ", device = " + device);
			
			Map<String, Object> pars = new TreeMap<String, Object>();

			pars.put("clientId", itineraryId);
			pars.put("userId", userId);
			Date date = new Date(System.currentTimeMillis());
			String day = shortSdf.format(date);
			TrackedInstance res2 = storage.searchDomainObject(pars, TrackedInstance.class);
			if (res2 == null) {
				res2 = new TrackedInstance();
				res2.setClientId(itineraryId);
				res2.setDay(day);
				res2.setUserId(userId);
				res2.setTime(timeSdf.format(date));
			}

			if (device != null) {
				res2.setDeviceInfo(device);
			}
			res2.setStarted(true);
			res2.setFreeTrackingTransport(transport);
			res2.setAppId(appId);
			storage.saveTrackedInstance(res2);

		} catch (Exception e) {
			logger.error("Error in start freetracking: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/gamification/journey/{itineraryId}")
	public @ResponseBody void startItinerary(@PathVariable String itineraryId, @RequestHeader(required = true, value = "appId") String appId, @RequestHeader(required = false, value = "device") String device, HttpServletResponse response)
			throws Exception {
		logger.info("Starting journey for gamification, device = " + device);
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.error("Start planned journey, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.error("Start planned journey, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			Map<String, Object> pars = new TreeMap<String, Object>();

			pars.put("clientId", itineraryId);
			pars.put("userId", userId);
			ItineraryObject res = storage.searchDomainObject(pars, ItineraryObject.class);
			if (res != null && !userId.equals(res.getUserId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				logger.info("Unauthorized.");
				return;
			}
			if (res == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				// TODO report problem better
				logger.info("Start planned journey, itinerary not found.");
				return;
			}

			Date date = new Date(System.currentTimeMillis());
			String day = shortSdf.format(date);
			pars.put("day", day);
			TrackedInstance res2 = storage.searchDomainObject(pars, TrackedInstance.class);
			if (res2 == null) {
				res2 = new TrackedInstance();
				res2.setClientId(itineraryId);
				res2.setDay(day);
				res2.setUserId(userId);
				res2.setTime(timeSdf.format(date));
			}
			res2.setItinerary(res);

			// boolean canSave = true;
//			if (!res2.getStarted() && !res2.getComplete()) {
//				// canSave =
//				gamificationManager.sendIntineraryDataToGamificationEngine(appId, userId, itineraryId + "_" + day, res);
//			}

			if (device != null) {
				res2.setDeviceInfo(device);
			}
			res2.setStarted(true);
			res2.setAppId(appId);
			storage.saveTrackedInstance(res2);

		} catch (Exception e) {
			// TODO correct log, report relevant info
			logger.error("Error in start planned journey: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@PutMapping("/gamification/temporary")
	public @ResponseBody void startTemporaryItinerary(@RequestBody(required=true) ItineraryObject itinerary, @RequestHeader(required = true, value = "appId") String appId, @RequestHeader(required = false, value = "device") String device, HttpServletResponse response)
			throws Exception {
		logger.info("Starting temporary journey for gamification, device = " + device);
		try {
			String userId = getUserId();
			if (userId == null) {
				logger.error("Start temporary journey, user not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = getGameId(appId);
			if (gameId == null) {
				logger.error("Start temporary journey, gameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			Date date = new Date(System.currentTimeMillis());
			String day = shortSdf.format(date);
			TrackedInstance ti = new TrackedInstance();

			ti.setClientId(itinerary.getClientId());
			ti.setDay(day);
			ti.setUserId(userId);
			ti.setTime(timeSdf.format(date));
			
			convertParkWalk(itinerary);
			ti.setItinerary(itinerary);

			if (device != null) {
				ti.setDeviceInfo(device);
			}
			ti.setStarted(true);
			ti.setAppId(appId);
			storage.saveTrackedInstance(ti);

		} catch (Exception e) {
			logger.error("Error in start temporary journey: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}	
	
	private void convertParkWalk(ItineraryObject itinerary) {
		for (Leg leg: itinerary.getData().getLeg()) {
			if (leg.getTransport() != null && TType.PARKWALK.equals(leg.getTransport().getType())) {
				leg.getTransport().setType(TType.WALK);
			}
		}
	}
	
	@GetMapping("/gamification/traveldetails/{id}")
	public @ResponseBody TravelDetails getTravelDetails(@PathVariable String id, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws ParseException {
		String userId = getUserId();
		if (userId == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		}

		Criteria criteria = new Criteria("id").is(id).and("appId").is(appId).and("userId").is(userId);
		
		Query query = new Query(criteria);

		TrackedInstance instance = storage.searchDomainObject(query, TrackedInstance.class);
		
		TravelDetails result = null;
		if (instance != null) {
			result = new TravelDetails();
			result.setFreeTrackingTransport(instance.getFreeTrackingTransport());
			result.setItinerary(instance.getItinerary());
			if (instance.getGeolocationEvents() != null && !instance.getGeolocationEvents().isEmpty()) {
				List<Geolocation> geo = Lists.newArrayList(instance.getGeolocationEvents());
				geo = GamificationHelper.optimize(geo);
				Collections.sort(geo);				
				result.setGeolocationPolyline(GamificationHelper.encodePoly(geo));
			}
			overrideWithOverridden(instance);
			result.setValidationResult(instance.getValidationResult());
			if (instance.getChangedValidity() != null) {
				result.setValidity(instance.getChangedValidity());	
			} else {
				result.setValidity(instance.getValidationResult().getTravelValidity());
			}			
		}
		
		return result;
	}	
	
	private void overrideWithOverridden(TrackedInstance ti) {
		if (ti.getOverriddenDistances() == null || ti.getOverriddenDistances().isEmpty() || ti.getValidationResult() == null || ti.getValidationResult().getValidationStatus() == null) {
			return;
		}
		
		if (ti.getFreeTrackingTransport() != null) {
			ti.getOverriddenDistances().entrySet().forEach(entry -> ti.getValidationResult().getValidationStatus().getEffectiveDistances().put(TrackValidator.toModeType(entry.getKey()), entry.getValue()));
		} else {
			ti.getOverriddenDistances().entrySet().forEach(entry -> ti.getValidationResult().getValidationStatus().getPlannedDistances().put(TrackValidator.toModeType(entry.getKey()), entry.getValue()));
		}
	}
	
	
	@PostMapping("/gamification/console/validate")
	public @ResponseBody void validate(@RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints, @RequestParam(required = false) Boolean toCheck, @RequestParam(required = false) Boolean pendingOnly, @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterTravelId, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws Exception {

//		Criteria criteria = new Criteria("appId").is(appId);
//
//		if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
//			criteria = criteria.and("estimatedScore").gt(0);
//		}
//		if (toCheck != null && toCheck.booleanValue()) {
//			criteria = criteria.and("toCheck").is(true);
//		}	
//		
//		if (fromDate != null) {
//			String fd = shortSdf.format(new Date(fromDate));
//			criteria = criteria.and("day").gte(fd);
//		}
//		
//		if (toDate != null) {
//			String td = shortSdf.format(new Date(toDate));
//			criteria = criteria.andOperator(new Criteria("day").lte(td));
//		}
//		
		
		Criteria criteria = generateFilterCriteria(appId, filterUserId, filterTravelId, fromDate, toDate, excludeZeroPoints, false, toCheck, pendingOnly);
		Query query = new Query(criteria);	
		
		List<TrackedInstance> result = storage.searchDomainObjects(query, TrackedInstance.class);
		
		for (TrackedInstance ti : result) {
			try {
				if (ti.getItinerary() != null) {
					logger.info("Validating planned " + ti.getId());
					ValidationResult vr = gamificationValidator.validatePlannedJourney(ti.getItinerary(), ti.getGeolocationEvents(), appId);
					ti.setValidationResult(vr);
					if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
						Map<String, Object> data = gamificationValidator.computePlannedJourneyScore(appId, ti.getUserId(), ti.getItinerary().getData(), ti.getGeolocationEvents(), vr.getValidationStatus(), ti.getOverriddenDistances(), false);
						if (ti.getScoreStatus() == null || ScoreStatus.UNASSIGNED.equals(ti.getScoreStatus())) {
							ti.setScoreStatus(ScoreStatus.COMPUTED);
						}
						ti.setScore((Long) data.get("estimatedScore"));
						storage.saveTrackedInstance(ti);
					}

				} else {
					logger.info("Validating free tracking " + ti.getId());
					
					ValidationResult vr = gamificationValidator.validateFreeTracking(ti.getGeolocationEvents(), ti.getFreeTrackingTransport(), appId);
//					if (vr != null && TravelValidity.VALID.equals(vr.getTravelValidity())) {
////						TODO reenabled
//						boolean isGroup = gamificationValidator.isTripsGroup(ti.getGeolocationEvents(), ti.getUserId(), appId, ti.getFreeTrackingTransport());
//						if (isGroup) {
//							if ("bus".equals(ti.getFreeTrackingTransport()) || "train".equals(ti.getFreeTrackingTransport())) {
//								vr.getValidationStatus().setValidationOutcome(TravelValidity.PENDING);
//								logger.info("In a group");
//							}
//						}
//					}
					
					ti.setValidationResult(vr);
					Map<String, Object> data = gamificationValidator.computeFreeTrackingScore(appId, ti.getUserId(), ti.getGeolocationEvents(), ti.getFreeTrackingTransport(), vr.getValidationStatus(), ti.getOverriddenDistances());
					if (ti.getScoreStatus() == null || ScoreStatus.UNASSIGNED.equals(ti.getScoreStatus())) {
						ti.setScoreStatus(ScoreStatus.COMPUTED);
					}					
					ti.setScore((Long) data.get("estimatedScore"));
					storage.saveTrackedInstance(ti);
				}
			} catch (Exception e) {
				// TODO fix log
				logger.error("Failed to validate tracked itinerary: " + ti.getId(), e);
			}

		}
	}
	
//	@PostMapping("/gamification/console/assignScore")
//	public @ResponseBody TrackedInstance assignScore(@PathVariable String instanceId, HttpServletResponse response) throws Exception {
//		Map<String, Object> pars = new TreeMap<String, Object>();
//		pars.put("id", instanceId);
//		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
//		if (instance.getScoreStatus() != null && !ScoreStatus.UNASSIGNED.equals(instance.getScoreStatus())) {
//			response.setStatus(HttpServletResponse.SC_CONFLICT);
//			return null;
//		}
//		
//		if (instance.getItinerary() != null) {
//			Map<String, Object> trackingData = gamificationValidator.computePlannedJourneyScore(instance.getAppId(), instance.getItinerary().getData(), instance.getGeolocationEvents(), false);
//			if (trackingData.containsKey("estimatedScore")) {
//				instance.setScore((Long) trackingData.get("estimatedScore"));
//			}
//			trackingData.put(TRAVEL_ID, instance.getId());
//			trackingData.put(START_TIME, getStartTime(instance));
//			gamificationManager.sendIntineraryDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId() + "_" + instance.getDay(), instance.getItinerary(), trackingData);
//			instance.setScoreStatus(ScoreStatus.SENT);
//		} else if (instance.getFreeTrackingTransport() != null) {
//			Map<String, Object> trackingData = gamificationValidator.computeFreeTrackingScore(instance.getAppId(), instance.getGeolocationEvents(), instance.getFreeTrackingTransport(), instance.getValidationResult().getValidationStatus());
//			trackingData.put(TRAVEL_ID, instance.getId());
//			trackingData.put(START_TIME, getStartTime(instance));
//			if (trackingData.containsKey("estimatedScore")) {
//				instance.setScore((Long) trackingData.get("estimatedScore"));
//			}
//			
//			gamificationManager.sendFreeTrackingDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId(), instance.getGeolocationEvents(), instance.getFreeTrackingTransport(), trackingData);
//			instance.setScoreStatus(ScoreStatus.SENT);
//		}
//		
//		storage.saveTrackedInstance(instance);
//		return instance;
//	}
	

	@PostMapping("/gamification/console/itinerary/changeValidity/{instanceId}")
	public @ResponseBody TrackedInstance changeValidity(@PathVariable String instanceId, @RequestParam(required = true) TravelValidity value) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		instance.setChangedValidity(value);
		storage.saveTrackedInstance(instance);
		logger.info("Changed validity for " + instanceId + " to " + value);
		return instance;
	}
	
	@PostMapping("/gamification/console/itinerary/overrideDistances/{instanceId}")
	public @ResponseBody TrackedInstance overrideDistances(@PathVariable String instanceId, @RequestBody Map<String, Double> value) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		
		value = value.entrySet().stream().filter(x -> x.getValue() != null).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		
		instance.setOverriddenDistances(value);
		storage.saveTrackedInstance(instance);
		logger.info("Changed distances for " + instanceId + " to " + value);
		return instance;
	}	
	
	@PostMapping("/gamification/console/itinerary/toCheck/{instanceId}")
	public @ResponseBody TrackedInstance toCheck(@PathVariable String instanceId, @RequestParam(required = true) boolean value) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		instance.setToCheck(value);
		storage.saveTrackedInstance(instance);
		logger.info("Changed \"to check\" for " + instanceId + " to " + value);
		return instance;
	}	

	@PostMapping("/gamification/console/approveFiltered")
	public @ResponseBody void approveFiltered(@RequestHeader(required = false, value = "appId") String appId, @RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints, @RequestParam(required = false) Boolean toCheck, @RequestParam(required = false) Boolean pendingOnly, @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterTravelId) throws Exception {
//		Criteria criteria = new Criteria("changedValidity").ne(null).and("approved").ne(true);
//
//		if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
//			criteria = criteria.and("estimatedScore").gt(0);
//		}
//		if (toCheck != null && toCheck.booleanValue()) {
//			criteria = criteria.and("toCheck").is(true);
//		}	
//		
//		if (fromDate != null) {
//			String fd = shortSdf.format(new Date(fromDate));
//			criteria = criteria.and("day").gte(fd);
//		}
//		
//		if (toDate != null) {
//			String td = shortSdf.format(new Date(toDate));
//			criteria = criteria.andOperator(new Criteria("day").lte(td));
//		}		
//
		
		Criteria criteria = generateFilterCriteria(appId, filterUserId, filterTravelId, fromDate, toDate, excludeZeroPoints, true, toCheck, pendingOnly);
		Query query = new Query(criteria);	
		
		List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
		for (TrackedInstance ti : instances) {
			logger.info("ApproveAndSendScore for " + ti.getId());
			approveAndSendScore(ti, false);
		}
	}
	
	private void approveAndSendScore(TrackedInstance instance, boolean forceQueue) throws Exception {
//		if (!TravelValidity.VALID.equals(instance.getValidationResult().getTravelValidity()) && TravelValidity.VALID.equals(instance.getChangedValidity()) || ScoreStatus.COMPUTED.equals(instance.getScoreStatus())) {
		if (TravelValidity.INVALID.equals(instance.getValidationResult().getTravelValidity()) && !TravelValidity.INVALID.equals(instance.getChangedValidity()) || ScoreStatus.COMPUTED.equals(instance.getScoreStatus())) {
			logger.info("Sending approved itinerary data to GE: " + instance.getId());
			if (instance.getItinerary() != null && instance.getValidationResult() != null) {
				Map<String, Object> trackingData = gamificationValidator.computePlannedJourneyScore(instance.getAppId(), instance.getUserId(), instance.getItinerary().getData(), instance.getGeolocationEvents(), instance.getValidationResult().getValidationStatus(), instance.getOverriddenDistances(), false);
				instance.setScoreStatus(ScoreStatus.COMPUTED);
				if (trackingData.containsKey("estimatedScore")) {
					instance.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, instance.getId());
				trackingData.put(START_TIME, getStartTime(instance));
				if (forceQueue) {
					gamificationManager.removeIdFromQueue(instance.getClientId() + "_" + instance.getDay());
				}
				if (gamificationManager.sendIntineraryDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId() + "_" + instance.getDay(), instance.getItinerary(),
						trackingData)) {
					instance.setScoreStatus(ScoreStatus.SENT);
					logger.info("Sent: " + instance.getId());
					instance.setApproved(true);
				}
			} else if (instance.getFreeTrackingTransport() != null) {
				Map<String, Object> trackingData = gamificationValidator.computeFreeTrackingScore(instance.getAppId(), instance.getUserId(), instance.getGeolocationEvents(), instance.getFreeTrackingTransport(), instance.getValidationResult().getValidationStatus(), instance.getOverriddenDistances());
				instance.setScoreStatus(ScoreStatus.COMPUTED);				
				if (trackingData.containsKey("estimatedScore")) {
					instance.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, instance.getId());
				trackingData.put(START_TIME, getStartTime(instance));
				if (forceQueue) {
					gamificationManager.removeIdFromQueue(instance.getClientId());				}				
				if (gamificationManager.sendFreeTrackingDataToGamificationEngine(instance.getAppId(), instance.getUserId(), instance.getClientId(), instance.getGeolocationEvents(),
						instance.getFreeTrackingTransport(), trackingData)) {
					instance.setScoreStatus(ScoreStatus.SENT);
					logger.info("Sent: " + instance.getId());
					instance.setApproved(true);
				}
			}
		} else {
			logger.info("Not sending approved itinerary data to GE: " + instance.getId());
			instance.setApproved(true);
		}

		storage.saveTrackedInstance(instance);
	}

	@PostMapping("/gamification/console/synchronize")
	public @ResponseBody void synchronize(@RequestHeader(required = true, value = "appId") String appId) throws Exception {
		Criteria criteria = new Criteria("appId").is(appId);
		criteria = criteria.and("scoreStatus").is(ScoreStatus.COMPUTED); //.and("approved").ne(true);
		
		Query query = new Query(criteria);
		
		List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
		
		logger.info("Found " + instances.size() + " COMPUTED trips.");
		instances.forEach(x -> {
			try {
				logger.error("Synchronizing " + x.getId());
				approveAndSendScore(x, true);
			} catch (Exception e) {
				logger.error("Error approving and sending " + x.getId());
			}
		});
	}
	
	// TODO update
	@GetMapping("/gamification/console/report")
	public @ResponseBody void generareReport(HttpServletResponse response, @RequestParam(required = true, value = "appId") String appId, @RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate) throws IOException {
		Criteria criteria = new Criteria("appId").is(appId).and("changedValidity").ne(null).and("approved").ne(true);

		String fileName = "report";
		
		if (fromDate != null) {
			String fd = shortSdf.format(new Date(fromDate));
			criteria = criteria.and("day").gte(fd);
			fileName += "_" + shortSdf.format(new Date(fromDate));
		}
		
		if (toDate != null) {
			String td = shortSdf.format(new Date(toDate));
			criteria = criteria.andOperator(new Criteria("day").lte(td));
			fileName += "_" + shortSdf.format(new Date(toDate));
		}		
		
		Query query = new Query(criteria).with(new Sort(Direction.DESC, "userId"));

		List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
		StringBuffer sb = new StringBuffer("userId;id;freeTracking;itineraryName;score;valid\r\n");
		for (TrackedInstance ti : instances) {
			if (ti.getScore() == null) {
				// TODO freetracking, planned as freetracking...
				if (ti.getItinerary() != null) {
					Itinerary itinerary = ti.getItinerary().getData();
					long score = gamificationValidator.computeEstimatedGameScore(ti.getAppId(), ti.getUserId(), itinerary, ti.getGeolocationEvents(), false);
					ti.setScore(score);
					storage.saveTrackedInstance(ti);
				}
			}
			sb.append(ti.getUserId() + ";" + ti.getId() + ";" + (ti.getFreeTrackingTransport() != null) + ";" 
		+ ((ti.getItinerary() != null) ? ti.getItinerary().getName() : "") + ";" + ti.getScore() + ";" + ((ti.getChangedValidity() != null) ? ti.getChangedValidity() : ti.getValidationResult().getTravelValidity()) + "\r\n");
		}

		response.setContentType("application/csv; charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".csv\"");
		response.getWriter().write(sb.toString());
	}

	@GetMapping("/gamification/console/players")
	public @ResponseBody Iterable<Player> getPlayers(HttpServletResponse response) throws IOException {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		AppInfo ai = appSetup.findAppById(appId);
		if (ai == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}
		return playerRepo.findAllByGameId(ai.getGameId());
	}
	@GetMapping("/gamification/console/checkin/events")
	public @ResponseBody List<String> getCheckingEvents(HttpServletResponse response) throws IOException {
		List<String> list = config.getActiveCheckinEvents();
		return list;
	}

	@PutMapping("/gamification/console/players/{playerId}/checkin/{event}")
	public @ResponseBody Player checkin(@PathVariable String playerId, @PathVariable String event, HttpServletResponse response) throws IOException {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		AppInfo ai = appSetup.findAppById(appId);
		if (ai == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}
		Player p = playerRepo.findByPlayerIdAndGameId(playerId, ai.getGameId());
		List<Event> checkIn = p.getEventsCheckIn();
		if (checkIn == null) checkIn = new LinkedList<>();
		if (!checkIn.stream().anyMatch(e -> event.equals(e.getName()))) {
			
			try {
				gamificationManager.sendCheckin(event, p.getPlayerId(), appId);
			} catch (Exception e1) {
				logger.error(e1.getMessage(), e1);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return null;
			}
			Event e = new Event(event, event, System.currentTimeMillis());
			checkIn.add(e);
			p.setEventsCheckIn(checkIn);
			playerRepo.save(p);
		}
		return p;	
	}
	
	
	@GetMapping("/gamification/console/email/template")
	public @ResponseBody Map<String, String> getEmailTemplate(HttpServletResponse response) throws IOException {
		return Collections.singletonMap("template", config.getGenericEmailTemplate());
	}

	@PutMapping("/gamification/console/email")
	public @ResponseBody void sendEmail(@RequestBody Map<String, Object> body, HttpServletResponse response) throws IOException {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		String text = (String)body.get("html");
		Boolean all = (Boolean) body.get("all");
		String subj = (String)body.get("subject");
		
		try {
			if (all) {
				emailSender.sendGenericMailToAll(text, subj, appId);
			} else {
				String emailsString = (String)body.get("emails");
				Set<String> emails = StringUtils.commaDelimitedListToSet(emailsString);				
				emailSender.sendGenericMailToUsers(text, subj, appId, emails);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		}
	}	

	@GetMapping(value = "/gamification/console/appId", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	public @ResponseBody String getAppId(HttpServletResponse response) throws Exception {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		return appId;
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/gamification/console/useritinerary/{userId}")
	public @ResponseBody List<ItineraryDescriptor> getItineraryListForUser(@PathVariable String userId, @RequestHeader(required = true, value = "appId") String appId,
			@RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints,
			@RequestParam(required = false) Boolean unapprovedOnly, @RequestParam(required = false) Boolean pendingOnly, @RequestParam(required = false) Boolean toCheck,
			@RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterTravelId) throws Exception {
		List<ItineraryDescriptor> list = new ArrayList<ItineraryDescriptor>();

		try {
			String actualUserId = (filterUserId == null || filterUserId.isEmpty()) ? userId : filterUserId;

			Criteria criteria = generateFilterCriteria(appId, actualUserId, filterTravelId, fromDate, toDate, excludeZeroPoints, unapprovedOnly, toCheck, pendingOnly);
			Query query = new Query(criteria);	

			logger.debug("Start itinerary query for " + userId);
			List<TrackedInstance> instances = storage.searchDomainObjects(query, TrackedInstance.class);
			logger.debug("End itinerary query for " + userId);

			Map<String, Double> scores = gamificationManager.getScoreNotification(appId, userId);
			
			if (instances != null) {
				for (TrackedInstance o : instances) {
					List<Geolocation> geo = Lists.newArrayList(o.getGeolocationEvents());
					Collections.sort(geo);
					o.setGeolocationEvents(geo);

					if (!scores.containsKey(o.getId())) {
						logger.info("Missing travel score in GE: " + o.getId());
					}
					if (scores.containsKey(o.getId()) && !ScoreStatus.ASSIGNED.equals(o.getScoreStatus())) {
						logger.info("Set assigned status to trip " + o.getId());
						o.setScore(scores.get(o.getId()).longValue());
						o.setScoreStatus(ScoreStatus.ASSIGNED);
						storage.saveTrackedInstance(o);
					}
				}

//				Collections.reverse(instances);
//				instances = aggregateFollowingTrackedInstances(instances);
//				gamificationValidator.findOverlappedTrips(instances);
				for (TrackedInstance o : instances) {
					
					if (o.getValidationResult().getValidationStatus().getPolyline() == null) {
						List<Geolocation> points = Lists.newArrayList(o.getGeolocationEvents());
						points = TrackValidator.removeStarredClusters(points);
						points = TrackValidator.preprocessTrack(points);
						TrackValidator.shortenByHighSpeed(points);
						String polyline = GamificationHelper.encodePoly(points);
						logger.debug("Generated polyline for " + o.getId() + " = " + polyline);
						o.getValidationResult().getValidationStatus().setPolyline(polyline);
					}
					
//					TODO reenabled
					if (o.getSuspect() == null) {
						o.setSuspect(gamificationValidator.isSuspect(o));
					}
					
					ItineraryDescriptor descr = new ItineraryDescriptor();
					if (o.getUserId() != null) {
						descr.setUserId(o.getUserId());
					} else {
						ItineraryObject itinerary = storage.searchDomainObject(Collections.<String, Object>singletonMap("clientId", o.getClientId()), ItineraryObject.class);
						if (itinerary != null) {
							descr.setUserId(itinerary.getUserId());
						} else {
							continue;
						}
					}
					descr.setTripId(o.getClientId());

					if (o.getGeolocationEvents() != null && !o.getGeolocationEvents().isEmpty()) {
						Geolocation event = o.getGeolocationEvents().iterator().next();
						descr.setStartTime(event.getRecorded_at().getTime());
					} else if (o.getDay() != null && o.getTime() != null) {
						String dt = o.getDay() + " " + o.getTime();
						descr.setStartTime(fullSdf.parse(dt).getTime());
					} else if (o.getDay() != null) {
						descr.setStartTime(shortSdf.parse(o.getDay()).getTime());
					}

					if (o.getItinerary() != null) {
						descr.setEndTime(o.getItinerary().getData().getEndtime());
						descr.setTripName(o.getItinerary().getName() + " (" + o.getId() + ")");
						descr.setRecurrency(o.getItinerary().getRecurrency());
					} else {
						descr.setFreeTrackingTransport(o.getFreeTrackingTransport());
						descr.setTripName(o.getId());
						
						gamificationValidator.setPolylines(o);						
					}
					descr.setInstance(o);
					
					if (o.getDeviceInfo() != null) {
						Map<String, Object> map = mapper.readValue(o.getDeviceInfo(), Map.class);
						if (map != null) {
							map.remove("uuid");
							map.remove("cordova");
							map.remove("available");
							map.remove("manufacturer");
							map.remove("serial");
							o.setDeviceInfo(mapper.writeValueAsString(map));
						}
					}
					list.add(descr);
				}
			}

			Collections.sort(list);
//			Collections.reverse(list);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return list;
	}

	@GetMapping("/gamification/console/users")
	public @ResponseBody List<UserDescriptor> getTrackInstancesUsers(@RequestHeader(required = true, value = "appId") String appId, @RequestParam(required = false) Long fromDate,
			@RequestParam(required = false) Long toDate, @RequestParam(required = false) Boolean excludeZeroPoints, @RequestParam(required = false) Boolean unapprovedOnly, @RequestParam(required = false) Boolean pendingOnly,
			@RequestParam(required = false) Boolean toCheck, @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterTravelId,
			@RequestParam(required = false) RankingType rankingType, @RequestParam(required = false) final Integer maxRanking) throws Exception {
		List<UserDescriptor> userList = null;

		List<ClassificationData> ranking = null;
		
		if (rankingType != null) {
			switch (rankingType) {
			case CURRENT:
				ranking = rankingManager.getCurrentIncClassification().get(appId);
				break;
			case PREVIOUS:
				ranking = rankingManager.getPreviousIncClassification().get(appId);
				break;
			case GLOBAL:
			default:
				ranking = rankingManager.getGlobalClassification().get(appId);
			}
		} else {
			ranking = rankingManager.getGlobalClassification().get(appId);
		}
		
		Set<String> rankingPlayers = null;
		
		if (ranking != null) {
			rankingPlayers = ranking.stream().filter(x -> x.getPosition() <= (maxRanking == null ? 50 : maxRanking)).map(x -> x.getPlayerId()).collect(Collectors.toSet());
		}
		
		try {
			Map<String, UserDescriptor> users = new HashMap<String, UserDescriptor>();
			Set<String> keys = new HashSet<String>();
			keys.add("userId");
			keys.add("validationResult");
			keys.add("approved");
			keys.add("complete");
			keys.add("changedValidity");
			
			AppInfo app = appSetup.findAppById(appId);
			GameInfo game = gameSetup.findGameById(app.getGameId());			

//			Criteria criteria = new Criteria("appId").is(appId);
//			if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
//				criteria = criteria.and("estimatedScore").gt(0);
//			}
//			if (unapprovedOnly != null && unapprovedOnly.booleanValue()) {
//				criteria = criteria.and("approved").ne(true).and("changedValidity").ne(null);
//			}
//			if (toCheck != null && toCheck.booleanValue()) {
//				criteria = criteria.and("toCheck").is(true);
//			}
//
//			if (fromDate != null) {
//				String fd = shortSdf.format(new Date(fromDate));
//				criteria = criteria.and("day").gte(fd);
//			}
//
//			if (toDate != null) {
//				String td = shortSdf.format(new Date(toDate));
//				criteria = criteria.andOperator(new Criteria("day").lte(td));
//			}

			Criteria criteria = generateFilterCriteria(appId, filterUserId, filterTravelId, fromDate, toDate, excludeZeroPoints, unapprovedOnly, toCheck, pendingOnly);
			Query query = new Query(criteria);

			List<TrackedInstance> tis = storage.searchDomainObjects(query, keys, TrackedInstance.class);

			for (TrackedInstance ti : tis) {
				String userId = ti.getUserId();
				if (userId == null) {
					continue;
				}
				if (StringUtils.isEmpty(filterTravelId) && StringUtils.isEmpty(filterUserId) && rankingPlayers != null && !rankingPlayers.contains(userId)) {
					continue;
				}
				UserDescriptor ud = users.get(userId);
				if (ud == null) {
					ud = new UserDescriptor();
					ud.setUserId(userId);
					ud.setValid(0);
					ud.setTotal(0);
				}
				
				ud.setBanned(bannedChecker.isBanned(userId, game.getId()));
				users.put(userId, ud);
				
				ud.setTotal(ud.getTotal() + 1);
				TravelValidity validity = ti.getValidationResult().getTravelValidity();
				if (ti.getApproved() != null && ti.getApproved().booleanValue() && ti.getChangedValidity() != null) {
					validity = ti.getChangedValidity();
				}
				switch (validity) {
				case VALID:
					ud.setValid(ud.getValid() + 1);
					break;
				case INVALID:
					ud.setInvalid(ud.getInvalid() + 1);
					break;
				case PENDING:
					if (ti.getComplete()) {
						ud.setPending(ud.getPending() + 1);
					}
					break;
				}
			}

			userList = new ArrayList<UserDescriptor>(users.values());
			Collections.sort(userList);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return userList;
	}

	@GetMapping("/gamification/console/itinerary/{instanceId}")
	public @ResponseBody TrackedInstance getItineraryData(@PathVariable String instanceId) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("id", instanceId);
		TrackedInstance instance = storage.searchDomainObject(pars, TrackedInstance.class);
		return instance;
	}
	
	@GetMapping("/gamification/statistics/player")
	public @ResponseBody StatisticsGroup playerStatistics(@RequestParam(required=false) Long from, @RequestParam(required=false) Long to, @RequestParam(required=false) AggregationGranularity granularity,
			@RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws Exception {
		StatisticsGroup result = null;
		try {
			String userId = null;
			try {
				userId = getUserId();
			} catch (SecurityException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}	

			long start = 0;
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return null;
			}
			GameInfo game = gameSetup.findGameById(ai.getGameId());			
			
			String startDate = game.getStart();
			start = reverseShortSdf.parse(startDate).getTime();
			
			if (from == null) {
				from = start;
			} else {
				from = Math.max(from, start);
			}			
			if (to == null) {
				to = System.currentTimeMillis();
			}
			if (granularity == null) {
				granularity = AggregationGranularity.total;
			}
			
			logger.debug("Reading " + granularity + " statistics for player " + userId);
			result = statisticsBuilder.computeStatistics(userId, appId, from, to, granularity);
		} catch (Exception e) {
			logger.error("Failed retrieving player statistics events: "+e.getMessage(),e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		logger.debug("Returning " + granularity + " statistics");
		return result;
		
	}
	
	@GetMapping("/gamification/statistics/global/player")
	public @ResponseBody GlobalStatistics globalPlayerStatistics(@RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response) throws Exception {
		GlobalStatistics result = null;
		try {
			String userId = null;
			try {
				userId = getUserId();
			} catch (SecurityException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return null;
			}	

			long start = 0;
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return null;
			}
			GameInfo game = gameSetup.findGameById(ai.getGameId());
			
			String startDate = game.getStart();
			start = reverseShortSdf.parse(startDate).getTime();
			startDate = shortSdf.format(new Date(start));
			

			result = statisticsBuilder.getGlobalStatistics(userId, appId, startDate, true);

		} catch (Exception e) {
			logger.error("Failed retrieving player statistics events: "+e.getMessage(),e);
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return result;
		
	}	
	
//	private List<TrackedInstance> aggregateFollowingTrackedInstances(List<TrackedInstance> instances) {
//		List<TrackedInstance> sortedInstances = Lists.newArrayList(instances);
//		Collections.sort(sortedInstances, new Comparator<TrackedInstance>() {
//
//			@Override
//			public int compare(TrackedInstance o1, TrackedInstance o2) {
//				if (o1.getGeolocationEvents() == null || o1.getGeolocationEvents().isEmpty()) {
//					return -1;
//				}
//				if (o2.getGeolocationEvents() == null || o2.getGeolocationEvents().isEmpty()) {
//					return 1;
//				}
//				return (o1.getGeolocationEvents().iterator().next().compareTo(o2.getGeolocationEvents().iterator().next()));
//			}
//		});
//
//		int groupId = 1;
//		if (sortedInstances.size() > 1) {
//			for (int i = 1; i < sortedInstances.size(); i++) {
//				List<Geolocation> ge1 = (List) sortedInstances.get(i).getGeolocationEvents();
//				List<Geolocation> ge2 = (List) sortedInstances.get(i - 1).getGeolocationEvents();
//				if (sortedInstances.get(i).getFreeTrackingTransport()  == null || sortedInstances.get(i - 1).getFreeTrackingTransport() == null) {
//					continue;
//				}
////				if (!sortedInstances.get(i).getFreeTrackingTransport().equals(sortedInstances.get(i - 1).getFreeTrackingTransport())) {
////					continue;
////				}
//				
//				if (ge1.isEmpty() || ge2.isEmpty()) {
//					continue;
//				}
//				if (Math.abs(ge2.get(ge2.size() - 1).getRecorded_at().getTime() - ge1.get(0).getRecorded_at().getTime()) < GamificationValidator.SAME_TRIP_INTERVAL
//						&& sortedInstances.get(i).getFreeTrackingTransport().equals(sortedInstances.get(i - 1).getFreeTrackingTransport())) {
//					sortedInstances.get(i).setGroupId(groupId);
//					sortedInstances.get(i - 1).setGroupId(groupId);
//				} else {
//					groupId++;
//				}
//			}
//		}
//
//		return sortedInstances;
//	}

	private long getStartTime(TrackedInstance trackedInstance) throws ParseException {
		long time = 0;
		if (trackedInstance.getGeolocationEvents() != null && !trackedInstance.getGeolocationEvents().isEmpty()) {
			Geolocation event = trackedInstance.getGeolocationEvents().stream().sorted().findFirst().get();
//			Geolocation event = trackedInstance.getGeolocationEvents().iterator().next();
			time = event.getRecorded_at().getTime();
		} else if (trackedInstance.getDay() != null && trackedInstance.getTime() != null) {
			String dt = trackedInstance.getDay() + " " + trackedInstance.getTime();
			time = fullSdf.parse(dt).getTime();
		} else if (trackedInstance.getDay() != null) {
			time = shortSdf.parse(trackedInstance.getDay()).getTime();
		}
		return time;
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

	protected String getUserId() {
		String principal = (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return principal;
	}
	
	private Criteria generateFilterCriteria(String appId, String userId, String travelId, Long fromDate, Long toDate, Boolean excludeZeroPoints, Boolean unapprovedOnly, Boolean toCheck, Boolean pendingOnly) {
		Criteria criteria = new Criteria("appId").is(appId);
		
		if (userId != null && !userId.isEmpty()) {
			criteria = criteria.and("userId").is(userId);
		}
		
		if (travelId != null && !travelId.isEmpty()) {
			criteria = criteria.and("_id").is(travelId.trim());
		} else {

			if (excludeZeroPoints != null && excludeZeroPoints.booleanValue()) {
				criteria = criteria.and("estimatedScore").gt(0);
			}
			if (unapprovedOnly != null && unapprovedOnly.booleanValue()) {
				criteria = criteria.and("approved").ne(true).and("changedValidity").ne(null);
				// Criteria("changedValidity").ne(null).and("approved").ne(true);
			}
			if (toCheck != null && toCheck.booleanValue()) {
				criteria = criteria.and("toCheck").is(true);
			}

			if (fromDate != null) {
				String fd = shortSdf.format(new Date(fromDate));
				criteria = criteria.and("day").gte(fd);
			}

			if (toDate != null) {
				String td = shortSdf.format(new Date(toDate));
				criteria = criteria.andOperator(new Criteria("day").lte(td));
			}
			if (pendingOnly != null && pendingOnly) {
				criteria = criteria.orOperator(new Criteria("validationResult.validationStatus.validationOutcome").is(TravelValidity.PENDING).and("changedValidity").is(null),
						new Criteria("changedValidity").is(TravelValidity.PENDING), new Criteria("validationResult.validationStatus.validationOutcome").is(null));
			}
		}
		
		return criteria;
	}
	
}
