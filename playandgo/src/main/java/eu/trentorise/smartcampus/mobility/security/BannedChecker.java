package eu.trentorise.smartcampus.mobility.security;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;

@Component
public class BannedChecker {

	@Autowired
	@Value("${externalDataDir}")
	private String externalDataDir;			
	
	private Multimap<String, String> banned;
	
	private static Log logger = LogFactory.getLog(BannedChecker.class);
	
	public BannedChecker() {
		banned = HashMultimap.create();
	}
	
	@PostConstruct
	@Scheduled(cron="0 0/15 * * * *")
	private void init() {
		logger.info("Reading banned players list");
		String src = externalDataDir + "/banned.csv";
		List<String> lines = null;
		try {
			lines = Resources.readLines(new File(src).toURI().toURL(), Charsets.UTF_8);
		} catch (IOException e) {
			logger.error("Error reading " + src);
			return;
		}
		banned.clear();
		for (String line: lines) {
			String[] vals = line.split(",");
			if (vals.length != 2) {
				logger.error("Bad format for line: " + line);
				continue;
			}
			banned.put(vals[0], vals[1]);
		}
		logger.info("Read banned players: " + banned);
	}
	
	public boolean isBanned(String playerId, String gameId) {
		if (!banned.containsKey(gameId)) {
			return false;
		}
		if (banned.get(gameId).contains(playerId)) {
			return true;
		}
		return false;
	}
	
}
