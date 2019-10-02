package eu.trentorise.smartcampus.mobility.storage;

import org.springframework.data.annotation.Id;

import eu.trentorise.smartcampus.mobility.model.RouteMonitoring;

public class RouteMonitoringObject extends RouteMonitoring {

	@Id
	private String id;

	private String userId;
	
	public RouteMonitoringObject() {
	}
	
	public RouteMonitoringObject(RouteMonitoring rm) {
		this.clientId = rm.getClientId();
		this.appId = rm.getAgencyId();
		this.agencyId = rm.getAgencyId();
		this.routeId = rm.getRouteId();
		this.recurrency = rm.getRecurrency();
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
