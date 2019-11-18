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

package eu.trentorise.smartcampus.mobility.email;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.CharStreams;

import eu.trentorise.smartcampus.mobility.gamification.GamificationCache;
import eu.trentorise.smartcampus.mobility.gamificationweb.ReportEmailSender;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;


/**
 * @author raman
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableConfigurationProperties
public class TestEmails {

	@Autowired
	private ReportEmailSender sender;
	@Autowired
	private AppSetup appSetup;

	@Autowired
	private PlayerRepositoryDao playerRepositoryDao;	

	@Autowired
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@MockBean
	private GamificationCache cache;
	
	private AppInfo appInfo;
	private String profile;

	@Before
	public void setUp() throws IOException {
		appInfo = appSetup.getApps().get(0);
		Player p = new Player("1", appInfo.getGameId(), "name", "surname", "nick", "name@surname", "it", true, Collections.singletonMap("timestamp", System.currentTimeMillis()), null, true); // default sendMail attribute value is true
		playerRepositoryDao.save(p);
		profile = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/userstatus.json")));

		Mockito.when(cache.getPlayerState(Mockito.any(), Mockito.any()))
		.thenReturn(profile);
	}
	
	@After
	public void tearDown() {
		playerRepositoryDao.deleteAll();
	}
	
	@Test
	public void testWeeklyNotification() throws Exception {
		sender.sendWeeklyNotification();
	}
}
