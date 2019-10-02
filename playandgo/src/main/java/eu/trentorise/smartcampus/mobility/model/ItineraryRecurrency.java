package eu.trentorise.smartcampus.mobility.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.google.common.collect.Sets;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItineraryRecurrency {

	private Set<Integer> daysOfWeek;
	
	private String fromHour;
	private String toHour;
	
	private Long fromDate;
	private Long toDate;

	public ItineraryRecurrency() {
		daysOfWeek = Sets.newHashSet();
	}
	
	public Set<Integer> getDaysOfWeek() {
		return daysOfWeek;
	}

	public void setDaysOfWeek(Set<Integer> daysOfWeek) {
		this.daysOfWeek = daysOfWeek;
	}

	public String getFromHour() {
		return fromHour;
	}

	public void setFromHour(String fromHour) {
		this.fromHour = fromHour;
	}

	public String getToHour() {
		return toHour;
	}

	public void setToHour(String toHour) {
		this.toHour = toHour;
	}

	public Long getFromDate() {
		return fromDate;
	}

	public void setFromDate(Long fromDate) {
		this.fromDate = fromDate;
	}

	public Long getToDate() {
		return toDate;
	}

	public void setToDate(Long toDate) {
		this.toDate = toDate;
	}
	
	
}
