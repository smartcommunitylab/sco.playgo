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

package eu.trentorise.smartcampus.mobility.logging;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.sayservice.platform.smartplanner.data.message.alerts.Alert;

/**
 * @author raman
 *
 */
@Component
public class StatLogger {

	@Autowired
	@Qualifier("logMongoTemplate")
	private MongoTemplate mongoTemplate;
	
	@Value("${statlogging.enabled}")
	private Boolean loggingEnabled;
	
	@Value("${statlogging.excluded}")
	private String excluded;	
	
	@Value("${statlogging.samplingPeriod:1}")
	private int samplingPeriod;	
	
	private List<String> excludedList;	
	
	private Map<String, Long> classLogTimestamp = Maps.newConcurrentMap();	

	@PostConstruct
	public void init() {
		if (excluded != null) {
			excludedList = Splitter.on(",").splitToList(excluded);
		} else {
			excludedList = Lists.newArrayList();
		}
	}
	
	public void log(Object sj, String userId) {
		String className = sj.getClass().getSimpleName();
		
		if (excludedList.contains(className)) {
			return;
		}		
		
		if (sj instanceof Alert) {
			className += " - " + ((Alert)sj).getNote();
		}
		
		long last = classLogTimestamp.getOrDefault(className, 0L);
		long delta = System.currentTimeMillis() - last;
		
		boolean write = true;
		if (delta > 1000 * (60 * samplingPeriod - 10)) {
			classLogTimestamp.put(className, System.currentTimeMillis());
		} else {
			write = false;
		}
		
		if (write && Boolean.TRUE.equals(loggingEnabled)) {
			mongoTemplate.save(createData(sj, userId));
		}
	}

	/**
	 * @param sj
	 * @param userId
	 * @return
	 */
	private Object createData(Object sj, String userId) {
		return new StatDataObject(sj, userId);
	}

	
	
}
