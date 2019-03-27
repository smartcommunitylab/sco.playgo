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
package eu.trentorise.smartcampus.mobility.util;

import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class HTTPConnector {
	
	private static final Logger logger = LoggerFactory.getLogger(HTTPConnector.class);
	
	public static String doGet(String address, String req, String accept, String contentType, String encoding) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		String url = address + ((req != null) ? ("?" + req) : "");

		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(MapUtils.putAll(new TreeMap<String, String>(), new String[][] {{"Accept", accept}, {"Content-Type", contentType}}))), String.class);

		if (!res.getStatusCode().is2xxSuccessful()) {
			throw new ConnectorException("Failed : HTTP error code : " + res.getStatusCode(), res.getStatusCode().value());
		}

		return res.getBody();
	}	
	
	public static InputStream doStreamGet(String address, String req, String accept, String contentType) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		String url = address + ((req != null) ? ("?" + req) : "");
		
		ResponseEntity<Resource> res = restTemplate.exchange(url, HttpMethod.GET, 
				new HttpEntity<Object>(null, createHeaders(MapUtils.putAll(new TreeMap<String, String>(), new String[][] {{"Accept", accept}, {"Content-Type", contentType}}))),
				Resource.class);

		if (!res.getStatusCode().is2xxSuccessful()) {
			throw new ConnectorException("Failed : HTTP error code : " + res.getStatusCode(), res.getStatusCode().value());
		}		
		
		// TODO check
		return res.getBody().getInputStream();
	}	
	
	public static String doBasicAuthenticationPost(String address, String req, String accept, String contentType, String user, String password) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		String s = user + ":" + password;
		byte[] b = Base64.encodeBase64(s.getBytes());
		String es = new String(b);
		
		ResponseEntity<String> res = restTemplate.exchange(address, HttpMethod.POST, new HttpEntity<Object>(req, createHeaders(MapUtils.putAll(new TreeMap<String, String>(), new String[][] {{"Accept", accept}, {"Content-Type", contentType}, {"Authorization", "Basic " + es}}))),
				String.class);

		if (!res.getStatusCode().is2xxSuccessful()) {
			throw new ConnectorException("Failed : HTTP error code : " + res.getStatusCode(), res.getStatusCode().value());
		}

		return res.getBody();		
	}
	
	public static String doTokenAuthenticationPost(String address, String req, String accept, String contentType, String token) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		ResponseEntity<String> res = restTemplate.exchange(address, HttpMethod.POST, new HttpEntity<Object>(req, createHeaders(MapUtils.putAll(new TreeMap<String, String>(), new String[][] {{"Accept", accept}, {"Content-Type", contentType}, {"Authorization", "Bearer " + token}}))), String.class);

		if (!res.getStatusCode().is2xxSuccessful()) {
			throw new ConnectorException("Failed : HTTP error code : " + res.getStatusCode(), res.getStatusCode().value());
		}

		return res.getBody();		
	}	
	
	public static String doPost(String address, String req, String accept, String contentType) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		ResponseEntity<String> res = restTemplate.exchange(address, HttpMethod.POST, new HttpEntity<Object>(req, createHeaders(MapUtils.putAll(new TreeMap<String, String>(), new String[][] {{"Accept", accept}, {"Content-Type", contentType}}))),
				String.class);

		if (!res.getStatusCode().is2xxSuccessful()) {
			throw new ConnectorException("Failed : HTTP error code : " + res.getStatusCode(), res.getStatusCode().value());
		}

		return res.getBody();			
	}
	
	

	static HttpHeaders createHeaders(Map<String, String> pars) {
		return new HttpHeaders() {
			{
				for (String key: pars.keySet()) {
					if (pars.get(key) != null) {
						set(key, pars.get(key));
					}
				}
			}
		};
	}	
	
}
