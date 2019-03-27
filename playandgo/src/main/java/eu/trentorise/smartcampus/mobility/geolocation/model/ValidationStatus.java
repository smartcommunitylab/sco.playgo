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

package eu.trentorise.smartcampus.mobility.geolocation.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

/**
 * @author raman
 *
 */
public class ValidationStatus {

	public  static final int ACCURACY_THRESHOLD = 100;

	private static final SimpleDateFormat DT_FORMATTER = new SimpleDateFormat("HH:mm:ss");

	public enum TRIP_TYPE {FREE, PLANNED};
	public enum MODE_TYPE {WALK, BIKE, BUS, TRAIN, MULTI, OTHER, CAR};
	
	public enum ERROR_TYPE {TOO_SHORT, TOO_SLOW, TOO_FAST, OUT_OF_AREA, DOES_NOT_MATCH, DATA_HOLE, NO_DATA};
	
	private TRIP_TYPE tripType;
	private MODE_TYPE modeType;
	
	private long duration; // seconds
	private double distance; // meters
	private int locations; // number of geopoints
	
	private double averageSpeed; // km/h
	private double maxSpeed; // km/h
	
	private double accuracyRank; // percentage of points with high precision (< 50m)
	
	// in case of planned trip, distances mapped on transport modes as of plan
	private Map<MODE_TYPE, Double> plannedDistances;
	// in case of planned trip, actual distances mapped on transport modes
	// in case of free trip, actual distances out of measurements
	private Map<MODE_TYPE, Double> effectiveDistances;
	
	// split data: thresholds, speed, intervals
	private double splitSpeedThreshold;
	private double splitStopTimeThreshold;
	private double splitMinFastDurationThreshold; 
	// validation data: validity match threshold
	private double validityThreshold;// percentage of coverage
	private double matchThreshold; // Hausdorff distance for track match
	private double coverageThreshold; // percentage of distance matched to consider valid
	
	private List<Interval> intervals;
	private int matchedIntervals;

	private TravelValidity validationOutcome;
	
	private ERROR_TYPE error;
	
	private boolean certified;
	
	private String polyline;
	
	/**
	 * Compute percentage of points with high accuracy
	 * @param points
	 */
	public void computeAccuracy(List<Geolocation> points) {
		int accurate = 0;
		for (Geolocation point: points) {
			if (point.getAccuracy() < ACCURACY_THRESHOLD) {
				accurate++;
			}
		}
		this.accuracyRank = 100.0 * accurate / points.size();
	}
	/**
	 * Compute track properties: duration, distance, number of locations, max and average speed
	 * @param points
	 */
	public void updateMetrics(List<Geolocation> points) {
		this.duration = points.size() < 2 ? 0: (points.get(points.size()-1).getRecorded_at().getTime() - points.get(0).getRecorded_at().getTime()) / 1000;
		this.distance = 0;
		this.locations = points.size() / 2 + 1;
		
		this.maxSpeed = 0;
	
		
		// consider max speed as an average speed for intervals of 5
		double intervalDistance = 0;
		double realTime = 0, realDist = 0;
		double prevDist = 0;
		for (int i = 1; i < points.size(); i++) {
			double d = GamificationHelper.harvesineDistance(points.get(i), points.get(i - 1));
			this.distance += 1000.0 * d;
			if (i % 5 == 0) {
				double speed = (1000.0 * intervalDistance / ((double) (points.get(i-1).getRecorded_at().getTime() - points.get(i-5).getRecorded_at().getTime()) / 1000)) * 3.6;
				maxSpeed = Math.max(speed, maxSpeed);
				intervalDistance = 0;					
			}
			intervalDistance += d;
			// effective average
			long t = points.get(i).getRecorded_at().getTime() - points.get(i-1).getRecorded_at().getTime();
			if (t > 0) {
				d += prevDist;
				double pointSpeed = (1000.0 * d / ((double) t / 1000)) * 3.6;
				// not still
				if (pointSpeed > 0.1 && pointSpeed < 144) {
					realTime += t;
					realDist += d;
				}
				prevDist = 0;
			} else {
				prevDist = d;
			}
			if (!StringUtils.isEmpty(points.get(i).getCertificate())) {
				setCertified(true);
			}
		}
		realTime = realTime * 0.001;
		if (realTime > 0) averageSpeed = ( realDist*1000.0 / ((double) realTime)) * 3.6;
	}
	


