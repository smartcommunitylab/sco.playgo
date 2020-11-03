package eu.trentorise.smartcampus.mobility.gamification;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Striped;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance.ScoreStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.Activity;
import eu.trentorise.smartcampus.mobility.geolocation.model.Battery;
import eu.trentorise.smartcampus.mobility.geolocation.model.Coords;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.GeolocationsEvent;
import eu.trentorise.smartcampus.mobility.geolocation.model.Location;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

@Component
public class GeolocationsProcessor {

	/**
	 * 
	 */
	private static final int LOCATION_STORE_INTERVAL = 2 * 24 * 3600 * 1000;
	private static FastDateFormat shortSdf = FastDateFormat.getInstance("yyyy/MM/dd");
	private static FastDateFormat timeSdf = FastDateFormat.getInstance("HH:mm");
	private static FastDateFormat fullSdf = FastDateFormat.getInstance("yyyy/MM/dd HH:mm");

	private static final String TRAVEL_ID = "travelId";
	public static final String START_TIME = "startTime";
	private static final int MAX_LOCATIONS = 10000;
	

	@Autowired
	private DomainStorage storage;

	@Autowired
	private GamificationValidator gamificationValidator;

	@Autowired
	private GamificationManager gamificationManager;
	
	@Autowired
	private GamificationCache gamificationCache;		
	
	private Striped<Lock> striped = Striped.lock(20);

	private static Log logger = LogFactory.getLog(GeolocationsProcessor.class);

	public void storeGeolocationEvents(GeolocationsEvent geolocationsEvent, String appId, String userId, String gameId) throws Exception {
		// logger.info("Receiving geolocation events, token = " + token + ", " + geolocationsEvent.getLocation().size() + " events");

		ObjectMapper mapper = new ObjectMapper();

		Lock lock = striped.get(userId);

		try {
			lock.lock();
			int pointCount = 0;
			if (geolocationsEvent.getLocation() != null) {
				pointCount = geolocationsEvent.getLocation().size();
			}
			logger.info("Received " + pointCount + " geolocations for " + userId + ", " + geolocationsEvent.getDevice());

			boolean virtual = false;
			
			if (geolocationsEvent.getDevice() != null) {
				virtual = (boolean) geolocationsEvent.getDevice().getOrDefault("isVirtual", false);
			}

			if (!virtual) {
				gamificationCache.invalidatePlayer(userId, appId);
				
				checkEventsOrder(geolocationsEvent, userId);

				Multimap<String, Geolocation> geolocationsByItinerary = ArrayListMultimap.create();
				Map<String, String> freeTracks = new HashMap<String, String>();
				Map<String, Long> freeTrackStarts = new HashMap<String, Long>();
				String deviceInfo = mapper.writeValueAsString(geolocationsEvent.getDevice());

				groupByItinerary(geolocationsEvent, userId, geolocationsByItinerary, freeTracks, freeTrackStarts);

				List<TrackedInstance> instances = Lists.newArrayList();
				for (String key : geolocationsByItinerary.keySet()) {
					TrackedInstance ti = preSaveTrackedInstance(key, userId, appId, deviceInfo, geolocationsByItinerary, freeTracks, freeTrackStarts);
					if (ti != null) {
						instances.add(ti);
					}
				}

				for (TrackedInstance ti : instances) {
					// limit number of points to avoid failure of saving data
					if (ti.getGeolocationEvents() != null) {
						int mul = 1; 
						while (ti.getGeolocationEvents().size() > (mul * MAX_LOCATIONS)) mul++;
						if (mul > 1) {
							logger.info("TOO MANY GEOLOCATION EVENTS, user: " + userId + ", travel: " + ti.getId() + ", " + ti.getGeolocationEvents().size() + " events.");
							List<Geolocation> src = new LinkedList<>(ti.getGeolocationEvents());
							List<Geolocation> res = new LinkedList<>();
							for (int i = 0; i < src.size(); i += mul) {
								res.add(src.get(i));
							}
							ti.setGeolocationEvents(res);
						}
					}
					sendTrackedInstance(userId, appId, ti);

					storage.saveTrackedInstance(ti);
					logger.info("Saved geolocation events, user: " + userId + ", travel: " + ti.getId() + ", " + ti.getGeolocationEvents().size() + " events.");
				}
			} else {
				logger.error("Device of user " + userId + " is virtual: " + geolocationsEvent.getDevice());
			}
		} finally {
			lock.unlock();
		}
	}

