package eu.trentorise.smartcampus.mobility.security;

import java.io.Serializable;
import java.util.List;

public class GameInfo implements Serializable {

	private String id;
	private String user;
	private String password;
	private String start;
	private List<Shape> areas;
	private Boolean send;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public List<Shape> getAreas() {
		return areas;
	}

	public void setAreas(List<Shape> areas) {
		this.areas = areas;
	}

	public Boolean getSend() {
		return send;
	}

	public void setSend(Boolean sendEmail) {
		this.send = sendEmail;
	}


}
