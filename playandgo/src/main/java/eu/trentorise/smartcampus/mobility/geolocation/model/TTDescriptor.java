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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

/**
 * @author raman
 *
 */
public class TTDescriptor {

	private static final Pattern TIME_REG = Pattern.compile("(\\d{2}):(\\d{2}):\\d{2}");
	
	private static final Logger logger = LoggerFactory.getLogger(TTDescriptor.class);
	
	private static final int MAX_DESCRIPTORS = 5;
	
	private Map<Integer, String> routeMap = new HashMap<>();
	private Map<String, Integer> routeIDMap = new HashMap<>();
	private Map<String, Integer> shapeIDMap = new HashMap<>();
	private Map<String, Integer> stopIDMap = new HashMap<>();
	
	private Map<Integer, List<Geolocation>> shapeMap = new HashMap<>();
	private Map<Integer, Geolocation> stopMap = new HashMap<>();
	
	private Map<Integer, String> polylineMap = new HashMap<>();
	
	// map shapes to set of time 
	private ArrayListMultimap<Integer, int[]> shapeTimeMap = ArrayListMultimap.create();
	// map stopId to set of descriptors
	private Multimap<Integer, TTLineDescriptor> stopDescriptors = LinkedHashMultimap.create();
	// map matrix index to stopIds. Represent the cells with values to avoid sparse matrix traversal
	private Multimap<Integer,Integer> stopMatrix = LinkedHashMultimap.create();
	// matrix coordinates
	private double[] nw = new double[]{Double.MIN_VALUE, Double.MAX_VALUE}, se = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
	// cell size
	private double distance = 0;
	// dimensions
	int width = 0, height = 0;

	/**
	 * Load GTFS data. Take GTFS files and construct mapping of a stop to the passing lines/shapes.
	 * With this map create the area coverage matrix.
	 * @param stopSrc
	 * @param tripSrc
	 * @param stopTimeSrc
	 * @param shapeSrc
	 */
	public synchronized void load(InputStream stopSrc, InputStream tripSrc, InputStream stopTimeSrc, InputStream shapeSrc) {
		loadShapes(shapeSrc);
		loadStops(stopSrc);
		loadStopTimes(stopTimeSrc, loadTrips(tripSrc));
		
		buildPolylines();
	}
	
	private void buildPolylines() {
		shapeMap.entrySet().stream().forEach(x -> polylineMap.put(x.getKey(), GamificationHelper.encodePoly(x.getValue())));
	}

	/**
	 * Build a matrix of the area covered by the transit stops
	 * @param error for cell dimensions
	 */
	public synchronized void build(double error) {
		// normalize distance: in km and along coordinate
		distance = error / 1000 / 2 / Math.sqrt(2);
		
		// use all the stops as a reference for matrix construction
		// identify matrix coordinates
		stopMap.values().forEach(g -> {
			nw[0] = Math.max(nw[0], g.getLatitude()); nw[1] = Math.min(nw[1], g.getLongitude());
			se[0] = Math.min(se[0], g.getLatitude()); se[1] = Math.max(se[1], g.getLongitude());
		});
		
		width = (int) Math.ceil(GamificationHelper.harvesineDistance(nw[0],nw[1], nw[0], se[1]) / distance);
		height = (int) Math.ceil(GamificationHelper.harvesineDistance(nw[0],nw[1], se[0], nw[1]) / distance);
		
		// fill in the matrix from test track
		stopMap.keySet().forEach(key -> {
			final Geolocation g = stopMap.get(key);
			final int row = row(se[0], g.getLatitude(), g.getLongitude(), distance);
			final int col = col(nw[1], g.getLatitude(), g.getLongitude(), distance);
			if (row >= height || col >= width) return; // should never happen
			stopMatrix.put(row*width+col, key); 
		});
	}
	
