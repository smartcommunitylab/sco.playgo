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
import it.sayservice.platform.smartplanner.data.message.otpbeans.TransitTimeTable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author raman
 *
 */
public class Timetable {

	private List<String> stopIds;
	private List<String> stopNames;
	private List<TripData> trips;
	/**
	 * @return the stopIds
	 */
	public List<String> getStopIds() {
		return stopIds;
	}
	/**
	 * @param stopIds the stopIds to set
	 */
	public void setStopIds(List<String> stopIds) {
		this.stopIds = stopIds;
	}
	/**
	 * @return the stopNames
	 */
	public List<String> getStopNames() {
		return stopNames;
	}
	/**
	 * @param stopNames the stopNames to set
	 */
	public void setStopNames(List<String> stopNames) {
		this.stopNames = stopNames;
	}
	/**
	 * @return the trips
	 */
	public List<TripData> getTrips() {
		return trips;
	}
	/**
	 * @param trips the trips to set
	 */
	public void setTrips(List<TripData> trips) {
		this.trips = trips;
	}
	
	/**
	 * Create {@link Timetable} object from a {@link TransitTimeTable} of 1 day.
	 * @param ttt
	 * @return
	 */
	public static Timetable fromTransitTimeTable(TransitTimeTable ttt) {
		Timetable result = new Timetable();
		result.setStopIds(ttt.getStopsId());
		result.setStopNames(ttt.getStops());
		result.setTrips(new LinkedList<TripData>());
		if (ttt.getTimes() != null && ttt.getTimes().size() > 0) {
			List<List<String>> trips = ttt.getTimes().get(0);
			for (int i = 0; i < trips.size(); i++) {
				TripData td = new TripData();
				td.setStopTimes(trips.get(i));
				td.setTripId(ttt.getTripIds().get(0).get(i));
				Map<String,String> delays = ttt.getDelays().get(0).get(i);
				if (delays != null) {
					td.setDelay(new HashMap<CreatorType, String>());
					for (Entry<String,String> entry : delays.entrySet()) {
						td.getDelay().put(CreatorType.valueOf(entry.getKey()), entry.getValue());
					}
				}
				result.getTrips().add(td);
			}
			
		}
		return result;
	}
}
