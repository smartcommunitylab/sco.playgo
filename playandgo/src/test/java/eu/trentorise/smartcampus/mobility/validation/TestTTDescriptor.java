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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.TTDescriptor;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

/**
 * @author raman
 *
 */
public class TestTTDescriptor {

	public List<List<Geolocation>> TRAIN_SHAPES = new ArrayList<>();
	public List<String> TRAIN_POLYLINES = new ArrayList<>();
	public TTDescriptor BUS_DESCRIPTOR = null;

	public static String shapeFolder = "src/main/resources/validation";
	
	@Test
	public void initValidationData() throws Exception{
		final File[] trainFiles = (new File(shapeFolder+"/train")).listFiles();
		if (trainFiles != null) {
			for (File f : trainFiles) {
				TRAIN_SHAPES.add(TrackValidator.parseShape(new FileInputStream(f)).get(0));
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
}