	/**
	 * Select shapes corresponding to the lines matching the specified track
	 * @param track
	 * @return list of matching shapes
	 */
	public List<List<Geolocation>> filterShapes(Collection<Geolocation> track){
		if (stopDescriptors.size() == 0) {
			return null;
		}
		List<Geolocation> points = new ArrayList<>(track);
		Map<TTLineDescriptor, int[]> occurences = new HashMap<>();
		Collections.sort(points, (o1, o2) -> (int)(o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime()));

		HashSet<Integer> set = new HashSet<>();
		for (int i = 0; i < points.size(); i++) {
			Geolocation g = points.get(i);
			final int row = row(se[0], g.getLatitude(), g.getLongitude(), distance);
			final int col = col(nw[1], g.getLatitude(), g.getLongitude(), distance);
			
			set.addAll(stopMatrix.get(row*width+col));
			if (row > 0 && col > 0) set.addAll(stopMatrix.get((row - 1) * width + col - 1));
			if (row > 0) set.addAll(stopMatrix.get((row - 1) * width + col));
			if (row < height - 1 && col < width - 1) set.addAll(stopMatrix.get((row + 1) * width + col + 1));
			if (row < height - 1) set.addAll(stopMatrix.get((row + 1) * width + col));
			if (row < height - 1 && col > 0) set.addAll(stopMatrix.get((row+1) * width + col - 1));
			if (col > 0) set.addAll(stopMatrix.get(row * width + col - 1));
			if (row > 0 && col < width - 1) set.addAll(stopMatrix.get((row -1) * width + col + 1));
			if (col < width - 1) set.addAll(stopMatrix.get(row * width + col + 1));
			
			if (set != null)
				for (Integer stopId: set) {
					Collection<TTLineDescriptor> descriptors = stopDescriptors.get(stopId);
					if (descriptors != null)
						for (TTLineDescriptor descriptor : descriptors) {
							int[] occ = occurences.getOrDefault(descriptor, new int[]{0,Integer.MAX_VALUE,0});
							occ[0]++;
							occ[1] = Math.min(occ[1], i);
							occ[2] = i - occ[1];
							occurences.put(descriptor, occ);
						}
				}
			set.clear();
		}
		Collection<TTLineDescriptor> descriptors = null;
		// filter occurrences for time of shapes
		Set<TTLineDescriptor> keys = new HashSet<>(occurences.keySet());
		for(TTLineDescriptor d : keys) {
			if (!validTime(d.shape, points.get(0).getRecorded_at(), points.get(points.size()-1).getRecorded_at())) {
				occurences.remove(d);
			}
		} 
		
		if (occurences.size() > MAX_DESCRIPTORS) {
			List<TTLineDescriptor> list = new ArrayList<>(occurences.keySet());
			
			descriptors = filterOccurences(list,occurences,MAX_DESCRIPTORS);
			
		} else {
			descriptors = occurences.keySet();
		}

		
		return descriptors.stream().map(d -> shapeMap.get(d.shape)).collect(Collectors.toList());
	}
	
