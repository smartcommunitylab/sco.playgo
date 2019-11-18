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

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus;
import eu.trentorise.smartcampus.mobility.security.Circle;
import eu.trentorise.smartcampus.mobility.security.Shape;

/**
 * @author raman
 *
 */
public class TestFreeTrackValidation {

	private Circle circle;

	@Before
	public void init() {
		circle = new Circle();
		circle.setCenter(new double[] {0,0});
		circle.setRadius(1000000);
	}

	/**
	 * Validate travel with two points only, but reasonable average speed for bike.
	 */
	@Test
	public void validate2PointTrack() {
		List<Geolocation> track = new LinkedList<>();
		track.add(new Geolocation(46.0600098832467, 11.1215887226056, new Date(1573726743000L)));
		track.get(0).setAccuracy(100L);
		track.add(new Geolocation(46.0700229171719, 11.1215292176946, new Date(1573727671000L)));
		track.get(1).setAccuracy(100L);

		List<Shape> areas = Collections.singletonList(circle );
		ValidationStatus status = TrackValidator.validateFreeBike(track, areas );
		Assert.assertEquals(TravelValidity.INVALID, status.getValidationOutcome());

	}
}
