package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class PlayerWaypoint {

	private String user_id;
	private String activity_id;
	private String activity_type;

	////
	
	private String timestamp;
	private Double latitude;
	private Double longitude;

	private Long accuracy;
	private Double speed;
	private String waypoint_activity_type;
	private Long waypoint_activity_confidence;

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Long getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(Long accuracy) {
		this.accuracy = accuracy;
	}

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	public String getWaypoint_activity_type() {
		return waypoint_activity_type;
	}

	public void setWaypoint_activity_type(String waypoint_activity_type) {
		this.waypoint_activity_type = waypoint_activity_type;
	}

	public Long getWaypoint_activity_confidence() {
		return waypoint_activity_confidence;
	}

	public void setWaypoint_activity_confidence(Long waypoint_activity_confidence) {
		this.waypoint_activity_confidence = waypoint_activity_confidence;
	}

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
	
	////
	
	
	

}
