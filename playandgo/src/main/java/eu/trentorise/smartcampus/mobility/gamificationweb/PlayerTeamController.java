/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import eu.trentorise.smartcampus.mobility.gamification.GamificationCache;
import eu.trentorise.smartcampus.mobility.gamification.model.PlayerTeam;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ClassificationData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo.Initiative;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.storage.PlayerTeamRepository;
import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.AccountProfile;

/**
 * @author raman
 *
 */
@Controller
public class PlayerTeamController {

	private static transient final Logger logger = LoggerFactory.getLogger(PlayerTeamController.class);

	@Autowired
	private PlayerTeamRepository repo;
	@Autowired
	private PlayerRepositoryDao playerRepo;
	
	@Autowired
	private StatusUtils statusUtils;
	@Autowired
	private GamificationCache gamificationCache;

	@Autowired
	private GameSetup gameSetup;
	@Autowired
	private AppSetup appSetup;
	@Value("${teammgmt.aacURL}")
	private String aacURL;	
	@Value("${teammgmt.clientId}")
	private String clientId;	

	private BasicProfileService profileService;
	
	private LoadingCache<String, TeamClassification> teamState;
	
	@PostConstruct
	private void init() {
		profileService = new BasicProfileService(aacURL);
		teamState = CacheBuilder.newBuilder().refreshAfterWrite(30, TimeUnit.MINUTES).build(new CacheLoader<String, TeamClassification>() {
			@Override
			public TeamClassification load(String id) {
				PlayerTeam team = repo.findById(id).orElse(null);
				if (team == null) return null;

				TeamClassification res = new TeamClassification();
				res.setId(team.getId());
				res.setCustomData(team.getCustomData());
				try {
					Double score = computeTeamScore(team.getGameId(), team.getInitiative(), id);
					if (team.getMembers() != null && team.getMembers().size() > 0) score = score / team.getMembers().size();
					res.setScore(score);
				} catch (Exception e) {
					logger.error("Error computing team score: "+ e.getMessage(), e);
					res.setScore(0d);
				}
				return res;
			}
		});
			
	}	

	
	@GetMapping("/initiatives/web/{gameId}/{initiative}")
	public 
	ModelAndView webMgmt(@PathVariable String gameId, @PathVariable String initiative) {
		ModelAndView model = new ModelAndView("web/initiatives/mgmt");
		Initiative obj = findInitiative(gameId, initiative);
		model.addObject("initiative", obj);
		model.addObject("gameId", gameId);
		model.addObject("aacEndpoint", aacURL);
		model.addObject("clientId", clientId);
		return model;
	}
	@GetMapping("/initiatives/web/{gameId}/{initiative}/board")
	public 
	ModelAndView webBoard(@PathVariable String gameId, @PathVariable String initiative) {
		ModelAndView model = new ModelAndView("web/initiatives/board");
		Initiative obj = findInitiative(gameId, initiative);
		model.addObject("initiative", obj);
		return model;
	}
	@GetMapping("/initiatives/web/{gameId}/{initiative}/board/rest")
	public 
	ResponseEntity<List<TeamClassification>> webBoardAPI(@PathVariable String gameId, @PathVariable String initiative) {
		List<PlayerTeam> teams = repo.findByGameIdAndInitiative(gameId, initiative);
		List<TeamClassification> list = teams.stream().map(t -> {
			try {
				return teamState.get(t.getId());
			} catch (ExecutionException e) {
				e.printStackTrace();
				return null;
			}
		}).filter(s -> s != null).collect(Collectors.toList());
		list.sort((a,b) -> a.getScore() > b.getScore() ? -1 : a.getScore() < b.getScore() ? 1 : 0);
		double points = -1;
		for (int i = 0; i < list.size(); i++) {
			TeamClassification c = list.get(i);
			if (c.getScore() == null) c.setScore(0d);
			if (c.getScore() != points) {
				c.setPosition(i+1);
			} else {
				c.setPosition(list.get(i-1).getPosition());
			}
			points = c.getScore();
		}
		
		return ResponseEntity.ok(list);
	}

