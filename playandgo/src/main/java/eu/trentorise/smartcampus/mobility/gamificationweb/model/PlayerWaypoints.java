package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.util.List;

import com.google.common.collect.Lists;


public class PlayerWaypoints {

	private String user_id;
	private String activity_id;
	private String activity_type;
	
	private List<PlayerWaypoint> waypoints = Lists.newArrayList();

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public String getActivity_id() {
		return activity_id;
	}

	public void setActivity_id(String activity_id) {
		this.activity_id = activity_id;
	}

	public String getActivity_type() {
		return activity_type;
	}

	public void setActivity_type(String activity_type) {
		this.activity_type = activity_type;
	}

	public List<PlayerWaypoint> getWaypoints() {
		return waypoints;
	}

	public void setWaypoints(List<PlayerWaypoint> waypoints) {
		this.waypoints = waypoints;
	}

	public void flatten() {
		for (PlayerWaypoint pw: waypoints) {
			pw.setActivity_id(activity_id);
			pw.setActivity_type(activity_type);
			pw.setUser_id(user_id);
		}
	}
	
}
