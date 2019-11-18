package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.Avatar;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.storage.AvatarRepository;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;
import eu.trentorise.smartcampus.mobility.util.ImageUtils;

@RestController
public class FileController {

	@Autowired
	@Value("${resourceDir}")
	private String resourceDir;

	private static final String DEFAULT_USER = "default_user";
	
	private final static int DIMENSION = 640;
	private final static int DIMENSION_SMALL = 160;

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private PlayerRepositoryDao playerRepository;
	
	@Autowired
	private AvatarRepository avatarRepository; 

	private static Log logger = LogFactory.getLog(FileController.class);

	@PostConstruct
	public void init() throws Exception {
		Avatar avatar = avatarRepository.findById(DEFAULT_USER).orElse(null);
		if (avatar == null) {
			avatar = new Avatar();
			
			InputStream is = Resources.asByteSource(new File(resourceDir + "img/"+  DEFAULT_USER + ".png").toURI().toURL()).openBufferedStream();
			BufferedImage bs = ImageIO.read(is);
			
			byte cb[] = ImageUtils.compressImage(bs, "image/png", DIMENSION);
			byte cbs[] = ImageUtils.compressImage(bs, "image/png", DIMENSION_SMALL);
			
			System.err.println(cb.length + "/" + cbs.length);
			
			Binary bb = new Binary(cb);
			Binary bbs = new Binary(cbs);
			avatar.setId(DEFAULT_USER);
			avatar.setAvatarData(bb);
			avatar.setAvatarDataSmall(bbs);
			avatar.setContentType("image/png");
			avatar.setFileName("default.png");

			avatarRepository.save(avatar);
		}
	}
	
	@PostMapping("/gamificationweb/player/avatar")
	public @ResponseBody void uploadPlayerAvatar(@RequestParam("data") MultipartFile data, @RequestHeader(required = true, value = "appId") String appId, HttpServletResponse response)
			throws Exception {
		Player player = null;

		try {
			String userId = getUserId();
			if (userId == null) {
				logger.error("User not found.");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String gameId = appSetup.findAppById(appId).getGameId();
			if (gameId == null) {
				logger.error("GameId not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			player = playerRepository.findByPlayerIdAndGameId(userId, gameId);

			if (player == null) {
				logger.error("Player not found.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			if (data.getSize() > 10 * 1024 * 1024) {
				logger.error("Image too big.");
				response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
				return;
			}
			
			MediaType mediaType = MediaType.parseMediaType(data.getContentType());
			
			if (!mediaType.isCompatibleWith(MediaType.IMAGE_GIF) && !mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) && !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
				logger.error("Image format not supported.");
				response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
				return;				
			}
			
			Avatar av = avatarRepository.findByPlayerIdAndGameId(userId, gameId);
			if (av == null) {
				av = new Avatar();
			}
			
			BufferedImage bs = ImageIO.read(data.getInputStream());
			
			byte cb[] = ImageUtils.compressImage(bs, data.getContentType(), DIMENSION);
			byte cbs[] = ImageUtils.compressImage(bs, data.getContentType(), DIMENSION_SMALL);

			System.err.println(cb.length + "/" + cbs.length);
			
			Binary bb = new Binary(cb);
			Binary bbs = new Binary(cbs);
			av.setAvatarData(bb);
			av.setAvatarDataSmall(bbs);
			av.setContentType(data.getContentType());
			av.setFileName(data.getOriginalFilename());
			av.setPlayerId(userId);
			av.setGameId(findGameId(appId));

			avatarRepository.save(av);
		} catch (Exception e) {
			logger.error("Error in post avatar: " + e.getMessage(), e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping(value = "/gamificationweb/player/avatar/{appId}/{playerId}") //, produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> getPlayerAvatarDataSmall(@PathVariable String appId, @PathVariable String playerId, HttpServletResponse response) throws Exception {
		Avatar avatar = avatarRepository.findByPlayerIdAndGameId(playerId, findGameId(appId));
		if (avatar == null || avatar.getAvatarDataSmall() == null) {
			avatar = avatarRepository.findById(DEFAULT_USER).orElse(null);
		}
		
		response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=86400");
		response.setHeader(HttpHeaders.CONTENT_TYPE, avatar.getContentType());
		response.setIntHeader(HttpHeaders.CONTENT_LENGTH, avatar.getAvatarDataSmall().getData().length);

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(avatar.getContentType())).body(avatar.getAvatarDataSmall().getData());		
	}	
	
	@GetMapping(value = "/gamificationweb/player/avatar/{appId}/{playerId}/big") //, produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> getPlayerAvatarData(@PathVariable String appId, @PathVariable String playerId, HttpServletResponse response) throws Exception {
		Avatar avatar = avatarRepository.findByPlayerIdAndGameId(playerId, findGameId(appId));
		if (avatar == null || avatar.getAvatarData() == null) {
			avatar = avatarRepository.findById(DEFAULT_USER).orElse(null);
		}
		
		response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=86400");
		response.setHeader(HttpHeaders.CONTENT_TYPE, avatar.getContentType());
		response.setIntHeader(HttpHeaders.CONTENT_LENGTH, avatar.getAvatarData().getData().length);

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(avatar.getContentType())).body(avatar.getAvatarData().getData());
	}		
	
	protected String getUserId() {
		String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return principal;
	}

	private String findGameId(String appId) {
		String gameId = appSetup.findAppById(appId).getGameId();
		return gameId;
	}
	
}
