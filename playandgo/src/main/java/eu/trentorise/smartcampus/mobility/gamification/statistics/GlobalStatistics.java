package eu.trentorise.smartcampus.mobility.gamification.statistics;

import java.util.Map;

import org.springframework.data.annotation.Id;

public class GlobalStatistics {

	@Id
	private String userId;
	
	private long updateTime;
	private Map<AggregationGranularity, Map<String, Object>> stats;
	
	private String appId;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public long getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
	public Map<AggregationGranularity, Map<String, Object>> getStats() {
		return stats;
	}
	public void setStats(Map<AggregationGranularity, Map<String, Object>> stats) {
		this.stats = stats;
	}
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	
	
}
