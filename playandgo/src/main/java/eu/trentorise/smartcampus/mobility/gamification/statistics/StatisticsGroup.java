package eu.trentorise.smartcampus.mobility.gamification.statistics;

import java.util.List;

public class StatisticsGroup {

	private Long firstBefore;
	private Long firstAfter;
	
	private List<StatisticsAggregation> stats;
	
//	private Map<AggregationGranularity, Map<String, Object>> globalStats;

	public Long getFirstBefore() {
		return firstBefore;
	}

	public void setFirstBefore(Long firstBefore) {
		this.firstBefore = firstBefore;
	}

	public Long getFirstAfter() {
		return firstAfter;
	}

	public void setFirstAfter(Long firstAfter) {
		this.firstAfter = firstAfter;
	}

	public List<StatisticsAggregation> getStats() {
		return stats;
	}

	public void setStats(List<StatisticsAggregation> stats) {
		this.stats = stats;
	}

//	public Map<AggregationGranularity, Map<String, Object>> getGlobalStats() {
//		return globalStats;
//	}
//
//	public void setGlobalStats(Map<AggregationGranularity, Map<String, Object>> globalStats) {
//		this.globalStats = globalStats;
//	}


	
	
}
