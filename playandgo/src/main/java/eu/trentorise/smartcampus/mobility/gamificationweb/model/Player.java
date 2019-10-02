package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="player")
public class Player {
	
	@Id
	private String id;
	
	private String playerId;
	private String gameId;
	
	private String name;
	private String surname;
	private String nickname;
	private String mail;
	private String language;
	private boolean sendMail;
	private Map<String, Object> personalData;
	private Map<String,Map<String,Object>> surveys;
	private boolean checkedRecommendation;
	private List<Event> eventsCheckIn;
	
//	private String avatar;
	
	public Player() {
		super();
	}

	public Player(String playerId, String gameId, String name, String surname, String nickname,
			//String mail, String language, boolean sendMail, PersonalData personalData, SurveyData surveyData, String type) {
			String mail, String language, boolean sendMail, Map<String, Object> personalData, Map<String,Map<String,Object>> surveyData, boolean checkRecommendation) {
		super();
		this.playerId = playerId;
		this.gameId = gameId;
		this.name = name;
		this.surname = surname;
		this.nickname = nickname;
		this.mail = mail;
		this.language = language;
		this.sendMail = sendMail;
		this.personalData = personalData;
		this.surveys = surveyData;
		this.checkedRecommendation = checkRecommendation;
	}

	public String getName() {
		return name;
	}

	public String getSurname() {
		return surname;
	}

	public String getNickname() {
		return nickname;
	}

	public String getMail() {
		return mail;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public void setNickname(String nickName) {
		this.nickname = nickName;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getId() {
		return id;
	}

	public void setId(String pid) {
		this.id = pid;
	}
	
	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String appId) {
		this.gameId = appId;
	}

	public Map<String, Object> getPersonalData() {
		return personalData;
	}

	public void setPersonalData(Map<String, Object> personalData) {
		this.personalData = personalData;
	}
	
	public Map<String, Map<String, Object>> getSurveys() {
		if (surveys == null) {
			surveys = new HashMap<String, Map<String,Object>>();
		}
		return surveys;
	}

	public void setSurveys(Map<String, Map<String, Object>> surveys) {
		this.surveys = surveys;
	}

	public boolean isSendMail() {
		return sendMail;
	}

	public void setSendMail(boolean sendMail) {
		this.sendMail = sendMail;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isCheckedRecommendation() {
		return checkedRecommendation;
	}

	public void setCheckedRecommendation(boolean checkedRecommendation) {
		this.checkedRecommendation = checkedRecommendation;
	}

	public List<Event> getEventsCheckIn() {
		return eventsCheckIn;
	}

	public void setEventsCheckIn(List<Event> eventsCheckIn) {
		this.eventsCheckIn = eventsCheckIn;
	}

	public String toJSONString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
		return tsb.build();
	}

	/**
	 * @param first
	 * @param surveyData
	 */
	public void addSurvey(String key, Map<String, Object> surveyData) {
		if (surveys == null) {
			surveys = new HashMap<String, Map<String,Object>>();
		}
		surveys.put(key, surveyData);
	}
	
}