	private void checkEventsOrder(GeolocationsEvent geolocationsEvent, String userId) {
		if (geolocationsEvent.getLocation() != null && !geolocationsEvent.getLocation().isEmpty()) {
			Location lastOk = geolocationsEvent.getLocation().get(geolocationsEvent.getLocation().size() - 1);
			adjustTimestamp(lastOk);
			
			ArrayList<Location> toKeep = Lists.newArrayList();
			toKeep.add(lastOk);
			for (int i = geolocationsEvent.getLocation().size() - 2; i >= 0; i--) {
				Location l1 = geolocationsEvent.getLocation().get(i);
				adjustTimestamp(l1);

				Date dOk = lastOk.getTimestamp();
				Date d1 = l1.getTimestamp();
				if (d1 == null) {
					logger.warn("Missing timestamp in location object: " + l1.toString());
					continue;
				}

				int comp = d1.compareTo(dOk);
				if (comp < 0) {
					lastOk = l1;
					toKeep.add(l1);
				} else {
					String tidOk = null;
					String tid1 = null;

					if (lastOk.getExtras() != null && lastOk.getExtras().containsKey("idTrip")) {
						tidOk = (String) lastOk.getExtras().get("idTrip");
					}
					if (l1.getExtras() != null && l1.getExtras().containsKey("idTrip")) {
						tid1 = (String) l1.getExtras().get("idTrip");
					}
					logger.debug("'Unordered' events for user: " + userId + ", tripId: " + tid1 + " / " + tidOk + ", times: " + d1 + " / " + dOk + ", coordinates: " + l1.getCoords() + " / "
							+ lastOk.getCoords());
				}
			}

			geolocationsEvent.setLocation(toKeep);

			Collections.sort(geolocationsEvent.getLocation());
		} else {
			logger.info("No geolocations found.");
		}
	}

	/**
	 * Happens in strange situations: the timestamp of GPS is far in the past. Need to adjust
	 * @param lastOk
	 */
	private void adjustTimestamp(Location l) {
		Date lastDate = l.getTimestamp();
		Long startTs = null; 
		if (l.getExtras() != null && l.getExtras().containsKey("start")) {
			startTs = Long.parseLong(l.getExtras().get("start").toString());
		} else {
			// should not happen
			startTs = System.currentTimeMillis();
		}
		if (lastDate.getTime() < startTs) {
			Calendar tsc = Calendar.getInstance();
			tsc.setTime(l.getTimestamp());
			Calendar sc = Calendar.getInstance();
			sc.setTimeInMillis(startTs);
			tsc.set(Calendar.YEAR, sc.get(Calendar.YEAR));
			tsc.set(Calendar.MONTH, sc.get(Calendar.MONTH));
			tsc.set(Calendar.DATE, sc.get(Calendar.DATE));
			logger.info("Adjusting time point: " + l.getTimestamp().getTime());
			l.setTimestamp(tsc.getTime());
		}
	}

