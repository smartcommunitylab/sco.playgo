package eu.trentorise.smartcampus.mobility.gamification.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ChallengeConcept extends GameConcept {
	
    public static enum ChallengeState {
        PROPOSED, ASSIGNED, ACTIVE, COMPLETED, FAILED, REFUSED, AUTO_DISCARDED
    }	
	
	private String modelName;
	private Map<String, Object> fields = new HashMap<String, Object>();
	private Date start;
	private Date end;

	// metadata fields
	private boolean completed = false;
	private Date dateCompleted;
	
	private String state;
	
    private Map<ChallengeState, Date> stateDate = new HashMap<>();
    private String origin;	

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public Map<String, Object> getFields() {
		return fields;
	}

	public void setFields(Map<String, Object> fields) {
		this.fields = fields;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public Date getDateCompleted() {
		return dateCompleted;
	}

	/**
	 * Helper method of challenge
	 * 
	 * @return
	 */
	public boolean completed() {
		completed = true;
		dateCompleted = new Date();
		return true;
	}

	public boolean isCompleted() {
		return completed;
	}
	
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Map<ChallengeState, Date> getStateDate() {
		return stateDate;
	}

	public String getOrigin() {
		return origin;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
