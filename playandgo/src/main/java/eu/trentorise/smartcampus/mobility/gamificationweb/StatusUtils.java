package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.gamification.model.Inventory;
import eu.trentorise.smartcampus.mobility.gamification.model.PlayerLevel;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgeCollectionConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerClassification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerStatus;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PointConceptPeriod;

@Component
public class StatusUtils {



	private static final String STATE = "state";
	private static final String PLAYER_ID = "playerId";
//	private static final String BADGE_COLLECTION_CONCEPT = "BadgeCollectionConcept";
//	private static final String LEVELS = "levels";
//	private static final String BC_NAME = "name";
//	private static final String BC_BADGE_EARNED = "badgeEarned";
	private static final String POINT_CONCEPT = "PointConcept";
	private static final String PC_GREEN_LEAVES = "green leaves";
	private static final String PC_NAME = "name";
	private static final String PC_SCORE = "score";
	private static final String PC_PERIOD = "period";
	private static final String PC_PERIODS = "periods";
	private static final String PC_START = "start";
	private static final String PC_WEEKLY = "weekly";	
//	private static final String PC_PERIOD_DURATION = "period";
	private static final String PC_IDENTIFIER = "identifier";
	private static final String PC_INSTANCES = "instances";
	private static final String PC_END = "end";
//	private static final String PARK_RIDE_PIONEER = "park and ride pioneer";
//	private static final String BIKE_SHARING_PIONEER = "bike sharing pioneer";
	// private static final String PC_CLASSIFICATION_WEEK =
	// "green leaves week ";
	// private static final String PC_CLASSIFICATION_WEEK_TEST =
	// "green leaves week test";

	public static final long MILLIS_IN_WEEK = 1000 * 60 * 60 * 24 * 7;

	@Autowired
	private ChallengesUtils challUtils;
	
	@Autowired
	private BadgesCache badgeCache;

	ObjectMapper mapper = new ObjectMapper(); 
	{
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PlayerStatus convertPlayerData(String profile, String playerId, String gameId, String nickName, String gamificationUrl, int challType, String language)
			throws Exception {

		PlayerStatus ps = new PlayerStatus();
		
		Map<String, Object> stateMap = mapper.readValue(profile, Map.class);
		
		Map<String, Object> state = (Map<String, Object>)stateMap.get("state");
		List<BadgeCollectionConcept> badges = mapper.convertValue(state.get("BadgeCollectionConcept"), new TypeReference<List<BadgeCollectionConcept>>() {});
		badges.forEach(x -> {
			x.getBadgeEarned().forEach(y -> {
				y.setUrl(getUrlFromBadgeName(gamificationUrl, y.getName()));
			});
		});
		ps.setBadgeCollectionConcept(badges);
		
		List<Map> gePointsMap = mapper.convertValue(state.get("PointConcept"), new TypeReference<List<Map>>() {});
		List<PointConcept> points = convertGEPointConcept(gePointsMap);
		
		ChallengeConcept challenges = challUtils.convertChallengeData(playerId, gameId, profile, challType, language, points, badges);
		ps.setChallengeConcept(challenges);
	
		List<PlayerLevel> levels = mapper.convertValue((List)stateMap.get("levels"), new TypeReference<List<PlayerLevel>>() {});
		ps.setLevels(levels);		

		Inventory inventory = mapper.convertValue(stateMap.get("inventory"), Inventory.class);
		ps.setInventory(inventory);	
		
		Map<String, Object> playerData = buildPlayerData(playerId, gameId, nickName);
		ps.setPlayerData(playerData);
		
		points.removeIf(x -> !PC_GREEN_LEAVES.equals(x.getName()));
		ps.setPointConcept(points);		
		
		Calendar c = Calendar.getInstance();
		Calendar from = Calendar.getInstance(); from.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY); from.set(Calendar.HOUR_OF_DAY, 0); from.set(Calendar.MINUTE, 0); from.set(Calendar.SECOND, 0);
		Calendar to = Calendar.getInstance(); to.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY); to.set(Calendar.HOUR_OF_DAY, 12); to.set(Calendar.MINUTE, 0); to.set(Calendar.SECOND, 0);
		ps.setCanInvite(c.before(to) && c.after(from));
		
