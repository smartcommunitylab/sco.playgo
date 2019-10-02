/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.gamification.model;

import eu.trentorise.smartcampus.mobility.model.ItineraryRecurrency;

/**
 * @author raman
 *
 */
public class ItineraryDescriptor implements Comparable<ItineraryDescriptor> {

	private String userId;
	private String tripId;

	private String tripName;
	
	private long startTime;
	private long endTime;
	private ItineraryRecurrency recurrency;
	
	private String freeTrackingTransport;
	
	private TrackedInstance instance;
	
	public ItineraryDescriptor() {
	}
	

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the tripId
	 */
	public String getTripId() {
		return tripId;
	}

	/**
	 * @param tripId the tripId to set
	 */
	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	/**
	 * @return the tripName
	 */
	public String getTripName() {
		return tripName;
	}

	/**
	 * @param tripName the tripName to set
	 */
	public void setTripName(String tripName) {
		this.tripName = tripName;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the recurrency
	 */
	public ItineraryRecurrency getRecurrency() {
		return recurrency;
	}

	/**
	 * @param recurrency the recurrency to set
	 */
	public void setRecurrency(ItineraryRecurrency recurrency) {
		this.recurrency = recurrency;
	}

	/**
	 * @return the instances
	 */
	public TrackedInstance getInstance() {
		return instance;
	}

	/**
	 * @param instances the instance to set
	 */
	public void setInstance(TrackedInstance instance) {
		this.instance = instance;
	}

	/**
	 * @return the freeTrackingTransport
	 */
	public String getFreeTrackingTransport() {
		return freeTrackingTransport;
	}

	/**
	 * @param freeTrackingTransport the freeTrackingTransport to set
	 */
	public void setFreeTrackingTransport(String freeTrackingTransport) {
		this.freeTrackingTransport = freeTrackingTransport;
	}

	@Override
	public int compareTo(ItineraryDescriptor o) {
		return new Long(o.startTime).compareTo(new Long(startTime));
	}
	
	
}
