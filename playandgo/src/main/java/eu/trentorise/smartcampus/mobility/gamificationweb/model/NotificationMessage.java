package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;
import java.util.Map;

public class NotificationMessage {

	private String id;
	private String type;
	private Map<String, String> title;
	private Map<String, String> description;
	
	private Map<String, List<NotificationMessageExtra>> extras;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getTitle() {
		return title;
	}

	public void setTitle(Map<String, String> title) {
		this.title = title;
	}

	public Map<String, String> getDescription() {
		return description;
	}

	public void setDescription(Map<String, String> description) {
		this.description = description;
	}

	public Map<String, List<NotificationMessageExtra>> getExtras() {
		return extras;
	}

	public void setExtras(Map<String, List<NotificationMessageExtra>> extras) {
		this.extras = extras;
	}



}