		return ps;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<PointConcept> convertGEPointConcept(List<Map> gePointsMap) {
		List<PointConcept> result = Lists.newArrayList();
		
		for (Map gePointMap: gePointsMap) {
			PointConcept pc = new PointConcept();
			pc.setName((String)gePointMap.get(PC_NAME));
			pc.setScore(((Double)gePointMap.get(PC_SCORE)));
			pc.setPeriodType(PC_WEEKLY);
			
			Map periods = (Map)gePointMap.get(PC_PERIODS);
			Map weekly = (Map)periods.get(PC_WEEKLY);
			if (weekly != null) {
				pc.setStart((Long)weekly.get(PC_START));
				pc.setPeriodDuration((Integer)weekly.get(PC_PERIOD));
				pc.setPeriodIdentifier((String)weekly.get(PC_IDENTIFIER));
				if (weekly.containsKey(PC_INSTANCES)) {
					Map<Object, Map> instances = (Map<Object, Map>)weekly.get(PC_INSTANCES);
					for (Map inst: instances.values()) {
						PointConceptPeriod pcp = mapper.convertValue(inst, PointConceptPeriod.class);
						pc.getInstances().add(pcp);
					}
				}
			}
			result.add(pc);
		}
		return result;
	}
	
	// Method cleanFromGenericBadges: useful method used to remove from the
	// badges list the generic badge (used in P&R and bikeSharing)
//	private List<BadgeCollectionConcept> cleanFromGenericBadges(List<BadgeCollectionConcept> inputBadges) {
//		List<BadgeCollectionConcept> correctedBadges = null;
//		if (inputBadges != null) {
//			for (BadgeCollectionConcept bcc : inputBadges) {
//				if (bcc.getName().compareTo(PARK_RIDE_PIONEER) == 0) {
//					List<BadgeConcept> badgeList = bcc.getBadgeEarned();
//					for (int i = badgeList.size() - 1; i >= 0; i--) {
//						if (badgeList.get(i).getUrl().contains("/img/gamification/pr/p&rLeaves.png")) {
//							badgeList.remove(i);
//						}
//					}
//				}
//				if (bcc.getName().compareTo(BIKE_SHARING_PIONEER) == 0) {
//					List<BadgeConcept> badgeList = bcc.getBadgeEarned();
//					for (int i = badgeList.size() - 1; i >= 0; i--) {
//						if (badgeList.get(i).getUrl().contains("/img/gamification/bike_sharing/bikeSharingPioneer.png")) {
//							badgeList.remove(i);
//						}
//					}
//				}
//			}
//			correctedBadges = inputBadges;
//		}
//		return correctedBadges;
//	};