	private void groupByItinerary(GeolocationsEvent geolocationsEvent, String userId, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks, Map<String, Long> freeTrackStarts) throws Exception {
		Long now = System.currentTimeMillis();
		Map<String, Object> device = geolocationsEvent.getDevice();

		Multimap<String, Long> freeTrackStartsByKey = ArrayListMultimap.create();
		
		if (geolocationsEvent.getLocation() != null) {
			int skippedOld = 0;
			int skippedNoId = 0;
			for (Location location : geolocationsEvent.getLocation()) {
				String locationTravelId = null;
				Long locationTs = null;
				if (location.getExtras() != null && location.getExtras().containsKey("idTrip")) {
					locationTravelId = (String) location.getExtras().get("idTrip");
					locationTs = location.getExtras().get("start") != null ? Long.parseLong("" + location.getExtras().get("start")) : null;
				} else {
					// now the plugin supports correctly the extras for each
					// location.
					// locations with empty idTrip are possible only upon
					// initialization/synchronization.
					// we skip them here
					skippedNoId++;
//					logger.warn("location without idTrip, user: " + userId + ", extras = " + location.getExtras());
					continue;
				}

				if (location.getTimestamp() == null) {
					logger.warn("Missing timestamp in location object: " + location.toString());
					continue;
				}

				if (locationTs == null) {
					locationTs = location.getTimestamp().getTime();
				}

				// discard event older than 2 days
				if (now - LOCATION_STORE_INTERVAL > location.getTimestamp().getTime()) {
					logger.info("Skipped point at time " + location.getTimestamp().getTime());
					skippedOld++;
//					continue;
				}

				Geolocation geolocation = buildGeolocation(location, userId, locationTravelId, device, now);
				
				
				String key = geolocation.getTravelId() + (geolocation.getMultimodalId() != null ? ("#" + geolocation.getMultimodalId()):""); // + "@" + day;
				geolocationsByItinerary.put(key, geolocation);
				if (StringUtils.hasText((String) location.getExtras().get("transportType"))) {
					freeTracks.put(key, (String) location.getExtras().get("transportType"));
				}

				freeTrackStartsByKey.put(key, locationTs);

				// storage.saveGeolocation(geolocation);
			}
			
			for (String key: freeTrackStartsByKey.keySet()) {
				Long min = freeTrackStartsByKey.get(key).stream().min(Long::compare).orElse(0L);
				freeTrackStarts.put(key, min);
			}
			
			if (skippedOld > 0) {
//				logger.warn("Timestamps too old, skipped " + skippedOld + " locations.");
				logger.warn("Found " + skippedOld + " locations to old.");
			}
			if (skippedNoId > 0) {
				logger.warn("Locations without idTrip, skipped " + skippedNoId + " locations.");
			}
		}

		logger.info("Group keys: " + geolocationsByItinerary.keySet());
		if (geolocationsByItinerary.keySet() == null || geolocationsByItinerary.keySet().isEmpty()) {
			logger.error("No geolocationsByItinerary set.");
		}
	}
	
	private Geolocation buildGeolocation(Location location, String userId, String locationTravelId, Map<String, Object> device, Long now) {
		Coords coords = location.getCoords();
		Activity activity = location.getActivity();
		Battery battery = location.getBattery();

		Geolocation geolocation = new Geolocation();

		geolocation.setUserId(userId);

		geolocation.setTravelId(locationTravelId);

		geolocation.setUuid(location.getUuid());
		if (device != null) {
			geolocation.setDevice_id((String) device.get("uuid"));
			geolocation.setDevice_model((String) device.get("model"));
		} else {
			geolocation.setDevice_model("UNKNOWN");
		}
		if (coords != null) {
			geolocation.setLatitude(coords.getLatitude());
			geolocation.setLongitude(coords.getLongitude());
			double c[] = new double[2];
			c[0] = geolocation.getLongitude();
			c[1] = geolocation.getLatitude();
			geolocation.setGeocoding(c);
			geolocation.setAccuracy(coords.getAccuracy());
			geolocation.setAltitude(coords.getAltitude());
			geolocation.setSpeed(coords.getSpeed());
			geolocation.setHeading(coords.getHeading());
		}
		if (activity != null) {
			geolocation.setActivity_type(activity.getType());
			geolocation.setActivity_confidence(activity.getConfidence());
		}
		if (battery != null) {
			geolocation.setBattery_level(battery.getLevel());
			geolocation.setBattery_is_charging(battery.getIs_charging());
		}

		geolocation.setIs_moving(location.getIs_moving());

		geolocation.setRecorded_at(new Date(location.getTimestamp().getTime()));

		geolocation.setCreated_at(new Date(now++));

		geolocation.setGeofence(location.getGeofence());

		if (StringUtils.hasText((String) location.getExtras().get("btDeviceId"))) {
			geolocation.setCertificate((String) location.getExtras().get("btDeviceId"));
		}
		if (StringUtils.hasText((String) location.getExtras().get("multimodalId"))) {
			geolocation.setMultimodalId((String) location.getExtras().get("multimodalId"));
		}		
		if (StringUtils.hasText((String) location.getExtras().get("sharedTravelId"))) {
			geolocation.setSharedTravelId((String) location.getExtras().get("sharedTravelId"));
		}		

		return geolocation;
	}

