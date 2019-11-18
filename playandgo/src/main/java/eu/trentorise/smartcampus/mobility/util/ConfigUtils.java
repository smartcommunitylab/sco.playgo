package eu.trentorise.smartcampus.mobility.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.CheckinData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekPrizeData;

@Component
public class ConfigUtils {

	@Autowired
	@Value("${weeklyDataDir}")
	private String weeklyDataDir;		
	
	private List<WeekConfData> weekConfData = null;
	
//	private LoadingCache<String, List<CheckinData>> checkinEvents;

	public enum ConfigDataType {
		WEEK_DATA, CHECKIN_EVENT_DATA;
	}
	
	private LoadingCache<ConfigDataType, List<?>> configData;
	
	private LoadingCache<String, List<WeekPrizeData>> prizesData;
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
	
	private static String genericTemplate;
	
	@PostConstruct
	public void init() throws IOException{
//		checkinEvents = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
//				.build(new CacheLoader<String, List<CheckinData>>() {
//					@Override
//					public List<CheckinData> load(String any) throws Exception {
//						String cvsSplitBy = ",";
//						List<CheckinData> checkinDataList = Lists.newLinkedList();
//
//						List<String> lines = Resources.readLines(new File(weeklyDataDir + "/checkin_configuration.csv").toURI().toURL(), Charsets.UTF_8);
//
//						for (int i = 1; i < lines.size(); i++) {
//							String line = lines.get(i);
//							if (line.trim().isEmpty()) continue;
//
//							// use comma as separator
//							String[] checkinValues = line.split(cvsSplitBy);
//							LocalDate from = null, to = null;
//							from = LocalDate.parse(checkinValues[1]);
//							to = LocalDate.parse(checkinValues[2]);
//							CheckinData event = new CheckinData();
//							event.setFrom(from);
//							event.setTo(to);
//							event.setName(checkinValues[0]);
//							
//							logger.debug(String.format("Checkin file: checkin name %s, from %s, to %s", checkinValues[0], from, to));
//							checkinDataList.add(event);
//						}
//
//						return checkinDataList;
//					}
//				});		
		
		
		configData = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
				.build(new CacheLoader<ConfigDataType, List<?>>() {
					@Override
					public List<?> load(ConfigDataType key) {
						try {
						switch (key) {
						case WEEK_DATA:
							return loadWeekConfFile();
						case CHECKIN_EVENT_DATA:
							return loadCheckinFile();
						}
						} catch (Exception e) {
							logger.error("Error reading " + key, e);
						}
						
						return Collections.emptyList();
					}
				});	
		
		
		prizesData = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
				.build(new CacheLoader<String, List<WeekPrizeData>>() {
					@Override
					public List<WeekPrizeData> load(String lang) {
						try {
							return loadWeekPrizesFile(lang);
						} catch (Exception e) {
							logger.error("Error reading week prizes " + lang, e);
						}
						return Collections.emptyList();
					}
				});		
		
		genericTemplate = readGenericEmailTemplate();
		
	}
	
	
	public List<WeekConfData> getWeekConfData() throws Exception {
		return (List<WeekConfData>)configData.get(ConfigDataType.WEEK_DATA);
	}
	
	public List<CheckinData> getCheckinEventData() throws Exception {
		return (List<CheckinData>)configData.get(ConfigDataType.CHECKIN_EVENT_DATA);
	}	
	
	public List<WeekPrizeData> getWeekPrizes(int weeknum, String lang) {
		List<WeekPrizeData> prizeWeekData = Lists.newArrayList();
		
		try {
		List<WeekPrizeData> allPrizes = prizesData.get(lang);
		
		for (int i = 0; i < allPrizes.size(); i++) {
			if (allPrizes.get(i).getWeekNum() == weeknum) {
				prizeWeekData.add(allPrizes.get(i));
			}
		}
		} catch (Exception e) {
			logger.error("Error getting week prizes " + lang + " for week " + weeknum, e);
		}
		return prizeWeekData;
	}	
	
