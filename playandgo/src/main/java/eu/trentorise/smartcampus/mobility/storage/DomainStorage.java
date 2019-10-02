package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;

import eu.trentorise.smartcampus.mobility.gamification.model.PlanObject;
import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.model.RouteMonitoring;
import eu.trentorise.smartcampus.network.JsonUtils;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

@Component
public class DomainStorage {

	private static final String GEOLOCATIONS = "geolocations";
	private static final String TRACKED = "trackedInstances";
	private static final String SAVED = "savedtrips";
	private static final String MONITORING = "routesMonitoring";
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;
	private static final Logger logger = LoggerFactory.getLogger(DomainStorage.class);

	public DomainStorage() {
	}

	private String getClassCollection(Class<?> cls) {
		if (cls == Geolocation.class) {
			return GEOLOCATIONS;
		}
		if (cls == TrackedInstance.class) {
			return TRACKED;
		}	
		if (cls == SavedTrip.class) {
			return SAVED;
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
			
			template.updateFirst(query, update, TRACKED);
		}
	}
	
	public void saveSavedTrips(SavedTrip savedTrip) {
		template.save(savedTrip, SAVED);
	}
	
	public void saveRouteMonitoring(RouteMonitoringObject rmo) {
		Query query = new Query(new Criteria("clientId").is(rmo.getClientId()));
		RouteMonitoringObject monitoringDB = searchDomainObject(query, RouteMonitoringObject.class);
		if (monitoringDB == null) {
			template.save(rmo, MONITORING);
		} else {
			Update update = new Update();
			update.set("agencyId", rmo.getAgencyId());
			update.set("routeId", rmo.getRouteId());
			update.set("recurrency", rmo.getRecurrency());
			template.updateFirst(query, update, MONITORING);
		}
	}	
	
	public void deleteRouteMonitoring(String clientdId) {
		BasicDBObject query = new BasicDBObject();
		query.put("clientId", clientdId);
		template.getCollection(MONITORING).deleteOne(query);
	}	
	
	public Geolocation getLastGeolocationByUserId(String userId) {
		Criteria criteria = new Criteria("userId").is(userId);
		Query query = new Query(criteria).with(new Sort(Sort.Direction.DESC, "created_at"));
		return searchDomainObject(query, Geolocation.class);
	}
	
	public <T> List<T> searchDomainObjects(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.find(query, clz, getClassCollection(clz));
	}
	
	public <T> List<T> searchDomainObjects(Query query, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.find(query, clz, getClassCollection(clz));
	}	
	
	public <T> T searchDomainObject(Query query, Class<T> clz) {
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		return template.findOne(query, clz, getClassCollection(clz));
	}	
	
	public <T> List<T> searchDomainObjects(Map<String, Object> pars, Class<T> clz) {
		Criteria criteria = new Criteria();
		for (String key: pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}
		
		Query query = new Query(criteria);
		
		return template.find(query, clz, getClassCollection(clz));
	}
	public <T> List<T> searchDomainObjects(Map<String, Object> pars, Set<String> keys, Class<T> clz) {
		Criteria criteria = new Criteria();
		for (String key: pars.keySet()) {
			criteria.and(key).is(pars.get(key));
		}
		
		Query query = new Query(criteria);
		if (keys != null){
			for (String key : keys) {
				query.fields().include(key);
			}
		}
		
		return template.find(query, clz, getClassCollection(clz));
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
	
	public <T> void deleteDomainObject(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		template.remove(query, getClassCollection(clz));
	}	
		
	public <T> long count(Criteria criteria, Class<T> clz) {
		Query query = new Query(criteria);
		logger .debug("query: {}",JsonUtils.toJSON(query.getQueryObject()));
		long result = template.count(query, getClassCollection(clz));
		return result;
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
	
}
