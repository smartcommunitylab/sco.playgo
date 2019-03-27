package eu.trentorise.smartcampus.mobility.storage;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Position;

import org.springframework.data.annotation.Id;

import eu.trentorise.smartcampus.mobility.model.BasicItinerary;

public class ItineraryObject extends BasicItinerary {

	@Id
	private String id;
	private String userId;

	public ItineraryObject() {
	}

	public ItineraryObject(String userId, String clientId, Itinerary data, Position originalFrom, Position originalTo, String name) {
		super();
		this.userId = userId;
		this.clientId = clientId;
		this.data = data;
		this.originalFrom = originalFrom;
		this.originalTo = originalTo;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
