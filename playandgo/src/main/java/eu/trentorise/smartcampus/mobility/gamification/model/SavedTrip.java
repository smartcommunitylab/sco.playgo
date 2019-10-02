package eu.trentorise.smartcampus.mobility.gamification.model;

import java.util.Date;

import org.springframework.data.annotation.Id;

import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

public class SavedTrip {

	@Id
	private String id;	
	
	private Date createdAt;
	private ItineraryObject itinerary;
	private String request;

	public SavedTrip() {
	}
	
	public SavedTrip(Date createdAt, ItineraryObject itinerary, String  request) {
		this.createdAt = createdAt;
		this.itinerary = itinerary;
		this.request = request;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public ItineraryObject getItinerary() {
		return itinerary;
	}

	public void setItinerary(ItineraryObject itinerary) {
		this.itinerary = itinerary;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

}
