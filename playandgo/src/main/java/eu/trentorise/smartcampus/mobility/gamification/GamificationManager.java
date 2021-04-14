/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.gamification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.controller.rest.GamificationController;
import eu.trentorise.smartcampus.mobility.gamification.model.ExecutionDataDTO;
import eu.trentorise.smartcampus.mobility.gamification.model.MessageNotification;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.model.BasicItinerary;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.BannedChecker;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.util.HTTPConnector;
import eu.trentorise.smartcampus.network.JsonUtils;

@Component
public class GamificationManager {

	private static final Logger logger = LoggerFactory.getLogger(GamificationManager.class);

	private static final String SAVE_ITINERARY = "save_itinerary";
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	private ExecutorService executorService;	
	
	@Autowired
	private BannedChecker bannedChecker;
	
	@Autowired
	private GamificationCache gamificationCache;		

	@Autowired(required = false)
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	private Set<String> publishQueue = Sets.newConcurrentHashSet();
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	public synchronized boolean sendFreeTrackingDataToGamificationEngine(String appId, String playerId, String travelId, Collection<Geolocation> geolocationEvents, String ttype, Map<String, Object> trackingData) {
		logger.info("Send free tracking data for user " + playerId + ", trip " + travelId);
		if (publishQueue.contains(travelId)) {
			logger.info("publishQueue contains travelId " + travelId + ", returning");
			return false;
		}
		boolean result = saveFreeTracking(travelId, appId, playerId, geolocationEvents, ttype, trackingData);
		if (result) {
			publishQueue.add(travelId);
		}
		return result;
	}
	
	public synchronized boolean sendItineraryDataToGamificationEngine(String appId, String playerId, String publishKey, ItineraryObject itinerary, Map<String, Object> trackingData) throws Exception {
		logger.info("Send data for user " + playerId + ", trip " + itinerary.getClientId());
		if (publishQueue.contains(publishKey)) {
			logger.info("Already sent, returning " + playerId + ", trip " + itinerary.getClientId());
			return false;
		}
		boolean result = saveItinerary(itinerary, appId, playerId, trackingData);
		if (result) {
			publishQueue.add(publishKey);			
		}
		return result;
	}
	
	public boolean isTripExisting(String id) {
		return publishQueue.contains(id);
	}

	public void removeIdFromQueue(String id) {
		publishQueue.remove(id);
	}		
	
	private boolean saveFreetracking(String travelId, String appId, String playerId, Collection<Geolocation> geolocationEvents, String ttype, Map<String, Object> trackingData) {
		if ((Long)trackingData.get("estimatedScore") == 0) {
			logger.debug("EstimatedScore is 0, returning.");
			return false;
		}
		trackingData.remove("estimatedScore");
		
		if (trackingData.isEmpty()) {
			logger.debug("Data is empty, returning.");
			return false;
		}
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());
		
		if (bannedChecker.isBanned(playerId, app.getGameId())) {
			logger.info("Not sending for banned player " + playerId);
			return false;
		}

		if (!game.getActive()) {
			logger.warn("Not sending for disabled game " + game.getId());
			return false;
		}
		
