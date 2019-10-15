package eu.trentorise.smartcampus.mobility.gamification.statistics;

import java.util.Map;

public class StatisticsAggregation implements Comparable<StatisticsAggregation> {

	private long from;
	private long to;
	private Map<String, Double> data;

	public long getFrom() {
		return from;
	}

	public void setFrom(long from) {
		this.from = from;
	}

	public long getTo() {
		return to;
	}

	public void setTo(long to) {
		this.to = to;
	}

	public Map<String, Double> getData() {
		return data;
	}

	public void setData(Map<String, Double> means) {
		this.data = means;
	}

	@Override
	public int compareTo(StatisticsAggregation o) {
		return (int)((from - o.from) / 1000);
	}

}