	/**
	 * Select polylines corresponding to the lines matching the specified track
	 * @param track
	 * @return list of matching shapes
	 */
	public Map<String, String> filteredPolylines(Collection<Geolocation> track){
		if (stopDescriptors.size() == 0) {
			return null;
		}
		List<Geolocation> points = new ArrayList<>(track);
		Map<TTLineDescriptor, int[]> occurences = new HashMap<>();
		Collections.sort(points, (o1, o2) -> (int)(o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime()));

		HashSet<Integer> set = new HashSet<>();
		for (int i = 0; i < points.size(); i++) {
			Geolocation g = points.get(i);
			final int row = row(se[0], g.getLatitude(), g.getLongitude(), distance);
			final int col = col(nw[1], g.getLatitude(), g.getLongitude(), distance);
			
			set.addAll(stopMatrix.get(row*width+col));
			if (row > 0 && col > 0) set.addAll(stopMatrix.get((row - 1) * width + col - 1));
			if (row > 0) set.addAll(stopMatrix.get((row - 1) * width + col));
			if (row < height - 1 && col < width - 1) set.addAll(stopMatrix.get((row + 1) * width + col + 1));
			if (row < height - 1) set.addAll(stopMatrix.get((row + 1) * width + col));
			if (row < height - 1 && col > 0) set.addAll(stopMatrix.get((row+1) * width + col - 1));
			if (col > 0) set.addAll(stopMatrix.get(row * width + col - 1));
			if (row > 0 && col < width - 1) set.addAll(stopMatrix.get((row -1) * width + col + 1));
			if (col < width - 1) set.addAll(stopMatrix.get(row * width + col + 1));
			
			if (set != null)
				for (Integer stopId: set) {
					Collection<TTLineDescriptor> descriptors = stopDescriptors.get(stopId);
					if (descriptors != null)
						for (TTLineDescriptor descriptor : descriptors) {
							int[] occ = occurences.getOrDefault(descriptor, new int[]{0,Integer.MAX_VALUE,0});
							occ[0]++;
							occ[1] = Math.min(occ[1], i);
							occ[2] = i - occ[1];
							occurences.put(descriptor, occ);
						}
				}
			set.clear();
		}
		Collection<TTLineDescriptor> descriptors = null;
		// filter occurrences for time of shapes
		Set<TTLineDescriptor> keys = new HashSet<>(occurences.keySet());
		for(TTLineDescriptor d : keys) {
			if (!validTime(d.shape, points.get(0).getRecorded_at(), points.get(points.size()-1).getRecorded_at())) {
				occurences.remove(d);
			}
		} 
		
		if (occurences.size() > MAX_DESCRIPTORS) {
			List<TTLineDescriptor> list = new ArrayList<>(occurences.keySet());
			
			descriptors = filterOccurences(list,occurences,MAX_DESCRIPTORS);
		} else {
			descriptors = occurences.keySet();
		}
		
		Map<String,String> polys = Maps.newTreeMap();
		descriptors.forEach(x -> {
			polys.put(routeMap.get(x.route) + "#" + x.shape, polylineMap.get(x.shape));
		});
		
		return polys;
	}	
	
	
	private List<TTLineDescriptor> filterOccurences(List<TTLineDescriptor> list, Map<TTLineDescriptor, int[]> occurences, int max) {
		List<TTLineDescriptor> result = Lists.newArrayList();
		
		Multimap<Integer, TTLineDescriptor> sorted = ArrayListMultimap.create();
		for (TTLineDescriptor tt: list) {
			sorted.put(occurences.get(tt)[0], tt);
		}
		
		SortedSet<Integer> scores = Sets.newTreeSet(Collections.reverseOrder());
		scores.addAll(sorted.keySet());
		
		sorted.clear();
		for (TTLineDescriptor tt: list) {
			sorted.put(occurences.get(tt)[2], tt);
		}
		scores.addAll(sorted.keySet());		
		
		scores.stream().limit(max).forEach(x -> result.addAll(sorted.get(x)));
		
		return result;
	}
	
	private void sortOccurences(List<TTLineDescriptor> list, Map<TTLineDescriptor, int[]> occurences) {
		Map<TTLineDescriptor, Integer> score = Maps.newHashMap();
		
		list.sort((a,b) -> {
			return    (occurences.get(b)[0] - occurences.get(a)[0]) != 0 
					? (occurences.get(b)[0] - occurences.get(a)[0]) 
					: (occurences.get(b)[2] - occurences.get(a)[2]); 
		});
		list.stream().forEach(x -> score.put(x, list.indexOf(x)));
		list.sort((a,b) -> {
			return    (occurences.get(b)[2] - occurences.get(a)[2]) != 0 
					? (occurences.get(b)[2] - occurences.get(a)[2]) 
					: (occurences.get(b)[0] - occurences.get(a)[0]); 
		});
		list.stream().forEach(x -> score.put(x, score.get(x) + list.indexOf(x)));
		
		list.sort((a,b) -> {
			return score.get(a) - score.get(b);
		});

	}
	
	
	/**
	 * @param shape
	 * @param recorded_at
	 * @param recorded_at2
	 * @return true if tracked time interval overlaps with the shape intervals
	 */
	private boolean validTime(int shape, Date from, Date to) {
		List<int[]> intervals = shapeTimeMap.get(shape);
		int[] passInterval = new int[]{timeToInt(from)-30, timeToInt(to)+30};
		for (int[] interval: intervals) {
			if (passInterval[1] >= interval[0] && passInterval[0] <= interval[1]) return true;
		}
		return false;
	}

