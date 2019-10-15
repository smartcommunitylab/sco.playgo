package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class ChallengeConcept {

	public enum ChallengeDataType {
		ACTIVE, OLD, PROPOSED, FUTURE
	}

	private Map<ChallengeDataType, List<ChallengesData>> challengeData;
	
	public ChallengeConcept() {
		super();
		challengeData = Maps.newHashMap();
	}

	public Map<ChallengeDataType, List<ChallengesData>> getChallengeData() {
		return challengeData;
	}

	public void setChallengeData(Map<ChallengeDataType, List<ChallengesData>> challengeData) {
		this.challengeData = challengeData;
	}

}
