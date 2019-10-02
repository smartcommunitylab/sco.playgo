package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public class Timestamp {

	@Id
	private ObjectId id;
	
	private String gameId;
	private String type;
	private Long timestamp;

	public Timestamp() {
	}

	public Timestamp(String gameId, String type, Long timestamp) {
		super();
		this.gameId = gameId;
		this.type = type;
		this.timestamp = timestamp;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String appId) {
		this.gameId = appId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

}
