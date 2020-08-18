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

package eu.trentorise.smartcampus.mobility.gamification;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.TrackSplit;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.ERROR_TYPE;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.Interval;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.MODE_TYPE;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.TRIP_TYPE;
import eu.trentorise.smartcampus.mobility.security.Shape;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;

/**
 * @author raman
 *
 */
public class TrackValidator {

	private static final int WALK_SPEED_THRESHOLD = 7; // km/h
	private static final int WALK_AVG_SPEED_THRESHOLD = 8; // km/h
	private static final double WALK_GUARANTEED_AVG_SPEED_THRESHOLD = 5; // km/h

	private static final int BIKE_SPEED_THRESHOLD = 30; // km/h
	private static final int BIKE_AVG_SPEED_THRESHOLD = 27; // km/h
	private static final double BIKE_GUARANTEED_AVG_SPEED_THRESHOLD = 18; // km/h

	public static final double VALIDITY_THRESHOLD = 80; // %
	public static final double ACCURACY_THRESHOLD = 150; // meters
	

	public static final double MIN_COVERAGE_THRESHOLD = 30; // %

	public static final double SHARED_TRIP_DISTANCE_THRESHOLD = 1000; // meters 

	public static final double DISTANCE_THRESHOLD = 250; // meters 
	public static final long DATA_HOLE_THRESHOLD = 10*60; // seconds
	public static final double BIKE_DISTANCE_THRESHOLD = 100;// meters 
	private static final double MAX_AVG_SPEED_THRESHOLD = 200; // km/h
	
	public static final int PENDING_COVERAGE_THRESHOLD = 60;
	public static final int COVERAGE_THRESHOLD = 80; // %
	public static final int CERTIFIED_COVERAGE_THRESHOLD_VALID = 70;
	public static final int CERTIFIED_COVERAGE_THRESHOLD_PENDING = 50;	
	private static final double GUARANTEED_COVERAGE_THRESHOLD_VALID = 90; // %
	private static final double GUARANTEED_COVERAGE_THRESHOLD_PENDING = 80; // %
	
//	private static transient final Logger logger = LoggerFactory.getLogger(TrackValidator.class);
	
	/**
	 * Preprocess tracked data: spline, remove outstanding points, remove potentially erroneous start / stop points 
	 * @param points
	 * @return
	 */
	public static List<Geolocation> preprocessTrack(List<Geolocation> points) {
		if (points.size() > 4) points.remove(0);
		if (points.size() > 2) GamificationHelper.removeOutliers(points);
//		if (points.size() > 3) removeNoise(points);
		points = GamificationHelper.transform(points);
		return points;
	}
	
	/**
	 * Remove intermediate points. Compare distances between point and two successors. If the angle formed by
	 * 3 points is sharp and distance shift is small, remove intermediate
	 * @param points
	 */
	public static void removeNoise(List<Geolocation> points) {
		int i = 0;
		while (i < points.size()-3) {
			double dist = GamificationHelper.harvesineDistance(points.get(i), points.get(i+2));
			double dist1 = GamificationHelper.harvesineDistance(points.get(i), points.get(i+1));
			if (dist1 > 2*dist && dist < 0.05) {
				points.remove(i+1);
			} else {
				i++;
			}
		}
	}

	/**
	 * Preprocess and prevalidate track data.
	 * Check for data 'holes'
	 * Basic validation: minimal length and points, game area. 
	 * @param track
	 * @param status
	 * @param areas
	 * @param distanceThreshold 
	 * @return preprocessed track data
	 */
	public static List<Geolocation> prevalidate(Collection<Geolocation> track, ValidationStatus status, List<Shape> areas, double distanceThreshold) {
		// check data present
		if (track == null || track.size() <= 1) {
			status.setError(ERROR_TYPE.NO_DATA);
			status.setValidationOutcome(TravelValidity.INVALID);
			return Collections.emptyList();
		}
		List<Geolocation> points = new ArrayList<Geolocation>(track);
		Collections.sort(points, (o1, o2) -> (int)(o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime()));
		// check for data holes. If there is missing data, set status to PENDING and stop
//		for (int i = 1; i < points.size(); i++) {
//			long interval = points.get(i).getRecorded_at().getTime() - points.get(i-1).getRecorded_at().getTime();
//			if (interval > DATA_HOLE_THRESHOLD * 1000) {
//				double dist = GamificationHelper.harvesineDistance(points.get(i), points.get(i-1));
//				// if moving faster than 1 m/s through out the interval then data is missing
//				if (dist*1000000 > interval) {
//					status.setError(ERROR_TYPE.DATA_HOLE);
//					status.setValidationOutcome(TravelValidity.PENDING);
//					return Collections.emptyList();
//				}
//			}
//		}
		
		// preprocess
		status.computeAccuracy(points);
//		System.err.println(GamificationHelper.encodePoly(points));
		points = removeStarredClusters(points);
		points = preprocessTrack(points);
		TrackValidator.shortenByHighSpeed(points);
		
//		System.err.println(GamificationHelper.encodePoly(points));
		
		Collections.sort(points, (o1, o2) -> (int)(o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime()));
		status.updateMetrics(points);

		if (points.size() < 2) {
			status.setValidationOutcome(TravelValidity.INVALID);
			status.setError(ERROR_TYPE.NO_DATA);
			return points;
		}
		
		// check if the track is in the area of interest
		boolean inRange = true;
		if (areas != null && !areas.isEmpty()) {
			inRange = false;
			inRange |= GamificationHelper.inAreas(areas, points.get(0));
			inRange |= GamificationHelper.inAreas(areas, points.get(points.size() - 1));
		}	
		if (!inRange) {
			status.setValidationOutcome(TravelValidity.INVALID);
			status.setError(ERROR_TYPE.OUT_OF_AREA);
		}
		// min distance 
		else if (status.getDistance() < distanceThreshold) {
			status.setValidationOutcome(TravelValidity.INVALID);
			status.setError(ERROR_TYPE.TOO_SHORT);
		}
		
		return points;
	}
	