	@SuppressWarnings("unchecked")
	public ClassificationData correctPlayerClassificationData(String profile, String playerId, String nickName, Long timestamp, String type) throws JSONException {
		ClassificationData playerClass = new ClassificationData();
		if (profile != null && !profile.isEmpty()) {

			int score = 0;
			// long time = (timestamp == null || timestamp.longValue() == 0L) ?
			// System.currentTimeMillis() : timestamp.longValue();
			// int weekNum = getActualWeek(time, type);

			JSONObject profileData = new JSONObject(profile);
			JSONObject stateData = (!profileData.isNull(STATE)) ? profileData.getJSONObject(STATE) : null;
			// System.out.println("My state " + stateData.toString());
			JSONArray pointConceptData = null;
			if (stateData != null) {
				pointConceptData = (!stateData.isNull(POINT_CONCEPT)) ? stateData.getJSONArray(POINT_CONCEPT) : null;
				if (pointConceptData != null) {
					for (int i = 0; i < pointConceptData.length(); i++) {
						JSONObject point = pointConceptData.getJSONObject(i);
						String pc_name = (!point.isNull(PC_NAME)) ? point.getString(PC_NAME) : null;
						if (timestamp == null || timestamp.longValue() == 0L) { // global
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								score = (!point.isNull(PC_SCORE)) ? point.getInt(PC_SCORE) : null;
							}
						} else { // specific week
							if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
								JSONObject pc_period = (!point.isNull(PC_PERIODS)) ? point.getJSONObject(PC_PERIODS) : null;
								if (pc_period != null) {
									Iterator<String> keys = pc_period.keys();
									while (keys.hasNext()) {
										String key = keys.next();
										JSONObject pc_weekly = pc_period.getJSONObject(key);
										if (pc_weekly != null) {
											JSONObject pc_instances = pc_weekly.getJSONObject(PC_INSTANCES);

											if (pc_instances != null) {
												Iterator<String> instancesKeys = pc_instances.keys();
												while (instancesKeys.hasNext()) {
													JSONObject pc_instance = pc_instances.getJSONObject(instancesKeys.next());
													int instance_score = (!pc_instance.isNull(PC_SCORE)) ? pc_instance.getInt(PC_SCORE) : 0;
													long instance_start = (!pc_instance.isNull(PC_START)) ? pc_instance.getLong(PC_START) : 0L;
													long instance_end = (!pc_instance.isNull(PC_END)) ? pc_instance.getLong(PC_END) : 0L;
													if (timestamp >= instance_start && timestamp <= instance_end) {
														score = instance_score;
														break;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				playerClass.setNickName(nickName);
				playerClass.setPlayerId(playerId);
				playerClass.setScore(score);
				if (nickName == null || nickName.isEmpty()) {
					playerClass.setPosition(-1); // used for user without
													// nickName
				}
			}

		}
		return playerClass;
	}

	@SuppressWarnings("unchecked")
	public List<ClassificationData> correctClassificationData(String allStatus, Map<String, String> allNicks, Long timestamp, String type) throws JSONException {
		List<ClassificationData> playerClassList = new ArrayList<ClassificationData>();
		if (allStatus != null && !allStatus.isEmpty()) {

			int score = 0;
			// long time = (timestamp == null || timestamp.longValue() == 0L) ?
			// System.currentTimeMillis() : timestamp;
			// int weekNum = getActualWeek(time, type);

			JSONObject allPlayersData = new JSONObject(allStatus);
			JSONArray allPlayersDataList = (!allPlayersData.isNull("content")) ? allPlayersData.getJSONArray("content") : null;
			if (allPlayersDataList != null) {
				for (int i = 0; i < allPlayersDataList.length(); i++) {
					JSONObject profileData = allPlayersDataList.getJSONObject(i);
					String playerId = (!profileData.isNull(PLAYER_ID)) ? profileData.getString(PLAYER_ID) : "0";
					score = 0; // here I reset the score value to avoid
								// classification problem
					// System.out.println("User " + playerId + " state " +
					// profileData.toString());
					JSONObject stateData = (!profileData.isNull(STATE)) ? profileData.getJSONObject(STATE) : null;
					JSONArray pointConceptData = null;
					if (stateData != null) {
						pointConceptData = (!stateData.isNull(POINT_CONCEPT)) ? stateData.getJSONArray(POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (int j = 0; j < pointConceptData.length(); j++) {
								JSONObject point = pointConceptData.getJSONObject(j);
								String pc_name = (!point.isNull(PC_NAME)) ? point.getString(PC_NAME) : null;
								if (timestamp == null || timestamp.longValue() == 0L) { // global
									if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
										score = (!point.isNull(PC_SCORE)) ? point.getInt(PC_SCORE) : null;
									}
								} else { // specific week
									if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
										JSONObject pc_period = (!point.isNull(PC_PERIODS)) ? point.getJSONObject(PC_PERIODS) : null;
										if (pc_period != null) {
											Iterator<String> keys = pc_period.keys();
											while (keys.hasNext()) {
												String key = keys.next();
												JSONObject pc_weekly = pc_period.getJSONObject(key);
												if (pc_weekly != null) {
													JSONObject pc_instances = pc_weekly.getJSONObject(PC_INSTANCES);
													if (pc_instances != null) {
														Iterator<String> instancesKeys = pc_instances.keys();
														while (instancesKeys.hasNext()) {
															JSONObject pc_instance = pc_instances.getJSONObject(instancesKeys.next());
															int instance_score = (!pc_instance.isNull(PC_SCORE)) ? pc_instance.getInt(PC_SCORE) : 0;
															long instance_start = (!pc_instance.isNull(PC_START)) ? pc_instance.getLong(PC_START) : 0L;
															long instance_end = (!pc_instance.isNull(PC_END)) ? pc_instance.getLong(PC_END) : 0L;
															if (timestamp >= instance_start && timestamp <= instance_end) {
																score = instance_score;
																break;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
						String nickName = getPlayerNickNameById(allNicks, playerId); // getPlayerNameById(allNicks,
																						// playerId);
						ClassificationData playerClass = new ClassificationData();
						playerClass.setNickName(nickName);
						playerClass.setPlayerId(playerId);
						playerClass.setScore(score);
						if (nickName != null && !nickName.isEmpty()) { // if
																				// nickName
																				// present
																				// (user
																				// registered
																				// and
																				// active)
							playerClassList.add(playerClass);
						}
					}
				}
			}

		}
		return playerClassList;
	}

	// Method correctGlobalClassification: return a map 'playerId, score' of the
	// global classification
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Integer> correctGlobalClassification(String allStatus) throws JSONException {
		Map classification = new HashMap<String, Integer>();
		if (allStatus != null && !allStatus.isEmpty()) {
			int score = 0;
			JSONObject allPlayersData = new JSONObject(allStatus);
			JSONArray allPlayersDataList = (!allPlayersData.isNull("content")) ? allPlayersData.getJSONArray("content") : null;
			if (allPlayersDataList != null) {
				for (int i = 0; i < allPlayersDataList.length(); i++) {
					JSONObject profileData = allPlayersDataList.getJSONObject(i);
					String playerId = (!profileData.isNull(PLAYER_ID)) ? profileData.getString(PLAYER_ID) : "0";
					score = 0; // here I reset the score value to avoid
								// classification problem
					JSONObject stateData = (!profileData.isNull(STATE)) ? profileData.getJSONObject(STATE) : null;
					JSONArray pointConceptData = null;
					if (stateData != null) {
						pointConceptData = (!stateData.isNull(POINT_CONCEPT)) ? stateData.getJSONArray(POINT_CONCEPT) : null;
						if (pointConceptData != null) {
							for (int j = 0; j < pointConceptData.length(); j++) {
								JSONObject point = pointConceptData.getJSONObject(j);
								String pc_name = (!point.isNull(PC_NAME)) ? point.getString(PC_NAME) : null;
								if (pc_name != null && pc_name.compareTo(PC_GREEN_LEAVES) == 0) {
									score = (!point.isNull(PC_SCORE)) ? point.getInt(PC_SCORE) : null;
								}
							}
						}
						classification.put(playerId, score);
					}
				}
			}

		}
		return classification;
	}

	public List<ClassificationData> correctClassificationIncData(String allStatus, Map<String, String> allNicks, /*
																													 * List<Player> allNicks,
																													 */Long timestamp, String type) throws JSONException {
		List<ClassificationData> playerClassList = new ArrayList<ClassificationData>();

		/*
		 * allStatus = "{" + "\"pointConceptName\": \"green leaves\"," + "\"type\": \"INCREMENTAL\"," + "\"board\": [" + "{" + "\"score\": 12," + "\"playerId\": \"3\"" + "}," + "{" + "\"score\": 10," + "\"playerId\": \"16\"" + "}," + "{" + "\"score\": 4," + "\"playerId\": \"4\"" + "}" + "]" + "}";
		 */

		if (allStatus != null && !allStatus.isEmpty()) {
			JSONObject allIncClassData = new JSONObject(allStatus);
			if (allIncClassData != null) {
				JSONArray allPlayersDataList = (!allIncClassData.isNull("board")) ? allIncClassData.getJSONArray("board") : null;
				if (allPlayersDataList != null) {
					for (int i = 0; i < allPlayersDataList.length(); i++) {
						JSONObject profileData = allPlayersDataList.getJSONObject(i);
						String playerId = (!profileData.isNull(PLAYER_ID)) ? profileData.getString(PLAYER_ID) : "0";
						Integer playerScore = (!profileData.isNull(PC_SCORE)) ? profileData.getInt(PC_SCORE) : 0;
						String nickName = getPlayerNickNameById(allNicks, playerId); // getPlayerNameById(allNicks,
																						// playerId);
						ClassificationData playerClass = new ClassificationData();
						playerClass.setNickName(nickName);
						playerClass.setPlayerId(playerId);
						playerClass.setScore(playerScore);
						if (nickName != null && !nickName.isEmpty()) { // if
																				// nickName
																				// present
																				// (user
																				// registered
																				// and
																				// active)
							playerClassList.add(playerClass);
						}
					}
				}
			}
		}
		return playerClassList;
	}

	public PlayerClassification completeClassificationPosition(List<ClassificationData> playersClass, ClassificationData actualPlayerClass, Integer from, Integer to) {
		List<ClassificationData> playersClassCorr = new ArrayList<ClassificationData>();
		int from_index = 0;
		PlayerClassification pc = new PlayerClassification();
		List<ClassificationData> cleanedList = new ArrayList<ClassificationData>();
		boolean myPosFind = false;
		if (playersClass != null && !playersClass.isEmpty()) {
			ClassificationData prec_pt = null;
			for (int i = 0; i < playersClass.size(); i++) {
				ClassificationData pt = playersClass.get(i);
				if (i > 0) {
					if (pt.getScore() < prec_pt.getScore()) {
						pt.setPosition(i + 1);
					} else {
						pt.setPosition(prec_pt.getPosition());
					}
				} else {
					pt.setPosition(i + 1);
				}
				prec_pt = pt;
				if (pt.getPlayerId().compareTo(actualPlayerClass.getPlayerId()) == 0) {
					myPosFind = true;
					actualPlayerClass.setPosition(pt.getPosition());
				}
				playersClassCorr.add(pt);
			}
			if (!myPosFind) {
				ClassificationData lastPlayer = playersClass.get(playersClass.size() - 1);
				if (lastPlayer.getScore() == actualPlayerClass.getScore()) {
					actualPlayerClass.setPosition(lastPlayer.getPosition());
				} else {
					actualPlayerClass.setPosition(lastPlayer.getPosition() + 1);
				}
				playersClassCorr.add(actualPlayerClass);
			}
			int to_index = playersClassCorr.size();
			if (from != null) {
				from_index = from.intValue();
			}
			if (to != null) {
				to_index = to.intValue();
			}
			if (from_index < 0)
				from_index = 0;
			if (from_index > playersClassCorr.size())
				from_index = playersClassCorr.size();
			if (to_index < 0)
				to_index = 0;
			if (to_index > playersClassCorr.size())
				to_index = playersClassCorr.size();
			if (from_index >= to_index)
				from_index = to_index;
			try {
				cleanedList = playersClassCorr.subList(from_index, to_index);
			} catch (Exception ex) {
				// do nothings
			}
			pc.setClassificationList(cleanedList);
		} else {
			pc.setClassificationList(playersClass);
			actualPlayerClass.setPosition(0);
		}
		pc.setActualUser(actualPlayerClass);
		return pc;
	}

	/*
	 * private int getActualWeek(long timestamp, String type){ int currWeek = 0; long millisFromGameStart = (type.compareTo("test") == 0) ? timestamp - GAME_STARTING_TIME_TEST : timestamp - GAME_STARTING_TIME; currWeek = (int)Math.ceil((float)millisFromGameStart / MILLIS_IN_WEEK); if(type.compareTo("test") == 0){ currWeek = 2; // forced actual week to 2 week in dev test } return currWeek; }
	 */

//	private String getPlayerNameById(List<Player> allNicks, String id) throws JSONException {
//		boolean find = false;
//		String name = "";
//		if (allNicks != null && !allNicks.isEmpty()) {
//			// JSONObject playersData = new JSONObject(allNickJson);
//			// JSONArray allNicksObjects = (!playersData.isNull("players")) ?
//			// playersData.getJSONArray("players") : null;
//			for (int i = 0; (i < allNicks.size()) && !find; i++) {
//				Player player = allNicks.get(i);
//				if (player != null) {
//					String socialId = player.getPlayerId();
//					if (socialId.compareTo(id) == 0) {
//						name = player.getNickname();
//						find = true;
//					}
//				}
//			}
//		}
//		return name;
//	}

	private String getPlayerNickNameById(Map<String, String> allNicks, String id) {
		String name = "";
		if (allNicks != null && !allNicks.isEmpty()) {
			name = allNicks.get(id);
		}
		return name;
	}

	public String getUrlFromBadgeName(String gamificationUrl, String b_name) {
		BadgesData badge = badgeCache.getBadge(b_name);
		if (badge != null) {
			return gamificationUrl + "/" + badge.getPath();
		}
		return null;
	}
	
	private Map<String, Object> buildPlayerData(String playerId, String gameId, String nickName) {
		Map<String, Object> map = Maps.newTreeMap();
		map.put("playerId", playerId);
		map.put("gameId", gameId);
		map.put("nickName", nickName);
		return map;
	}

}
