package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;

import com.google.common.collect.Lists;

public class PointConcept {

	private String name;
	private double score;
	private String periodType;
	private long start;
	private long periodDuration;
	private String periodIdentifier;
	private List<PointConceptPeriod> instances = Lists.newArrayList();
	
	public String getName() {
		return name;
	}

	public double getScore() {
		return score;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getPeriodType() {
		return periodType;
	}

	public long getStart() {
		return start;
	}

	public long getPeriodDuration() {
		return periodDuration;
	}

	public String getPeriodIdentifier() {
		return periodIdentifier;
	}

	public List<PointConceptPeriod> getInstances() {
		return instances;
	}

	public void setPeriodType(String periodType) {
		this.periodType = periodType;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public void setPeriodDuration(long periodDuration) {
		this.periodDuration = periodDuration;
	}

	public void setPeriodIdentifier(String periodIdentifier) {
		this.periodIdentifier = periodIdentifier;
	}

	public void setInstances(List<PointConceptPeriod> instances) {
		this.instances = instances;
	}

	public PointConcept() {
		super();
	}

	public PointConcept(String name, double score, String periodType, long start, long periodDuration,
			String periodIdentifier, List<PointConceptPeriod> instances) {
		super();
		this.name = name;
		this.score = score;
		this.periodType = periodType;
		this.start = start;
		this.periodDuration = periodDuration;
		this.periodIdentifier = periodIdentifier;
		this.instances = instances;
	}
	
	@Override
	public String toString() {
		return name + "/" + periodType;
	}
	
	
}
