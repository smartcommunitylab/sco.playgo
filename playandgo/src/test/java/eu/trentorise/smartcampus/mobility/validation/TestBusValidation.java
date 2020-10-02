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

package eu.trentorise.smartcampus.mobility.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.TTDescriptor;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

/**
 * @author raman
 *
 */
public class TestBusValidation {

	public List<List<Geolocation>> TRAIN_SHAPES = new ArrayList<>();
	public List<String> TRAIN_POLYLINES = new ArrayList<>();
	public TTDescriptor BUS_DESCRIPTOR = null;

	public static String shapeFolder = "src/main/resources/validation";
	
	private void initValidationData() throws Exception{
		final File[] trainFiles = (new File(shapeFolder+"/train")).listFiles();
		if (trainFiles != null) {
			for (File f : trainFiles) {
				TRAIN_SHAPES.addAll(TrackValidator.parseShape(new FileInputStream(f)));
			}
			TRAIN_POLYLINES = TRAIN_SHAPES.stream().map(x -> GamificationHelper.encodePoly(x)).collect(Collectors.toList());
		}
		BUS_DESCRIPTOR = new TTDescriptor();
		loadBusFolder(new File(shapeFolder+"/bus"));
		BUS_DESCRIPTOR.build(100);
	}
	
	private void loadBusFolder(File file) throws Exception {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			InputStream shapes = null, stops = null, trips = null, stopTimes = null;
			
			for (File f : files) {
				if (f.isDirectory()) {
					loadBusFolder(f);
				} else {
					if ("stops.txt".equals(f.getName())) stops = new FileInputStream(f);
					if ("shapes.txt".equals(f.getName())) shapes = new FileInputStream(f);
					if ("stop_times.txt".equals(f.getName())) stopTimes = new FileInputStream(f);
					if ("trips.txt".equals(f.getName())) trips = new FileInputStream(f);
				}
			}
			if (shapes != null && stops != null && stopTimes != null && trips != null) {
				BUS_DESCRIPTOR.load(stops, trips, stopTimes, shapes);
			}
		}
	}
	
	@Before
	public void tearUp() throws Exception {
		initValidationData();
	}
	
	@Test
	public void testLoad() {
		Assert.assertTrue(true);
	}
	
	@Test
	public void testBusTrip() throws JsonParseException, JsonMappingException, IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		Map map = mapper.readValue(TestBusValidation.class.getResourceAsStream("/bustrip2.json"), Map.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Geolocation> track = ((Collection<?>)map.get("geolocationEvents")).stream().map(o -> mapper.convertValue(o, Geolocation.class)).collect(Collectors.toList());
		Map<String, String> shapes = BUS_DESCRIPTOR.filteredPolylines(track);
		System.err.println(shapes.keySet());
		ValidationStatus validateFreeBus = TrackValidator.validateFreeBus(track, BUS_DESCRIPTOR.filterShapes(track), null);
		System.err.println(validateFreeBus);
		
	}
	
	@Test
	public void testTrainTrip() throws JsonParseException, JsonMappingException, IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		Map map = mapper.readValue(TestBusValidation.class.getResourceAsStream("/traintrip.json"), Map.class);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Geolocation> track = ((Collection<?>)map.get("geolocationEvents")).stream().map(o -> mapper.convertValue(o, Geolocation.class)).collect(Collectors.toList());
		ValidationStatus validateFreeBus = TrackValidator.validateFreeTrain(track, TRAIN_SHAPES, null);
		System.err.println(validateFreeBus);
		
	}

}
