package eu.trentorise.smartcampus.mobility.security;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.storage.GameStateRepository;

@Component
public class GameSetup {

	@Value("classpath:/games-info.yml")
	private Resource resource;

	private List<GameInfo> games;
	private Map<String, GameInfo> gamesMap;
	
	@Autowired
	private GameStateRepository gameStateRepo;
	
	private static Log logger = LogFactory.getLog(GameSetup.class);

	public GameSetup() {
	}	
	
	@PostConstruct
	public void init() throws Exception {
		Constructor constructor = new Constructor(GameSetup.class);
		constructor.addTypeDescription(new TypeDescription(Circle.class, "!circle"));
		constructor.addTypeDescription(new TypeDescription(Polygon.class, "!polygon"));
		
		Yaml yaml = new Yaml(constructor);
		GameSetup data = (GameSetup) yaml.load(resource.getInputStream());
		this.games = data.games;
		
		if (gamesMap == null) {
			gamesMap = Maps.newTreeMap();
			for (GameInfo game : games) {
				GameState gameState = gameStateRepo.findById(game.getId()).orElse(null);
				if (gameState == null) {
					gameState = new GameState();
					gameState.setId(game.getId());
					gameState.setActive(Boolean.TRUE.equals(game.getSend()));
					gameState.setSendMail(gameState.getActive());
					gameState = gameStateRepo.save(gameState);
				}
				game.setSend(Boolean.TRUE.equals(gameState.getSendMail()));
				game.setActive(gameState.getActive());
				
				gamesMap.put(game.getId(), game);
			}
		}
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public List<GameInfo> getGames() {
		return games;
	}

	public void setGames(List<GameInfo> games) {
		this.games = games;
	}

	public Map<String, GameInfo> getGamesMap() {
		return gamesMap;
	}

	public void setGamesMap(Map<String, GameInfo> gamesMap) {
		this.gamesMap = gamesMap;
	}

	public static Log getLogger() {
		return logger;
	}

	public static void setLogger(Log logger) {
		GameSetup.logger = logger;
	}
	
	public GameInfo findGameById(String id) {
		return gamesMap.get(id);
	}

	/**
	 * @param gameId
	 */
	public void changeState(String gameId, boolean state) {
		GameInfo game = findGameById(gameId);
		if (game == null) {
			throw new IllegalArgumentException();
		}
		game.setActive(state);
		if (!state) game.setSend(false);
		
		gameStateRepo.findById(gameId).ifPresent(g -> {
			g.setActive(state);
			if (!state) g.setSendMail(false);
			gameStateRepo.save(g);
		});
	}	
	
	public void changeSendMail(String gameId, boolean state) {
		GameInfo game = findGameById(gameId);
		if (game == null) {
			throw new IllegalArgumentException();
		}
		game.setSend(state);
		
		gameStateRepo.findById(gameId).ifPresent(g -> {
			g.setSendMail(state);
			gameStateRepo.save(g);
		});
	}	
	
	
}
