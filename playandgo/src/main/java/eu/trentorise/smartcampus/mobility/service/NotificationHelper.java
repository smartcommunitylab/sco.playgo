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

package eu.trentorise.smartcampus.mobility.service;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.communicator.CommunicatorConnector;
import eu.trentorise.smartcampus.communicator.CommunicatorConnectorException;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.mobility.util.TokenHelper;
import eu.trentorise.smartcampus.network.RemoteConnector;

/**
 * @author raman
 *
 */
@Component
public class NotificationHelper extends RemoteConnector {

	public static final String MS_APP = "core.mobility";
	
	@Autowired
	@Value("${communicatorURL}")
	private String communicatorURL;
	
	@Autowired
	private TokenHelper tokenHelper;
	
	private CommunicatorConnector connector = null;

	private Log logger = LogFactory.getLog(getClass());

	private CommunicatorConnector connector() {
		if (connector == null) {
			try {
				connector = new CommunicatorConnector(communicatorURL, MS_APP);
			} catch (Exception e) {
				logger.error("Failed to instantiate connector: "+e.getMessage(), e);
				e.printStackTrace();
			}
		}
		return connector;
	}


	public void notify(Notification n, String userId, String appId) {
			long when = System.currentTimeMillis();
			n.setTimestamp(when);
			try {
				String token = tokenHelper.getToken();
				logger.info("Token: " + token);
				connector().sendAppNotification(n, appId, Collections.singletonList(userId), token);
			} catch (CommunicatorConnectorException e) {
				e.printStackTrace();
				logger .error("Failed to send notifications: "+e.getMessage(), e);
			}
	}
	
}