	private TrackedInstance preSaveTrackedInstance(String key, String userId, String appId, String deviceInfo, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks,
			Map<String, Long> freeTrackStarts) throws Exception {
		String splitKey[] = key.split("@");
		String travelId = splitKey[0];
		String multimodalId = null;

		String splitId[] = travelId.split("#");
		if (splitId.length == 2) {
			travelId = splitId[0];
			multimodalId = splitId[1];
		}

		TrackedInstance res = null;

		res = getStoredTrackedInstance(key, travelId, multimodalId, userId, geolocationsByItinerary, freeTracks, freeTrackStarts);

		if (res.getComplete() != null && res.getComplete()) {
			logger.info("Skipping complete trip " + res.getId());
			return null;
		} else if (gamificationManager.isTripExisting(travelId)){
			logger.info("Skipping already sent trip " + res.getId());
			return null;
		} else {			
			if (geolocationsByItinerary.get(key) != null) {
				logger.info("Adding " + geolocationsByItinerary.get(key).size() + " geolocations to existing " + res.getGeolocationEvents().size() + ".");
				res.getGeolocationEvents().addAll(geolocationsByItinerary.get(key));
				String sharedId = res.getGeolocationEvents().stream().filter(e -> e.getSharedTravelId() != null).findFirst().map(e -> e.getSharedTravelId()).orElse(null);
				res.setSharedTravelId(sharedId);
				logger.info("Resulting events: " + res.getGeolocationEvents().size());
			}

			res.setAppId(appId);
			res.setDeviceInfo(deviceInfo);
		}

		return res;

	}
	
	private void sendTrackedInstance(String userId, String appId, TrackedInstance res) throws Exception {
		if (res.getItinerary() != null) {
			sendPlanned(res, userId, res.getClientId(), res.getDay(), appId);
		} else if (res.getFreeTrackingTransport() != null && StringUtils.isEmpty(res.getSharedTravelId())) {
			sendFreeTracking(res, userId, res.getClientId(), appId);
		} else if (!StringUtils.isEmpty(res.getSharedTravelId())) {
			sendSharedTravel(res, userId, res.getClientId(), appId);
		}
//		storage.saveTrackedInstance(res);
	}
	

	private TrackedInstance getStoredTrackedInstance(String key, String travelId, String multimodalId, String userId, Multimap<String, Geolocation> geolocationsByItinerary, Map<String, String> freeTracks,
			Map<String, Long> freeTrackStarts) throws Exception {
		
		String day = shortSdf.format(freeTrackStarts.get(key));
		String time = timeSdf.format(freeTrackStarts.get(key));
		
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("clientId", travelId);
		pars.put("day", day);
		pars.put("userId", userId);
		
		logger.info("Pars: " + pars);

		TrackedInstance res = storage.searchDomainObject(pars, TrackedInstance.class);
		if (res == null) {
			logger.error("No existing TrackedInstance found.");
			res = new TrackedInstance();
			res.setClientId(travelId);
			res.setDay(day);
			res.setTime(time);
			res.setUserId(userId);
			res.setId(ObjectId.get().toString());
			pars.remove("day");
			ItineraryObject saved = storage.getSavedTrip(userId, travelId);
			if (saved != null) {
				res.setItinerary(saved);
			} else {
				logger.warn("No existing SavedTrip found.");
			}
			if (res.getItinerary() == null) {
				if (travelId.contains("_temporary_")) {
					logger.error("Orphan temporary, skipping clientId: " + travelId);
					return null;
				}
				String ftt = freeTracks.get(key);
				if (ftt == null) {
					logger.warn("No freetracking transport found, extracting from clientId: " + travelId);
					String[] cid = travelId.split("_");
					if (cid != null && cid.length > 1) {
						ftt = cid[0];
					} else {
						logger.error("Cannot find transport type for " + key);
					}
				}
				res.setFreeTrackingTransport(ftt);
				if (freeTrackStarts.containsKey(key)) {
					res.setTime(timeSdf.format(new Date(freeTrackStarts.get(key))));
				}
			}
		}

		res.setMultimodalId(multimodalId);
		return res;
	}

