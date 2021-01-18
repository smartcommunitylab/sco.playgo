package eu.trentorise.smartcampus.mobility.controller.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.gamificationweb.EncryptDecrypt;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerProfile;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerWaypoint;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.PlayerWaypoints;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameSetup;

@RestController
public class ProfileController {

	@Value("${gamification.secretKey1}")
	private String secretKey1;
	@Autowired
	@Value("${gamification.secretKey2}")
	private String secretKey2;	
	
	private static Log logger = LogFactory.getLog(ProfileController.class);

	private static FastDateFormat shortSdfApi = FastDateFormat.getInstance("yyyy-MM-dd");
	private static FastDateFormat shortSdfDb = FastDateFormat.getInstance("yyyy/MM/dd");
	private static FastDateFormat shortSdfGameInfo = FastDateFormat.getInstance("dd/MM/yyyy");
	private static FastDateFormat extendedSdf = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss,Z");
	private static FastDateFormat monthSdf = FastDateFormat.getInstance("yyyy-MM");

	private CsvMapper csvMapper;
	private CsvSchema schema;
	
	@Value("${waypointsDir}")
	private String waypointsDir;

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;

	@Autowired
	private AppSetup appSetup;

	@Autowired
	private GameSetup gameSetup;
	
	private EncryptDecrypt cryptUtils;

	@PostConstruct
	public void init() throws Exception {
		JsonFactory jsonFactory = new JsonFactory();
		jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		
		csvMapper = new CsvMapper();
		csvMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
		csvMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
		
//		schema = csvMapper.schemaFor(new TypeReference<PlayerWaypoint>() {
//		});
		
//		schema = csvMapper.schemaFor(PlayerWaypoint.class);
		
		CsvSchema.Builder builder = new CsvSchema.Builder();
        builder.setUseHeader(true);
        builder.setNullValue("");
        builder.addColumn("user_id");
        builder.addColumn("activity_id");
        builder.addColumn("activity_type");
        builder.addColumn("timestamp");
        builder.addColumn("latitude");
        builder.addColumn("longitude");
        builder.addColumn("accuracy");
        builder.addColumn("speed");
        builder.addColumn("waypoint_activity_type");
        builder.addColumn("waypoint_activity_confidence");
        
        schema = builder.build();
		
		cryptUtils = new EncryptDecrypt(secretKey1, secretKey2);
		
//		generateWaypoints();
	}
	
	
	@GetMapping("/profile/{campaignId}")
	public @ResponseBody List<PlayerProfile> profile(@PathVariable String campaignId, @RequestParam(required = false) String date_from, @RequestParam(required = false) String date_to)
			throws Exception {
		AppInfo app = appSetup.findAppById(campaignId);
		String gameId = app.getGameId();
		Date startGame = shortSdfGameInfo.parse(gameSetup.findGameById(gameId).getStart());
		Calendar c = new GregorianCalendar();
		c.setTime(startGame);
		int year = c.get(Calendar.YEAR);

		Criteria criteria = new Criteria("gameId").is(gameId);
		List<Criteria> criterias = Lists.newArrayList();
		if (date_from != null) {
			long from = shortSdfApi.parse(date_from).getTime();
			criterias.add(new Criteria("personalData.timestamp").gte(from));
		}
		if (date_to != null) {
			long to = shortSdfApi.parse(date_to).getTime();
			criterias.add(new Criteria("personalData.timestamp").lte(to));
		}
		if (!criterias.isEmpty()) {
			Criteria[] cs = criterias.toArray(new Criteria[criterias.size()]);
			criteria = criteria.andOperator(cs);
		}

		Query query = new Query(criteria);

		List<Player> players = template.find(query, Player.class);

		List<PlayerProfile> profiles = players.stream().map(x -> convertPlayer(x, year)).collect(Collectors.toList());
		Collections.sort(profiles);

		return profiles;
	}
	
//	@GetMapping("/waypoints/generate")
//	public @ResponseBody void generateWaypointsFiles() throws Exception {
//		generateWaypoints();
//	}
	

//	@Scheduled(cron = "0 0 4 * * *")
	public void generateWaypoints() throws Exception {
		logger.info("Starting waypoints generation");
		List<String> campaignIds = appSetup.getApps().stream().map(x -> x.getAppId()).collect(Collectors.toList());
		for (String campaignId : campaignIds) {
			try {
				generateWaypoints(campaignId);
				logger.info("Generation for " + campaignId + " ended");
			} catch (Exception e) {
				logger.error("Error generating waypoints for " + campaignId);
			}
		}
		logger.info("Ended waypoints generation");
	}
	
	
	public void generateWaypoints(String campaignId) throws Exception {
		try {
		AppInfo app = appSetup.findAppById(campaignId);
		String gameId = app.getGameId();
		if (gameId == null) {
			return;
		}
		
		Stopwatch sw = Stopwatch.createStarted();
		int created = 0;
		
		logger.info("Generating waypoints for " + campaignId);

		Date startGame = shortSdfGameInfo.parse(gameSetup.findGameById(gameId).getStart());		
		
		String currentMonth = monthSdf.format(new Date());
		String gameStart = shortSdfDb.format(startGame);		
		
		SortedSet<String> months = findMonths(campaignId, gameStart);	
		logger.info("Months with trips: " + months.size());
		
		Criteria criteria0 = new Criteria("gameId").is(gameId);
		Query query0 = new Query(criteria0);
		query0.fields().include("playerId");
		List<Player> players = template.find(query0, Player.class);
		logger.info("Found players: " + players.size());
//		List<Player> players = template.findAll(Player.class);
		Collections.sort(players, new Comparator<Player>() {

			@Override
			public int compare(Player o1, Player o2) {
				return Strings.padStart(o1.getPlayerId(), 6, '0').compareTo(Strings.padStart(o2.getPlayerId(), 6, '0'));
			}
		});
		
		for (String month: months) {
			created += createMissingMonthCsv(month, currentMonth, campaignId, players);
		}
		
		sw.stop();
		logger.info("Total Waypoints generated: " + created + ", time elapsed: " + sw.elapsed(TimeUnit.SECONDS));
		} catch (Exception e) {
			logger.error("Error generating points for " + campaignId, e);
		}

	}

