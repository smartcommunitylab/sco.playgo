package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.mobility.gamification.model.PlanObject;
import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.model.RouteMonitoring;
import eu.trentorise.smartcampus.network.JsonUtils;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

@Component
public class DomainStorage {

	private static final String ITINERARY = "itinerary";
	private static final String RECURRENT = "recurrent";
	private static final String GEOLOCATIONS = "geolocations";
	private static final String TRACKED = "trackedInstances";
	private static final String SAVED = "savedtrips";
	private static final String NEWS = "news";
	private static final String MONITORING = "routesMonitoring";
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;
	private static final Logger logger = LoggerFactory.getLogger(DomainStorage.class);

	public DomainStorage() {
	}

	private String getClassCollection(Class<?> cls) {
		if (cls == ItineraryObject.class) {
			return ITINERARY;
		}
		if (cls == RecurrentJourneyObject.class) {
			return RECURRENT;
		}
		if (cls == Geolocation.class) {
			return GEOLOCATIONS;
		}
		if (cls == TrackedInstance.class) {
			return TRACKED;
		}	
		if (cls == SavedTrip.class) {
			return SAVED;
		}	
		if (cls == Announcement.class) {
			return NEWS;
		}
		if (cls == RouteMonitoringObject.class || cls == RouteMonitoring.class) {
			return MONITORING;
		}			
		throw new IllegalArgumentException("Unknown class: " + cls.getName());
	}
	
	public void deleteTrackedInstance(TrackedInstance tracked) {
		Query query = new Query(
				new Criteria("clientId").is(tracked.getClientId())
				.and("day").is(tracked.getDay())
				.and("userId").is(tracked.getUserId()));
		template.remove(query, TrackedInstance.class, TRACKED);
	}
	
	public void saveTrackedInstance(TrackedInstance tracked) {
		Query query = new Query(
				new Criteria("clientId").is(tracked.getClientId())
				.and("day").is(tracked.getDay())
				.and("userId").is(tracked.getUserId()));
		TrackedInstance trackedDB = searchDomainObject(query, TrackedInstance.class);
		if (trackedDB == null) {
			template.save(tracked, TRACKED);
		} else {
			Update update = new Update();
			if (tracked.getItinerary() != null) {
				update.set("itinerary", tracked.getItinerary());
			}
			if (tracked.getGeolocationEvents() != null && !tracked.getGeolocationEvents().isEmpty()) {
				update.set("geolocationEvents", tracked.getGeolocationEvents());
			}

			if (tracked.getStarted() != null) {
				update.set("started", tracked.getStarted());
			}
			if (tracked.getComplete() != null) {
				update.set("complete", tracked.getComplete());
			}
			if (tracked.getValidationResult() != null) {
				update.set("validationResult", tracked.getValidationResult());
			}	
			if (tracked.getScore() != null) {
				update.set("score", tracked.getScore());
			}
			if (tracked.getDeviceInfo() != null && !tracked.getDeviceInfo().isEmpty()) {
				update.set("deviceInfo", tracked.getDeviceInfo());
			}
			update.set("changedValidity", tracked.getChangedValidity());
			update.set("scoreStatus", tracked.getScoreStatus());
			if (tracked.getApproved() != null) {
				update.set("approved", tracked.getApproved());
			}
			if (tracked.getOverriddenDistances() != null) {
				update.set("overriddenDistances", tracked.getOverriddenDistances());
				
			}
			update.set("toCheck", tracked.getToCheck());
			update.set("appId", tracked.getAppId());
			update.set("multimodalId", tracked.getMultimodalId());
			update.set("sharedTravelId", tracked.getSharedTravelId());
			
			template.updateFirst(query, update, TRACKED);
		}
	}
	
	public void saveSavedTrip(SavedTrip savedTrip) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("itinerary.clientId", savedTrip.getItinerary().getClientId());
		pars.put("itinerary.userId", savedTrip.getItinerary().getUserId());
		SavedTrip old = searchDomainObject(pars, SavedTrip.class);
		if (old != null) {
			savedTrip.setId(old.getId());
			savedTrip.setCreatedAt(old.getCreatedAt());
		}

		template.save(savedTrip, SAVED);
	}
	public ItineraryObject getSavedTrip(String userId, String clientId) {
		Map<String, Object> pars = new TreeMap<String, Object>();
		pars.put("itinerary.clientId", clientId);
		pars.put("itinerary.userId", userId);
		SavedTrip old = searchDomainObject(pars, SavedTrip.class);
		if (old != null) return old.getItinerary();
		return null;
	}
	
	public <T> List<T> searchDomainObjects(Query query, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.find(query, clz, getClassCollection(clz));
	}	
	
	public <T> T searchDomainObject(Query query, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.findOne(query, clz, getClassCollection(clz));
	}	
	
	
	public <T> List<T> searchDomainObjects(Query query, Set<String> keys, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		
		if (keys != null){
			for (String key : keys) {
				query.fields().include(key);
			}
		}

		return template.find(query, clz, getClassCollection(clz));
	}		
	
	public <T> T searchDomainObject(Map<String, Object> pars, Class<T> clz) {
		Criteria criteria = new Criteria();
		for (String key : pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}

		Query query = new Query(criteria);
		return template.findOne(query, clz, getClassCollection(clz));
	}	
	
//	public <T> T searchDomainObjectFixForSpring(Map<String, Object> pars, Class<T> clz) {
//		Criteria criteria = new Criteria();
//		for (String key : pars.keySet()) {
//			criteria.and(key).is(pars.get(key));
//		}
//
//		Query query = new Query(criteria);
//
//			BasicDBObject obj = (BasicDBObject) template.getCollection(getClassCollection(clz)).findOne(query.getQueryObject());
//			ObjectMapper mapper = new ObjectMapper();
//			mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//			return mapper.convertValue(obj, clz);
//	}	
//

	/**
	 * @param journeyRequest
	 * @param userId
	 * @param appName
	 */
	public void savePlanRequest(SingleJourney journeyRequest, String userId, String appName) {
		template.save(new PlanObject(journeyRequest, userId, appName));
	}
	
	public void saveNews(Announcement announcment) {
		template.save(announcment, "news");
	}


	
}