	private void sendPlanned(TrackedInstance res, String userId, String travelId, String day, String appId) throws Exception {

		if (!res.getComplete()) {
			ValidationResult vr = gamificationValidator.validatePlannedJourney(res.getItinerary(), res.getGeolocationEvents(), appId);
			res.setValidationResult(vr);

			if (vr != null && !TravelValidity.INVALID.equals(vr.getTravelValidity())) {
				Map<String, Object> trackingData = gamificationValidator.computePlannedJourneyScore(appId, userId, res.getItinerary().getData(), res.getGeolocationEvents(), vr.getValidationStatus(),
						res.getOverriddenDistances(), false);
				res.setScoreStatus(ScoreStatus.COMPUTED);
				if (trackingData.containsKey("estimatedScore")) {
					res.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, res.getId());
				trackingData.put(START_TIME, getStartTime(res));
				if (gamificationManager.sendItineraryDataToGamificationEngine(appId, userId, travelId + "_" + day, res.getItinerary(), trackingData)) {
					res.setScoreStatus(ScoreStatus.SENT);
				}
			}
			res.setComplete(true);
		}
	}

	private void sendFreeTracking(TrackedInstance res, String userId, String travelId, String appId) throws Exception {
		if (!res.getComplete()) {
			ValidationResult vr = gamificationValidator.validateFreeTracking(res.getGeolocationEvents(), res.getFreeTrackingTransport(), appId);
//			if (vr != null && !TravelValidity.INVALID.equals(vr.getTravelValidity())) {
//				// TODO reenabled
//				boolean isGroup = gamificationValidator.isTripsGroup(res.getGeolocationEvents(), userId, appId, res.getFreeTrackingTransport());
//				if (isGroup) {
//					if ("bus".equals(res.getFreeTrackingTransport()) || "train".equals(res.getFreeTrackingTransport()) || "boat".equals(res.getFreeTrackingTransport())) {
//						vr.getValidationStatus().setValidationOutcome(TravelValidity.PENDING);
//						logger.info("In a group");
//					}
//				}
//			}

			res.setValidationResult(vr);
			if (vr != null && !TravelValidity.INVALID.equals(vr.getTravelValidity())) {
				// canSave =
				Map<String, Object> trackingData = gamificationValidator.computeFreeTrackingScore(appId, userId, res.getGeolocationEvents(), res.getFreeTrackingTransport(), vr.getValidationStatus(),
						res.getOverriddenDistances());
				res.setScoreStatus(ScoreStatus.COMPUTED);
				if (trackingData.containsKey("estimatedScore")) {
					res.setScore((Long) trackingData.get("estimatedScore"));
				}
				trackingData.put(TRAVEL_ID, res.getId());
				trackingData.put(START_TIME, getStartTime(res));
				if (gamificationManager.sendFreeTrackingDataToGamificationEngine(appId, userId, travelId, res.getGeolocationEvents(), res.getFreeTrackingTransport(), trackingData)) {
					res.setScoreStatus(ScoreStatus.SENT);
				}
			} else {
				logger.debug("Validation result null, not sending data to gamification");
			}
		}
		res.setComplete(true);
	}
	
	private void sendSharedTravel(TrackedInstance res, String userId, String travelId, String appId) throws Exception {
		logger.info("processing shared travel: " + travelId + ", sharedId = " + res.getSharedTravelId());
		if (!res.getComplete()) {
			String sharedId = res.getSharedTravelId();
			if (gamificationValidator.isDriver(sharedId)) {
				String passengerTravelId = gamificationValidator.getPassengerTravelId(sharedId);
				Query query = Query.query(Criteria
						.where("appId").is(appId)
						.and("sharedTravelId").is(passengerTravelId)
						.and("complete").is(true)
						.and("userId").ne(userId));
				List<TrackedInstance> list = storage.searchDomainObjects(query, TrackedInstance.class);
				if (!list.isEmpty()) {
					for (TrackedInstance passengerTravel: list) {
						validateSharedTripPair(passengerTravel, passengerTravel.getUserId(), travelId, appId, res);
						storage.saveTrackedInstance(passengerTravel);
					}
				}
			} else {
				String driverTravelId = gamificationValidator.getDriverTravelId(sharedId);
				Query query = Query.query(Criteria
						.where("appId").is(appId)
						.and("sharedTravelId").is(driverTravelId)
						.and("complete").is(true)
						.and("userId").ne(userId));
				TrackedInstance driverTravel = storage.searchDomainObject(query, TrackedInstance.class);
				if (driverTravel != null) {
					validateSharedTripPair(res, userId, travelId, appId, driverTravel);
					storage.saveTrackedInstance(driverTravel);
				}
			}
		}
		res.setComplete(true);
	}

