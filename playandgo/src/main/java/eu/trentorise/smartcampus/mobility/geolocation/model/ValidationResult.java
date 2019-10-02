package eu.trentorise.smartcampus.mobility.geolocation.model;

public class ValidationResult {

	public enum TravelValidity {
		VALID, INVALID, PENDING
	}
	
//	private int geoLocationsN;
//	private int legsLocationsN;
//	private int matchedLocationsN;
//	private Boolean matchedLocations;
//	private Boolean tooFewPoints;
//	private Boolean inAreas;
//	
//	private Set<String> geoActivities;
//	private Set<String> legsActivities;
//	private Boolean matchedActivities;
//	private Boolean tooFast;
//	
//	private TravelValidity travelValidity = TravelValidity.PENDING;
//	
//	private Double averageSpeed;
//	private Double maxSpeed;
//	
//	private Double distance;
//	private Long time;
//	
//	private Double validatedDistance;
//	private Long validatedTime;	
	
	private Boolean plannedAsFreeTracking;
	private ValidationStatus validationStatus;

	public ValidationStatus getValidationStatus() {
		if (validationStatus == null) {
			validationStatus = new ValidationStatus();
			validationStatus.setValidationOutcome(TravelValidity.PENDING);
		}
		return validationStatus;
	}
	public void setValidationStatus(ValidationStatus validationStatus) {
		this.validationStatus = validationStatus;
	}
	
//	public void reset() {
//		matchedLocations = true;
//		matchedActivities = true;
//		tooFast = false;
//		travelValidity =  TravelValidity.PENDING;
//	}
//	
//	public int getGeoLocationsN() {
//		return geoLocationsN;
//	}
//
//	public void setGeoLocationsN(int geoLocationsN) {
//		this.geoLocationsN = geoLocationsN;
//	}
//
//	public int getLegsLocationsN() {
//		return legsLocationsN;
//	}
//
//	public void setLegsLocationsN(int legsLocationsN) {
//		this.legsLocationsN = legsLocationsN;
//	}
//
//	public int getMatchedLocationsN() {
//		return matchedLocationsN;
//	}
//
//	public void setMatchedLocationsN(int matchedLocationsN) {
//		this.matchedLocationsN = matchedLocationsN;
//	}
//
//	public Boolean getMatchedLocations() {
//		return matchedLocations;
//	}
//
//	public void setMatchedLocations(Boolean matchedLocations) {
//		this.matchedLocations = matchedLocations;
//	}
//
//	public Boolean getTooFewPoints() {
//		return tooFewPoints;
//	}
//
//	public void setTooFewPoints(Boolean tooFewPoints) {
//		this.tooFewPoints = tooFewPoints;
//	}
//
//	public Boolean getTooFast() {
//		return tooFast;
//	}
//
//	public Boolean getInAreas() {
//		return inAreas;
//	}
//
//	public void setInAreas(Boolean inAreas) {
//		this.inAreas = inAreas;
//	}
//
//	public void setTooFast(Boolean walkOnly) {
//		this.tooFast = walkOnly;
//	}
//
//	public Set<String> getGeoActivities() {
//		return geoActivities;
//	}
//
//	public void setGeoActivities(Set<String> geoActivities) {
//		this.geoActivities = geoActivities;
//	}
//
//	public Set<String> getLegsActivities() {
//		return legsActivities;
//	}
//
//	public void setLegsActivities(Set<String> legsActivities) {
//		this.legsActivities = legsActivities;
//	}
//
//	public Boolean getMatchedActivities() {
//		return matchedActivities;
//	}
//
//	public void setMatchedActivities(Boolean matchedActivities) {
//		this.matchedActivities = matchedActivities;
//	}
//
//	public Double getAverageSpeed() {
//		return averageSpeed;
//	}
//
//	public void setAverageSpeed(Double averageSpeed) {
//		this.averageSpeed = averageSpeed;
//	}
//
//	public Double getMaxSpeed() {
//		return maxSpeed;
//	}
//
//	public void setMaxSpeed(Double maxSpeed) {
//		this.maxSpeed = maxSpeed;
//	}
//
	public Double getDistance() {
		return getValidationStatus().getDistance();
	}
//
//	public void setDistance(Double distance) {
//		this.distance = distance;
//	}
//
	public Long getTime() {
		return getValidationStatus().getDuration();
	}
//
//	public void setTime(Long time) {
//		this.time = time;
//	}
//
	public TravelValidity getTravelValidity() {
		return getValidationStatus().getValidationOutcome();
	}
//
//	public void setTravelValidity(TravelValidity travelValidity) {
//		this.travelValidity = travelValidity;
//	}
//
//	public Double getValidatedDistance() {
//		return validatedDistance;
//	}
//
//	public void setValidatedDistance(Double validatedDistance) {
//		this.validatedDistance = validatedDistance;
//	}
//
//	public Long getValidatedTime() {
//		return validatedTime;
//	}
//
//	public void setValidatedTime(Long validatedTime) {
//		this.validatedTime = validatedTime;
//	}

	public Boolean getPlannedAsFreeTracking() {
		return plannedAsFreeTracking;
	}

	public void setPlannedAsFreeTracking(Boolean plannedAsFreeTracking) {
		this.plannedAsFreeTracking = plannedAsFreeTracking;
	}

	boolean valid;

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
	
}
