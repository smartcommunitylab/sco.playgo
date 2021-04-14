package eu.trentorise.smartcampus.mobility.gamification.model;

import java.util.Map;

public class ChallengeRequestDTO {
	private Map<String, Object> data;
	private String start;
	private String end;

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String end) {
		this.end = end;
	}

}