	private void validateSharedTripPair(TrackedInstance passengerTravel, String passengerId, String passengerTravelId, String appId, TrackedInstance driverTravel) throws ParseException {
		ValidationResult vr = gamificationValidator.validateSharedTripPassenger(passengerTravel.getGeolocationEvents(), driverTravel.getGeolocationEvents(), appId);
		passengerTravel.setValidationResult(vr);
		
		if (driverTravel.getValidationResult() == null || driverTravel.getValidationResult().getValidationStatus() == null || TravelValidity.PENDING.equals(driverTravel.getValidationResult().getValidationStatus().getValidationOutcome())) {
			ValidationResult driverVr = gamificationValidator.validateSharedTripDriver(driverTravel.getGeolocationEvents(), appId);
			driverTravel.setValidationResult(driverVr);
		}
		
		// passenger trip is valid: points are assigned to both
		if (vr != null && !TravelValidity.INVALID.equals(vr.getTravelValidity())) {
			boolean firstTime = !ScoreStatus.SENT.equals(driverTravel.getScoreStatus()) && !ScoreStatus.ASSIGNED.equals(driverTravel.getScoreStatus());
			Map<String, Object> trackingData = gamificationValidator.computeSharedTravelScoreForDriver(appId, driverTravel.getUserId(), driverTravel.getGeolocationEvents(), vr.getValidationStatus(), driverTravel.getOverriddenDistances(), firstTime);
			if (trackingData.containsKey("estimatedScore")) {
				long score = driverTravel.getScore() != null ? driverTravel.getScore() : 0l;
				driverTravel.setScore(score + (Long) trackingData.get("estimatedScore"));
			}
			trackingData.put(TRAVEL_ID, driverTravel.getId());
			trackingData.put(START_TIME, getStartTime(driverTravel));
			if (gamificationManager.sendSharedTravelDataToGamificationEngine(appId, driverTravel.getUserId(), passengerTravel.getUserId(), driverTravel.getId(), driverTravel.getGeolocationEvents(), trackingData)) {
				if (firstTime) driverTravel.setScoreStatus(ScoreStatus.SENT);
			}
			
			trackingData = gamificationValidator.computeSharedTravelScoreForPassenger(appId, passengerId, passengerTravel.getGeolocationEvents(), vr.getValidationStatus(), passengerTravel.getOverriddenDistances());
			passengerTravel.setScoreStatus(ScoreStatus.COMPUTED);
			if (trackingData.containsKey("estimatedScore")) {
				passengerTravel.setScore((Long) trackingData.get("estimatedScore"));
			}
			trackingData.put(TRAVEL_ID, passengerTravel.getId());
			trackingData.put(START_TIME, getStartTime(passengerTravel));
			if (gamificationManager.sendSharedTravelDataToGamificationEngine(appId, passengerId, driverTravel.getUserId(), passengerTravelId, passengerTravel.getGeolocationEvents(), trackingData)) {
				passengerTravel.setScoreStatus(ScoreStatus.SENT);
			}
		} else {
			logger.debug("Validation result null, not sending data to gamification");
		}
	}

	private long getStartTime(TrackedInstance trackedInstance) throws ParseException {
		long time = 0;
		if (trackedInstance.getGeolocationEvents() != null && !trackedInstance.getGeolocationEvents().isEmpty()) {
			Geolocation event = trackedInstance.getGeolocationEvents().stream().sorted().findFirst().get();
			time = event.getRecorded_at().getTime();
		} else if (trackedInstance.getDay() != null && trackedInstance.getTime() != null) {
			String dt = trackedInstance.getDay() + " " + trackedInstance.getTime();
			time = fullSdf.parse(dt).getTime();
		} else if (trackedInstance.getDay() != null) {
			time = shortSdf.parse(trackedInstance.getDay()).getTime();
		}
		return time;
	}

}