	/**
	 * Update stats from split data considering fast tracks
	 * @param trackSplit
	 */
	public void updateFastSplit(TrackSplit trackSplit, double maxSpeedThreshold) {
		splitMinFastDurationThreshold = trackSplit.getMinTrackThreshold();
		splitSpeedThreshold = trackSplit.getSpeedThreshold();
		splitStopTimeThreshold = trackSplit.getTimeThreshold();
		intervals = new ArrayList<ValidationStatus.Interval>(trackSplit.getFastIntervals().size());
		double fastDistance = 0;
		for (int[] range: trackSplit.getFastIntervals()) {
			Interval interval = new Interval();
			interval.setStart(range[0]);
			interval.setEnd(range[1]);
			List<Geolocation> subtrack = trackSplit.getTrack().subList(range[0], range[1]);
			double intervalDistance = 0;
			for (int i = 1; i < subtrack.size(); i++) {
				intervalDistance += 1000.0*GamificationHelper.harvesineDistance(subtrack.get(i), subtrack.get(i-1));
			}
			interval.setStartTime(subtrack.get(0).getRecorded_at().getTime());
			interval.setEndTime(subtrack.get(subtrack.size()-1).getRecorded_at().getTime());
			double speed = 3.6 * 1000.0 * intervalDistance / (interval.getEndTime() - interval.getStartTime()); 
			if (speed > maxSpeedThreshold) {
				continue;
			}
			
			fastDistance += intervalDistance;
			interval.setDistance(intervalDistance);
			intervals.add(interval);
		}
		effectiveDistances = new HashMap<>();
		effectiveDistances.put(modeType, fastDistance);
		effectiveDistances.put(MODE_TYPE.OTHER, distance - fastDistance);
	}
	
	/**
	 * Update stats from split data considering slow tracks
	 * @param trackSplit
	 * @param firstonly
	 */
	public void updateSlowSplit(TrackSplit trackSplit, boolean firstonly) {
		splitMinFastDurationThreshold = trackSplit.getMinTrackThreshold();
		splitSpeedThreshold = trackSplit.getSpeedThreshold();
		splitStopTimeThreshold = trackSplit.getTimeThreshold();
		intervals = new ArrayList<ValidationStatus.Interval>(trackSplit.getSlowIntervals().size());
		double slowDistance = 0;
		for (int[] range: trackSplit.getSlowIntervals()) {
			Interval interval = new Interval();
			interval.setStart(range[0]);
			interval.setEnd(range[1]);
			List<Geolocation> subtrack = trackSplit.getTrack().subList(range[0], range[1]);
			double intervalDistance = 0;
			for (int i = 1; i < subtrack.size(); i++) {
				intervalDistance += 1000.0*GamificationHelper.harvesineDistance(subtrack.get(i), subtrack.get(i-1));
			}
			slowDistance += intervalDistance;
			interval.setDistance(intervalDistance);
			interval.setStartTime(subtrack.get(0).getRecorded_at().getTime());
			interval.setEndTime(subtrack.get(subtrack.size()-1).getRecorded_at().getTime());
			intervals.add(interval);
			if (firstonly) break;
		}
		effectiveDistances = new HashMap<>();
		effectiveDistances.put(modeType, slowDistance);
		effectiveDistances.put(MODE_TYPE.OTHER, distance - slowDistance);
	}
	
	public void updatePlannedDistances(Itinerary itinerary) {
		final Map<TType, Double> map = itinerary.getLeg().stream().collect(Collectors.groupingBy(leg -> leg.getTransport().getType(), Collectors.summingDouble(Leg::getLength)));
		final Map<MODE_TYPE, Double> plannedDistances = new HashMap<>();
		map.entrySet().forEach(entry -> plannedDistances.put(toModeType(entry.getKey()), entry.getValue()));
		setPlannedDistances(plannedDistances);
		
	}
	
	/**
	 * @param key
	 * @return
	 */
	private MODE_TYPE toModeType(TType key) {
		switch(key) {
		case CAR:
		case CARWITHPARKING:
		case SHAREDCAR:
		case SHAREDCAR_WITHOUT_STATION:
			return MODE_TYPE.CAR;
		case TRAIN: 
			return MODE_TYPE.TRAIN;
		case BICYCLE:
		case SHAREDBIKE:
		case SHAREDBIKE_WITHOUT_STATION:
			return MODE_TYPE.BIKE;
		case WALK:
			return MODE_TYPE.WALK;
		default:
			return MODE_TYPE.BUS;
		}
	}
	
	public TRIP_TYPE getTripType() {
		return tripType;
	}


	public void setTripType(TRIP_TYPE tripType) {
		this.tripType = tripType;
	}


	public MODE_TYPE getModeType() {
		return modeType;
	}


	public void setModeType(MODE_TYPE modeType) {
		this.modeType = modeType;
	}


	public long getDuration() {
		return duration;
	}


	public void setDuration(long duration) {
		this.duration = duration;
	}


	public double getDistance() {
		return distance;
	}


	public void setDistance(double distance) {
		this.distance = distance;
	}


	public int getLocations() {
		return locations;
	}


	public void setLocations(int locations) {
		this.locations = locations;
	}


	public double getAverageSpeed() {
		return averageSpeed;
	}


	public void setAverageSpeed(double averageSpeed) {
		this.averageSpeed = averageSpeed;
	}


	public double getMaxSpeed() {
		return maxSpeed;
	}


	public void setMaxSpeed(double maxSpeed) {
		this.maxSpeed = maxSpeed;
	}


	public Map<MODE_TYPE, Double> getPlannedDistances() {
		if (plannedDistances == null) plannedDistances = new HashMap<>();
		return plannedDistances;
	}


	public void setPlannedDistances(Map<MODE_TYPE, Double> plannedDistances) {
		this.plannedDistances = plannedDistances;
	}