	private PlayerProfile convertPlayer(Player p, int year) {
		PlayerProfile pp = new PlayerProfile();
		try {
			pp.setUser_id(cryptUtils.encrypt(p.getPlayerId()));
		} catch (Exception e) {
			logger.error("Error encrypting player id", e);
		}
		Long rd = (Long) p.getPersonalData().getOrDefault("timestamp", 0);
		pp.setDate_of_registration(shortSdfApi.format(rd));

		completeWithSurvey(pp, p, year);

		return pp;
	}

	private void completeWithSurvey(PlayerProfile pp, Player p, int year) {
		if (!p.getSurveys().containsKey("start")) {
			return;
		}
		Map<String, Object> survey = p.getSurveys().get("start");
		String gender;
		switch ((String) survey.getOrDefault("gender", "")) {
		case "maschio":
			gender = "male";
			break;
		case "femmina":
			gender = "female";
			break;
		default:
			gender = "unknown";
		}
		pp.setGender(gender);

		if (survey.containsKey("age")) {
			String age = (String) survey.get("age");
			if (age.contains("+")) {
				pp.setYear_of_birth_max(findYear(age.replace("+", ""), year));
			} else {
				String years[] = age.split("-");
				pp.setYear_of_birth_min(findYear(years[1], year));
				pp.setYear_of_birth_max(findYear(years[0], year));
			}
		}

		if (survey.containsKey("mean")) {
			String mean = null;
			switch ((String) survey.get("mean")) {
			case "auto":
				mean = "private car";
				break;
			case "public":
				mean = "public transport";
				break;
			case "bike":
				mean = "bike";
				break;
			case "walk":
				mean = "walking";
				break;
			case "bikesharing":
				mean = "bike sharing";
				break;
			case "carsharing":
				mean = "car sharing/pooling";
				break;
			}
			pp.setMain_mode_pre_campaign(mean);
		}

		if (survey.containsKey("kms")) {
			String kms = (String) survey.get("kms");
			if (kms.contains("+")) {
				pp.setDaily_travelled_distance_min(kms.replace("+", ""));
			} else {
				String km[] = kms.split("-");
				pp.setDaily_travelled_distance_min(km[0]);
				pp.setDaily_travelled_distance_max(km[1]);
			}
		}
	}

	private String findYear(String age, int year) {
		return "" + (year - Integer.parseInt(age));
	}
	
