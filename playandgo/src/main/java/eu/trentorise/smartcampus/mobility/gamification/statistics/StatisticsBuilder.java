package eu.trentorise.smartcampus.mobility.gamification.statistics;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.MODE_TYPE;

@Component
public class StatisticsBuilder {

	private static final String GLOBAL_STATISTICS = "globalStatistics";

	private static FastDateFormat sdf = FastDateFormat.getInstance("yyyy/MM/dd");
	
	private static Log logger = LogFactory.getLog(StatisticsBuilder.class);
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;

	public StatisticsGroup computeStatistics(String userId, String appId, long from, long to, AggregationGranularity granularity) throws Exception {
		String fromDay = sdf.format(new Date(from));
		String toDay = sdf.format(new Date(to));
		
		return computeStatistics(userId, appId, fromDay, toDay, granularity);
	}		
	
	private StatisticsGroup computeStatistics(String userId, String appId, String from, String to, AggregationGranularity granularity) throws Exception {
		StatisticsGroup result = statsByGranularity(userId, appId, from, to, granularity);
		return result;
	}
	
	public GlobalStatistics getGlobalStatistics(String userId, String appId, String start, boolean dates) throws Exception {
		Criteria criteria = new Criteria("userId").is(userId).and("appId").is(appId);
		criteria = criteria.and("updateTime").gt(System.currentTimeMillis() - 1000 * 60 * 60 * 0);
		Query query = new Query(criteria);

		logger.debug("Start getGlobalStatistics - findOne");
		GlobalStatistics statistics = template.findOne(query, GlobalStatistics.class, GLOBAL_STATISTICS);
		logger.debug("End getGlobalStatistics - findOne");
		if (statistics == null) {
			statistics = new GlobalStatistics();
			statistics.setUserId(userId);
			statistics.setUpdateTime(System.currentTimeMillis());
			statistics.setStats(computeGlobalStatistics(userId, appId, start, dates));
			statistics.setAppId(appId);
			logger.debug("Start getGlobalStatistics - save");
			template.save(statistics, GLOBAL_STATISTICS);
			logger.debug("End getGlobalStatistics - save");
		}
		
		return statistics;
	}
	
	private Map<AggregationGranularity, Map<String, Object>> computeGlobalStatistics(String userId, String appId, String start, boolean dates) throws Exception {
		Map<AggregationGranularity, Map<String, Object>> result = Maps.newHashMap();
		for (AggregationGranularity granularity: AggregationGranularity.values()) {
			result.put(granularity, computeGlobalStatistics(userId, appId, start, granularity, dates));
		}
		
		return result;
	}
	
	private Map<String, Object> computeGlobalStatistics(String userId, String appId, String start, AggregationGranularity granularity, boolean dates) throws Exception {
		List<TrackedInstance> instances = findAll(userId, appId);
		Multimap<String, TrackedInstance> byDay = groupByDay(instances);
		Multimap<Range, TrackedInstance> byWeek = mergeByGranularity(byDay, granularity, start, sdf.format(new Date()));
		Map<Range, Map<String,Double>> rangeSum = statsByRanges(byWeek);
		
		Map<String, Object> result = Maps.newTreeMap();
		for (Range range: rangeSum.keySet()) {
			Map<String,Double> value = rangeSum.get(range);
			for (String key: value.keySet()) {
				if (value.get(key) > (Double)result.getOrDefault("max " + key, 0.0)) {
					result.put("max " + key, Math.max((Double)result.getOrDefault("max " + key, 0.0), value.get(key)));
					if (dates) {
						result.put("date max " + key, convertRange(range));
					}
				}
			}
		}
		
		return result;
	}
	
	private Map<String, Long> convertRange(Range range) throws Exception {
		Map<String, Long> result = Maps.newTreeMap();
		result.put(range.from, sdf.parse(range.from).getTime());
		result.put(range.to, sdf.parse(range.to).getTime());
		return result;
	}
	
