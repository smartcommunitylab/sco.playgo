package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import org.bson.types.Binary;
import org.springframework.data.annotation.Id;

public class Avatar {
	
	@Id
	private String id;
	
	private Binary avatarData;
	private Binary avatarDataSmall;
	
	private String contentType;
	private String fileName;
	
	private String playerId;
	private String gameId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Binary getAvatarData() {
		return avatarData;
	}

	public void setAvatarData(Binary avatarData) {
		this.avatarData = avatarData;
	}

	public Binary getAvatarDataSmall() {
		return avatarDataSmall;
	}

	public void setAvatarDataSmall(Binary avatarDataSmall) {
		this.avatarDataSmall = avatarDataSmall;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
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
	
}