	public String getGenericEmailTemplate() {
		return genericTemplate;
	}	
	// Method used to read a week conf data file and store all values in a list of WeekConfData object
	private List<WeekConfData> loadWeekConfFile() throws Exception {
		synchronized(this) {
			
			String src = weeklyDataDir + "/game_week_configuration.csv";
			String cvsSplitBy = ",";
			weekConfData = Lists.newArrayList();

			List<String> lines = Resources.readLines(new File(src).toURI().toURL(), Charsets.UTF_8);

			for (int i = 1; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line.trim().isEmpty()) continue;
				
				// use comma as separator
				String[] weekConfValues = line.split(cvsSplitBy);
				int weekNum = Integer.parseInt(weekConfValues[0]);
				String weekTheme = weekConfValues[1];
				String weekThemeEng = weekConfValues[2];
				String areChallenges = weekConfValues[3];
				String arePrizes = weekConfValues[4];
				String arePrizesLast = weekConfValues[5];
				String actualWeek = weekConfValues[6];
				String actualWeekEnd = weekConfValues[7];
				logger.debug(String.format("Week conf file: week num %s, theme %s, challenges %s, prizes %s, prizes last %s, actual week %s", weekNum, weekTheme, areChallenges, arePrizes, arePrizesLast,
						actualWeek));
				// value conversion from string to boolean
				Boolean areChall = (areChallenges.compareTo("Y") == 0) ? true : false;
				Boolean arePriz = (arePrizes.compareTo("Y") == 0) ? true : false;
				Boolean arePrizLast = (arePrizesLast.compareTo("Y") == 0) ? true : false;
				WeekConfData wconf = new WeekConfData(weekNum, weekTheme, weekThemeEng, areChall, arePriz, arePrizLast, actualWeek, actualWeekEnd);
				weekConfData.add(wconf);
			}
			
			return weekConfData;
		}
	}	
	
	private List<CheckinData> loadCheckinFile() throws Exception {
		String cvsSplitBy = ",";
		List<CheckinData> checkinDataList = Lists.newLinkedList();

		List<String> lines = Resources.readLines(new File(weeklyDataDir + "/checkin_configuration.csv").toURI().toURL(), Charsets.UTF_8);

		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.trim().isEmpty()) continue;

			// use comma as separator
			String[] checkinValues = line.split(cvsSplitBy);
			LocalDate from = null, to = null;
			from = LocalDate.parse(checkinValues[1]);
			to = LocalDate.parse(checkinValues[2]);
			CheckinData event = new CheckinData();
			event.setFrom(from);
			event.setTo(to);
			event.setName(checkinValues[0]);
			
			logger.debug(String.format("Checkin file: checkin name %s, from %s, to %s", checkinValues[0], from, to));
			checkinDataList.add(event);
		}

		return checkinDataList;
	}
	
	private List<WeekPrizeData> loadWeekPrizesFile(String lang) throws Exception {
		String cvsSplitBy = ",";
		List<WeekPrizeData> prizeWeekFileData = Lists.newArrayList();

		List<String> lines = Resources.readLines(new File(weeklyDataDir + "/game_week_prize_" + lang + ".csv").toURI().toURL(), Charsets.UTF_8);

		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.trim().isEmpty()) continue;

			// use comma as separator
			String[] weekPrizeValues = line.split(cvsSplitBy);
			int weekNum = Integer.parseInt(weekPrizeValues[0]);
			String weekPrize = weekPrizeValues[1];
			String target = weekPrizeValues[2];
			String sponsor = weekPrizeValues.length > 3 ? weekPrizeValues[3] : "-";
			logger.debug(String.format("Week prize file: week num %s, prize %s, target %s, sponsor %s", weekNum, weekPrize, target, sponsor));
			WeekPrizeData wPrize = new WeekPrizeData(weekNum, weekPrize, target, sponsor);
			prizeWeekFileData.add(wPrize);
		}

		return prizeWeekFileData;
	}

	public WeekConfData getWeek(long timestamp) {
		try {
			for (WeekConfData week : getWeekConfData()) {
				if (week.isWeek(timestamp)) {
					return week;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;		
	}
	
	public WeekConfData getCurrentWeekConf() {
		try {
			for (WeekConfData week : getWeekConfData()) {
				if (week.currentWeek()) {
					return week;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}	
	
	public WeekConfData getPreviousWeekConf() {
		try {
			for (WeekConfData week : getWeekConfData()) {
				if (week.previousWeek()) {
					return week;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}		
	
	public List<String> getActiveCheckinEvents() {
		final LocalDate now = LocalDate.now();
		
		try{
			return getCheckinEventData().stream().filter(e -> !e.getFrom().isAfter(now) && !e.getTo().isBefore(now)).map(e -> e.getName()).collect(Collectors.toList());
		} catch (Exception e){
			logger.error("Error reading checkin list", e);
			return Collections.emptyList();
		}
	}
	
	private String readGenericEmailTemplate() throws IOException {
		String src = weeklyDataDir + "/email-generic-content.html";
		return Files.toString(new File(src), Charset.forName("UTF-8"));
	}
	
}
