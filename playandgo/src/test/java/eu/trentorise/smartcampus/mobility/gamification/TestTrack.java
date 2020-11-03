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

package eu.trentorise.smartcampus.mobility.gamification;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.trentorise.smartcampus.mobility.geolocation.model.GeolocationsEvent;


/**
 * @author raman
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableConfigurationProperties
public class TestTrack {

	@Autowired
	private GeolocationsProcessor processor;

	@Before
	public void setUp() throws IOException {
	}
	
	
	@Test
	public void testUploadTrack() throws Exception {
		GeolocationsEvent track = new ObjectMapper().readValue(getClass().getResourceAsStream("/track.json"), GeolocationsEvent.class);
		processor.storeGeolocationEvents(track, "trentinoplaygo2019", "1", "5d9353a3f0856342b2dded7f");
	}
}
