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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.TTDescriptor;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.MODE_TYPE;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

@Component
public class GamificationValidator {

//	private static final String ON_FOOT = "on_foot";
//	private static final String ON_BICYCLE = "on_bicycle";
//	private static final String IN_VEHICLE = "in_vehicle";
//	private static final String WALKING = "walking";
//	private static final String RUNNING = "running";
//	private static final String UNKNOWN = "unknown";
//	private static final String EMPTY = "unknown";
//	private static final double SPACE_ERROR = 0.1;
	
	public static final int SAME_TRIP_INTERVAL = 5 * 60 * 1000; // 5 minutes

	private static final Logger logger = LoggerFactory.getLogger(GamificationValidator.class);

//	private static final List<TType> FAST_TRANSPORTS = Lists.newArrayList(TType.BUS, TType.CAR, TType.GONDOLA, TType.SHUTTLE, TType.TRAIN, TType.TRANSIT);
//	private static final Set<String> WALKLIKE = Sets.newHashSet(ON_FOOT, WALKING, RUNNING, UNKNOWN, EMPTY);

	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;	
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;	

	public List<List<Geolocation>> TRAIN_SHAPES = new ArrayList<>();
	public List<String> TRAIN_POLYLINES = new ArrayList<>();
	public TTDescriptor BUS_DESCRIPTOR = null;

	@Autowired
	@Value("${validation.shapefolder}")
	private String shapeFolder;	
	
	@PostConstruct
	public void initValidationData() throws Exception{
		final File[] trainFiles = (new File(shapeFolder+"/train")).listFiles();
		if (trainFiles != null) {
			for (File f : trainFiles) {
				TRAIN_SHAPES.add(TrackValidator.parseShape(new FileInputStream(f)).get(0));
			}
			TRAIN_POLYLINES = TRAIN_SHAPES.stream().map(x -> GamificationHelper.encodePoly(x)).collect(Collectors.toList());
		}
		BUS_DESCRIPTOR = new TTDescriptor();
		loadBusFolder(new File(shapeFolder+"/bus"));
		BUS_DESCRIPTOR.build(100);
	}
	
	
	
