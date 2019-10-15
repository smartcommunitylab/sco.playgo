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

package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.security.InvalidKeyException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tool for generating different links for web pages
 * 
 * @author raman
 *
 */
@Component
public class WebLinkUtils {

	@Autowired
	@Value("${playgoURL}")
	private String playgoURL;
	@Autowired
	@Value("${gamification.secretKey1}")
	private String secretKey1;
	@Autowired
	@Value("${gamification.secretKey2}")
	private String secretKey2;

	private static final String SURVEY_URL = "%s/gamificationweb/survey/%s/%s/%s";
	private static final String UNSUBSCRIBE_URL = "%s/gamificationweb/unsubscribeMail/%s";
	
	EncryptDecrypt cryptUtils;

	@PostConstruct
	public void init() throws Exception {
		cryptUtils = new EncryptDecrypt(secretKey1, secretKey2);
	}
	
	/**
	 * Generate a survey URL
	 * @param playerId
	 * @param survey
	 * @param lang
	 * @return
	 * @throws Exception
	 */
	public  String createSurveyUrl(String playerId, String gameId, String survey, String lang) throws Exception {
		String id = cryptUtils.encrypt(playerId+":"+gameId);
		String compileSurveyUrl = String.format(SURVEY_URL, playgoURL, lang, survey, id);
		return compileSurveyUrl;
	}
	
	/**
	 * Generate a survey URL
	 * @param playerId
	 * @param survey
	 * @param lang
	 * @return
	 * @throws Exception
	 */
	public  String createUnsubscribeUrl(String playerId, String gameId) throws Exception {
		String id = cryptUtils.encrypt(playerId + ":" + gameId);
		String compileSurveyUrl = String.format(UNSUBSCRIBE_URL, playgoURL, id);
		return compileSurveyUrl;
	}
	

	/**
	 * @param playerId
	 * @return identity corresponding to the string
	 * @throws Exception 
	 */
	public PlayerIdentity decryptIdentity(String value) throws Exception {
		String decrypted = cryptUtils.decrypt(value);
		String[] parts = decrypted.split(":");
		if (parts == null || parts.length != 2) throw new InvalidKeyException("Invalid identity content: "+decrypted);
		PlayerIdentity identity = new PlayerIdentity();
		identity.playerId = parts[0];
		identity.gameId = parts[1];
		return identity;
	}
	
	/**
	 * Player identity structure
	 * @author raman
	 *
	 */
	public static class PlayerIdentity {
		public String playerId, gameId;
	}
	
}
