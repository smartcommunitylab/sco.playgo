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
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.CharStreams;

import eu.trentorise.smartcampus.mobility.gamificationweb.StatusUtils;


/**
 * @author raman
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableConfigurationProperties
public class TestChallenges {

	@Autowired
	private StatusUtils utils;
	
	private String profile;

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;	

	@Before
	public void setUp() throws IOException {
		profile = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/userstatus.json")));
	}
	
	@Test
	public void empty() {
		Assert.assertTrue(true);
	}
	
	@Test
	public void testParseChallenges() throws Exception {
		utils.convertPlayerData(profile, "1", "2", "nick", gamificationUrl, 1, "it");
	}
}