	public Map<MODE_TYPE, Double> getEffectiveDistances() {
		if (effectiveDistances == null) effectiveDistances = new HashMap<>();
		return effectiveDistances;
	}


	public void setEffectiveDistances(Map<MODE_TYPE, Double> effectiveDistances) {
		this.effectiveDistances = effectiveDistances;
	}


	public double getSplitSpeedThreshold() {
		return splitSpeedThreshold;
	}


	public void setSplitSpeedThreshold(double splitSpeedThreshold) {
		this.splitSpeedThreshold = splitSpeedThreshold;
	}


	public double getSplitStopTimeThreshold() {
		return splitStopTimeThreshold;
	}


	public void setSplitStopTimeThreshold(double splitStopTimeThreshold) {
		this.splitStopTimeThreshold = splitStopTimeThreshold;
	}


	public double getSplitMinFastDurationThreshold() {
		return splitMinFastDurationThreshold;
	}


	public void setSplitMinFastDurationThreshold(double splitMinFastDurationThreshold) {
		this.splitMinFastDurationThreshold = splitMinFastDurationThreshold;
	}


	public double getValidityThreshold() {
		return validityThreshold;
	}


	public void setValidityThreshold(double validityThreshold) {
		this.validityThreshold = validityThreshold;
	}

	public double getMatchThreshold() {
		return matchThreshold;
	}



	public void setMatchThreshold(double matchThreshold) {
		this.matchThreshold = matchThreshold;
	}


	public double getCoverageThreshold() {
		return coverageThreshold;
	}



	public void setCoverageThreshold(double coverageThreshold) {
		this.coverageThreshold = coverageThreshold;
	}



	public List<Interval> getIntervals() {
		return intervals;
	}


	public void setIntervals(List<Interval> intervals) {
		this.intervals = intervals;
	}

	public int getMatchedIntervals() {
		return matchedIntervals;
	}



	public void setMatchedIntervals(int matchedIntervals) {
		this.matchedIntervals = matchedIntervals;
	}



	public TravelValidity getValidationOutcome() {
		return validationOutcome;
	}


	public void setValidationOutcome(TravelValidity validationOutcome) {
//		if (TravelValidity.PENDING.equals(validationOutcome)) {
//			this.validationOutcome = TravelValidity.VALID;
//		} else {
//			this.validationOutcome = validationOutcome;
//		}
		this.validationOutcome = validationOutcome;
	}

	public String getPolyline() {
		return polyline;
	}
	public void setPolyline(String polyline) {
		this.polyline = polyline;
	}
	public ERROR_TYPE getError() {
		return error;
	}

	public void setError(ERROR_TYPE error) {
		this.error = error;
	}


	public double getAccuracyRank() {
		return accuracyRank;
	}

	public void setAccuracyRank(double accuracyRank) {
		this.accuracyRank = accuracyRank;
	}

	public boolean isCertified() {
		return certified;
	}
	public void setCertified(boolean certified) {
		this.certified = certified;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ValidationStatus: \n");
		sb.append(String.format("      stats (duration / length / points / dist freq. / time freq. / accuracy): %d / %.0f / %d / %.4f / %.4f / %.2f\n", duration, (float)distance, locations, (float)(distance > 0 ? 1000.0 * locations / distance : 0), (float)(duration > 0 ? 60.0 * locations / duration : 0), (float) accuracyRank));
		sb.append(String.format("      speed (average / max): %.2f / %.2f\n", averageSpeed, maxSpeed));
		if (effectiveDistances != null){
			sb.append(			"      effective distances: "+effectiveDistances+"\n");
		}
		if (plannedDistances != null){
			sb.append(			"      planned distances: "+plannedDistances+"\n");
		}
		if (intervals != null){
			sb.append(String.format("      intervals: %d / %d\n", matchedIntervals, intervals.size()));
			for (Interval interval : intervals){
				sb.append(String.format("      		%s - %s: %.2f (%.2f%% of points, %.2f%% of distance)\n",DT_FORMATTER.format(new Date(interval.getStartTime())), DT_FORMATTER.format(new Date(interval.getEndTime())), interval.getMatch(), (float)(100.0*(interval.getEnd()-interval.getStart())/2/locations), interval.getDistance()/distance*100));
			}
		}
		sb.append(	          "      outcome: "); sb.append(validationOutcome);
		if (error != null) {
			sb.append(" (");sb.append(error); sb.append(")");
		}
		return sb.toString();
	}




	public static class Interval {
		private int start, end;
		private long startTime, endTime;
		private double distance;
		private double match; // percent of validity
		public int getStart() {
			return start;
		}
		public void setStart(int start) {
			this.start = start;
		}
		public int getEnd() {
			return end;
		}
		public void setEnd(int end) {
			this.end = end;
		}
		public long getStartTime() {
			return startTime;
		}
		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}
		public long getEndTime() {
			return endTime;
		}
		public void setEndTime(long endTime) {
			this.endTime = endTime;
		}
		public double getDistance() {
			return distance;
		}
		public void setDistance(double distance) {
			this.distance = distance;
		}
		public double getMatch() {
			return match;
		}
		public void setMatch(double match) {
			this.match = match;
		}
		
	}

}