	private StatisticsGroup statsByGranularity(String userId, String appId, String from, String to, AggregationGranularity granularity) throws Exception {
		List<TrackedInstance> instances = find(userId, appId, from, to);
		Multimap<String, TrackedInstance> byDay = groupByDay(instances);
		Multimap<Range, TrackedInstance> byWeek = mergeByGranularity(byDay, granularity, from, to);
		Map<Range, Map<String,Double>> rangeSum = statsByRanges(byWeek);
		
		Map<String, String> outside = outside(userId, appId, from, to);
		
		StatisticsGroup result = new StatisticsGroup();
		if (outside.containsKey("before")) {
			result.setFirstBefore(sdf.parse(outside.get("before")).getTime());
		}
		if (outside.containsKey("after")) {
			result.setFirstAfter(sdf.parse(outside.get("after")).getTime());
		}
		
		List<StatisticsAggregation> aggregations = Lists.newArrayList();
		for (Range range: rangeSum.keySet()) {
			StatisticsAggregation aggregation = new StatisticsAggregation();
			aggregation.setFrom(sdf.parse(range.from).getTime());
			aggregation.setTo(endOfDay(range.to));
			aggregation.setData(rangeSum.get(range));
			aggregations.add(aggregation);
		}
		Collections.sort(aggregations);
		result.setStats(aggregations);
		return result;
	}	
	
	private List<TrackedInstance> findAll(String userId, String appId) {
		Criteria criteria = new Criteria("userId").is(userId).and("appId").is(appId).and("complete").is(true);
		criteria.orOperator(
				new Criteria("validationResult.validationStatus.validationOutcome").ne(TravelValidity.INVALID.toString()).and("changedValidity").is(null),
				new Criteria("changedValidity").is(TravelValidity.VALID.toString()));
		Query query = new Query(criteria);
		query.fields().include("validationResult.validationStatus").include("day").include("freeTrackingTransport").include("itinerary").include("overriddenDistances");
		
		logger.debug("Start findAll - find");
		List<TrackedInstance> result = template.find(query, TrackedInstance.class, "trackedInstances");
		logger.debug("End findAll - find");
		
		result = result.stream().filter(x -> x.getDay() != null).collect(Collectors.toList());
		
		return result;
	}	
	
	private List<TrackedInstance> find(String userId, String appId, String from, String to) {
		Criteria criteria = new Criteria("userId").is(userId).and("appId").is(appId).and("complete").is(true);
		criteria.orOperator(
				new Criteria("validationResult.validationStatus.validationOutcome").ne(TravelValidity.INVALID.toString()).and("changedValidity").is(null),
				new Criteria("changedValidity").is(TravelValidity.VALID.toString()));
		criteria.andOperator(Criteria.where("day").gte(from).lte(to));
		Query query = new Query(criteria);
		query.fields().include("validationResult.validationStatus").include("day").include("freeTrackingTransport").include("itinerary").include("overriddenDistances");
		
		logger.debug("Start find - find");
		List<TrackedInstance> result = template.find(query, TrackedInstance.class, "trackedInstances");
		logger.debug("End find - find");
		
		result = result.stream().filter(x -> x.getDay() != null).collect(Collectors.toList());
		
		return result;
	}
	
	private Map<String, String> outside(String userId, String appId, String from, String to) {
		Map<String, String> result = Maps.newTreeMap();
		
		Criteria criteria = new Criteria("userId").is(userId).and("appId").is(appId).and("complete").is(true).and("day").lt(from);
		criteria.orOperator(
				new Criteria("validationResult.validationStatus.validationOutcome").ne(TravelValidity.INVALID.toString()).and("changedValidity").is(null),
				new Criteria("changedValidity").is(TravelValidity.VALID.toString()));		
		Query query = new Query(criteria);
		query.fields().include("day");
		query.with(Sort.by(Direction.DESC, "day"));
		query.limit(1);
		
		logger.debug("Start outside - findOne 1b: " + query);
		List<String> before = Lists.newArrayList(); 
		TrackedInstance prev = template.findOne(query, TrackedInstance.class, "trackedInstances");
		if (prev != null) before.add(prev.getDay());
		Collections.sort(before);
		Collections.reverse(before);
		if (!before.isEmpty()) {
			result.put("before", before.get(0));
		}
		logger.debug("End outside - findOne 1b = " + result.get("before"));
		
		criteria = new Criteria("userId").is(userId).and("appId").is(appId).and("complete").is(true).and("day").gt(to);
		criteria.orOperator(
				new Criteria("validationResult.validationStatus.validationOutcome").ne(TravelValidity.INVALID.toString()).and("changedValidity").is(null),
				new Criteria("changedValidity").is(TravelValidity.VALID.toString()));			
		query = new Query(criteria);
		query.fields().include("day");		
		query.with(Sort.by(Direction.ASC, "day"));
		query.limit(1);

		logger.debug("Start outside - findOne 2b");
		List<String> after = Lists.newArrayList();
		TrackedInstance nxt = template.findOne(query, TrackedInstance.class, "trackedInstances");
		if (nxt != null) after.add(nxt.getDay());
		Collections.sort(after);
		if (!after.isEmpty()) {
			result.put("after", after.get(0));
		}
		logger.debug("End outside - findOne 2b = " + result.get("after"));
		
		return result;
	}		
	
