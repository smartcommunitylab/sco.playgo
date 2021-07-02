/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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

package eu.trentorise.smartcampus.mobility.controller.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.MODE_TYPE;
import eu.trentorise.smartcampus.mobility.security.AppDetails;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.Circle;
import eu.trentorise.smartcampus.mobility.security.Shape;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

/**
 * @author raman
 *
 */
@RestController
public class TrackingDataController {
	
	private static final Logger logger = LoggerFactory.getLogger(TrackingDataController.class);

	@Autowired
	private AppSetup appSetup;

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;
	private static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/ddHH:mm");

	@PostMapping("/gamification/trackingdata")
	public List<TrackingDataDTO> getTrackingData(@RequestBody TrackingDataRequestDTO dto) {
		try {
			String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
			AppInfo app = appSetup.findAppById(appId);
			if (app == null) return Collections.emptyList();
			
			Criteria criteria = new Criteria("appId").is(appId).and("scoreStatus").in("ASSIGNED", "COMPUTED", "SENT");
			List<Criteria> criterias = Lists.newArrayList();
			String fFrom = LocalDate.parse(dto.getFrom()).format(DATE_FORMAT);
			String fTo = LocalDate.parse(dto.getTo()).plusDays(1).format(DATE_FORMAT);
			
			criterias.add(new Criteria("day").gte(fFrom).lt(fTo));
			criterias.add(new Criteria("validationResult.validationStatus.effectiveDistances").exists(true));
			if (Boolean.FALSE.equals(dto.getMultimodal())) {
				criterias.add(new Criteria("freeTrackingTransport").in(dto.getMeans()));
			}
			if (dto.getPlayerId() != null && dto.getPlayerId().size() > 0) {
				criterias.add(new Criteria("userId").in(dto.getPlayerId()));
			}
			if (!criterias.isEmpty()) {
				Criteria[] cs = criterias.toArray(new Criteria[criterias.size()]);
				criteria = criteria.andOperator(cs);
			}
			Query query = Query.query(criteria);
			query.fields()
				.include("userId")
				.include("day")
				.include("time")
				.include("freeTrackingTransport")
				.include("geolocationEvents")
				.include("validationResult.validationStatus.distance")
				.include("validationResult.validationStatus.effectiveDistances");
			
			List<TrackedInstance> list = template.find(query, TrackedInstance.class);

			List<TrackingDataDTO> result = new LinkedList<>();
			List<Shape> areas = dto.getLocations().stream().map(l -> toShape(l)).collect(Collectors.toList());
			
			// whether should consider also the multimodals
			if (Boolean.FALSE.equals(dto.getMultimodal())) {
				for (TrackedInstance ti : list) {
					if (matchLocations(ti, areas)) {
						result.add(toTrackingDataDTO(ti));
					}
				}
			} else {
				ListMultimap<String, TrackedInstance> index = Multimaps.index(list, ti -> ti.getMultimodalId() == null ? ti.getId() : ti.getMultimodalId());
				for (String key : index.keySet()) {
					List<TrackedInstance> group = index.get(key);
					if (group.stream().anyMatch(ti -> matchLocations(ti, areas))) {
						group.stream().filter(ti -> dto.getMeans().contains(ti.getFreeTrackingTransport())).forEach(ti -> result.add(toTrackingDataDTO(ti)));
					}
				}
			}
			
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	} 
	
	/**
	 * @param l
	 * @return
	 */
	private Shape toShape(LocationDTO l) {
		Circle c = new Circle();
		c.setCenter(new double[] {l.getLat(), l.getLng()});
		c.setRadius(l.getRad());
		return c;
	}

	/**
	 * @param ti
	 * @return
	 */
	private TrackingDataDTO toTrackingDataDTO(TrackedInstance ti) {
		TrackingDataDTO dto = new TrackingDataDTO();
		dto.setMode(ti.getFreeTrackingTransport());
		dto.setDistance(ti.getValidationResult().getValidationStatus().getEffectiveDistances().get(toMode(dto.getMode())));
		dto.setStartedAt(LocalDateTime.parse(ti.getDay()+ti.getTime(), DATE_TIME_FORMAT).toString());
		dto.setPlayerId(ti.getUserId());
		dto.setTrackId(ti.getId());
		return dto;
	}

	/**
	 * @param mode
	 * @return
	 */
	private MODE_TYPE toMode(String mode) {
		return MODE_TYPE.valueOf(mode.toUpperCase());
	}

	/**
	 * @param ti
	 * @param locations
	 * @return
	 */
	private boolean matchLocations(TrackedInstance ti, List<Shape> areas) {
		Object[] arr = ti.getGeolocationEvents().toArray();
		Arrays.sort(arr, (a,b) -> {
			return ((Geolocation)a).getRecorded_at().compareTo(((Geolocation)b).getRecorded_at()); 
		});
		
		if (GamificationHelper.inAreas(areas, (Geolocation)arr[0])) return true;
		if (GamificationHelper.inAreas(areas, (Geolocation)arr[arr.length-1])) return true;
		if (arr.length > 3) {
			if (GamificationHelper.inAreas(areas, (Geolocation)arr[1])) return true;
			if (GamificationHelper.inAreas(areas, (Geolocation)arr[arr.length-2])) return true;
		}
		if (arr.length > 5) {
			if (GamificationHelper.inAreas(areas, (Geolocation)arr[2])) return true;
			if (GamificationHelper.inAreas(areas, (Geolocation)arr[arr.length-3])) return true;
		}
//		if (arr.length > 7) {
//			if (GamificationHelper.inAreas(areas, (Geolocation)arr[3])) return true;
//			if (GamificationHelper.inAreas(areas, (Geolocation)arr[arr.length-4])) return true;
//		}

		return false;
	}

	public static class TrackingDataRequestDTO {
		private List<String> playerId;
		private String from, to;
		private List<String> means;
		private Boolean multimodal;
		
		private List<LocationDTO> locations;

		public List<String> getPlayerId() {
			return playerId;
		}

		public void setPlayerId(List<String> playerId) {
			this.playerId = playerId;
		}

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public List<String> getMeans() {
			return means;
		}

		public void setMeans(List<String> means) {
			this.means = means;
		}

		public List<LocationDTO> getLocations() {
			return locations;
		}

		public void setLocations(List<LocationDTO> locations) {
			this.locations = locations;
		}

		public Boolean getMultimodal() {
			return multimodal;
		}

		public void setMultimodal(Boolean multimodal) {
			this.multimodal = multimodal;
		}
	}
	
	public static class LocationDTO {
		private double lng, lat, rad;

		public double getLng() {
			return lng;
		}

		public void setLng(double lng) {
			this.lng = lng;
		}

		public double getLat() {
			return lat;
		}

		public void setLat(double lat) {
			this.lat = lat;
		}

		public double getRad() {
			return rad;
		}

		public void setRad(double rad) {
			this.rad = rad;
		}
	}
	
	public static class TrackingDataDTO {
		private String trackId;
		private String playerId;
		private String startedAt;
		private String mode;
		private double distance;
		
		public String getTrackId() {
			return trackId;
		}
		public void setTrackId(String trackId) {
			this.trackId = trackId;
		}
		public String getStartedAt() {
			return startedAt;
		}
		public void setStartedAt(String startedAt) {
			this.startedAt = startedAt;
		}
		public String getMode() {
			return mode;
		}
		public void setMode(String mode) {
			this.mode = mode;
		}
		public double getDistance() {
			return distance;
		}
		public void setDistance(double distance) {
			this.distance = distance;
		}
		public String getPlayerId() {
			return playerId;
		}
		public void setPlayerId(String playerId) {
			this.playerId = playerId;
		}
	}

}