	public static void shortenByHighSpeed(List<Geolocation> points) {
		double prevSpeed = 0;

		int start = 1;
		boolean removed = false;
//		int n = 0;
		do {
			removed = false;
			Integer min = null;
			Integer max = null;
			for (int i = start; i < points.size(); i++) {
				double d = GamificationHelper.harvesineDistance(points.get(i), points.get(i - 1));
				long t = points.get(i).getRecorded_at().getTime() - points.get(i - 1).getRecorded_at().getTime();
				if (t > 0) {
					double speed = (1000.0 * d / ((double) t / 1000)) * 3.6;
//					System.err.println((i - 1) + " -> " + i + " = " + speed + " / " + points.get(i - 1) + " -> " + points.get(i));
					if (speed > 30 && speed > prevSpeed * 10 && prevSpeed > 1) {
//						System.err.println("prev " + prevSpeed);
						Integer found = findReachableBySpeed(i - 1, speed, points);
						if (found != null) { // && found - i < 50) {
//							System.err.println("[" + (i) + "," + found + "]");
							min = i;
							max = found;
							break;
						}
					}
					prevSpeed = speed;
				}
			}

			if (min != null) {// && max - min < 5) {
				start = min + 1;
				for (int j = 0; j <= max - min ; j++) {
					int m = min.intValue();
//					System.err.println("removing " + (j + min));
					points.remove(m);
				}
				removed = true;
			}
			
//			n++;
		} while (removed);

	}
	
	private static Integer findReachableBySpeed(int index, double prevSpeed, List<Geolocation> points) {
		Integer found = null;
		double d0 = GamificationHelper.harvesineDistance(points.get(index), points.get(index - 1));
		for (int i = index; i < points.size(); i++) {
			double d = GamificationHelper.harvesineDistance(points.get(i), points.get(index));
			long t = points.get(i).getRecorded_at().getTime() - points.get(index).getRecorded_at().getTime();
			if (t > 0) {
//				double speed = (1000.0 * d / ((double) t / 1000)) * 3.6;
//				System.err.println("\t" + i + " / " + speed + " / " + d);
				if (d < d0) {
//					System.err.println("\t\t" +index + " -> " + i + " = " + speed + " / " + d);
					found = i - 1;
					break;
				}
			}
		}

		if (found != null && found == points.size() - 1) {
			found = null;
		}

		return found;
	}
	
	public static List<Geolocation> removeStarredClusters(List<Geolocation> origPoints) {
		List<Geolocation> points = new ArrayList<Geolocation>(origPoints);
		Collections.sort(points);		
		List<List<Geolocation>> groups = computeAngles(points);
		Multimap<Geolocation, Geolocation> barycenterMap = ArrayListMultimap.create();
		
		for (List<Geolocation> group: groups) {
			Geolocation barycenter = buildBarycenterGeolocation(group);
			for (Geolocation point: group) {
				barycenterMap.put(barycenter, point);
			}
		}
		
		Geolocation prev = null;
		List<Geolocation> newPoints = Lists.newArrayList();
		for (int i = 0; i < points.size(); i++) {
			Geolocation barycenter = null;
			for (Geolocation bar: barycenterMap.keySet()) {
				if (barycenterMap.get(bar).contains(points.get(i))) {
					barycenter = bar;
					break;
				}
			}
			if (barycenter != null) {
				if (prev == null) {
					newPoints.add(barycenter);
					prev = barycenter;
				} else if (barycenter != prev){
					newPoints.add(barycenter);
					prev = barycenter;
				}
			} else {
				newPoints.add(points.get(i));
				prev = null;
			}
		}	
		
//		if (origPoints.size() != newPoints.size()) {
//			logger.debug("Original polyline: " + GamificationHelper.encodePoly(origPoints));
//			logger.debug("Reduced polyline: " + GamificationHelper.encodePoly(newPoints));
//		}
		
		return newPoints;
	}
	
