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

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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

	@Autowired
	@Value("${surveysDir}")
	private String surveysConfigDir;

	
	private static final String SURVEY_URL = "%s/gamificationweb/survey/%s/%s/%s";
	private static final String UNSUBSCRIBE_URL = "%s/gamificationweb/unsubscribeMail/%s";
	
	EncryptDecrypt cryptUtils;

	private LoadingCache<String, Map<String, String>> surveyExternalLinkCache = CacheBuilder.newBuilder()
		       .maximumSize(100)
		       .expireAfterWrite(30, TimeUnit.MINUTES)
		       .build(new CacheLoader<String, Map<String, String>>() {
					@SuppressWarnings("unchecked")
					@Override
					public Map<String, String> load(String key) throws Exception {
						// ignore key -> load always whole config
						try {
							URL url = new File(surveysConfigDir + (surveysConfigDir.endsWith("/") ? "" : "/")+ "external.json").toURI().toURL();
							Map<String, String> map = new ObjectMapper().readValue(url, Map.class);
							return map;
						} catch (Exception e) {
						}
						return Collections.emptyMap();
					}
		    	   
		       });
	
	
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
		String external = surveyExternalLinkCache.get("external").get(survey);
		if (external != null) {
			// assume external is pattern-based with String Template framework
			ST st = new ST(external);
			st.add("id", id);
			st.add("lang", lang);
			return st.render();
		}
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