	/**
	 * @param file
	 * @throws FileNotFoundException 
	 */
	private void loadBusFolder(File file) throws Exception {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			InputStream shapes = null, stops = null, trips = null, stopTimes = null;
			
			for (File f : files) {
				if (f.isDirectory()) {
					loadBusFolder(f);
				} else {
					if ("stops.txt".equals(f.getName())) stops = new FileInputStream(f);
					if ("shapes.txt".equals(f.getName())) shapes = new FileInputStream(f);
					if ("stop_times.txt".equals(f.getName())) stopTimes = new FileInputStream(f);
					if ("trips.txt".equals(f.getName())) trips = new FileInputStream(f);
				}
			}
			if (shapes != null && stops != null && stopTimes != null && trips != null) {
				BUS_DESCRIPTOR.load(stops, trips, stopTimes, shapes);
			}
		}
	}

	public Map<String, Object> computeEstimatedPlannedJourneyScore(String appId, Itinerary itinerary, Collection<Geolocation> geolocations, boolean log) {
//		if (geolocations != null) {
//			String ttype = GamificationHelper.getFreetrackingTransportForItinerary(itinerary);
//			if (ttype != null && "walk".equals(ttype) || "bike".equals(ttype)) {
//				logger.info("Planned has single ttype: " + ttype + ", computing as freetracking");
//				return computeFreeTrackingScore(geolocations, ttype);
//			}
//		}
//		
//		
		Map<String, Object> data = Maps.newTreeMap();

		String parkName = null; // name of the parking
		String startBikesharingName = null; // name of starting bike sharing
											// station
		String endBikesharingName = null; // name of ending bike sharing station
		boolean pnr = false; // (park-n-ride)
		boolean bikeSharing = false;
		double bikeDist = 0; // km
		double walkDist = 0; // km
		double trainDist = 0; // km
		double busDist = 0; // km
		double carDist = 0; // km
		double transitDist = 0;

		logger.debug("Analyzing itinerary for gamification.");
		if (itinerary != null) {
			for (Leg leg : itinerary.getLeg()) {
				if (leg.getTransport().getType().equals(TType.CAR)) {
					carDist += leg.getLength() / 1000;
					if (leg.getTo().getStopId() != null) {
						if (leg.getTo().getStopId().getExtra() != null) {
							if (leg.getTo().getStopId().getExtra().containsKey("parkAndRide")) {
								pnr |= (Boolean) leg.getTo().getStopId().getExtra().get("parkAndRide");
							}
						}
						parkName = leg.getTo().getStopId().getId();
					}
				} else if (leg.getTransport().getType().equals(TType.BICYCLE)) {
					bikeDist += leg.getLength() / 1000;
					if (leg.getFrom().getStopId() != null && leg.getFrom().getStopId().getAgencyId() != null) {
						if (leg.getFrom().getStopId().getAgencyId().startsWith("BIKE_SHARING")) {
						bikeSharing = true;
						startBikesharingName = leg.getFrom().getStopId().getId();
					}
					}
					if (leg.getTo().getStopId() != null && leg.getTo().getStopId().getAgencyId() != null) {
						if (leg.getTo().getStopId().getAgencyId().startsWith("BIKE_SHARING")) {
						bikeSharing = true;
						endBikesharingName = leg.getTo().getStopId().getId();
						}
					}
				} else if (leg.getTransport().getType().equals(TType.WALK)) {
					walkDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRAIN)) {
					trainDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.BUS)) {
					busDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRANSIT)) {
					transitDist += leg.getLength() / 1000;
				}
			}
		}

		if (log) {
			logger.debug("Analysis results:");
			logger.debug("Distances [walk = " + walkDist + ", bike = " + bikeDist + ", train = " + trainDist + ", bus = " + busDist + ", car = " + carDist + "]");
			logger.debug("Park and ride = " + pnr + " , Bikesharing = " + bikeSharing);
			logger.debug("Park = " + parkName);
			logger.debug("Bikesharing = " + startBikesharingName + " / " + endBikesharingName);
		}

		Double score = 0.0;
		// score += (walkDist < 0.1 ? 0 : Math.min(3.5, walkDist)) * 10; Rovereto
		score += (walkDist < 0.25 ? 0 : Math.min(10, walkDist)) * 10;
//		score += (walkDist < 0.25 ? 0 : walkDist) * 15;
		score += Math.min(30, bikeDist) * 5;
//		score += bikeDist * 7;

//		double busTrainTransitDist = busDist + trainDist;
//		if (busTrainTransitDist > 0) {
//			score += (busTrainTransitDist > 0 && busTrainTransitDist < 1) ? 10 : ((busTrainTransitDist > 1 && busTrainTransitDist < 5) ? 15 : (busTrainTransitDist >= 5 && busTrainTransitDist < 10) ? 20
//					: (busTrainTransitDist >= 10 && busTrainTransitDist < 30) ? 30 : 40);
//		}
		
		if (busDist > 0) {
			score += (busDist > 0 && busDist < 1) ? 10 : ((busDist > 1 && busDist < 5) ? 15 : 20);
		}		
		if (trainDist > 0) {
			score += (trainDist > 0 && trainDist < 1) ? 10 : ((trainDist > 1 && trainDist < 5) ? 15 : 20);
		}				
		
		
		// Trento only
		if (transitDist > 0) {
//			score += 25;
			score += 15;
		}

		boolean zeroImpact = (busDist + carDist + trainDist + transitDist == 0 && walkDist + bikeDist > 0);
