package eu.trentorise.smartcampus.mobility.geolocation.model;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class GeolocationsEvent {
	
//	private String travelId;
	
	private Map<String, Object> device;

	private ArrayList<Location> location;

//	public String getTravelId() {
//		return travelId;
//	}
//
//	public void setTravelId(String travelId) {
//		this.travelId = travelId;
//	}

	public ArrayList<Location> getLocation() {
		return this.location;
	}

	public Map<String, Object> getDevice() {
		return device;
	}

	public void setDevice(Map<String, Object> device) {
		this.device = device;
	}

	public void setLocation(ArrayList<Location> location) {
		this.location = location;
	}
}