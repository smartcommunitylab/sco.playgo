package eu.trentorise.smartcampus.mobility.geolocation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Coords {
	private long accuracy;

	private double altitude;

	private double heading;

	private double latitude;

	private double longitude;

	private double speed;

	public long getAccuracy() {
		return this.accuracy;
	}

	public double getAltitude() {
		return this.altitude;
	}

	public double getHeading() {
		return this.heading;
	}

	public double getLatitude() {
		return this.latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public double getSpeed() {
		return this.speed;
	}

	public void setAccuracy(long accuracy) {
		this.accuracy = accuracy;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public void setHeading(double heading) {
		this.heading = heading;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	@Override
	public String toString() {
		return latitude + "," + longitude;
	}
}
