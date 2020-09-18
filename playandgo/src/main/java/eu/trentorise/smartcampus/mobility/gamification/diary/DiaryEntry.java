package eu.trentorise.smartcampus.mobility.gamification.diary;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance.ScoreStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;

@JsonInclude(Include.NON_NULL)
public class DiaryEntry implements Comparable<DiaryEntry> {

	public enum DiaryEntryType {
		TRAVEL, BADGE, CHALLENGE, CHALLENGE_WON, RECOMMENDED, NEW_LEVEL
	}
	
	public enum TravelType {
		PLANNED, FREETRACKING, SHARED
	}	

	private long timestamp;
	private DiaryEntryType type;
	private String entityId;

	private TravelType travelType;
	private Set<String> travelModes;
	private Map<String, Double> travelDistances;
	private Date travelDate;
	private Double travelLength;
	private Long travelScore;
	private ScoreStatus scoreStatus;
	private TravelValidity travelValidity;
	private String clientId;
	private String multimodalId;
	
	private List<DiaryEntry> children;

	private String badge;
	private String badgeText;
	private String badgeCollection;
	
	private String challengeName;
	private Long challengeStart;
	private Long challengeEnd;
//	private Boolean challengeCompleted;
//	private Long challengeCompletedDate;
	private Integer challengeBonus;
	
	private String recommendedNickname;
	
	private String levelName;

	@JsonGetter
	public String getDate() {
		return new Date(timestamp).toString();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public DiaryEntryType getType() {
		return type;
	}

	public void setType(DiaryEntryType type) {
		this.type = type;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public Date getTravelDate() {
		return travelDate;
	}

	public TravelType getTravelType() {
		return travelType;
	}

	public void setTravelType(TravelType travelType) {
		this.travelType = travelType;
	}

	public Set<String> getTravelModes() {
		return travelModes;
	}

	public void setTravelModes(Set<String> travelModes) {
		this.travelModes = travelModes;
	}

	public Map<String, Double> getTravelDistances() {
		return travelDistances;
	}

	public void setTravelDistances(Map<String, Double> travelDistances) {
		this.travelDistances = travelDistances;
	}

	public void setTravelDate(Date travelDate) {
		this.travelDate = travelDate;
	}

	public Double getTravelLength() {
		return travelLength;
	}

	public void setTravelLength(Double travelLength) {
		this.travelLength = travelLength;
	}

	public Long getTravelScore() {
		return travelScore;
	}

	public void setTravelScore(Long travelEstimatedScore) {
		this.travelScore = travelEstimatedScore;
	}


	public ScoreStatus getScoreStatus() {
		return scoreStatus;
	}

	public void setScoreStatus(ScoreStatus scoreStatus) {
		this.scoreStatus = scoreStatus;
	}

	public TravelValidity getTravelValidity() {
		return travelValidity;
	}

	public void setTravelValidity(TravelValidity travelValidity) {
		this.travelValidity = travelValidity;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getMultimodalId() {
		return multimodalId;
	}

	public void setMultimodalId(String multimodalId) {
		this.multimodalId = multimodalId;
	}

	public List<DiaryEntry> getChildren() {
		return children;
	}

	public void setChildren(List<DiaryEntry> children) {
		this.children = children;
	}

	public String getBadge() {
		return badge;
	}

	public void setBadge(String badge) {
		this.badge = badge;
	}

	public String getBadgeText() {
		return badgeText;
	}

	public void setBadgeText(String badgeText) {
		this.badgeText = badgeText;
	}

	public String getBadgeCollection() {
		return badgeCollection;
	}

	public void setBadgeCollection(String badgeCollection) {
		this.badgeCollection = badgeCollection;
	}

	public String getChallengeName() {
		return challengeName;
	}

	public void setChallengeName(String challengeName) {
		this.challengeName = challengeName;
	}

	public Long getChallengeStart() {
		return challengeStart;
	}

	public void setChallengeStart(Long challengeStart) {
		this.challengeStart = challengeStart;
	}

	public Long getChallengeEnd() {
		return challengeEnd;
	}

	public void setChallengeEnd(Long challengeEnd) {
		this.challengeEnd = challengeEnd;
	}

//	public Boolean getChallengeCompleted() {
//		return challengeCompleted;
//	}
//
//	public void setChallengeCompleted(Boolean challengeCompleted) {
//		this.challengeCompleted = challengeCompleted;
//	}
//
//	public Long getChallengeCompletedDate() {
//		return challengeCompletedDate;
//	}
//
//	public void setChallengeCompletedDate(Long challengeCompletedDate) {
//		this.challengeCompletedDate = challengeCompletedDate;
//	}

	public Integer getChallengeBonus() {
		return challengeBonus;
	}

	public void setChallengeBonus(Integer challengeBonus) {
		this.challengeBonus = challengeBonus;
	}

	public String getRecommendedNickname() {
		return recommendedNickname;
	}

	public void setRecommendedNickname(String recommendedNickname) {
		this.recommendedNickname = recommendedNickname;
	}

	public String getLevelName() {
		return levelName;
	}

	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
		return tsb.build();
	}

	@Override
	public int compareTo(DiaryEntry o) {
		return (int) ((timestamp - o.timestamp) / 1000);
	}

}