	private static List<List<Geolocation>> computeAngles(List<Geolocation> points) {
		List<List<Geolocation>> groups = Lists.newLinkedList();
		List<Integer> indexes = Lists.newLinkedList();
		for (int i = 1; i < points.size() - 1; i++) {
			double d1 = GamificationHelper.harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude());
			double d2 = GamificationHelper.harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude());
			double d3 = GamificationHelper.harvesineDistance(points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude(), points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude());

			double a1 = (Math.acos((d1 * d1 + d2 * d2 - d3 * d3) / (2 * d1 * d2)));

			Double acc = points.get(i).getAccuracy() / 1000.0;
			
			double angle = Math.toDegrees(a1);
			// or an angle, or put together the points within accuracy when accuracy is bad
			if (Math.abs(angle) < 30 || acc > 1 && d1 < acc && d2 < acc && d3 < acc) {
				// add head of an "arrow"
				indexes.add(i);
			}
		}

		int start = -1;
		int end = -1;
		boolean closed = false;
		for (int i = 1; i < indexes.size(); i++) {
			// initialize first star
			if (start == -1) {
				start = i - 1;
			}
			// end star: or the nodes are not subsequent or cut by 5 mins 
			if (start != -1 && (indexes.get(i) - indexes.get(i - 1) > 3 || points.get(indexes.get(i)).getRecorded_at().getTime() - points.get(indexes.get(start) - 1).getRecorded_at().getTime() > 5*60*1000)) {
				end = i - 1;
			}
			// close the last star
			if (i == indexes.size() - 1 && start != -1 && end == -1) {
				closed = true;
				end = i;
			}
			// compute group
			if (end != -1) {
				List<Geolocation> group = Lists.newLinkedList();
				for (int j = indexes.get(start) -1; j <= indexes.get(end) + 1; j++) {
					group.add(points.get(j));
				}
				groups.add(group);
				start = -1;
				end = -1;
			}

			
//			if (indexes.get(i) - indexes.get(i - 1) == 1 && start == -1) {
//				start = i - 1;
//			}
//			if (start != -1 && indexes.get(i) - indexes.get(i - 1) > 5) {
//				end = i - 1;
//			}
//			if (i == indexes.size() - 1 && start != -1) {
//				end = i;
//			}
//			if (start != -1 && end != -1 && (end - start >= 20)) {
//				List<Geolocation> group = Lists.newArrayList();
//				for (int j = indexes.get(start); j <= indexes.get(end); j++) {
//					group.add(points.get(j));
//				}
//				groups.add(group);
//			}
//			if (start != -1 && end != -1) {
//				start = -1;
//				end = -1;
//			}
		}
		if (!closed && indexes.size() > 0) {
			List<Geolocation> group = Lists.newLinkedList();
			for (int j = indexes.get(indexes.size()-1) -1; j <= indexes.get(indexes.size()-1) + 1; j++) {
				group.add(points.get(j));
			}
			groups.add(group);
		}
		
		return groups;
	}
	
	private static Geolocation buildBarycenterGeolocation(Collection<Geolocation> points) {
		double result[] = new double[]{0,0};
		long time[] = new long[]{0};
		long acc[] = new long[]{0};
		points.stream().forEach(p -> {
			result[0] = result[0] + p.getLatitude();
			result[1] = result[1] + p.getLongitude();
			time[0] += p.getRecorded_at().getTime();
			acc[0] += p.getAccuracy();
		});
		result[0] /= points.size();
		result[1] /= points.size();
		time[0] /= points.size();
		acc[0] /= points.size();
		
		Geolocation geoloc = new Geolocation(result[0], result[1], new Date(time[0]));
		geoloc.setAccuracy(acc[0]);
		
		return geoloc;
	}	

	/**
	 * Validate free tracking: train. Take reference train shapes as input.
	 * Preprocess track data; check if contains more than 2 points; split into blocks with 15km/h - 3 mins for stop - at least 1 min fast track; 
	 * match fragments against reference trac with 150m error and 80% coverage. Consider VALID if at least 80% of length is matched. If no
	 * fast fragment found consider PENDING with TOO_SLOW error. Otherwise consider PENDING.
	 * @param track
	 * @param referenceTracks
	 * @param areas
	 * @return
	 */
	public static ValidationStatus validateFreeTrain(Collection<Geolocation> track, List<List<Geolocation>> referenceTracks, List<Shape> areas) {
		MODE_TYPE mode = MODE_TYPE.TRAIN; 
		double speedThreshold = 15, timeThreshold = 3*60*1000, minTrackThreshold = 1*60*1000; 
		return validateFreePTMode(track, referenceTracks, areas, mode, speedThreshold, timeThreshold, minTrackThreshold, false);
	}

	/**
	 * Validate free tracking: bus. Take reference bus shapes as input.
	 * Preprocess track data; check if contains more than 2 points; split into blocks with 15km/h - 3 mins for stop - at least 30 sec fast track; 
	 * match fragments against reference trac with 150m error and 80% coverage. Consider VALID if at least 80% of length is matched and is certified. 
	 * If no fast fragment found consider PENDING with TOO_SLOW error. Otherwise consider PENDING.
	 * @param track
	 * @param referenceTracks
	 * @param areas
	 * @return
	 */
	public static ValidationStatus validateFreeBus(Collection<Geolocation> track, List<List<Geolocation>> referenceTracks, List<Shape> areas) {
		MODE_TYPE mode = MODE_TYPE.BUS; 
		double speedThreshold = 10, timeThreshold = 1*60*1000, minTrackThreshold = 30*1000; 
		return validateFreePTMode(track, referenceTracks, areas, mode, speedThreshold, timeThreshold, minTrackThreshold, true);
	}
	
	private static ValidationStatus validateFreePTMode(
			Collection<Geolocation> track, 
			List<List<Geolocation>> referenceTracks,
			List<Shape> areas, 
			MODE_TYPE mode, 
			double speedThreshold, 
			double timeThreshold, 
			double minTrackThreshold,
			boolean checkCertificate) 
	{
		ValidationStatus status = new ValidationStatus();
		// set parameters
		status.setTripType(TRIP_TYPE.FREE);
		status.setModeType(mode);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setMatchThreshold(ACCURACY_THRESHOLD);
		status.setCoverageThreshold(COVERAGE_THRESHOLD);

		// basic validation
		List<Geolocation> points = prevalidate(track, status, areas, DISTANCE_THRESHOLD);
		if (status.getValidationOutcome() != null) {
			// no too short error for PT trips. Replace with DOES_NOT_MATCH error. 
			if (ERROR_TYPE.TOO_SHORT.equals(status.getError())) {
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
			}
			return status;
		}
		
		
		// split track into pieces. For train consider 15km/h threshold
		TrackSplit trackSplit = TrackSplit.fastSplit(points, speedThreshold, timeThreshold, minTrackThreshold);
		if (trackSplit.getFastIntervals().isEmpty()) {
			status.setValidationOutcome(TravelValidity.PENDING);
			status.setError(ERROR_TYPE.TOO_SLOW);
			// for consistency, put the whole distance
			status.getEffectiveDistances().put(mode, status.getDistance());
			return status;
		}
		status.updateFastSplit(trackSplit, MAX_AVG_SPEED_THRESHOLD);

		if (referenceTracks != null && !referenceTracks.isEmpty()) {
			// compute matches checking each fast fragment against available reference train tracks
			// check max coverage for each fragment
			// if overall distance coverage is high enough, set trip valid 
			double matchedDistance = 0, transportDistance = 0.00000001, coveredDistance =  0;// to avoid division by 0
			for (Interval interval: status.getIntervals()) {
				List<Geolocation> subtrack = trackSplit.getTrack().subList(interval.getStart(), interval.getEnd());
				int effectiveLength = subtrack.size();
				int invalid = effectiveLength;
				double subtrackPrecision = 0;
				MatchModel subtrackModel = new MatchModel(subtrack, status.getMatchThreshold());
				for (List<Geolocation> ref: referenceTracks) {
					invalid = Math.min(trackMatch(subtrackModel, ref),invalid); 
					subtrackPrecision = 100.0 * (effectiveLength-invalid) / (effectiveLength);
					if (subtrackPrecision >= status.getValidityThreshold()) break;
				}
				interval.setMatch(subtrackPrecision);
				transportDistance += interval.getDistance();
				if (subtrackPrecision >= status.getValidityThreshold()) {
					status.setMatchedIntervals(status.getMatchedIntervals()+1);
					matchedDistance += interval.getDistance();
				}
				coveredDistance += interval.getDistance() * subtrackPrecision / 100.0;
			}
			double coverage = 100.0 * matchedDistance / transportDistance;// status.getDistance();
			if (checkCertificate) { // bus
				boolean certified = isCertifiedTrack(track); 
				
				if (certified && coverage >= COVERAGE_THRESHOLD) {
					status.setValidationOutcome(TravelValidity.VALID);
				} else if (certified && 100.0 * coveredDistance / transportDistance >= CERTIFIED_COVERAGE_THRESHOLD_VALID) {
					status.setValidationOutcome(TravelValidity.VALID);				
				} else if (certified && 100.0 * coveredDistance / transportDistance >= CERTIFIED_COVERAGE_THRESHOLD_PENDING) {
					status.setValidationOutcome(TravelValidity.PENDING);				
				} else if (!certified && 100.0 * coveredDistance / transportDistance >= GUARANTEED_COVERAGE_THRESHOLD_VALID) {
					status.setValidationOutcome(TravelValidity.VALID);				
				} else if (!certified && 100.0 * coveredDistance / transportDistance >= GUARANTEED_COVERAGE_THRESHOLD_PENDING) {
					status.setValidationOutcome(TravelValidity.PENDING);				
				} else {
					status.setValidationOutcome(TravelValidity.INVALID);				
				}
				
//				if (certified && coverage >= COVERAGE_THRESHOLD) {
//					status.setValidationOutcome(TravelValidity.VALID);
//				} else if (certified && 100.0 * coveredDistance / transportDistance >= CERTIFIED_COVERAGE_THRESHOLD) {
//					status.setValidationOutcome(TravelValidity.VALID);				
//				} else {
//					status.setValidationOutcome(TravelValidity.PENDING);				
//				}
			} else {
				if (coverage >= COVERAGE_THRESHOLD) {
					status.setValidationOutcome(TravelValidity.VALID);
				} else {
					status.setValidationOutcome(TravelValidity.INVALID);				
				}
//				status.setValidationOutcome(TravelValidity.INVALID);	
//				if (coverage > PENDING_COVERAGE_THRESHOLD) {
//					status.setValidationOutcome(TravelValidity.PENDING);	
//				}
//				if (coverage > COVERAGE_THRESHOLD) {
//					status.setValidationOutcome(TravelValidity.VALID);	
//				}
			}
			return status;
		} else {
			status.setValidationOutcome(TravelValidity.INVALID);
			return status;
		}
	}
	
	/**
	 * @param track
	 * @return true if the track contains certification markers in the points
	 */
	private static boolean isCertifiedTrack(Collection<Geolocation> track) {
		return track.stream().anyMatch(g -> !StringUtils.isEmpty(g.getCertificate()));
	}

	/**
	 * Validate free walk. Check track start/stop is within the areas,
	 * consider only the piece with valid walk speed (if any) and is longer than 250 meters.
	 * @param track
	 * @param areas
	 * @return
	 */
	public static ValidationStatus validateFreeWalk(Collection<Geolocation> track, List<Shape> areas) {

		MODE_TYPE mode = MODE_TYPE.WALK; 
		// TODO timeThreshold = 10 * 1000
		double speedThreshold = WALK_SPEED_THRESHOLD, timeThreshold = 30 * 1000, minTrackThreshold = 60*1000, avgSpeedThreshold = WALK_AVG_SPEED_THRESHOLD, guaranteedAvgSpeedThreshold = WALK_GUARANTEED_AVG_SPEED_THRESHOLD; 

		
		return validateFreeMode(track, areas, mode, speedThreshold, timeThreshold, minTrackThreshold, avgSpeedThreshold, guaranteedAvgSpeedThreshold, DISTANCE_THRESHOLD);
	}

	/**
	 * Validate free walk. Check track start/stop is within the areas,
	 * consider only the piece with valid bike speed (if any) and is longer than 250 meters.
	 * @param track
	 * @param areas
	 * @return
	 */
	public static ValidationStatus validateFreeBike(Collection<Geolocation> track, List<Shape> areas) {

		MODE_TYPE mode = MODE_TYPE.BIKE; 
		double speedThreshold = BIKE_SPEED_THRESHOLD, timeThreshold = 10 * 1000, minTrackThreshold = 60*1000, avgSpeedThreshold = BIKE_AVG_SPEED_THRESHOLD, guaranteedAvgSpeedThreshold = BIKE_GUARANTEED_AVG_SPEED_THRESHOLD; 
		ValidationStatus status = validateFreeMode(track, areas, mode, speedThreshold, timeThreshold, minTrackThreshold, avgSpeedThreshold, guaranteedAvgSpeedThreshold, BIKE_DISTANCE_THRESHOLD);

		if (TravelValidity.INVALID.equals(status.getValidationOutcome()) && ERROR_TYPE.TOO_SHORT.equals(status.getError())) {
			status.setError(ERROR_TYPE.DOES_NOT_MATCH);
		}
		return status;
	}

	
	private static ValidationStatus validateFreeMode(
			Collection<Geolocation> track,
			List<Shape> areas, 
			MODE_TYPE mode,
			double speedThreshold, 
			double timeThreshold, 
			double minTrackThreshold, 
			double avgSpeedThreshold,
			double guaranteedAvgSpeedThreshold,
			double distanceThreshold) {
		ValidationStatus status = new ValidationStatus();
		// set parameters
		status.setTripType(TRIP_TYPE.FREE);
		status.setModeType(mode);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setCoverageThreshold(COVERAGE_THRESHOLD);

		// basic validation
		List<Geolocation> points = prevalidate(track, status, areas, distanceThreshold);
		if (status.getValidationOutcome() != null) {
			return status;
		}
		
		// two points
		if (points.size() == 2) {
			status.setValidationOutcome(TravelValidity.INVALID);
			status.setError(ERROR_TYPE.NO_DATA);
//					long estimate = (long) (status.getDistance() * 1.5);
//					long speed = estimate / status.getDuration();
//					if (speed > avgSpeedThreshold) {
//						status.setValidationOutcome(TravelValidity.INVALID);
//						status.setError(ERROR_TYPE.TOO_FAST);
//					} else {
//						status.setValidationOutcome(TravelValidity.VALID);
//						status.setEffectiveDistances(Collections.singletonMap(mode, status.getDistance()));
//					} 
			
		}
		
		// split track into pieces. 
		TrackSplit trackSplit = TrackSplit.slowSplit(points, speedThreshold, timeThreshold, minTrackThreshold);
		// if no slow intervals or no fast intervals and speed is high, invalid
		if (trackSplit.getSlowIntervals().isEmpty() || trackSplit.getFastIntervals().isEmpty()) {
			if (status.getAverageSpeed() > avgSpeedThreshold) {
				status.setValidationOutcome(TravelValidity.INVALID);
				status.setError(ERROR_TYPE.TOO_FAST);
				return status;
			}	
		}
		status.updateSlowSplit(trackSplit, false);
		// distance should be non-trivial
		double distance = status.getEffectiveDistances().get(mode);
		// min distance 
		if (distance < distanceThreshold) {
			// check the average speed of fast part.
			int fastPart = trackSplit.getSlowIntervals().isEmpty() ? 0 : trackSplit.getSlowIntervals().getFirst()[0];
			if (getFragmentEffectiveAverage(points, fastPart, points.size()) > avgSpeedThreshold) {
				status.setValidationOutcome(TravelValidity.INVALID);
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
				return status;
			}
		}
		status.setValidationOutcome(status.getAverageSpeed() <= guaranteedAvgSpeedThreshold ? TravelValidity.VALID: TravelValidity.PENDING);
		
		return status;
	}
	/**
	 * Validate planned trip
	 * @param track
	 * @param areas
	 * @return
	 */
	public static ValidationStatus validatePlanned(Collection<Geolocation> track, Itinerary itinerary, List<Shape> areas) {
		ValidationStatus status = new ValidationStatus();
		// set parameters
		status.setTripType(TRIP_TYPE.PLANNED);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setMatchThreshold(ACCURACY_THRESHOLD);

		// basic validation
		List<Geolocation> points = prevalidate(track, status, areas, DISTANCE_THRESHOLD);
		if (status.getValidationOutcome() != null) {
			if (ERROR_TYPE.TOO_SHORT.equals(status.getError())) {
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
			}
			return status;
		}
		
		status.setValidationOutcome(TravelValidity.PENDING);
		if (itinerary != null) {
			status.updatePlannedDistances(itinerary);
			List<List<Geolocation>> legTraces = itinerary.getLeg().stream().map(leg -> GamificationHelper.decodePoly(leg)).collect(Collectors.toList());
			points = fillTrace(points, 100.0 / 1000 / 2 / Math.sqrt(2));
			// check leg coverage: if is more than threshold (e.g., 80%) - valid, if less than minimum threshold - invalid. Otherwise pending
			double matchedLength = 0, minMatchedLength = 0, totalLength = 0;
			for (int i = 0; i < legTraces.size(); i++) {
				Leg leg = itinerary.getLeg().get(i);
				double legLength = leg.getLength();
				List<Geolocation> legTrace = legTraces.get(i);
				int effectiveLength = legTrace.size();
				int invalid = trackMatch(legTrace, points, status.getMatchThreshold());
				double subtrackPrecision =  100.0 * (effectiveLength-invalid) / (effectiveLength);
				if (subtrackPrecision > COVERAGE_THRESHOLD) {
					matchedLength += legLength;
				}
				minMatchedLength += legLength * subtrackPrecision / 100.0;
				totalLength += legLength;
			}
			if ((100.0 * matchedLength / totalLength) > COVERAGE_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.VALID);
			}
			if ((100.0 * minMatchedLength / totalLength) < MIN_COVERAGE_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.INVALID);
			}
		} 
		return status;
	}
	

	/**
	 * @param key
	 * @return
	 */
	public static String toModeString(MODE_TYPE key) {
		return key.name().toLowerCase();
	}
	public static MODE_TYPE toModeType(String key) {
		switch(key) {
		case "bus":
			return MODE_TYPE.BUS;
		case "train": 
			return MODE_TYPE.TRAIN;
		case "bike":
			return MODE_TYPE.BIKE;
		case "walk":
			return MODE_TYPE.WALK;
		default:
			return MODE_TYPE.OTHER;
		}
	}
	private static final double getFragmentEffectiveAverage(List<Geolocation> points, int start, int end) {
		double realTime = 0;
		double remainingDistance = 0;
		double prevDist = 0;
		for (int i = start+1; i < end; i++) {
			double d = GamificationHelper.harvesineDistance(points.get(i), points.get(i - 1));
			remainingDistance += d;
			// effective average
			long t = points.get(i).getRecorded_at().getTime() - points.get(i-1).getRecorded_at().getTime();
			if (t > 0) {
				d += prevDist;
				double pointSpeed = (1000.0 * d / ((double) t / 1000)) * 3.6;
				// not still
				if (pointSpeed > 0.1 && pointSpeed < 144) {
					realTime += t;
				}
				prevDist = 0;
			} else {
				prevDist = d;
			}
		}
		realTime = realTime * 0.001;
		return ( remainingDistance / (realTime)) * 3.6;
		
	}
	
	/**
	 * Verify the trac1 matches track2 with a given precision using Hausdorff-based analysis.
	 * @param track1 track to match
	 * @param track2 track with which to perform match
	 * @param error in meters
	 * @return true if the Hausdorff distance is less than specified precision
	 */
	private static int trackMatch(List<Geolocation> track1, List<Geolocation> track2, double error) {
		return trackMatch(new MatchModel(track1, error), track2);
	}
	private static int trackMatch(MatchModel model, List<Geolocation> track2) {

		int matching = 0;
		Map<Integer,Integer> matrix = new HashMap<>(model.matrix);
		
		// fill in the matrix from reference track. include also additional cells
		for (Geolocation g : track2) {
			if (g.getLatitude() > model.nw[0] || g.getLatitude() < model.se[0] || g.getLongitude() < model.nw[1] || g.getLongitude() > model.se[1]) continue;
			
			int row = row(model.se[0], g.getLatitude(), g.getLongitude(), model.distance);
			int col = col(model.nw[1], g.getLatitude(), g.getLongitude(), model.distance);
			
			if (row >= model.height || col >= model.width) continue; // should never happen
						
			int idx = row*model.width+col;
			if ( matrix.getOrDefault(idx, 0) == 0)  matrix.put(idx, 2); // only 2nd track present
			else if ( matrix.getOrDefault(idx, 0) < 0)  matrix.put(idx, 3); // both present
			matching++;
		}
		if (matching == 0) return model.track.size();
			
		int invalidCount = 0;
		
		// check the cells with the test track items only: check 8 neighbors
		for (Integer idx : matrix.keySet()) {
			if (matrix.get(idx) < 0 &&
				matrix.getOrDefault(idx-model.width, 0) < 2 && matrix.getOrDefault(idx+model.width, 0) < 2 &&
				matrix.getOrDefault(idx-model.width-1, 0) < 2 && matrix.getOrDefault(idx+model.width-1, 0) < 2 &&
				matrix.getOrDefault(idx-model.width+1, 0) < 2 && matrix.getOrDefault(idx+model.width+1, 0) < 2 &&
				matrix.getOrDefault(idx-1, 0) < 2 && matrix.getOrDefault(idx+1, 0) < 2
				) 
			{
				invalidCount -= matrix.get(idx);
				//return false;
			}
		}
		return invalidCount;
	}
	private static int col(double w, double lat, double lon, double distance) {
		return (int) Math.ceil(GamificationHelper.harvesineDistance(lat, w, lat, lon) / distance);
	}
	private static int row(double s, double lat, double lon, double distance) {
		return (int) Math.ceil(GamificationHelper.harvesineDistance(s, lon, lat, lon) / distance);
	}

	public static List<List<Geolocation>> parseShape(InputStream is) {
		return new BufferedReader(new InputStreamReader(is))
		.lines()
		.map(s -> s.split(","))
		.filter(a -> !a[0].isEmpty() && !a[0].equals("shape_id"))
		.collect(Collectors.groupingBy(a -> a[0]))
		.values()
		.stream()
		.map(list -> {
			return list.stream()
					.sorted((a,b) -> Integer.parseInt(a[3]) - Integer.parseInt(b[3]))
					.map(a -> new Geolocation(Double.parseDouble(a[1]), Double.parseDouble(a[2]), null))
					.collect(Collectors.toList());
		})
		.map(track -> fillTrace(track, 100.0 / 1000 / 2 / Math.sqrt(2)))
		.collect(Collectors.toList());
		
	}
	
	public static List<Geolocation> fillTrace(List<Geolocation> track, double distance) {
		List<Geolocation> res = new LinkedList<>();
		res.add(track.get(0));
		// preprocess the reference track: add extra points 
		for (int i = 1; i < track.size(); i++) {
			double d = GamificationHelper.harvesineDistance(track.get(i), track.get(i-1));
			for (int j = 1; j * distance < d; j++) {
				double lng = track.get(i-1).getLongitude() + (j*distance)*(track.get(i).getLongitude() - track.get(i-1).getLongitude()) / d; 
				double lat = track.get(i-1).getLatitude() + (j*distance)*(track.get(i).getLatitude() - track.get(i-1).getLatitude()) / d; 
				res.add(new Geolocation(lat, lng, null));
			}
			res.add(track.get(i));
		}
		
		return res;
	}
	
	/**
	 * Validate planned trip
	 * @param track
	 * @param areas
	 * @return
	 */
	public static ValidationStatus validateSharedPassenger(Collection<Geolocation> passengerTrack, Collection<Geolocation> driverTrack, List<Shape> areas) {
		ValidationStatus status = new ValidationStatus();
		// set parameters
		status.setTripType(TRIP_TYPE.SHARED);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setMatchThreshold(ACCURACY_THRESHOLD);

		// basic validation
		List<Geolocation> points = prevalidate(passengerTrack, status, areas, SHARED_TRIP_DISTANCE_THRESHOLD);
		if (status.getValidationOutcome() != null) {
			if (ERROR_TYPE.TOO_SHORT.equals(status.getError())) {
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
			}
			return status;
		}
		List<Geolocation> driverPoints = prevalidate(driverTrack, status, areas, SHARED_TRIP_DISTANCE_THRESHOLD);
		if (status.getValidationOutcome() != null) {
			if (ERROR_TYPE.TOO_SHORT.equals(status.getError())) {
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
			}
			return status;
		}
		
		status.setValidationOutcome(TravelValidity.PENDING);
		
		
		if (driverTrack != null) {
			points = fillTrace(points, 100.0 / 1000 / 2 / Math.sqrt(2));
			// check leg coverage: if is more than threshold (e.g., 80%) - valid, if less than minimum threshold - invalid. Otherwise pending
			double matchedLength = 0, minMatchedLength = 0, totalLength = 0;
			int effectiveLength = driverPoints.size();
			int invalid = trackMatch(driverPoints, points, status.getMatchThreshold());
			double subtrackPrecision =  100.0 * (effectiveLength-invalid) / (effectiveLength);
			if (subtrackPrecision > COVERAGE_THRESHOLD) {
				matchedLength = status.getDistance();
			}
			minMatchedLength = matchedLength * subtrackPrecision / 100.0;
			totalLength = status.getDistance();

			if ((100.0 * matchedLength / totalLength) > COVERAGE_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.VALID);
			}
			if ((100.0 * minMatchedLength / totalLength) < MIN_COVERAGE_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.INVALID);
			}
		} 
		return status;
	}
	
	/**
	 * Validate planned trip
	 * @param track
	 * @param areas
	 * @return
	 */
	public static ValidationStatus validateSharedDriver(Collection<Geolocation> driverTrack, List<Shape> areas) {
		ValidationStatus status = new ValidationStatus();
		// set parameters
		status.setTripType(TRIP_TYPE.SHARED);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setMatchThreshold(ACCURACY_THRESHOLD);

		// basic validation
		prevalidate(driverTrack, status, areas, SHARED_TRIP_DISTANCE_THRESHOLD);
		if (status.getValidationOutcome() != null) {
			if (ERROR_TYPE.TOO_SHORT.equals(status.getError())) {
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
			}
			return status;
		}

		status.setValidationOutcome(TravelValidity.PENDING);
		return status;
	}


	private static class MatchModel {
		
		private List<Geolocation> track;
		private double distance;
		private double[] nw = new double[]{Double.MIN_VALUE, Double.MAX_VALUE}, se = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
		private int width = 0, height = 0;
		private Map<Integer,Integer> matrix = new HashMap<>();

		
		public MatchModel(List<Geolocation> track, double error) {
			super();
			this.track = track;
			distance = error / 1000 / 2 / Math.sqrt(2);
			
			// consider track1 is shorter and is used as a reference for matrix construction
			// identify matrix coordinates
			track.forEach(g -> {
				nw[0] = Math.max(nw[0], g.getLatitude()); nw[1] = Math.min(nw[1], g.getLongitude());
				se[0] = Math.min(se[0], g.getLatitude()); se[1] = Math.max(se[1], g.getLongitude());
			});
			
			// add extra row/col to matrix 
			width = 2 + (int) Math.ceil(GamificationHelper.harvesineDistance(nw[0],nw[1], nw[0], se[1]) / distance);
			height = 2 + (int) Math.ceil(GamificationHelper.harvesineDistance(nw[0],nw[1], se[0], nw[1]) / distance);
			
			// represent the cells with values to avoid sparse matrix traversal
			// fill in the matrix from test track
			track.forEach(g -> {
				int row = row(se[0], g.getLatitude(), g.getLongitude(), distance);
				int col = col(nw[1], g.getLatitude(), g.getLongitude(), distance);
				if (row >= height || col >= width) return; // should never happen
				matrix.put(row*width+col, matrix.getOrDefault(row*width+col,0)-1); // only first track present
			});		
		}

		
	}

}
