package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Range;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgeCollectionConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept.ChallengeDataType;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeLongDescrStructure;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeStructure;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.OtherAttendeeData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConceptPeriod;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;

@Component
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ChallengesUtils {
	
	@Autowired
	private WebLinkUtils utils;
	
	// challange fields
	private static final String CHAL_FIELDS_PERIOD_NAME = "periodName";
//	private static final String CHAL_FIELDS_BONUS_POINT_TYPE = "bonusPointType";
	private static final String CHAL_FIELDS_COUNTER_NAME = "counterName";
//	private static final String CHAL_FIELDS_BADGE_COLLECTION_NAME = "badgeCollectionName";
	private static final String CHAL_FIELDS_BONUS_SCORE = "bonusScore";
//	private static final String CHAL_FIELDS_BASELINE = "baseline";
	private static final String CHAL_FIELDS_TARGET = "target";
	private static final String CHAL_FIELDS_PERIOD_TARGET = "periodTarget";
	private static final String CHAL_FIELDS_INITIAL_BADGE_NUM = "initialBadgeNum";
	private static final String CHAL_FIELDS_OTHER_ATTENDEE_SCORES = "otherAttendeeScores";
	private static final String CHAL_FIELDS_CHALLENGE_SCORE = "challengeScore";
	private static final String CHAL_FIELDS_CHALLENGE_TARGET = "challengeTarget";
	private static final String CHAL_FIELDS_PLAYER_ID = "playerId";
	private static final String CHAL_FIELDS_PROPOSER = "proposer";
	private static final String CHAL_FIELDS_CHALLENGE_SCORE_NAME = "challengeScoreName";
	private static final String CHAL_FIELDS_CHALLENGE_REWARD = "rewardBonusScore";
	
	
//	private static final String CHAL_FIELDS_POS_MIN = "posMin";
//	private static final String CHAL_FIELDS_POS_MAX = "posMax";
	// new challenge types
	private static final String CHAL_MODEL_PERCENTAGE_INC = "percentageIncrement";
	private static final String CHAL_MODEL_ABSOLUTE_INC = "absoluteIncrement";
	private static final String CHAL_MODEL_REPETITIVE_BEAV = "repetitiveBehaviour";
	private static final String CHAL_MODEL_NEXT_BADGE = "nextBadge";
	private static final String CHAL_MODEL_COMPLETE_BADGE_COLL = "completeBadgeCollection";
	private static final String CHAL_MODEL_SURVEY = "survey";
	private static final String CHAL_MODEL_POICHECKIN = "poiCheckin";
	private static final String CHAL_MODEL_CHECKIN = "checkin";
	private static final String CHAL_MODEL_CLASSPOSITION = "leaderboardPosition";
	private static final String CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE = "groupCompetitivePerformance";
	private static final String CHAL_MODEL_GROUP_COMPETITIVE_TIME = "groupCompetitiveTime";
	private static final String CHAL_MODEL_GROUP_COOPERATIVE = "groupCooperative";
	private static final String CHAL_MODEL_INCENTIVE_GROUP = "incentiveGroupChallengeReward";
	
	// week delta in milliseconds
//	private static final Long W_DELTA = 2000L;
	private static final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
		
	private static final Logger logger = LoggerFactory.getLogger(ChallengesUtils.class);

	private ObjectMapper mapper = new ObjectMapper();


	private Map<String, ChallengeStructure> challengeStructureMap;
	private Map<String, ChallengeLongDescrStructure> challengeLongStructureMap;

	private Map<String, List> challengeDictionaryMap;
	private Map<String, String> challengeReplacements;

	private static final Map<String, String> UNIT_MAPPING = Stream.of(new String[][] {
		  { "daily", "days" }, 
		  { "weekly", "weeks" }, 
		}).collect(Collectors.toMap(data -> data[0], data -> data[1]));
	
	@Autowired
	private PlayerRepositoryDao playerRepository;
	
	@PostConstruct
	private void init() throws Exception {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// template.dropCollection(ChallengeStructure.class);

		challengeStructureMap = Maps.newTreeMap();
		challengeLongStructureMap = Maps.newTreeMap();

		List list = mapper.readValue(Resources.getResource("challenges/challenges.json"), List.class);
		for (Object o : list) {
			ChallengeStructure challenge = mapper.convertValue(o, ChallengeStructure.class);

			String key = challenge.getName() + (challenge.getFilter() != null ? ("#" + challenge.getFilter()) : "");
			challengeStructureMap.put(key, challenge);
//			template.save(challenge);
		}
		
		list = mapper.readValue(Resources.getResource("challenges/challenges_descriptions.json"), List.class);
		for (Object o : list) {
			ChallengeLongDescrStructure challenge = mapper.convertValue(o, ChallengeLongDescrStructure.class);

			String key = challenge.getModelName() + "#" + challenge.getFilter();
			challengeLongStructureMap.put(key, challenge);
//			template.save(challenge);
		}

		challengeDictionaryMap = mapper.readValue(Resources.getResource("challenges/challenges_dictionary.json"), Map.class);
		challengeReplacements = mapper.readValue(Resources.getResource("challenges/challenges_replacements.json"), Map.class);
	}

	public List<ChallengeConcept> parse(String data) throws Exception {
		List<ChallengeConcept> concepts = Lists.newArrayList();

		Map playerMap = mapper.readValue(data, Map.class);
		if (playerMap.containsKey("state")) {
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			if (stateMap.containsKey("ChallengeConcept")) {
				List conceptList = mapper.convertValue(stateMap.get("ChallengeConcept"), List.class);
				for (Object o : conceptList) {
					ChallengeConcept concept = mapper.convertValue(o, ChallengeConcept.class);
					concepts.add(concept);
				}
			}
		}
		return concepts;
	}
	
	
	// Method correctChallengeData: used to retrieve the challenge data objects from the user profile data
	public eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept convertChallengeData(String playerId, String gameId, String profile, int type, String language, List<PointConcept> pointConcept, List<BadgeCollectionConcept> bcc_list) throws Exception {
    	ListMultimap<ChallengeDataType, ChallengesData> challengesMap = ArrayListMultimap.create();
    	
    	eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept result = new eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept();
    	if(profile != null && !profile.isEmpty()){
    		
    		List<ChallengeConcept> challengeList = parse(profile);
    		
    		if(challengeList != null){
				for(ChallengeConcept challenge: challengeList){
					String name = challenge.getName();
					String modelName = challenge.getModelName();
					long start = challenge.getStart().getTime();
					long end = challenge.getEnd().getTime();
					Boolean completed = challenge.isCompleted();
					String state = challenge.getState();
					long dateCompleted = challenge.getDateCompleted() != null ? challenge.getDateCompleted().getTime() : 0L;
					int bonusScore = 0;
					String periodName = "";
					String counterName = "";
					double target = 0;
					double periodTarget = 0;
					String badgeCollectionName = "";
					int initialBadgeNum = 0;
					Map<String, Object> otherAttendeeScores = null;
					
					if(challenge.getFields() != null){
						bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_BONUS_SCORE, 0)).intValue();
						periodName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_PERIOD_NAME,"");
						counterName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_COUNTER_NAME,"");
						target =  ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_TARGET,0)).doubleValue(); 
						badgeCollectionName = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_COUNTER_NAME,"");
						initialBadgeNum = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_INITIAL_BADGE_NUM,0)).intValue();
						periodTarget = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_PERIOD_TARGET,0)).doubleValue();
						List otherAttendeeScoresList = (List)challenge.getFields().getOrDefault(CHAL_FIELDS_OTHER_ATTENDEE_SCORES, Collections.EMPTY_LIST);
						if (!otherAttendeeScoresList.isEmpty()) {
							otherAttendeeScores = (Map)otherAttendeeScoresList.get(0);
						}
					}

					if (target == 0) {
						target = 1;
					}
					
					// Convert data to old challenges models