	private static int col(double w, double lat, double lon, double distance) {
		return (int) Math.ceil(GamificationHelper.harvesineDistance(lat, w, lat, lon) / distance);
	}
	private static int row(double s, double lat, double lon, double distance) {
		return (int) Math.ceil(GamificationHelper.harvesineDistance(s, lon, lat, lon) / distance);
	}

	
	public void loadStopTimes(InputStream stopTimeSrc, Map<String, TTLineDescriptor> tripMap) {
		List<String[]> stopTimes = new BufferedReader(new InputStreamReader(stopTimeSrc))
		.lines()
		.map(s -> s.split(","))
		.filter(a -> !a[0].isEmpty() && !a[0].equals("trip_id"))
		.collect(Collectors.toList());
		
		Map<String, Integer> minMap = new HashMap<>();
		Map<String, Integer> maxMap = new HashMap<>();
		
		for (String[] str : stopTimes) {
			if (tripMap.get(str[0]) != null) {
				stopDescriptors.put(stopIDMap.get(str[3]), tripMap.get(str[0]));
				int time = 0;
				try {
					time = timeToInt(str[1]);
				} catch (Exception e) {
					logger.debug("Incorrect stop time string: "+ Arrays.toString(str));
					continue;
				}
				minMap.put(str[0], Math.min(time, minMap.getOrDefault(str[0], Integer.MAX_VALUE)));
				maxMap.put(str[0], Math.max(time, maxMap.getOrDefault(str[0], Integer.MIN_VALUE)));
//			} else {
//				continue;
			}
		}
		updateShapeTimes(minMap, maxMap, tripMap);
	}

	/**
	 * Populate shape time map using trip times. The overlapping trips for the same shape are merged
	 * @param minMap
	 * @param maxMap
	 * @param tripMap
	 */
	private void updateShapeTimes(Map<String, Integer> minMap, Map<String, Integer> maxMap, Map<String, TTLineDescriptor> tripMap) {
		// raw data: start/stop times of all the trips corresponding to a shape
		ArrayListMultimap<Integer, int[]> rawIntervals = ArrayListMultimap.create();
		minMap.keySet().forEach(t -> {
			rawIntervals.put(tripMap.get(t).shape, new int[]{minMap.get(t), maxMap.get(t)});
		});
		// process each shape
		for (int shape: rawIntervals.keySet()) {
			List<int[]> intervals = rawIntervals.get(shape);
			// sort ascending
			intervals.sort((ia,ib) -> {
				return 	  ia[0] != ib[0] 
						? ia[0] - ib[0]
						: ia[1] - ib[1];
			});
			List<int[]> newIntervals = new ArrayList<>();
			// merge intervals 
			int[] prev = intervals.get(0);
			for (int i = 1; i < intervals.size(); i++) {
				int[] curr = intervals.get(i);
				if (curr[0] <= prev[1]) {
					prev[1] = curr[1];
				} else {
					newIntervals.add(prev);
					prev = curr;
				}
			}
			newIntervals.add(prev);
			shapeTimeMap.putAll(shape, newIntervals);
		}
	}

	/**
	 * @param time
	 * @return minutes of the day
	 */
	private int timeToInt(String time) {
		// use regexp as hours can go beyond 23 
		Matcher matcher = TIME_REG.matcher(time);
		matcher.find();
		return Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
		
//		LocalTime lt = LocalTime.parse(time);
//		return lt.getHour() * 60 + lt.getMinute();
	}