		try {
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(app.getGameId());
			ed.setPlayerId(playerId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);
			
			Long time = (Long)trackingData.remove(GamificationController.START_TIME);
			ed.setExecutionMoment(new Date(time));			

			String content = JsonUtils.toJSON(ed);
			
			logger.debug("Sending to " + gamificationUrl + "/gengine/execute (" + SAVE_ITINERARY + ") = " + trackingData);
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());
			return true;
		} catch (Exception e) {
			logger.error("Error sending gamification action: " + e.getMessage());
			return false;
		}
	}

	/**
	 * @param travelId
	 * @param appId
	 * @param playerId
	 * @param geolocationEvents
	 */
	private boolean saveFreeTracking(final String travelId, final String appId, final String playerId, final Collection<Geolocation> geolocationEvents, final String ttype, Map<String, Object> trackingData) {
		if (gamificationUrl == null) {
			logger.debug("No gamification URL, returning.");
			return false;
		}
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());
		
		try {
			if (System.currentTimeMillis() < new SimpleDateFormat("dd/MM/yyyy").parse(game.getStart()).getTime()) {
				logger.debug("Game not yet started, returning.");
				return false;
			}
		} catch (ParseException e) {
			return false;
		}

		CallableSaveFreetracking save = new CallableSaveFreetracking(travelId, appId, playerId, geolocationEvents, ttype, trackingData);
		Future<Boolean> future = executorService.submit(save);
		try {
			return future.get();
		} catch (Exception e) {
			logger.debug("Error invoking GE, returning.");
			return false;
		}
	}
	
	private class CallableSaveFreetracking implements Callable<Boolean> {
		
		private String travelId;
		private String appId;
		private String playerId;
		private Collection<Geolocation> geolocationEvents;
		private String ttype;
		private Map<String, Object> trackingData;		
		
		public CallableSaveFreetracking(String travelId, String appId, String playerId, Collection<Geolocation> geolocationEvents, String ttype, Map<String, Object> trackingData) {
			super();
			this.travelId = travelId;
			this.appId = appId;
			this.playerId = playerId;
			this.geolocationEvents = geolocationEvents;
			this.ttype = ttype;
			this.trackingData = trackingData;
		}

		@Override
		public Boolean call() throws Exception {
			return saveFreetracking(travelId, appId, playerId, geolocationEvents, ttype, trackingData);
		}
	}	
	
	private boolean saveItinerary(final BasicItinerary itinerary, final String appId, final String userId, Map<String, Object> trackingData) throws ParseException {
		if (gamificationUrl == null) {
			return false;
		}
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());
		
		
		if (System.currentTimeMillis() < new SimpleDateFormat("dd/MM/yyyy").parse(game.getStart()).getTime()) {
			return false;
		}