//						final String ch_point_type = challData.getBonusPointType();
					final long now = System.currentTimeMillis();
					
	    			ChallengesData challengeData = new ChallengesData();
	    			challengeData.setChallId(name);

    				challengeData.setType(modelName);
    				challengeData.setActive(now < end);
    				challengeData.setSuccess(completed);
    				challengeData.setStartDate(start);
    				challengeData.setEndDate(end);
    				challengeData.setDaysToEnd(calculateRemainingDays(end, now));
    				challengeData.setChallCompletedDate(dateCompleted);
    				challengeData.setUnit(counterName);
	    			
    				double row_status = 0D;
    				int status = 0;
    				
    				switch (modelName) {
    					case CHAL_MODEL_REPETITIVE_BEAV:
		    				double successes = retrieveRepeatitiveStatusFromCounterName(counterName, periodName, pointConcept, start, end, target); 
		    				row_status = successes;
		    				status = Math.min(100, (int)(100.0 * successes / periodTarget));
		    				challengeData.setChallTarget((int)periodTarget);
		    				// update unit for repetitive behavior: correspond to the number of periods
		    				challengeData.setUnit(UNIT_MAPPING.get(periodName));
	    					break;
	    				case CHAL_MODEL_PERCENTAGE_INC:
	    				case CHAL_MODEL_ABSOLUTE_INC: {
		    				double earned = retrieveCorrectStatusFromCounterName(counterName, periodName, pointConcept, start, end); 
		    				row_status = earned;
		    				status = Math.min(100, (int)(100.0 * earned / target));
	    					break;
	    				}
	    				case CHAL_MODEL_NEXT_BADGE: {
		    				int count = getEarnedBadgesFromList(bcc_list, badgeCollectionName, initialBadgeNum);
		    				if(!challengeData.getActive()){	// NB: fix to avoid situation with challenge not win and count > target
		    					if(completed){
		    						count = (int)target;
		    					} else {
		    						count = (int)target - 1;
		    					}
		    				}
		    				row_status = count;
		    				status = Math.min(100, (int)(100.0 * count / target));
		    				break;
	    				}
	    				case CHAL_MODEL_SURVEY: {
		    				if(completed) {
	    						row_status = 1; 
	    						status = 100;
	    					}
		    				// survey link to be passed
		    				String link = utils.createSurveyUrl(playerId, gameId, (String)challenge.getFields().get("surveyType"), language);
		    				challenge.getFields().put("surveylink", link);
		    				break;
	    				}
	    				case CHAL_MODEL_INCENTIVE_GROUP: {
		    				if(completed) {
	    						row_status = 1; 
	    						status = 100;
	    					}	    					
	    					break;
	    				}
	    				case CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE : {
	    					row_status = (Double)challenge.getFields().get(CHAL_FIELDS_CHALLENGE_SCORE);
	    					double other_row_status = (Double)otherAttendeeScores.get(CHAL_FIELDS_CHALLENGE_SCORE);
	    					double total = row_status + other_row_status;
	    					int other_status = 0;
	    					if (total != 0) {
	    						status = (int)(100 * row_status / total);
	    						other_status = 100 - status;
	    					}
	    					
	    					String unit = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_SCORE_NAME, "");
	    					challengeData.setUnit(unit);
	    					
	    					String proposer = (String)challenge.getFields().get(CHAL_FIELDS_PROPOSER);
	    					challengeData.setProposerId(proposer);
	    					
	    					String otherPlayerId = (String)otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID); 
	    					Player otherPlayer = playerRepository.findByPlayerIdAndGameId(otherPlayerId, gameId);
	    					
	    					String nickname = null;
	    					if (otherPlayer != null) {
	    						nickname = otherPlayer.getNickname();
	    						challenge.getFields().put("opponent", nickname);
	    					}
	    					
	    					OtherAttendeeData otherAttendeeData = new OtherAttendeeData();
	    					otherAttendeeData.setRow_status(round(other_row_status, 2));
	    					otherAttendeeData.setStatus(other_status);
	    					otherAttendeeData.setPlayerId(otherPlayerId);
	    					otherAttendeeData.setNickname(nickname);
	    					
	    					challengeData.setOtherAttendeeData(otherAttendeeData);
	    					