	private Multimap<String, TrackedInstance> groupByDay(List<TrackedInstance> instances) {
		Multimap<String, TrackedInstance> result = Multimaps.index(instances, TrackedInstance::getDay);
		return result;
	}
	
	private Multimap<Range, TrackedInstance> mergeByGranularity(Multimap<String, TrackedInstance> byDay, AggregationGranularity granularity, String from, String to) throws Exception {
		Multimap<Range, TrackedInstance> result = ArrayListMultimap.create(); 
		Multimap<Range, String> weekDays = ArrayListMultimap.create();

		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		for (String day: byDay.keySet()) {
			Range range = buildRanges(day, granularity, from, to);
			weekDays.put(range, day);
		}
		
		for (Range range: weekDays.keySet()) {
			for (String day: weekDays.get(range)) {
				result.putAll(range, byDay.get(day));
			}
		}

		return result;
	}	
	
	private Map<Range, Map<String,Double>> statsByRanges(Multimap<Range, TrackedInstance> group) {
		Map<Range, Map<String,Double>> result = Maps.newHashMap();
		for (Range groupKey: group.keys()) {
			Map<String, Double> statByRange = Maps.newTreeMap();
			for (TrackedInstance ti: group.get(groupKey)) {
				Map<String, Double> dist = null;
//				Double val = ti.getValidationResult().getDistance();
//				if (val == null) {
//					val = 0.0;
//				}
				if (ti.getFreeTrackingTransport() != null) {
//					dist = computeFreeTrackingDistances(val, ti.getFreeTrackingTransport());
					dist = computeFreeTrackingDistances(ti, true);
//					statByRange.put("free tracking", statByRange.getOrDefault("free tracking", 0.0) + 1);
//					if (ti.getEstimatedScore() != null) {
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
//					}
					
				}
				if (ti.getItinerary() != null) {
//					dist = computePlannedJourneyDistances(ti.getValidationResult().getValidationStatus());
					dist = computePlannedJourneyDistances(ti, true);
//					statByRange.put("planned", statByRange.getOrDefault("planned", 0.0) + 1);
//					if (ti.getEstimatedScore() != null) {					
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
//					}
				}
				if (dist != null) {
					for (String key : dist.keySet()) {
						statByRange.put(key, statByRange.getOrDefault(key, 0.0) + dist.get(key));
						// statByRange.put("max " + key, Math.max(statByRange.getOrDefault("max " + key, 0.0),dist.get(key)));
					}
				}
			}
			result.put(groupKey, statByRange);
		}
		return result;
	}	
	
//	private Map<Map<String, String>, Map<String,Double>> computeGlobalStatistics(Multimap<Map<String, String>, TrackedInstance> group) {
//		Map<Map<String, String>, Map<String,Double>> result = Maps.newHashMap();
//		for (Map<String, String> groupKey: group.keys()) {
//			Map<String, Double> statByRange = Maps.newTreeMap();
//			for (TrackedInstance ti: group.get(groupKey)) {
//				Map<String, Double> dist = null;
//				if (ti.getFreeTrackingTransport() != null) {
//					dist = computeFreeTrackingDistances(ti.getValidationResult().getDistance(), ti.getFreeTrackingTransport());
//					statByRange.put("free tracking", statByRange.getOrDefault("free tracking", 0.0) + 1);
//					if (ti.getScore() != null) {
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getScore());
//					}
//					
//				}
//				if (ti.getItinerary() != null) {
//					dist = computePlannedJourneyDistances(ti.getItinerary());
//					statByRange.put("planned", statByRange.getOrDefault("planned", 0.0) + 1);
//					if (ti.getScore() != null) {					
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getScore());
//					}
//				}
//				for (String key: dist.keySet()) {
//					statByRange.put(key, statByRange.getOrDefault(key, 0.0) + dist.get(key));
//					statByRange.put("max " + key, Math.max(statByRange.getOrDefault("max " + key, 0.0), dist.get(key)));
//				}
//			}
//			result.put(groupKey, statByRange);
//		}
//		return result;
//	}		
	