	private int createMissingMonthCsv(String date, String currentDate, String campaignId, List<Player> players) throws Exception {
//		List<PlayerWaypoints> result = Lists.newArrayList();
		int resultN = 0;
		
		String suffix = date.replace("/", "-");
		File dir = new File(waypointsDir + "/" + campaignId + "_" + suffix);
		File fz = new File(waypointsDir + "/" + campaignId + "_" + suffix + ".zip");
		if (dir.exists() || fz.exists()) {
			if (suffix.equals(currentDate)) {
				logger.info("Overwriting current waypoints " + suffix);
				if (!dir.exists()) {
					dir.mkdir();
				}
			} else {
				logger.info("Skipping existing waypoints " + suffix);
				return 0;
			}
		} else {
			logger.info("Writing missing waypoints " + suffix);
			dir.mkdir();
		}

		Stopwatch sw = Stopwatch.createStarted();

		logger.info("Creating files in: " + dir);

		ObjectWriter csvWriter = csvMapper.writerFor(PlayerWaypoint.class).with(schema);

		Date monthDate = monthSdf.parse(suffix);
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(monthDate);
		int month = calendar.get(Calendar.MONTH);

		String today = shortSdfApi.format(new Date());
		
		while (calendar.get(Calendar.MONTH) == month) {
			String day = Strings.padStart("" + calendar.get(Calendar.DAY_OF_MONTH), 2, '0');

			File wfCsv = new File(dir, campaignId + "_" + suffix + "-" + day + ".csv");
			
//			if (!wfCsv.exists() || today.equals(suffix + "-" + day)) {
//				logger.info("Skipping already generated file " + wfCsv.getName());
//				continue;
//			}
			
			if (today.compareTo(suffix + "-" + day) < 0) {
				logger.info("Stopping at current day");
				break;
			}
			
			if (!wfCsv.exists() || today.equals(suffix + "-" + day)) {
				wfCsv.createNewFile();
				FileOutputStream csvFos = new FileOutputStream(wfCsv);
				logger.info("Adding file: " + wfCsv.getName());

				SequenceWriter csvSequenceWriter = csvWriter.writeValues(csvFos);
				csvSequenceWriter.init(true);

				if (players != null) {
					for (Player p : players) {
						Criteria criteria = new Criteria("appId").is(campaignId).and("userId").is(p.getPlayerId()).and("freeTrackingTransport").ne(null).and("day").is(date + "/" + day);

						Query query = new Query(criteria);

						CloseableIterator<TrackedInstance> it = template.stream(query, TrackedInstance.class);
						while (it.hasNext()) {
							TrackedInstance ti = it.next();
							PlayerWaypoints pws = convertTrackedInstance(ti);
							pws.flatten();
							resultN++;
							
							csvSequenceWriter.writeAll(pws.getWaypoints());
						}
					}

				}
				
				csvSequenceWriter.flush();
				csvSequenceWriter.close();				
				csvFos.close();
			}
			

			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		

		
		zipMissingMonth(date, currentDate, campaignId);
		
		sw.stop();
		logger.info("Waypoints generated: " + resultN + ", time elapsed: " + sw.elapsed(TimeUnit.SECONDS));
		return resultN;
	}		
	
	
	private void zipMissingMonth(String date, String currentDate, String campaignId) throws Exception {
		String suffix = date.replace("/", "-");
		File dir = new File(waypointsDir + "/" + campaignId + "_" + suffix);
		if (!dir.exists()) {
			logger.info("Directory " + dir.getName() + " does not exists, skipping" );
			return;
		}
		File f = new File(waypointsDir + "/" + campaignId + "_" + suffix + ".zip");
		if (f.exists() && !suffix.equals(currentDate)) {
			logger.info("Zip " + f.getName() + " already generated, skipping");
			return;
		}
		
		logger.info("Generating zip file " + f.getName());
		Map<String, String> env = Maps.newHashMap();
		env.put("create", "true");
		URI fsURI = new URI("jar:" + f.toURI().toString());
		FileSystem fs = FileSystems.newFileSystem(fsURI, env);
		
		for (File jf: dir.listFiles()) {
			if (jf.length() < 10) {
				logger.debug("Not adding empty file " + jf.getName());
				continue;
			}
			Files.copy(Paths.get(jf.toURI()), fs.getPath(jf.getName()), StandardCopyOption.REPLACE_EXISTING);
		}
		fs.close();
		
		if (!suffix.equals(currentDate)) {
			for (File jf: dir.listFiles()) {
				jf.delete();
			}			
			dir.delete();
		}
	}
	
	
//	private int createMissingMonth(String date, String currentDate, String campaignId, List<Player> players) throws Exception {
//		List<PlayerWaypoints> result = Lists.newArrayList();
//
//		String from = date + "/01";
//		String to = date + "/31";
//
//		String suffix = date.replace("/", "-");
//		File f = new File(waypointsDir + "/" + campaignId + "_" + suffix + ".zip");
//		if (f.exists()) {
//			if (suffix.equals(currentDate)) {
//				logger.info("Overwriting current waypoints " + suffix);
//			} else {
//				logger.info("Skipping existing waypoints " + suffix);
//				return 0;
//			}
//		} else {
//			logger.info("Writing missing waypoints " + suffix);
//		}
//
//		Stopwatch sw = Stopwatch.createStarted();
//
//		logger.info("Creating zip file: " + f);
//		FileOutputStream fos0 = new FileOutputStream(f);
//		ZipOutputStream fos = new ZipOutputStream(fos0);
//
//		ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
//
//		Date monthDate = monthSdf.parse(suffix);
//		Calendar calendar = new GregorianCalendar();
//		calendar.setTime(monthDate);
//		int month = calendar.get(Calendar.MONTH);
//
////		SequenceWriter sequenceWriter = writer.writeValues(fos);
//		while (calendar.get(Calendar.MONTH) == month) {
//			String day = Strings.padStart("" + calendar.get(Calendar.DAY_OF_MONTH), 2, '0');
//
//			ZipEntry ze = new ZipEntry(campaignId + "_" + suffix + "-" + day + ".json");
//			logger.info("Adding zip entry: " + ze.getName());
//			fos.putNextEntry(ze);
//
//			logger.info("Generating data");
//
//			SequenceWriter sequenceWriter = writer.writeValues(fos);
//			sequenceWriter.init(true);
//
//			if (players != null) {
//				for (Player p : players) {
//					Criteria criteria = new Criteria("appId").is(campaignId).and("userId").is(p.getPlayerId()).and("freeTrackingTransport").ne(null).and("day").is(date + "/" + day);
//					// Criteria criteriaFrom = new Criteria("day").gte(from);
//					// Criteria criteriaTo = new Criteria("day").lte(to);
//					//
//					// criteria = criteria.andOperator(criteriaFrom, criteriaTo);
//
//					Query query = new Query(criteria);
//
//					CloseableIterator<TrackedInstance> it = template.stream(query, TrackedInstance.class);
//					while (it.hasNext()) {
//						TrackedInstance ti = it.next();
//						PlayerWaypoints pws = convertTrackedInstance(ti, sequenceWriter);
//						result.add(pws);
//					}
//				}
//
//				calendar.add(Calendar.DAY_OF_MONTH, 1);
//			}
//			
//			sequenceWriter.flush();
//			sequenceWriter.close();
//		}
////		sequenceWriter.close();
//		fos.close();
//		
//		sw.stop();
//		logger.info("Waypoints generated: " + result.size() + ", time elapsed: " + sw.elapsed(TimeUnit.SECONDS));
//		return result.size();
//	}
	
	private SortedSet<String> findMonths(String appId, String gameStart) {
		Criteria criteria = new Criteria("appId").is(appId).and("freeTrackingTransport").ne(null).and("day").gte(gameStart);
		Query query = new Query(criteria);
		query.fields().include("day");
		
		Set<String> dates = template.find(query, TrackedInstance.class).stream().map(x -> x.getDay().substring(0, x.getDay().lastIndexOf("/"))).sorted().collect(Collectors.toSet());
		SortedSet<String> sortedDates = Sets.newTreeSet(dates);
		return sortedDates;
	}	

	private PlayerWaypoints convertTrackedInstance(TrackedInstance ti) throws Exception {
		PlayerWaypoints pws = new PlayerWaypoints();

		try {
			pws.setUser_id(cryptUtils.encrypt(ti.getUserId()));
		} catch (Exception e) {
			logger.error("Error encrypting player id", e);
		}
		pws.setActivity_id(ti.getId());
		pws.setActivity_type(ti.getFreeTrackingTransport());		
		for (Geolocation loc : ti.getGeolocationEvents()) {
			PlayerWaypoint pw = new PlayerWaypoint();

			pw.setLatitude(loc.getLatitude());
			pw.setLongitude(loc.getLongitude());
			pw.setTimestamp(extendedSdf.format(loc.getRecorded_at()));
			pw.setAccuracy(loc.getAccuracy());
			pw.setSpeed(loc.getSpeed());
			pw.setWaypoint_activity_confidence(loc.getActivity_confidence());
			pw.setWaypoint_activity_type(loc.getActivity_type());

			pws.getWaypoints().add(pw);
		}

		return pws;
	}

}
