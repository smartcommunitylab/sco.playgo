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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

/**
 * @author raman
 *
 */
public class TrackSplit {

	private double speedThreshold;
	private double timeThreshold;
	private double minTrackThreshold;
	
	private List<Geolocation> track;
	private LinkedList<int[]> slowIntervals = new LinkedList<int[]>(), fastIntervals = new LinkedList<int[]>();
	
	
	public static TrackSplit fastSplit(List<Geolocation> track, double speedThreshold, double timeThreshold, double minTrackThreshold) {
		TrackSplit split = new TrackSplit(track, speedThreshold, timeThreshold, minTrackThreshold);
		split.doFastSplit(track, speedThreshold, timeThreshold, minTrackThreshold);
		return split;
	}
	public static TrackSplit slowSplit(List<Geolocation> track, double speedThreshold, double timeThreshold, double minTrackThreshold) {
		TrackSplit split = new TrackSplit(track, speedThreshold, timeThreshold, minTrackThreshold);
		split.doSlowSplit(track, speedThreshold, timeThreshold, minTrackThreshold);
		return split;
	}
	
	private TrackSplit(List<Geolocation> track, double speedThreshold, double timeThreshold, double minTrackThreshold) {
		super();
		this.track = track;
		this.speedThreshold = speedThreshold;
		this.timeThreshold = timeThreshold;
		this.minTrackThreshold = minTrackThreshold;
//		doFastSplit(track, speedThreshold, timeThreshold, minTrackThreshold);
	}