//	private Map<String, Double> computePlannedJourneyDistances(ValidationStatus vs) {
//		Map<String, Double> result = Maps.newTreeMap();
//		if (vs.getPlannedDistances() != null && !vs.getPlannedDistances().isEmpty()) {
//			vs.getPlannedDistances().entrySet().forEach(entry -> result.put(TrackValidator.toModeString(entry.getKey()), entry.getValue()));
//		}
//		return result;
//	}
//	
//	private Map<String, Double> computeFreeTrackingDistances(Double distance, String ttype) {
//		Map<String, Double> result = Maps.newTreeMap();
//		result.put(ttype, distance);
//		return result;
//	}	
	
	
	private Map<String, Double> computePlannedJourneyDistances(TrackedInstance ti, boolean userOverride) {
		Map<String, Double> result = Maps.newTreeMap();
		if (ti.getValidationResult() != null && ti.getValidationResult().getValidationStatus() != null && ti.getValidationResult().getValidationStatus().getPlannedDistances() != null && !ti.getValidationResult().getValidationStatus().getPlannedDistances().isEmpty()) {
			ti.getValidationResult().getValidationStatus().getPlannedDistances().entrySet().forEach(entry -> result.put(TrackValidator.toModeString(entry.getKey()), entry.getValue()));
		}
		if (userOverride && ti.getOverriddenDistances() != null && !ti.getOverriddenDistances().isEmpty()) {
			ti.getOverriddenDistances().entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue()));
		}
		return result;
	}
	
	private Map<String, Double> computeFreeTrackingDistances(TrackedInstance ti, boolean userOverride) {
		Map<String, Double> result = Maps.newTreeMap();
		String ttype = ti.getFreeTrackingTransport();
		MODE_TYPE ettype = TrackValidator.toModeType(ttype);
		if (ti.getValidationResult() != null && ti.getValidationResult().getValidationStatus() != null && ti.getValidationResult().getValidationStatus().getEffectiveDistances() != null) {
			Double value = ti.getValidationResult().getValidationStatus().getEffectiveDistances().get(ettype);
			result.put(ttype, value != null ? value : 0);
		}
		if (userOverride && ti.getOverriddenDistances() != null && ti.getOverriddenDistances().containsKey(ttype)) {
			Double value = ti.getOverriddenDistances().get(ttype);
			result.put(ttype, value != null ? value : 0);
		}		
		
		return result;
	}		
	
	
	private static Range buildRanges(String day, AggregationGranularity granularity, String from, String to) throws Exception {
		switch (granularity) {
			case day:
				return new Range(day, day);
			case week: {
				Range range = new Range();
				Calendar c = Calendar.getInstance();
				c.setFirstDayOfWeek(Calendar.MONDAY);
				c.setTimeInMillis(sdf.parse(day).getTime());
				if (c.get(Calendar.DAY_OF_WEEK) < Calendar.SATURDAY) {
					c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
					c.add(Calendar.DAY_OF_YEAR, -2); // past saturday
				} else {
					c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY); // past saturday
				}
				range.from = sdf.format(new Date(c.getTimeInMillis()));
				c.setTimeInMillis(sdf.parse(day).getTime());
				if (c.get(Calendar.DAY_OF_WEEK) < Calendar.SATURDAY) {
					c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
					c.add(Calendar.DAY_OF_YEAR, -2); // next friday
				} else {
					c.add(Calendar.DAY_OF_YEAR, 7);
					c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY); // next friday
				}
				range.to = sdf.format(new Date(c.getTimeInMillis()));
				
				return range;
			}	
			case month: {
				Range range = new Range();
				Calendar c = Calendar.getInstance();
				c.setFirstDayOfWeek(Calendar.MONDAY);
				c.setTimeInMillis(sdf.parse(day).getTime());
				c.set(Calendar.DAY_OF_MONTH, 1);
				range.from = sdf.format(new Date(c.getTimeInMillis()));
				c.add(Calendar.MONTH, 1);
				c.add(Calendar.DAY_OF_YEAR, -1);
				range.to = sdf.format(new Date(c.getTimeInMillis()));
				return range;
			}	
			case total:
			default:
				return new Range(from, to);
		}
	}	
	
	private long endOfDay(String day) throws Exception {
		Calendar c = new GregorianCalendar();
		c.setTime(sdf.parse(day));
		c.add(Calendar.DAY_OF_YEAR, 1);
		c.add(Calendar.SECOND, -1);
		return c.getTimeInMillis();
	}
	
	private static class Range {
		String from, to;

		public Range() {}

		public Range(String from, String to) {
			super();
			this.from = from;
			this.to = to;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
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
			Range other = (Range) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return "[" + from + "," + to + "]";
		}
		
	}

}
