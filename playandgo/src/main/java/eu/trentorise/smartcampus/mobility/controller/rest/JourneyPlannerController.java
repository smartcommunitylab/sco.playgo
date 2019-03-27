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
package eu.trentorise.smartcampus.mobility.controller.rest;

import java.net.URI;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.smartcampus.mobility.gamification.GamificationValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.SavedTrip;
import eu.trentorise.smartcampus.mobility.logging.StatLogger;
import eu.trentorise.smartcampus.mobility.model.BasicItinerary;
import eu.trentorise.smartcampus.mobility.model.BasicRecurrentJourney;
import eu.trentorise.smartcampus.mobility.model.RouteMonitoring;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

@Controller
public class JourneyPlannerController {

	@Value("${mobility.server}")
	private String viaggiaServer;

	@Value("${mobility.port}")
	private String viaggiaPort;	
	
//	@Value("${viaggiaURL}")
//	private String viaggiaURL;		
	
	@Value("${mobilityURL}")
	private String mobilityURL;
	
	@Autowired
	private StatLogger statLogger;
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private GamificationValidator gamificationValidator;

	@Autowired
	private DomainStorage domainStorage;

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	// no crud
	@PostMapping("/plansinglejourney")
	public @ResponseBody List<Itinerary> planSingleJourney(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) SingleJourney journeyRequest, @RequestParam(required = false, defaultValue="default") String policyId,
			@RequestHeader(required = false, value = "UserID") String userId, @RequestHeader(required = false, value = "AppName") String appName) throws Exception {
		try {
			List<Itinerary> result;
			
			try {
				result = (List<Itinerary>) (forward(journeyRequest, HttpMethod.POST, request, response, new TypeReference<List<Itinerary>>() {
				}).getBody());
			} catch (HttpClientErrorException e0) {
				response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
				return null;
			}			
			
			for (Itinerary itinerary : result) {
				gamificationValidator.computeEstimatedGameScore(appName, itinerary, null, false);
			}			
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			response.addHeader("error_msg", e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}	
	
	@PostMapping("/itinerary")
	public @ResponseBody BasicItinerary saveItinerary(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) BasicItinerary itinerary) throws Exception {
		try {
			BasicItinerary result = null;
			
			try {
			result = (BasicItinerary)(forward(itinerary, HttpMethod.POST, request, response, new TypeReference<BasicItinerary>() {}).getBody());
			} catch (HttpClientErrorException e0) {
				response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
				return null;
			}
			String userId = getUserId();
			String clientId = itinerary.getClientId();
			
			ItineraryObject io = new ItineraryObject();
			io.setClientId(clientId);
			io.setUserId(userId);
			io.setOriginalFrom(itinerary.getOriginalFrom());
			io.setOriginalTo(itinerary.getOriginalTo());
			io.setName(itinerary.getName());
			io.setData(itinerary.getData());
			if (itinerary.getAppId() == null || itinerary.getAppId().isEmpty()) {
				io.setAppId(NotificationHelper.MS_APP);
			} else {
				io.setAppId(itinerary.getAppId());
			}
			io.setRecurrency(itinerary.getRecurrency());

			SavedTrip st = new SavedTrip(new Date(), io, RequestMethod.POST.toString());
			domainStorage.saveSavedTrips(st);

			result.setClientId(clientId);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return null;
	}	
	
	@PutMapping("/itinerary/{itineraryId}")
	public @ResponseBody ItineraryObject updateItinerary(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) BasicItinerary itinerary, @PathVariable String itineraryId) throws Exception {
		ItineraryObject result = null;
			try {
			result = (ItineraryObject)(forward(itinerary, HttpMethod.PUT, request, response, new TypeReference<BasicItinerary>() {}).getBody());
			
			SavedTrip st = new SavedTrip(new Date(), result, RequestMethod.PUT.toString());
			domainStorage.saveSavedTrips(st);
			
			return result;
			} catch (HttpClientErrorException e0) {
				response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
				return null;
			}
	}

	@GetMapping("/itinerary")
	public @ResponseBody List<ItineraryObject> getItineraries(HttpServletRequest request, HttpServletResponse response) throws Exception {
		List<ItineraryObject> result = null;
		
		try {
		result = (List<ItineraryObject>)(forward(null, HttpMethod.GET, request, response, new TypeReference<List<ItineraryObject>>() {}).getBody());
		return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}

	@GetMapping("/itinerary/{itineraryId}")
	public @ResponseBody BasicItinerary getItinerary(HttpServletRequest request, HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
		BasicItinerary result = null;

		try {
			result = (BasicItinerary) (forward(null, HttpMethod.GET, request, response, new TypeReference<BasicItinerary>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}

	@DeleteMapping("/itinerary/{itineraryId}")
	public @ResponseBody Boolean deleteItinerary(HttpServletRequest request, HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
		Boolean result = null;

		try {
			result = (Boolean) (forward(null, HttpMethod.DELETE, request, response, new TypeReference<Boolean>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return false;
		}
	}

	@GetMapping("/itinerary/{itineraryId}/monitor/{monitor}")
	public @ResponseBody Boolean monitorItinerary(HttpServletRequest request, HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws Exception {
		Boolean result = null;

		try {
			result = (Boolean) (forward(null, HttpMethod.GET, request, response, new TypeReference<Boolean>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return false;
		}
	}

	// RECURRENT

	@PostMapping("/planrecurrent")
	public @ResponseBody RecurrentJourney planRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) RecurrentJourneyParameters parameters) throws Exception {
		RecurrentJourney result = null;

		try {
			result = (RecurrentJourney) (forward(null, HttpMethod.POST, request, response, new TypeReference<RecurrentJourney>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}

	@PostMapping("/recurrent")
	public @ResponseBody BasicRecurrentJourney saveRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) BasicRecurrentJourney recurrent) throws Exception {
		BasicRecurrentJourney result = null;

		try {
			result = (BasicRecurrentJourney) (forward(recurrent, HttpMethod.POST, request, response, new TypeReference<BasicRecurrentJourney>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}		
	}

	@PostMapping("/recurrent/replan/{itineraryId}")
	public @ResponseBody RecurrentJourney planRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) RecurrentJourneyParameters parameters, @PathVariable String itineraryId)
			throws Exception {
		RecurrentJourney result = null;

		try {
			result = (RecurrentJourney) (forward(parameters, HttpMethod.POST, request, response, new TypeReference<RecurrentJourney>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}	
	}

	@PutMapping("/recurrent/{itineraryId}")
	public @ResponseBody Boolean updateRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) BasicRecurrentJourney recurrent, @PathVariable String itineraryId) throws Exception {
		Boolean result = null;

		try {
			result = (Boolean) (forward(recurrent, HttpMethod.PUT, request, response, new TypeReference<Boolean>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return false;
		}
	}

	@GetMapping("/recurrent")
	public @ResponseBody List<RecurrentJourneyObject> getRecurrentJourneys(HttpServletRequest request, HttpServletResponse response) throws Exception {
		List<RecurrentJourneyObject> result = null;

		try {
			result = (List<RecurrentJourneyObject>) (forward(null, HttpMethod.GET, request, response, new TypeReference<List<RecurrentJourneyObject>>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}

	@GetMapping("/recurrent/{itineraryId}")
	public @ResponseBody RecurrentJourneyObject getRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
		RecurrentJourneyObject result = null;

		try {
			result = (RecurrentJourneyObject) (forward(null, HttpMethod.GET, request, response, new TypeReference<List<RecurrentJourneyObject>>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}

	@DeleteMapping("/recurrent/{itineraryId}")
	public @ResponseBody Boolean deleteRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @PathVariable String itineraryId) throws Exception {
		Boolean result = null;

		try {
			result = (Boolean) (forward(null, HttpMethod.DELETE, request, response, new TypeReference<Boolean>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return false;
		}
	}

	@GetMapping("/recurrent/{itineraryId}/monitor/{monitor}")
	public @ResponseBody Boolean monitorRecurrentJourney(HttpServletRequest request, HttpServletResponse response, @PathVariable String itineraryId, @PathVariable boolean monitor) throws Exception {
		Boolean result = null;

		try {
			result = (Boolean) (forward(null, HttpMethod.GET, request, response, new TypeReference<Boolean>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return false;
		}
	}

	// no crud
	@PostMapping("/monitorroute")
	public @ResponseBody RouteMonitoring saveMonitorRoutes(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) RouteMonitoring req) throws Exception {
		RouteMonitoring result = null;

		try {
			result = (RouteMonitoring) (forward(req, HttpMethod.POST, request, response, new TypeReference<RouteMonitoring>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}
	
	@PutMapping("/monitorroute/{clientId}")
	public @ResponseBody RouteMonitoring updateMonitorRoutes(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) RouteMonitoring req, @PathVariable String clientId) throws Exception {
		RouteMonitoring result = null;

		try {
			result = (RouteMonitoring) (forward(req, HttpMethod.PUT, request, response, new TypeReference<RouteMonitoring>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}	
	
	
	@GetMapping("/monitorroute")
	public @ResponseBody List<RouteMonitoring> getMonitorRoutes(HttpServletRequest request, HttpServletResponse response,  @RequestParam(required = false, value = "active") Boolean active) throws Exception {
		List<RouteMonitoring> result = null;

		try {
			result = (List<RouteMonitoring>) (forward(null, HttpMethod.GET, request, response, new TypeReference<List<RouteMonitoring>>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return null;
		}
	}	
	
	@DeleteMapping("/monitorroute/{clientId}")
	public @ResponseBody Boolean deletetMonitorRoutes(HttpServletRequest request, HttpServletResponse response, @PathVariable String clientId) throws Exception {
		Boolean result = null;

		try {
			result = (Boolean) (forward(null, HttpMethod.DELETE, request, response, new TypeReference<Boolean>() {
			}).getBody());
			return result;
		} catch (HttpClientErrorException e0) {
			response.sendError(e0.getRawStatusCode(), e0.getResponseBodyAsString());
			return false;
		}
	}	
	
	
	public <T> ResponseEntity<?> forward(Object body, 
		    HttpMethod method, HttpServletRequest request, HttpServletResponse response, TypeReference<T> typeReference) 
		    throws Exception {
		    String requestUrl = request.getRequestURI();
		    
		    requestUrl = mobilityURL + requestUrl.substring(requestUrl.indexOf(request.getServletPath()));
		    
		    URI uri = new URI(requestUrl);
		    
		    uri = UriComponentsBuilder.fromUri(uri)
		                              .query(request.getQueryString())
		                              .build(true).toUri();

		    HttpHeaders headers = new HttpHeaders();
		    Enumeration<String> headerNames = request.getHeaderNames();
		    while (headerNames.hasMoreElements()) {
		        String headerName = headerNames.nextElement();
		        headers.set(headerName, request.getHeader(headerName));
		    }

		    HttpEntity<Object> httpEntity = new HttpEntity<Object>(body, headers);
		    RestTemplate restTemplate = new RestTemplate();
		    ResponseEntity<String> resp = null;
		    try {
		        resp = restTemplate.exchange(uri, method, httpEntity, String.class);
		        if (resp.getBody() != null) {
		        	T result = mapper.readValue(resp.getBody(), typeReference);
		        	return new ResponseEntity<T>(result, resp.getStatusCode());
		        } else {
		        	return new ResponseEntity<T>((T)null, resp.getStatusCode());
		        }
		    } catch(Exception e) {
		    	logger.error("Error forwarding request");
		    	throw e;
//		        return ResponseEntity.status(e.getRawStatusCode())
//		                             .headers(e.getResponseHeaders())
//		                             .body(e.getResponseBodyAsString());
		    }
		}
	

	// /////////////////////////////////////////////////////////////////////////////

	/**
	 * @return UserDetails instance from security context
	 */
//	protected String getClientId() {
//		OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
//		return auth.getOAuth2Request().getClientId();
////		return auth.getAuthorizationRequest().getClientId();
//	}

	protected String getUserId() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof String) {
			return (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		} else {
			return null;
		}
	}

}