	@GetMapping("/initiatives/mgmt/{gameId}/{initiative:.*}/my")
	public ResponseEntity<List<PlayerTeam>> getMyTeams(@PathVariable String gameId, @PathVariable String initiative) {
		String owner = getUsername();
		Initiative obj = findInitiative(gameId, initiative);
		if (obj == null || obj.getAccountMask() != null && !matches(owner, obj.getAccountMask())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(repo.findByGameIdAndInitiativeAndOwner(gameId, initiative, owner ));
	}

	@GetMapping("/initiatives/mgmt/{gameId}/{initiative:.*}/model")
	public ResponseEntity<Initiative> getInitative(@PathVariable String gameId, @PathVariable String initiative) {
		Initiative obj = findInitiative(gameId, initiative);
		if (obj == null) {
			throw new SecurityException("Not authorized to access initiative");
		}
		return ResponseEntity.ok(obj);
	}
	
	
	@PostMapping("/initiatives/mgmt/{gameId}/{initiative:.*}/team")
	public ResponseEntity<PlayerTeam> saveTeam(@PathVariable String gameId, @PathVariable String initiative, @RequestBody PlayerTeam team) {
		String owner = getUsername();
		Initiative obj = findInitiative(gameId, initiative);
		if (obj == null || obj.getAccountMask() != null && !matches(owner, obj.getAccountMask())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		team.setOwner(owner);
		team.setGameId(gameId);
		team.setInitiative(initiative);
		validate(team, obj);
		
		team = repo.save(team);
		teamState.refresh(team.getId());
		return ResponseEntity.ok(team); 
	}

	@DeleteMapping("/initiatives/mgmt/{gameId}/{initiative:.*}/team/{teamId}")
	public ResponseEntity<Void> deleteTeam(@PathVariable String gameId, @PathVariable String initiative, @PathVariable String teamId) {
		String owner = getUsername();
		Initiative obj = findInitiative(gameId, initiative);
		if (obj == null || obj.getAccountMask() != null && !matches(owner, obj.getAccountMask())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		PlayerTeam team = repo.findById(teamId).orElse(null);
		if (team != null && team.getOwner().equals(owner)) {
			repo.delete(team);
		}
		return ResponseEntity.ok(null); 
	}

	@GetMapping("/initiatives/mgmt/{gameId}/{initiative:.*}/team/candidates")
	public ResponseEntity<Set<String>> candidates(@PathVariable String gameId, @PathVariable String initiative) {
		String owner = getUsername();
		Initiative obj = findInitiative(gameId, initiative);
		if (obj == null || obj.getAccountMask() != null && !matches(owner, obj.getAccountMask())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Set<String> all = playerRepo.findNicknames(gameId).stream().map(p -> p.getNickname()).collect(Collectors.toSet());
		Set<String> used = repo.findByGameIdAndInitiative(gameId, initiative).stream().flatMap(t -> t.getMembers().stream()).collect(Collectors.toSet());
		all.removeAll(used);
		return ResponseEntity.ok(all); 
	}

	private Initiative findInitiative(String gameId, String initiative) {
		List<Initiative> initiatives = gameSetup.findGameById(gameId).getInitiatives();
		if (initiatives != null) {
			return initiatives.stream().filter(i -> i.getInitiative().equalsIgnoreCase(initiative)).findAny().orElse(null);
		}
		return null;
	}


	private Double computeTeamScore(String gameId, String initiative, String teamId) throws Exception {
		Initiative obj = findInitiative(gameId, initiative);
		PlayerTeam team = repo.findById(teamId).orElse(null);
		AppInfo appInfo = appSetup.findAppsByGameId(gameId).get(0);
		LocalDate from = LocalDate.parse(obj.getStartDate());
		if (team != null) {
			double score = 0;
			if (team.getMembers() != null && team.getMembers().size() > 0) {
				if (obj.getBonusThreshold() != null && obj.getBonus() != null && team.getExpected() != null && team.getExpected() > 0) {
					if (100.0* (team.getMembers().size() / team.getExpected()) > obj.getBonusThreshold()) {
						score += obj.getBonus()*team.getMembers().size();
					}
				}
				for (String m : team.getMembers()) {
					Player player = playerRepo.findByNicknameIgnoreCaseAndGameId("^"+m+"$", gameId);
					if (player != null) {
						try {
							String string = gamificationCache.getPlayerState(player.getPlayerId(), appInfo.getAppId());
							ClassificationData classificationData = statusUtils.playerClassificationSince(string, player.getPlayerId(), m, from.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
							score += classificationData.getScore();
						} catch (Exception e) {
							logger.error("Error computing team player score: "+ e.getMessage(), e);
						}
					}
				};
			}
			return score;
		} 
		return 0d;
	}
	
	/**
	 * @param team
	 * @param obj 
	 */
	private void validate(PlayerTeam team, Initiative obj) {
		// TODO check unique combination of fields
		// TODO check members
	}

	private boolean matches(String owner, String accountMask) {
		return owner.matches(accountMask);
	}

	protected String getUserId() {
		String principal = (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return principal;
	}

	protected String getUsername() {
		try {
			String token = (String)SecurityContextHolder.getContext().getAuthentication().getDetails();
			AccountProfile account = profileService.getAccountProfile(token);
			String username = null;
			for (String aName : account.getAccountNames()) {
				for (String key : account.getAccountAttributes(aName).keySet()) {
					if (key.toLowerCase().contains("email")) {
						username = account.getAccountAttributes(aName).get(key);
						if (username != null)
							break;
					}
				}
				if (username != null) break;
			}
			return username;
		} catch (Exception e) {
			return null;
		}
	}

	public static class TeamClassification {
		private String id;
		private Double score;
		private int position;
		private Map<String, String> customData;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public Double getScore() {
			return score;
		}
		public void setScore(Double score) {
			this.score = score;
		}
		public Map<String, String> getCustomData() {
			return customData;
		}
		public void setCustomData(Map<String, String> customData) {
			this.customData = customData;
		}
		public int getPosition() {
			return position;
		}
		public void setPosition(int position) {
			this.position = position;
		}
		
	}
}