//		executorService.execute(new Runnable() {
//			@Override
//			public void run() {
//				saveTrip(itinerary, appId, userId, trackingData);
//			}
//		});
		
		CallableSaveTrip save = new CallableSaveTrip(itinerary, appId, userId, trackingData);
		Future<Boolean> future = executorService.submit(save);
		try {
			return future.get();
		} catch (Exception e) {
			logger.debug("Error invoking GE, returning.");
			return false;
		}		
		
	}
	
	private class CallableSaveTrip implements Callable<Boolean> {
		
		private BasicItinerary itinerary;
		private String appId;
		private String userId;
		private Map<String, Object> trackingData;		
		
		public CallableSaveTrip(BasicItinerary itinerary, String appId, String userId, Map<String, Object> trackingData) {
			super();
			this.itinerary = itinerary;
			this.appId = appId;
			this.userId = userId;
			this.trackingData = trackingData;
		}

		@Override
		public Boolean call() throws Exception {
			return saveTrip(itinerary, appId, userId, trackingData);
		}
	}	

	private boolean saveTrip(BasicItinerary itinerary, String appId, String userId, Map<String, Object> trackingData) {
		try {
//			Map<String, Object> data = validator.computePlannedJourneyScore(itinerary.getData(), true);
			if ((Long)trackingData.get("estimatedScore") == 0) {
				logger.debug("EstimatedScore is 0, returning.");
				return false;
			}			
			trackingData.remove("estimatedScore");

			AppInfo app = appSetup.findAppById(appId);
			GameInfo game = gameSetup.findGameById(app.getGameId());
			
			if (bannedChecker.isBanned(userId, app.getGameId())) {
				logger.warn("Not sending for banned player " + userId);
				return false;
			}		
			if (!Boolean.TRUE.equals(game.getActive())) {
				logger.warn("Not sending for disabled game " + game.getId());
				return true;
			}
			
			ExecutionDataDTO ed = new ExecutionDataDTO();
			ed.setGameId(app.getGameId());
			ed.setPlayerId(userId);
			ed.setActionId(SAVE_ITINERARY);
			ed.setData(trackingData);
			
			
			Long time = (Long)trackingData.remove(GamificationController.START_TIME);
			ed.setExecutionMoment(new Date(time));

			String content = JsonUtils.toJSON(ed);
			
			logger.debug("Sending to " + gamificationUrl + "/gengine/execute (" + SAVE_ITINERARY + ") = " + trackingData);
			HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());
			return true;
		} catch (Exception e) {
			logger.error("Error sending gamification action: " + e.getMessage());
			return false;
		}
	}	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, Double> getScoreNotification(String appId, String userId) throws Exception {
		Map<String, Double> result = Maps.newTreeMap();
		
		try {
		AppInfo app = appSetup.findAppById(appId);
		
		if (app == null) {
			logger.error("App not found for user = " + userId + ", app = " + appId);
			return result;
		}
		
		String data = gamificationCache.getPlayerNotifications(userId, appId);
		
		Map<String, List> notsMap = mapper.readValue(data, Map.class);
		List<MessageNotification> nots = null;
		if (notsMap.containsKey("MessageNotification")) {
			nots = (List)mapper.convertValue(notsMap.get("MessageNotification"), new TypeReference<List<MessageNotification>>() {
			});
		} else {
			nots = Collections.emptyList();
		}

		for (MessageNotification msg: nots) {
			Map msgData = msg.getData();
			if (msgData.get("travelId") != null) {
				Double value = result.get((String)msgData.get("travelId"));
				if (value == null) value = 0d;
				if (msgData.get("score") != null) value += (Double)msgData.get("score");
				result.put((String)msgData.get("travelId"), value);
			} else {
				logger.warn("TravelId null in GE for user = " + userId + ", app = " + appId);
			}
		}		
		
//		logger.info("Got scores: " + result);
		} catch (Exception e) {
			logger.error("Error getting scores from GE", e);
		}
		
		return result;
		
	}

	/**
	 * @param event
	 * @param id
	 * @param gameId
	 * @throws Exception 
	 */
	public void sendCheckin(String event, String id, String appId) throws Exception {
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());

		if (bannedChecker.isBanned(id, app.getGameId())) {
			logger.info("Not sending for banned player " + id);
			return;
		}		
		
		ExecutionDataDTO ed = new ExecutionDataDTO();
		ed.setGameId(game.getId());
		ed.setPlayerId(id);
		ed.setActionId("checkin_"+event);
		ed.setData(Collections.singletonMap("checkinType",event));

		String content = JsonUtils.toJSON(ed);
		
		logger.debug("Sending to " + gamificationUrl + "/gengine/execute ('checkin') = " + content);
		HTTPConnector.doBasicAuthenticationPost(gamificationUrl + "/gengine/execute", content, "application/json", "application/json", game.getUser(), game.getPassword());
		
	}

	/**
	 * @param appId
	 * @param userId
	 * @param travelId
	 * @param geolocationEvents
	 * @param trackingData
	 * @return
	 */
	public boolean sendSharedTravelDataToGamificationEngine(String appId, String userId, String otherId,  String travelId, Collection<Geolocation> geolocationEvents, Map<String, Object> trackingData) {
		logger.info("Send shared tracking data for user " + userId + ", trip " + travelId);
		String key = travelId + "_" + userId + "_" + otherId;
		logger.info("Send shared tracking data " + key);
		if (publishQueue.contains(key)) {
			logger.info("publishQueue contains shared travel key " + key + ", returning");
			return false;
		}
		boolean result = saveFreeTracking(travelId, appId, userId, geolocationEvents, "carpooling", trackingData);
		if (result) {
			publishQueue.add(key);
		}
		return result;

	}	
	
}