	/**
	 * @param to
	 * @return
	 */
	private int timeToInt(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
	}

	public Map<String, TTLineDescriptor> loadTrips(InputStream tripSrc) {
		Map<String, TTLineDescriptor> tripMap = new HashMap<>();
		List<String[]> trips = new BufferedReader(new InputStreamReader(tripSrc))
		.lines()
		.map(s -> s.split(","))
		.filter(a -> !a[0].isEmpty() && !a[0].equals("route_id")).collect(Collectors.toList());
		for (String[] tripStr : trips) {
			String route = tripStr[0];
			routeIDMap.putIfAbsent(route, routeIDMap.size());
			Integer routeId = routeIDMap.get(route);
			routeMap.putIfAbsent(routeId, route);
			if (tripStr.length < 6) {
				continue;
			}
			Integer shapeId = shapeIDMap.get(tripStr[5]);
			if (shapeId == null) {
				continue;
			}
			tripMap.put(tripStr[2], new TTLineDescriptor(shapeId, routeId));
		}
		return tripMap;
	}

	/**
	 * Load stops from GTFS stops.txt
	 * @param stopSrc
	 */
	public void loadStops(InputStream stopSrc) {
		List<String[]> lines = new BufferedReader(new InputStreamReader(stopSrc))
				.lines()
				.map(s -> s.split(",")).collect(Collectors.toList());
		String[] firstLine = lines.get(0);
		if (!"stop_id".equals(firstLine[0])) return;
		int lat_idx = 0, lng_idx = 0;
		for (int i = 0; i < firstLine.length; i++) {
			if ("stop_lat".equals(firstLine[i])) lat_idx = i;
			if ("stop_lon".equals(firstLine[i])) lng_idx = i;
		}
		
		List<String[]> stops = lines.subList(1, lines.size());
		for (String[] stopStr : stops) {
			int id = stopIDMap.size();
			stopIDMap.put(stopStr[0], id);
			Geolocation stop = new Geolocation(Double.parseDouble(stopStr[lat_idx]), Double.parseDouble(stopStr[lng_idx]), null);
			stopMap.put(id, stop);
		}
	}

	/**
	 * Load shape from GTFS shape.txt
	 * @param shapeSrc
	 */
	private void loadShapes(InputStream shapeSrc) {
		Map<String, List<String[]>> shapes = new BufferedReader(new InputStreamReader(shapeSrc))
		.lines()
		.map(s -> s.split(","))
		.filter(a -> !a[0].isEmpty() && !a[0].equals("shape_id"))
		.collect(Collectors.groupingBy(a -> a[0]));
		for (Entry<String, List<String[]>> entry : shapes.entrySet()) {
			int id = shapeIDMap.size();
			shapeIDMap.put(entry.getKey(), id);
			List<Geolocation> shape = entry.getValue().stream()
					.sorted((a,b) -> Integer.parseInt(a[3]) - Integer.parseInt(b[3]))
					.map(a -> new Geolocation(Double.parseDouble(a[1]), Double.parseDouble(a[2]), null))
					.collect(Collectors.toList());
			shape = TrackValidator.fillTrace(shape, 100.0 / 1000 / 2 / Math.sqrt(2));
			shapeMap.put(id, shape);
		}
	}
	
	
	
	public Multimap<Integer, TTLineDescriptor> getStopDescriptors() {
		return Multimaps.unmodifiableMultimap(stopDescriptors);
	}

	public Multimap<Integer, Integer> getStopMatrix() {
		return Multimaps.unmodifiableMultimap(stopMatrix);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}



	private static class TTLineDescriptor {
		private int shape, route;

		public TTLineDescriptor(int shape, int route) {
			super();
			this.shape = shape;
			this.route = route;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + route;
			result = prime * result + shape;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TTLineDescriptor other = (TTLineDescriptor) obj;
			if (route != other.route)
				return false;
			if (shape != other.shape)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TTLineDescriptor [shape=" + shape + ", route=" + route + "]";
		}
	}
}
