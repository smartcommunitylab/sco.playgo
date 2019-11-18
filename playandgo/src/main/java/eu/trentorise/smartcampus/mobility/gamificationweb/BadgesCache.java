package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgesData;

@Component
public class BadgesCache {

	private Map<String, BadgesData> badges;
	
	@Autowired
	@Value("${resourceDir}")
	private String resourceDir;

	@Autowired
	@Value("${badgeConf}")
	private String badgeConf;

	@PostConstruct
	public void init() throws Exception {
		badges = Maps.newTreeMap();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<BadgesData> list = mapper.readValue(new File(badgeConf).toURI().toURL(), new TypeReference<List<BadgesData>>() {
		});
		for (BadgesData badge: list) {
			
			URL resource = new File(resourceDir + badge.getPath()).toURI().toURL(); 
			byte b[] = Resources.asByteSource(resource).read();

			badge.setImageByte(b);					
			badges.put(badge.getTextId(), badge);
		}
	}
	
	public BadgesData getBadge(String name) {
		return badges.get(name);
	}
	
	public List<BadgesData> getAllBadges() {
		return Lists.newArrayList(badges.values());
	}	
	
}
