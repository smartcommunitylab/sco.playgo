package eu.trentorise.smartcampus.mobility.geolocation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Battery {
	
	private boolean is_charging;

	private double level;

	public boolean getIs_charging() {
		return this.is_charging;
	}

	public double getLevel() {
		return this.level;
	}

	public void setIs_charging(boolean is_charging) {
		this.is_charging = is_charging;
	}

	public void setLevel(double level) {
		this.level = level;
	}
}