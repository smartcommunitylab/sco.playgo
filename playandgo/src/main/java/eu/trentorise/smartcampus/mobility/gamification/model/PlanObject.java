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

package eu.trentorise.smartcampus.mobility.gamification.model;

import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

/**
 * @author raman
 *
 */
public class PlanObject {

	private String userId, appName;
	private long timestamp;
	private SingleJourney data;

	public PlanObject() {
		super();
	}

	public PlanObject(SingleJourney data, String userId, String appName) {
		super();
		this.data = data;
		this.userId = userId;
		this.appName = appName;
		this.timestamp = System.currentTimeMillis();
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public Object getData() {
		return data;
	}
	public void setData(SingleJourney data) {
		this.data = data;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
}
