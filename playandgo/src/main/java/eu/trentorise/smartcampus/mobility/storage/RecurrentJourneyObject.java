package eu.trentorise.smartcampus.mobility.storage;

import org.springframework.data.annotation.Id;

import eu.trentorise.smartcampus.mobility.model.BasicRecurrentJourney;
import eu.trentorise.smartcampus.mobility.processor.alerts.RecurrentAlertsSent;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;

public class RecurrentJourneyObject extends BasicRecurrentJourney {

	@Id
	private String id;
	
	private String userId;
	
	RecurrentAlertsSent alertsSent;

	public RecurrentJourneyObject() {
	}

	public RecurrentJourneyObject(String userId, String clientId, RecurrentJourney data, String name) {
		super();
		this.userId = userId;
		this.clientId = clientId;
		this.data = data;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public RecurrentAlertsSent getAlertsSent() {
		return alertsSent;
	}

	public void setAlertsSent(RecurrentAlertsSent alertsSent) {
		this.alertsSent = alertsSent;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public RecurrentJourney getData() {
		return data;
	}

	public void setData(RecurrentJourney data) {
		this.data = data;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getMonitor() {
		return monitor;
	}

	public void setMonitor(Boolean monitor) {
		this.monitor = monitor;
	}
	
}
