package eu.trentorise.smartcampus.mobility.gamification.model;

import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;

public class TravelDetails {

	private ItineraryObject itinerary;
	private String freeTrackingTransport;
	private String geolocationPolyline;
	private ValidationResult validationResult;
	private TravelValidity validity;
	
	public TravelDetails() {
	}

	public ItineraryObject getItinerary() {
		return itinerary;
	}

	public void setItinerary(ItineraryObject itinerary) {
		this.itinerary = itinerary;
	}

	public String getFreeTrackingTransport() {
		return freeTrackingTransport;
	}

	public void setFreeTrackingTransport(String freeTrackingTransport) {
		this.freeTrackingTransport = freeTrackingTransport;
	}

	public String getGeolocationPolyline() {
		return geolocationPolyline;
	}

	public void setGeolocationPolyline(String geolocationPolyline) {
		this.geolocationPolyline = geolocationPolyline;
	}

	public ValidationResult getValidationResult() {
		return validationResult;
	}

	public void setValidationResult(ValidationResult validationResult) {
		this.validationResult = validationResult;
	}

	public TravelValidity getValidity() {
		return validity;
	}

	public void setValidity(TravelValidity validity) {
		this.validity = validity;
	}

}