//		Rovereto
//		if (zeroImpact && itinerary.isPromoted()) {
//			score *= 1.7;
//		} else {
//			if (zeroImpact) {
//				score *= 1.5;
//			}
//			if (itinerary.isPromoted()) {
//				score *= 1.2;
//			}
//		}
		
		
		if (pnr) {
//			score += 10;
			score += 15;
		}
		if (zeroImpact) {
			score *= 1.5;
		}

		if (bikeDist > 0) {
			data.put("bikeDistance", bikeDist);
		}
		if (walkDist > 0) {
			data.put("walkDistance", walkDist);
		}
		if (busDist > 0) {
			data.put("busDistance", busDist);
		}
		if (trainDist > 0) {
			data.put("trainDistance", trainDist);
		}
		if (transitDist > 0) {
			data.put("transitDistance", transitDist);
		}		
		if (carDist > 0) {
			data.put("carDistance", carDist);
		}
		if (bikeSharing) {
			data.put("bikesharing", bikeSharing);
		}
		if (parkName != null) {
			data.put("park", parkName);
		}
		if (startBikesharingName != null) {
			data.put("startBike", startBikesharingName);
		}
		if (endBikesharingName != null) {
			data.put("endBike", endBikesharingName);
		}
		if (pnr) {
			data.put("p+r", pnr);
		}
		data.put("sustainable", itinerary.isPromoted());
//		data.put("zeroimpact", zeroImpact);
		data.put("estimatedScore", Math.round(score));

		return data;
	}	
	

	public Map<String, Object> computePlannedJourneyScore(String appId, Itinerary itinerary, Collection<Geolocation> geolocations, ValidationStatus vs, Map<String, Double> overriddenDistances, boolean log) {
		boolean asFreetracking = false;
		
		logger.info("Computing planned score");
		
		if (geolocations != null) {
			String ttype = GamificationHelper.getFreetrackingTransportForItinerary(itinerary);
			if (ttype != null && "walk".equals(ttype) || "bike".equals(ttype)) {
				logger.info("Planned has single ttype: " + ttype + ", computing as freetracking");
				asFreetracking = true;
			}
		}

		Map<String, Object> data = Maps.newTreeMap();

		String parkName = null; // name of the parking
		String startBikesharingName = null; // name of starting bike sharing
											// station
		String endBikesharingName = null; // name of ending bike sharing station
		boolean pnr = false; // (park-n-ride)
		boolean bikeSharing = false;
		double bikeDist = 0; // km
		double walkDist = 0; // km
		double trainDist = 0; // km
		double busDist = 0; // km
		double carDist = 0; // km
		double transitDist = 0;

		logger.debug("Analyzing itinerary for gamification.");
		if (itinerary != null) {
			for (Leg leg : itinerary.getLeg()) {
				if (leg.getTransport().getType().equals(TType.CAR)) {
					carDist += leg.getLength() / 1000;
					if (leg.getTo().getStopId() != null) {
						if (leg.getTo().getStopId().getExtra() != null) {
							if (leg.getTo().getStopId().getExtra().containsKey("parkAndRide")) {
								pnr |= (Boolean) leg.getTo().getStopId().getExtra().get("parkAndRide");
							}
						}
						parkName = leg.getTo().getStopId().getId();
					}
				} else if (leg.getTransport().getType().equals(TType.BICYCLE)) {
					if (!asFreetracking) {
						bikeDist += leg.getLength() / 1000;
					}
					if (leg.getFrom().getStopId() != null && leg.getFrom().getStopId().getAgencyId() != null) {
						if (leg.getFrom().getStopId().getAgencyId().startsWith("BIKE_SHARING")) {
							bikeSharing = true;
							startBikesharingName = leg.getFrom().getStopId().getId();
						}
					}
					if (leg.getTo().getStopId() != null && leg.getTo().getStopId().getAgencyId() != null) {
						if (leg.getTo().getStopId().getAgencyId().startsWith("BIKE_SHARING")) {
							bikeSharing = true;
							endBikesharingName = leg.getTo().getStopId().getId();
						}
					}
				} else if (leg.getTransport().getType().equals(TType.WALK) && !asFreetracking) {
					walkDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRAIN)) {
					trainDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.BUS)) {
					busDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRANSIT)) {
					transitDist += leg.getLength() / 1000;
				}
			}
		}

		if (asFreetracking) {
			if (vs.getEffectiveDistances().containsKey(MODE_TYPE.WALK)) {
				double distance = (vs.getEffectiveDistances().get(MODE_TYPE.WALK) / 1000.0);
				logger.info("Effective walk distance: " + distance);
				walkDist = distance;
			}
			if (vs.getEffectiveDistances().containsKey(MODE_TYPE.BIKE)) {
				double distance = (vs.getEffectiveDistances().get(MODE_TYPE.BIKE) / 1000.0);
				logger.info("Effective bike distance: " + distance);
				bikeDist = distance;
			}
		}
		
		if (overriddenDistances != null) {
			if (overriddenDistances.containsKey("walk")) {
				double distance = overriddenDistances.get("walk") / 1000.0;
				logger.info("Overridden walk distance: " + distance);
				walkDist = distance;
			}
			if (overriddenDistances.containsKey("bike")) {
				double distance = overriddenDistances.get("bike") / 1000.0;
				logger.info("Overridden bike distance: " + distance);
				bikeDist = distance;
			}
			if (overriddenDistances.containsKey("bus")) {
				double distance = overriddenDistances.get("bus") / 1000.0;
				logger.info("Overridden bus distance: " + distance);
				busDist = distance;
			}
			if (overriddenDistances.containsKey("train")) {
				double distance = overriddenDistances.get("train") / 1000.0;
				logger.info("Overridden train distance: " + distance);
				trainDist = distance;
			}
		}
		

		if (log) {
			logger.debug("Analysis results:");
			logger.debug("Distances [walk = " + walkDist + ", bike = " + bikeDist + ", train = " + trainDist + ", bus = " + busDist + ", car = " + carDist + "]");
			logger.debug("Park and ride = " + pnr + " , Bikesharing = " + bikeSharing);
			logger.debug("Park = " + parkName);
			logger.debug("Bikesharing = " + startBikesharingName + " / " + endBikesharingName);
		}

		Double score = 0.0;
		// score += (walkDist < 0.1 ? 0 : Math.min(3.5, walkDist)) * 10; Rovereto
		score += (walkDist < 0.25 ? 0 : Math.min(10, walkDist)) * 10;
		// score += (walkDist < 0.25 ? 0 : walkDist) * 15;
		score += Math.min(30, bikeDist) * 5;
		// score += bikeDist * 7;

