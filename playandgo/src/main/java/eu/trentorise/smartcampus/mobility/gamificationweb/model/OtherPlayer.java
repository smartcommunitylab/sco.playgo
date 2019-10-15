package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class OtherPlayer {

	private String nickname;
	private int greenLeaves;
	private List<BadgeCollectionConcept> badgeCollectionConcept;
	private Map<String, Double> statistics = Maps.newTreeMap();
	private Map<String, Double> lastMonthStatistics = Maps.newTreeMap();

	private String level;
	
	private List<Map<String, Object>> wonChallenges = Lists.newArrayList();
	
//	private String avatar;
	
	private Long updated;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public int getGreenLeaves() {
		return greenLeaves;
	}

	public void setGreenLeaves(int greenLeaves) {
		this.greenLeaves = greenLeaves;
	}

	public List<BadgeCollectionConcept> getBadgeCollectionConcept() {
		return badgeCollectionConcept;
	}

	public void setBadgeCollectionConcept(List<BadgeCollectionConcept> badgeCollectionConcept) {
		this.badgeCollectionConcept = badgeCollectionConcept;
	}

	public Map<String, Double> getStatistics() {
		return statistics;
	}

	public void setStatistics(Map<String, Double> statistics) {
		this.statistics = statistics;
	}

	public Map<String, Double> getLastMonthStatistics() {
		return lastMonthStatistics;
	}

	public void setLastMonthStatistics(Map<String, Double> lastMonthStatistics) {
		this.lastMonthStatistics = lastMonthStatistics;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public List<Map<String, Object>> getWonChallenges() {
		return wonChallenges;
	}

	public void setWonChallenges(List<Map<String, Object>> wonChallenges) {
		this.wonChallenges = wonChallenges;
	}

	public Long getUpdated() {
		return updated;
	}

	public void setUpdated(Long updated) {
		this.updated = updated;
	}
	
	
}