//	    					bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, 0)).intValue();
	    					
	    					break;
	    				}	    		
						case CHAL_MODEL_GROUP_COMPETITIVE_TIME: {
							row_status = (Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_SCORE);
							double other_row_status = (Double) otherAttendeeScores.get(CHAL_FIELDS_CHALLENGE_SCORE);
							double challengeTarget = Math.ceil((Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_TARGET));
							target = (int)challengeTarget;
							int other_status = 0;
							if (challengeTarget != 0) {
								status = (int) (100 * row_status / challengeTarget);
								other_status = (int) (100 * other_row_status / challengeTarget);
							}
							
							if (status + other_status > 100) {
								float coeff = (float)(status + other_status) / 100;
								status = Math.round(status / coeff);
								other_status = Math.round(other_status / coeff);
							}							
	
							String unit = (String)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_SCORE_NAME, "");
							challengeData.setUnit(unit);
	
							String proposer = (String)challenge.getFields().get(CHAL_FIELDS_PROPOSER);
							challengeData.setProposerId(proposer);
	
							String otherPlayerId = (String) otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID);
							Player otherPlayer = playerRepository.findByPlayerIdAndGameId(otherPlayerId, gameId);
	
							String nickname = null;
							if (otherPlayer != null) {
								nickname = otherPlayer.getNickname();
								challenge.getFields().put("opponent", nickname);
							}
	
							OtherAttendeeData otherAttendeeData = new OtherAttendeeData();
							otherAttendeeData.setRow_status(round(other_row_status, 2));
							otherAttendeeData.setStatus(other_status);
							otherAttendeeData.setPlayerId(otherPlayerId);
							otherAttendeeData.setNickname(nickname);
	
							challengeData.setOtherAttendeeData(otherAttendeeData);
							
	    					bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, 0)).intValue();
	
							break;
						}	
						case CHAL_MODEL_GROUP_COOPERATIVE: {
							row_status = (Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_SCORE);
							double other_row_status = (Double) otherAttendeeScores.get(CHAL_FIELDS_CHALLENGE_SCORE);
							double challengeTarget = Math.ceil((Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_TARGET));
							target = (int)challengeTarget;
							int other_status = 0;
							if (challengeTarget != 0) {
								status = (int) (100 * row_status / challengeTarget);
								other_status = (int) (100 * other_row_status / challengeTarget);
								
								if (status + other_status > 100) {
									float coeff = (float)(status + other_status) / 100;
									status = Math.round(status / coeff);
									other_status = Math.round(other_status / coeff);
								}
								
							}
	
							Double reward = (Double) challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, "");
							
							String unit = (String) challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_SCORE_NAME, "");
							challengeData.setUnit(unit);
	
							String proposer = (String) challenge.getFields().get(CHAL_FIELDS_PROPOSER);
							challengeData.setProposerId(proposer);
	
							String otherPlayerId = (String) otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID);
							Player otherPlayer = playerRepository.findByPlayerIdAndGameId(otherPlayerId, gameId);
	
							String nickname = null;
							if (otherPlayer != null) {
								nickname = otherPlayer.getNickname();
								challenge.getFields().put("opponent", nickname);
							}
							challenge.getFields().put("reward", reward);
							challenge.getFields().put("target", target);
	
							OtherAttendeeData otherAttendeeData = new OtherAttendeeData();
							otherAttendeeData.setRow_status(round(other_row_status, 2));
							otherAttendeeData.setStatus(other_status);
							otherAttendeeData.setPlayerId(otherPlayerId);
							otherAttendeeData.setNickname(nickname);
	
							challengeData.setOtherAttendeeData(otherAttendeeData);
							
	    					bonusScore = ((Number)challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, 0)).intValue();
	
							break;
						}						
	    				// boolean status: 100 or 0
	    				case CHAL_MODEL_COMPLETE_BADGE_COLL: 
	    				case CHAL_MODEL_POICHECKIN: 
	    				case CHAL_MODEL_CLASSPOSITION: 
	    				default: {
		    				if(completed){
	    						row_status = 1;
	    						status = 100;
	    					}
	    				}
    				}
    				
    				challengeData.setChallTarget((int)target);
    				challengeData.setChallDesc(fillDescription(challenge, language));
    				challengeData.setChallCompleteDesc(fillLongDescription(challenge, getFilterByType(challengeData.getType()), language));

    				challengeData.setBonus(bonusScore);
    				challengeData.setStatus(status);
    				challengeData.setRow_status(round(row_status, 2));
    				
					if (type == 0) {
						if ("ASSIGNED".equals(state)) {
							if (now >= start - MILLIS_IN_DAY) { // if challenge is started (with one day of offset for mail)
								if (now < end - MILLIS_IN_DAY) { // if challenge is not ended
									// challenges.add(challengeData);
									challengesMap.put(ChallengeDataType.ACTIVE, challengeData);
								} else if (now < end + MILLIS_IN_DAY) { // CHAL_TS_OFFSET
									// oldChallenges.add(challengeData); // last week challenges
									challengesMap.put(ChallengeDataType.OLD, challengeData);
								}
							}
						}
					} else {
						if ("PROPOSED".equals(state)) {
							challengesMap.put(ChallengeDataType.PROPOSED, challengeData);
						} else if (now < end) { // if challenge is not ended
							if (now >= start) {
								challengesMap.put(ChallengeDataType.ACTIVE, challengeData);
							} else {
								challengesMap.put(ChallengeDataType.FUTURE, challengeData);
							}
						} else { // CHAL_TS_OFFSET
							challengesMap.put(ChallengeDataType.OLD, challengeData);
						}
					}
				}

				result.setChallengeData(Multimaps.asMap(challengesMap));
			}
    		
		}
    	
		result.getChallengeData().values().forEach(x -> {
			Collections.sort(x);
			Collections.reverse(x);
		});	
		
		if (result.getChallengeData().containsKey(ChallengeDataType.PROPOSED)) {
			Collections.sort(result.getChallengeData().get(ChallengeDataType.PROPOSED), new Comparator<ChallengesData>() {

				@Override
				public int compare(ChallengesData o1, ChallengesData o2) {
					String isGroup1 = o1.getProposerId() == null ? "1" : "0";
					String isGroup2 = o2.getProposerId() == null ? "1" : "0";
					int res = new String(isGroup1 + o1.getStartDate()).compareTo(new String(isGroup2 + o2.getStartDate()));
					if (res == 0) {
						res = o1.getChallId().compareTo(o2.getChallId());
					}
					return res;
				}

			});
		}
		
		
    	
    	return result;
    }
	
	public void fillMissingFields(ChallengeConcept challenge, String gameId) {
		List otherAttendeeScoresList = (List)challenge.getFields().getOrDefault(CHAL_FIELDS_OTHER_ATTENDEE_SCORES, Collections.EMPTY_LIST);
		Map<String, Object> otherAttendeeScores = null;
		
		if (!otherAttendeeScoresList.isEmpty()) {
			otherAttendeeScores = (Map)otherAttendeeScoresList.get(0);
		} else {
			return;
		}

		String otherPlayerId = (String)otherAttendeeScores.get(CHAL_FIELDS_PLAYER_ID); 
		Player otherPlayer = playerRepository.findByPlayerIdAndGameId(otherPlayerId, gameId);		
		
		switch (challenge.getModelName()) {
		case CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE : {
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}			
			break;
		}
		case CHAL_MODEL_GROUP_COMPETITIVE_TIME : {
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}			
			break;
		}
		case CHAL_MODEL_GROUP_COOPERATIVE : {
			Double reward = (Double) challenge.getFields().getOrDefault(CHAL_FIELDS_CHALLENGE_REWARD, "");
			Double target = Math.ceil((Double) challenge.getFields().get(CHAL_FIELDS_CHALLENGE_TARGET));
			if (otherPlayer != null) {
				String nickname = otherPlayer.getNickname();
				challenge.getFields().put("opponent", nickname);
			}
			challenge.getFields().put("reward", reward);
			challenge.getFields().put("target", target);
			break;
			}			
		}		
	}
	
	
	private String getFilterByType(String type) {
		switch(type) {
			case CHAL_MODEL_PERCENTAGE_INC:
			case CHAL_MODEL_ABSOLUTE_INC: {
				return "counterName";
			}
			case CHAL_MODEL_REPETITIVE_BEAV: {
				return "counterName";
			}
			case CHAL_MODEL_COMPLETE_BADGE_COLL:
			case CHAL_MODEL_NEXT_BADGE: {
				return "badgeCollectionName";
			}
			case CHAL_MODEL_POICHECKIN: {
				return "eventName";
			}
			case CHAL_MODEL_CLASSPOSITION: {
				return null;
			}
			case CHAL_MODEL_SURVEY: {
				return "surveyType";
			}
			case CHAL_MODEL_CHECKIN: {
				return "checkinType";
			}
			case CHAL_MODEL_GROUP_COMPETITIVE_PERFORMANCE:
			case CHAL_MODEL_GROUP_COMPETITIVE_TIME:
			case CHAL_MODEL_GROUP_COOPERATIVE:
				return "challengePointConceptName";
			default: {
				return null;
			}
		
		}
	}
	
	// Method retrieveCorrectStatusFromCounterName: used to get the correct player status starting from counter name field
	private double retrieveCorrectStatusFromCounterName(String cName, String periodType, List<PointConcept> pointConcept, Long chalStart, Long chalEnd){
		if (chalEnd <= chalStart) return 0;
		
		Range<Long> challengeRange = Range.open(chalStart, chalEnd);
		double actualStatus = 0; // km or trips
		if(cName != null && !cName.isEmpty()){
			for(PointConcept pt : pointConcept){
				if(cName.equals(pt.getName()) && periodType.equals(pt.getPeriodType())){
					List<PointConceptPeriod> allPeriods = pt.getInstances();
					for(PointConceptPeriod pcp : allPeriods) {
						Range<Long> pcpRange = Range.open(pcp.getStart(), pcp.getEnd()); 
						if(chalStart != null && chalEnd != null) {
							if (pcpRange.isConnected(challengeRange)) {
								actualStatus += pcp.getScore();
							}
						} 
					}
					break;
				}
			}
		}
		return actualStatus;
	}
	
	private int retrieveRepeatitiveStatusFromCounterName(String cName, String periodType, List<PointConcept> pointConcept, Long chalStart, Long chalEnd, double target){
		if (chalEnd <= chalStart) return 0;

		Range<Long> challengeRange = Range.open(chalStart, chalEnd);
		int countSuccesses = 0; // km or trips
		if(cName != null && !cName.isEmpty()){
			for(PointConcept pt : pointConcept){
				if(cName.equals(pt.getName()) && periodType.equals(pt.getPeriodType())){
					List<PointConceptPeriod> allPeriods = pt.getInstances();
					for(PointConceptPeriod pcp : allPeriods){
						if(chalStart != null && chalEnd != null){
							Range<Long> pcpRange = Range.open(pcp.getStart(), pcp.getEnd()); 
							if(chalStart != null && chalEnd != null && pcpRange.isConnected(challengeRange)) {
								countSuccesses += pcp.getScore() >= target ? 1 : 0;
							}
						}
					}
					break;
				}
			}
		}
		
		return countSuccesses;
	}	
	
	
	// Method getEarnedBadgesFromList: used to get the earned badge number during challenge
	private int getEarnedBadgesFromList(List<BadgeCollectionConcept> bcc_list, String badgeCollName, int initial){
		int earnedBadges = 0;
		for(BadgeCollectionConcept bcc : bcc_list){
			if(bcc.getName().compareTo(badgeCollName) == 0){
				earnedBadges = bcc.getBadgeEarned().size() - initial;
				break;
			}
		}
		return earnedBadges;
	}
	
	private int calculateRemainingDays(long endTime, long now){
    	int remainingDays = 0;
    	if(now < endTime){
    		long tmpMillis = endTime - now;
    		remainingDays = (int) Math.ceil((float)tmpMillis / MILLIS_IN_DAY);
    	}
    	return remainingDays;
    }
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
	
	public String fillDescription(ChallengeConcept challenge, String lang) {
		String filter = getFilterByType(challenge.getModelName());
		String description = null;
		String name = challenge.getModelName();
		String filterField = (String) challenge.getFields().get(filter);

		String counterNameA = null;
		String counterNameB = null;
		if (filterField != null) {
			if (CHAL_FIELDS_COUNTER_NAME.equals(filter)) {
				String counterNames[] = filterField.split("_");
				counterNameA = counterNames[0];
				if (counterNames.length == 2) {
					counterNameB = counterNames[1];

					if (counterNameA.startsWith("No")) {
						counterNameA = counterNameA.replace("No", "");
						counterNameB = "No" + counterNameB;
					}

				}
			}
		}

		ChallengeStructure challengeStructure = challengeStructureMap.getOrDefault(name + "#" + filterField, null);

		if (challengeStructure == null) {
			challengeStructure = challengeStructureMap.getOrDefault(name + (counterNameB != null ? ("#_" + counterNameB) : ""), null);
		}

		if (challengeStructure != null) {
			description = fillDescription(challengeStructure, counterNameA, counterNameB, challenge, lang);
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}			
		} else {
			logger.error("Cannot find structure for challenge: '" + name + "', " + filter + "=" + filterField);
			return "";
		}

		return description;
	}

	private String fillLongDescription(ChallengeConcept challenge, String filterField, String lang) {
		String description = null;
		String name = challenge.getModelName();
		String counterName = filterField != null ? (String) challenge.getFields().get(filterField) : null;

		ChallengeLongDescrStructure challengeStructure = challengeLongStructureMap.getOrDefault(name + "#" + counterName, null);

		if (challengeStructure != null) {
			description = fillLongDescription(challengeStructure, counterName, challenge, lang);
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}			
		} else {
			return "";
		}
		return description;
	}
	
	private String fillDescription(ChallengeStructure structure, String counterNameA, String counterNameB, ChallengeConcept challenge, String lang) {
		ST st = new ST(structure.getDescription().get(lang));

		boolean negative = counterNameB != null && counterNameB.startsWith("No");

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), negative, lang) : o));
		}

		st.add("counterNameA", instantiateWord(counterNameA, negative, lang));
		st.add("counterNameB", instantiateWord(counterNameB, negative, lang));

		return st.render();
	}

	private String fillLongDescription(ChallengeLongDescrStructure structure, String counterName, ChallengeConcept challenge, String lang)  {
		ST st = new ST(structure.getDescription().get(lang));

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
		}

		return st.render();
	}
	
	public String fillDescription(String name, String filterField, Map<String, Object> params, String lang) {
		ChallengeStructure challengeStructure = challengeStructureMap.getOrDefault(name + "#" + filterField, null);
		
		String description = "";
		if (challengeStructure != null) {
			ST st = new ST(challengeStructure.getDescription().get(lang));
			
			for (String field : params.keySet()) {
				Object o = params.get(field);
				st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
			}			
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}		
			
			return st.render();
		} else {
			logger.error("Cannot find structure for challenge preview: '" + name + "', " + filterField);
			return "";
		}		
	}
	
	public String fillLongDescription(String name, String filterField, Map<String, Object> params, String lang) {
		ChallengeLongDescrStructure challengeStructure = challengeLongStructureMap.getOrDefault(name + "#" + filterField, null);
		
		String description = "";
		if (challengeStructure != null) {
			ST st = new ST(challengeStructure.getDescription().get(lang));
			
			for (String field : params.keySet()) {
				Object o = params.get(field);
				st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), false, lang) : o));
			}			
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}		
			
			return st.render();
		} else {
			logger.error("Cannot find structure for challenge preview: '" + name + "', " + filterField);
			return "";
		}		
	}	
	
	private String instantiateWord(String word, boolean negative, String lang) {
		if (word != null) {
			List versions = challengeDictionaryMap.get(word.toLowerCase());
			if (versions != null) {
				Optional<Map> result = versions.stream().filter(x -> negative == (Boolean) ((Map) x).get("negative")).findFirst();
				if (result.isPresent()) {
					return (String)((Map)((Map) result.get()).get("word")).get(lang);
				}
			}
		}
		return word;
	}
	
}