//		double busTrainTransitDist = busDist + trainDist;
		// if (busTrainTransitDist > 0) {
		// score += (busTrainTransitDist > 0 && busTrainTransitDist < 1) ? 10 : ((busTrainTransitDist > 1 && busTrainTransitDist < 5) ? 15 : (busTrainTransitDist >= 5 && busTrainTransitDist < 10) ? 20
		// : (busTrainTransitDist >= 10 && busTrainTransitDist < 30) ? 30 : 40);
		// }

		if (busDist > 0) {
			score += (busDist > 0 && busDist < 1) ? 10 : ((busDist > 1 && busDist < 5) ? 15 : 20);
		}
		if (trainDist > 0) {
			score += (trainDist > 0 && trainDist < 1) ? 10 : ((trainDist > 1 && trainDist < 5) ? 15 : 20);
		}

		// Trento only
		if (transitDist > 0) {
			// score += 25;
			score += 15;
		}

		boolean zeroImpact = (busDist + carDist + trainDist + transitDist == 0 && walkDist + bikeDist > 0);
		// Rovereto
		// if (zeroImpact && itinerary.isPromoted()) {
		// score *= 1.7;
		// } else {
		// if (zeroImpact) {
		// score *= 1.5;
		// }
		// if (itinerary.isPromoted()) {
		// score *= 1.2;
		// }
		// }

		if (pnr) {
			// score += 10;
			score += 15;
		}
		if (zeroImpact) {
			score *= 1.5;
		}

		if (bikeDist > 0) {
			data.put("bikeDistance", bikeDist);
		}
		if (walkDist > 0) {
			data.put("walkDistance", walkDist);
		}
		if (busDist > 0) {
			data.put("busDistance", busDist);
		}
		if (trainDist > 0) {
			data.put("trainDistance", trainDist);
		}
		if (transitDist > 0) {
			data.put("transitDistance", transitDist);
		}
		if (carDist > 0) {
			data.put("carDistance", carDist);
		}
		if (bikeSharing) {
			data.put("bikesharing", bikeSharing);
		}
		if (parkName != null) {
			data.put("park", parkName);
		}
		if (startBikesharingName != null) {
			data.put("startBike", startBikesharingName);
		}
		if (endBikesharingName != null) {
			data.put("endBike", endBikesharingName);
		}
		if (pnr) {
			data.put("p+r", pnr);
		}
		data.put("sustainable", itinerary.isPromoted());
		// data.put("zeroimpact", zeroImpact);
		data.put("estimatedScore", Math.round(score));

		return data;
	}
	
	public Map<String, Object> computeFreeTrackingScore(String appId, Collection<Geolocation> geolocationEvents, String ttype, ValidationStatus vs, Map<String, Double> overriddenDistances) throws Exception {
		Map<String, Object> result = Maps.newTreeMap();
		Double score = 0.0;
		double distance = 0; 		
		
//		logger.info("Computing free tracking score");

		boolean isOverridden = overriddenDistances != null && !overriddenDistances.isEmpty();
		
		if (geolocationEvents != null & geolocationEvents.size() >= 2 || isOverridden) {
			if (vs == null) {
				vs = validateFreeTracking(geolocationEvents, ttype, appId).getValidationStatus();
			}
			
			if (!isOverridden) {
				overriddenDistances = Maps.newTreeMap();
			}

			boolean zeroImpact = false;
			if ("walk".equals(ttype)) {
				if (overriddenDistances.containsKey("walk")) {
					distance = overriddenDistances.get("walk") / 1000.0;
					logger.info("Overridden walk distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.WALK)) {
					distance = vs.getEffectiveDistances().get(MODE_TYPE.WALK) / 1000.0; 
				}
				result.put("walkDistance", distance);
//				score = (distance < 0.25 ? 0 : Math.min(3.5, distance)) * 10;
//				score = (distance < 0.25 ? 0 : distance) * 15;
				score += (distance < 0.25 ? 0 : Math.min(10, distance)) * 10;
				zeroImpact = true;
			}
			if ("bike".equals(ttype)) {
				if (overriddenDistances.containsKey("bike")) {
					distance = overriddenDistances.get("bike") / 1000.0;
					logger.info("Overridden bike distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.BIKE)) {				
					distance = vs.getEffectiveDistances().get(MODE_TYPE.BIKE) / 1000.0;
				}
				result.put("bikeDistance", distance);
//				score += Math.min(7, distance) * 5;
//				score += distance * 7;
				score += Math.min(30, distance) * 5;
				zeroImpact = true;
			} if ("bus".equals(ttype)) {
				if (overriddenDistances.containsKey("bus")) {
					distance = overriddenDistances.get("bus") / 1000.0;
					logger.info("Overridden bus distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.BUS)) {				
					distance = vs.getEffectiveDistances().get(MODE_TYPE.BUS) / 1000.0;
				}
				result.put("busDistance", distance);
				score += (distance > 0 && distance < 1) ? 10 : ((distance > 1 && distance < 5) ? 15 : 20);
			} if ("train".equals(ttype)) {
				if (overriddenDistances.containsKey("train")) {
					distance = overriddenDistances.get("train") / 1000.0;
					logger.info("Overridden train distance: " + distance);
				} else if (vs.getEffectiveDistances().containsKey(MODE_TYPE.TRAIN)) {						
					distance = vs.getEffectiveDistances().get(MODE_TYPE.TRAIN) / 1000.0;
				}
				result.put("trainDistance", distance);
				score += (distance > 0 && distance < 1) ? 10 : ((distance > 1 && distance < 5) ? 15 : 20);
			}
			
			if (zeroImpact) {
				score *= 1.5;
			}
		} else {
			logger.info("Skipping");
		}

		result.put("estimatedScore", Math.round(score));
		return result;
	}	
	

	// TODO: remove?
	public long computeEstimatedGameScore(String appId, Itinerary itinerary, Collection<Geolocation> geolocations, boolean log) {
		Long score = (Long) (computeEstimatedPlannedJourneyScore(appId, itinerary, geolocations, log).get("estimatedScore"));
		itinerary.getCustomData().put("estimatedScore", score);
		return score;
	}



	public ValidationResult validatePlannedJourney(ItineraryObject itinerary, Collection<Geolocation> geolocations, String appId) throws Exception {
		if (geolocations == null) {
			return null;
		}
		String ttype = GamificationHelper.getFreetrackingTransportForItinerary(itinerary);
		if ("walk".equals(ttype) || "bike".equals(ttype)) {
			logger.info("Planned has single ttype: " + ttype + ", validating as freetracking");
			ValidationResult vr = validateFreeTracking(geolocations, ttype, appId);
			vr.setPlannedAsFreeTracking(true);
			vr.getValidationStatus().updatePlannedDistances(itinerary.getData());
			return vr;
		}
		
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());

		ValidationResult vr = new ValidationResult();
		vr.setValidationStatus(TrackValidator.validatePlanned(geolocations, itinerary.getData(), game.getAreas()));
		
		return vr;
		
	}

	public ValidationResult validateFreeTracking(Collection<Geolocation> geolocations, String ttype, String appId) throws Exception {
		if (geolocations == null || ttype == null) {
			return null;
		}
		AppInfo app = appSetup.findAppById(appId);
		GameInfo game = gameSetup.findGameById(app.getGameId());

		ValidationResult vr = new ValidationResult();
		
		switch(ttype) {
		case "walk":
			vr.setValidationStatus(TrackValidator.validateFreeWalk(geolocations, game.getAreas()));
			break;
		case "bike": 
			vr.setValidationStatus(TrackValidator.validateFreeBike(geolocations, game.getAreas()));
			break;
		case "bus": 
			vr.setValidationStatus(TrackValidator.validateFreeBus(geolocations, BUS_DESCRIPTOR.filterShapes(geolocations), game.getAreas()));
			break;
		case "train": 
			vr.setValidationStatus(TrackValidator.validateFreeTrain(geolocations, TRAIN_SHAPES, game.getAreas()));
			break;
		}
		return vr;
		

	}
	
	public void setPolylines(TrackedInstance instance) throws Exception {
		if (instance.getGeolocationEvents() == null || instance.getGeolocationEvents().size() < 2 || instance.getFreeTrackingTransport() == null) {
			return;
		}
		
		Map<String, Object> polys = Maps.newTreeMap();
		
		switch(instance.getFreeTrackingTransport()) {
		case "bus": 
			polys.put("bus", BUS_DESCRIPTOR.filteredPolylines(instance.getGeolocationEvents()));
			instance.setRoutesPolylines(polys);
			break;
		case "train": 
			polys.put("train",TRAIN_POLYLINES);
			instance.setRoutesPolylines(polys);
			break;
		}
		

	}	

//	public boolean isTripsGroup(Collection<Geolocation> geolocations, String userId, String appId, String ttpye) {
//		try {
//			Range<Long> range = findGeolocationTimeRange(geolocations);
//			if (range == null) {
//				return false;
//			}
//			long start = range.lowerEndpoint();
//
//			Criteria criteria = new Criteria("userId").is(userId).and("appId").is(appId).and("freeTrackingTransport").is(ttpye);
//			Query query = new Query(criteria);
//			query.fields().include("geolocationEvents.recorded_at").include("_id").include("groupId");
//
//			List<TrackedInstance> tis = template.find(query, TrackedInstance.class, "trackedInstances");
//			Set<Integer> groupIds = Sets.newHashSet();
//
//			for (TrackedInstance ti : tis) {
//				groupIds.add(ti.getGroupId());
//			}
//
//			for (TrackedInstance ti : tis) {
//				long last = 0;
//				for (Geolocation loc : ti.getGeolocationEvents()) {
//					last = Math.max(last, loc.getRecorded_at().getTime());
//				}
//				if (start > last && start - last < SAME_TRIP_INTERVAL) {
//					return true;
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return false;
//	}
//
//	private Range<Long> findGeolocationTimeRange(Collection<Geolocation> geolocations) {
//		long first = Long.MAX_VALUE;
//		long last = 0;
//		for (Geolocation loc : geolocations) {
//			first = Math.min(first, loc.getRecorded_at().getTime());
//			last = Math.max(last, loc.getRecorded_at().getTime());
//		}
//		
//		if (first < last) {
//			Range<Long> range = Range.closed(first, last);
//			return range;
//		} else {
//			return null;
//		}
//	}
	
	public boolean isSuspect(TrackedInstance trackedInstance) {
		List<Geolocation> points = new ArrayList<Geolocation>(trackedInstance.getGeolocationEvents());
		
		if (points.size() < 2) {
			return false;
		}
		
		
		if (trackedInstance.getDeviceInfo() == null || !trackedInstance.getDeviceInfo().contains("\"isVirtual\":false")) {
			return true;
		}
		
		Collections.sort(points, new Comparator<Geolocation>() {

			@Override
			public int compare(Geolocation o1, Geolocation o2) {
				return (int) (o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime());
			}

		});
		
		List<Double> allSpeeds = Lists.newArrayList();
		for (Geolocation location: points) {
			if (location.getSpeed() > 0.0) {
				allSpeeds.add(location.getSpeed());
			}
		}

		Set<Double> speeds = Sets.newHashSet(allSpeeds);
		
		// too many distance duplicate values
		if (speeds.size() < allSpeeds.size() * .8 ) {
			return true;
		}
		
		if (!allSpeeds.isEmpty()) {
			double[] speedArray = Doubles.toArray(allSpeeds);

			double min = Doubles.min(speedArray);
			double max = Doubles.max(speedArray);

			allSpeeds.remove(min);
			allSpeeds.remove(max);

			if (!allSpeeds.isEmpty()) {
				speedArray = Doubles.toArray(allSpeeds);

				min = Doubles.min(speedArray);
				max = Doubles.max(speedArray);

				// speed range is too narrow
				if (max < min * 1.1) {
					return true;
				}
			}
		}
		
		return false;
	}
	
//	public void findOverlappedTrips(List<TrackedInstance> tis) {
//		try {
//
//			Map<String, Range<Long>> ranges = Maps.newTreeMap();
//			
//			for (TrackedInstance ti: tis) {
//				Range<Long> range = findGeolocationTimeRange(ti.getGeolocationEvents());
//				if (range != null) {
//					ranges.put(ti.getId(), range);
//				}
//			}
//			
//			for (int i = 0; i < tis.size(); i++) {
//				for (int j = 0; j < i; j++) {
//					String key1 = tis.get(i).getId();
//					String key2 = tis.get(j).getId();
//					
//					if (ranges.get(key1) != null &&  ranges.get(key2) != null && ranges.get(key1).isConnected(ranges.get(key2))) {
//						tis.get(i).setSuspect(true);
//						tis.get(j).setSuspect(true);
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

}