	private void doFastSplit(List<Geolocation> track, double speedThreshold, double timeThreshold, double minTrackThreshold) {
		Boolean slow = null;
		Long stopStartTime = null;
		Integer stopStartIdx = null;
		
		double prevDist  = 0;
		for (int i = 1; i < track.size(); i++) {
			double d = GamificationHelper.harvesineDistance(track.get(i), track.get(i - 1));
			long t = track.get(i).getRecorded_at().getTime() - track.get(i - 1).getRecorded_at().getTime();
//			System.err.println("t = "+t+", dist = "+d);
			if (t > 0) {
				d += prevDist;
				double speed = (1000.0 * d / ((double) t / 1000)) * 3.6;
//				System.err.println("speed = "+speed+", dist = "+d);
				// going fast, switch modality
				if (speed >= speedThreshold) {
					stopStartTime = null;
					stopStartIdx = null;
					if (slow == null || slow) {
						if (slowIntervals.size() > 0) slowIntervals.getLast()[1] = i-1; 
						int[] fragment = new int[2];
						fragment[0] = i-1;
						fastIntervals.add(fragment);
						slow = false;
					}					
				} 
				// going slow, first fragment
				else if (slow == null) {
					slow = true;
					int[] fragment = new int[2];
					fragment[0] = i-1;
					slowIntervals.add(fragment);
				} 
				// if speed == 0, remove this fragment from time threshold check
				else if (speed == 0 && stopStartTime != null) {
					stopStartTime += t;
				}				
				// going slow within potentially fast fragment
				else if (slow == false) {
					// set the stop start if not set
					if (stopStartTime == null) {
						stopStartTime = track.get(i - 1).getRecorded_at().getTime();
						stopStartIdx = i-1;
					}
					// if more than threshold, record this as a new slow fragment
					if (track.get(i).getRecorded_at().getTime() - stopStartTime > timeThreshold) {
						slow = true;

						fastIntervals.getLast()[1] = stopStartIdx; 
						int[] fragment = new int[2];
						fragment[0] = stopStartIdx;
						slowIntervals.add(fragment);
						stopStartTime = null;
						stopStartIdx = null;
					}
				}
				prevDist = 0;
			} else {
				prevDist = d;
			}
		}
		if (slow) {
			slowIntervals.getLast()[1] = track.size();
		}
		else fastIntervals.getLast()[1] = track.size();
		
		fastIntervals = new LinkedList<>(fastIntervals.stream().filter(intv -> track.get(intv[1]-1).getRecorded_at().getTime() - track.get(intv[0]).getRecorded_at().getTime() > minTrackThreshold).collect(Collectors.toList()));
	}
	private void doSlowSplit(List<Geolocation> track, double speedThreshold, double timeThreshold, double minTrackThreshold) {
		Boolean fast = null;
		Long fastStartTime = null;
		Integer fastStartIdx = null;
		
		double prevDist  = 0;
		for (int i = 1; i < track.size(); i++) {
			double d = GamificationHelper.harvesineDistance(track.get(i), track.get(i - 1));
			long t = track.get(i).getRecorded_at().getTime() - track.get(i - 1).getRecorded_at().getTime();
			if (t > 0) {
				d += prevDist;
				double speed = (1000.0 * d / ((double) t / 1000)) * 3.6;
				if (t > 10 * 60 * 1000 && d > 0.1) {
					speed = speedThreshold;
				}
//				System.err.println(speed);;
				// going slow, switch modality
				if (speed < speedThreshold) {
					fastStartTime = null;
					fastStartIdx = null;
					if (fast == null || fast) {
						if (fast != null) {
							// exclude noise: peaks that do not move significantly
							Geolocation refPoint = slowIntervals.size() > 0 ? track.get(slowIntervals.getLast()[1]) : track.get(fastIntervals.getLast()[0]);
							Geolocation refPoint2 = slowIntervals.size() > 0 ? track.get(slowIntervals.getLast()[0]) : track.get(fastIntervals.getLast()[0]);							
							double distance = GamificationHelper.harvesineDistance(track.get(i), refPoint);
							double distance2 = GamificationHelper.harvesineDistance(track.get(i), refPoint2);
							if (distance > 0.1 && distance2 > 0.1) {
								fastIntervals.getLast()[1] = i-1; 								
								int[] fragment = new int[2];
								fragment[0] = i-1;
								slowIntervals.add(fragment);
							} else {
								fastIntervals.removeLast();
								if (slowIntervals.size() == 0) {
									slowIntervals.add(new int[]{0,0});
								}
							}
						} else {
							int[] fragment = new int[2];
							fragment[0] = i-1;
							slowIntervals.add(fragment);
						}
						fast = false;
					}					
				} 
				// going fast, first fragment
				else if (fast == null) {
					fast = true;
					int[] fragment = new int[2];
					fragment[0] = i-1;
					fastIntervals.add(fragment);
				} 
				// going fast within potentially slow fragment
				else if (fast == false) {
					// set the fast move start if not set
					if (fastStartTime == null) {
						fastStartTime = track.get(i - 1).getRecorded_at().getTime();
						fastStartIdx = i-1;
					}
					// if more than threshold, record this as a new slow fragment
					if (track.get(i).getRecorded_at().getTime() - fastStartTime > timeThreshold) {
						fast = true;

						slowIntervals.getLast()[1] = fastStartIdx; 
						int[] fragment = new int[2];
						fragment[0] = fastStartIdx;
						fastIntervals.add(fragment);
						fastStartTime = null;
						fastStartIdx = null;
					}
				}
				prevDist = 0;
			} else {
				prevDist = d;
			}
		}
		if (fast != null && fast) {
			fastIntervals.getLast()[1] = track.size();
		}
		else slowIntervals.getLast()[1] = track.size();
		
		slowIntervals = new LinkedList<>(slowIntervals.stream().filter(intv -> track.get(intv[1]-1).getRecorded_at().getTime() - track.get(intv[0]).getRecorded_at().getTime() > minTrackThreshold).collect(Collectors.toList()));
		
//		slowIntervals.stream().forEach(x -> {
//			System.err.println("\t" + x[0] + "," + x[1] + " / ");
//		});
//		fastIntervals.stream().forEach(x -> {
//			System.err.println("\t" + x[0] + "," + x[1] + " // ");
//		});		
	}	
	public List<List<Geolocation>> slowFragments(){
		List<List<Geolocation>> res = new ArrayList<List<Geolocation>>();
		slowIntervals.forEach(fragment -> res.add(track.subList(fragment[0], fragment[1])));
		return res;
	}
	public List<List<Geolocation>> fastFragments(){
		List<List<Geolocation>> res = new ArrayList<List<Geolocation>>();
		fastIntervals.forEach(fragment -> res.add(track.subList(fragment[0], fragment[1])));
		return res;
	}

	public double getSpeedThreshold() {
		return speedThreshold;
	}

	public double getTimeThreshold() {
		return timeThreshold;
	}
	
	public double getMinTrackThreshold() {
		return minTrackThreshold;
	}

	public List<Geolocation> getTrack() {
		return track;
	}

	public LinkedList<int[]> getSlowIntervals() {
		return slowIntervals;
	}

	public LinkedList<int[]> getFastIntervals() {
		return fastIntervals;
	}

}
