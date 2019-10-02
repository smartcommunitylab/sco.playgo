/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
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

package eu.trentorise.smartcampus.mobility.model;

import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;

import java.util.List;
import java.util.Map;

/**
 * @author raman
 *
 */
public class TripData {

	private String tripId;
	private Map<CreatorType, String> delay;
	private List<String> stopTimes;
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
	 * @return the delay
	 */
	public Map<CreatorType, String> getDelay() {
		return delay;
	}
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(Map<CreatorType, String> delay) {
		this.delay = delay;
	}
	/**
	 * @return the stopTimes
	 */
	public List<String> getStopTimes() {
		return stopTimes;
	}
	/**
	 * @param stopTimes the stopTimes to set
	 */
	public void setStopTimes(List<String> stopTimes) {
		this.stopTimes = stopTimes;
	}
}
