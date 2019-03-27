package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import eu.trentorise.smartcampus.mobility.gamification.GamificationCache;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeAssignedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeCompletedNotication;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeFailedNotication;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeInvitationAcceptedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeInvitationCanceledNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeInvitationRefusedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.LevelGainedNotification;
import eu.trentorise.smartcampus.mobility.gamification.model.Notification;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.NotificationMessage;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.NotificationMessageExtra;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Timestamp;
import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.GameInfo;
import eu.trentorise.smartcampus.mobility.security.GameSetup;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.storage.PlayerRepositoryDao;

@Component
public class NotificationsManager {

	private static final List<Class> notificationClasses = Lists.newArrayList(new Class[] 
	{ LevelGainedNotification.class, ChallengeInvitationAcceptedNotification.class, ChallengeInvitationRefusedNotification.class, ChallengeInvitationCanceledNotification.class,
		ChallengeAssignedNotification.class, ChallengeCompletedNotication.class, ChallengeFailedNotication.class });
	private Map<String, Class> notificationClassesMap;
	
	private static transient final Logger logger = LoggerFactory.getLogger(NotificationsManager.class);
	
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@Value("${rabbitmq.host}")
	private String rabbitMQHost;	

	@Value("${rabbitmq.virtualhost}")
	private String rabbitMQVirtualHost;		
	
	@Value("${rabbitmq.port}")
	private Integer rabbitMQPort;
	
	@Value("${rabbitmq.user}")
	private String rabbitMQUser;
	
	@Value("${rabbitmq.password}")
	private String rabbitMQPassword;	
	
	@Value("${rabbitmq.geExchangeName}")
	private String rabbitMQExchangeName;		
	
	@Value("${rabbitmq.geRoutingKeyPrefix}")
	private String rabbitMQroutingKeyPrefix;
	
	@Autowired
	private AppSetup appSetup;
	
	@Autowired
	private GameSetup gameSetup;		
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;		
	
	@Autowired
	private PlayerRepositoryDao playerRepository;
	
	@Autowired
	private ChallengesUtils challengeUtils;	
	
	@Autowired
	private NotificationHelper notificatioHelper;
	
	@Autowired
	private GamificationCache gamificationCache;	
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	private Map<String, NotificationMessage> notificationsMessages;

	@PostConstruct
	public void init() throws Exception {
		notificationClassesMap = Maps.newTreeMap();
		notificationClasses.forEach(x -> {
			notificationClassesMap.put(x.getSimpleName(), x);
		});
		List<NotificationMessage> messages = mapper.readValue(Resources.getResource("notifications/notifications.json"), new TypeReference<List<NotificationMessage>>() {
		});
		notificationsMessages = messages.stream().collect(Collectors.toMap(NotificationMessage::getId, Function.identity()));
		initRabbitMQ();
	}
	
	private void initRabbitMQ() throws Exception {
		Timer timer = new Timer();

		TimerTask tt = new TimerTask() {

			@Override
			public void run() {

				boolean ok = true;

				do {
					logger.info("Connecting to RabbitMQ");
					try {
						ConnectionFactory connectionFactory = new ConnectionFactory();
						connectionFactory.setUsername(rabbitMQUser);
						connectionFactory.setPassword(rabbitMQPassword);
						connectionFactory.setVirtualHost(rabbitMQVirtualHost);
						connectionFactory.setHost(rabbitMQHost);
						connectionFactory.setPort(rabbitMQPort);
						connectionFactory.setAutomaticRecoveryEnabled(true);

						Connection connection = connectionFactory.newConnection();
						Channel rabbitMQChannel = connection.createChannel();
						rabbitMQChannel.basicQos(1);
						rabbitMQChannel.exchangeDeclare(rabbitMQExchangeName, "direct", true);

						Set<String> queues = Sets.newHashSet();
						Set<String> gameIds = appSetup.getApps().stream().map(x -> x.getGameId()).collect(Collectors.toSet());
						
						DefaultConsumer consumer = new DefaultConsumer(rabbitMQChannel) {
							@Override
							public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] b) throws IOException {
								long deliveryTag = envelope.getDeliveryTag();

								String body = new String(b);
								try {
									 processNotification(body);
								} catch (Exception e) {
									logger.error("Error processing message", e);
								}
							}
						};
						
						for (String gameId : gameIds) {
							if (gameId == null) {
								continue;
							}
							GameInfo game = gameSetup.findGameById(gameId);
							if (game == null) { // || game.getSend() == null || !game.getSend()) {
								continue;
							}

							// String queueName = rabbitMQChannel.queueDeclare().getQueue();
							String queueName = rabbitMQChannel.queueDeclare("queue-" + gameId, true, false, false, null).getQueue();
							rabbitMQChannel.queueBind(queueName, rabbitMQExchangeName, rabbitMQroutingKeyPrefix + "-" + gameId);

							boolean autoAck = true;
							String consumerTag = rabbitMQChannel.basicConsume(queueName, autoAck, "", consumer);
							queues.add(queueName);
						}
						ok = true;
						logger.info("Connected to RabbitMQ queues: " + queues);
					} catch (Exception e) {
						logger.error("Problems connecting to RabbitMQ: " + e.getMessage());
						ok = false;
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e1) {
						}
					}
				} while (!ok);
			}
		};

		timer.schedule(tt, 5000);
	}
	
	
	@Scheduled(cron="0 0 12 * * WED")
	public void checkProposedPending() throws Exception {
		for (AppInfo appInfo : appSetup.getApps()) {
			try {
				if (appInfo.getGameId() != null && !appInfo.getGameId().isEmpty()) {
					GameInfo game = gameSetup.findGameById(appInfo.getGameId());
					if (game.getSend() == null || !game.getSend()) {
						continue;
					}
					checkProposedPending(appInfo);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	
	public void sendDirectNotification(String appId, Player toPlayer, String type, Map<String, String> extraData) {
		AppInfo appInfo = appSetup.findAppById(appId);
		
		logger.info("Sending direct notification '" + type + "' to " + toPlayer.getPlayerId());
		eu.trentorise.smartcampus.communicator.model.Notification notification = null;
		
		try {
			notification = buildSimpleNotification(toPlayer.getLanguage(), type, extraData);
		} catch (Exception e) {
			logger.error("Error building notification", e);
		}
		
		if (notification != null) {
			try {
				notificatioHelper.notify(notification, toPlayer.getPlayerId(), appInfo.getMessagingAppId());
			} catch (Exception e) {
				logger.error("Error sending notification", e);
			}
		}
	}
	
	private void checkProposedPending(AppInfo appInfo) throws Exception {
		logger.info("Sending notifications for app " + appInfo.getAppId());

		List<eu.trentorise.smartcampus.communicator.model.Notification> nots = Lists.newArrayList();

		List<Player> players = playerRepository.findAllByGameId(appInfo.getGameId());
		for (Player p : players) {
			String data = gamificationCache.getPlayerState(p.getPlayerId(), appInfo.getAppId());

			List<ChallengeConcept> challengeConcepts = challengeUtils.parse(data);

			boolean proposed = false;
			for (ChallengeConcept challengeConcept : challengeConcepts) {
				if ("PROPOSED".equals(challengeConcept.getState())) {
					proposed = true;
					break;
				}
			}

			if (proposed) {
				logger.info("Sending PROPOSED notification to " + p.getPlayerId());
				eu.trentorise.smartcampus.communicator.model.Notification notification = null;
				try {
					notification = buildSimpleNotification(p.getLanguage(), "PROPOSED", null);
				} catch (Exception e) {
					logger.error("Error building notification", e);
				}

				if (notification != null) {
					try {
						notificatioHelper.notify(notification, p.getPlayerId(), appInfo.getMessagingAppId());
						continue;
					} catch (Exception e) {
						logger.error("Error sending notification", e);
					}
				}
			}

		}
	}
	
	private void processNotification(String body) throws Exception {
		Map<String, Object> map = (Map<String, Object>) mapper.readValue(body, Map.class);
		String type = (String) map.get("type");
		Map obj = (Map) map.get("obj");
		if (type == null || obj == null) {
			logger.error("Bad notification content: " + body);
			return;
		}

		Optional<String> opt = notificationClassesMap.keySet().stream().filter(x -> type.contains(x)).findFirst();

		if (opt.isPresent()) {
			Class clz = notificationClassesMap.get(opt.get());
			Notification not = (Notification) mapper.convertValue(obj, clz);
			
			Criteria criteria = new Criteria("gameId").is(not.getGameId()).and("type").is(type);
			Query query = new Query(criteria);
			
			Update update = new Update();
			update.set("timestamp", not.getTimestamp());
			template.upsert(query, update, Timestamp.class);
			
//			Timestamp old = template.findOne(query, Timestamp.class);
//			
//			long time = not.getTimestamp();
//			if (old == null) {
//				old = new Timestamp(not.getGameId(), type, time);
//			}
//			if (not.getTimestamp() >= old.getTimestamp()){
//				old.setTimestamp(time);
//				template.save(old);
//			}
			
			GameInfo game = gameSetup.findGameById(not.getGameId());

			Player p = playerRepository.findByPlayerIdAndGameId(not.getPlayerId(), not.getGameId());

			if (p != null) {
				eu.trentorise.smartcampus.communicator.model.Notification notification = null;

				try {
					notification = buildNotification(p.getLanguage(), not);
				} catch (Exception e) {
					logger.error("Error building notification", e);
				}
				if (notification != null) {
					List<AppInfo> apps = appSetup.findAppsByGameId(not.getGameId());
					for (AppInfo appInfo : apps) {
						if (game.getSend() == null || !game.getSend()) {
							continue;
						}
						
						try {
							logger.info("Sending '" + not.getClass().getSimpleName() + "' notification to " + not.getPlayerId() + " (" + appInfo.getAppId() + ")");
							notificatioHelper.notify(notification, not.getPlayerId(), appInfo.getMessagingAppId());
						} catch (Exception e) {
							logger.error("Error sending notification", e);
						}
					}
				}
			} else {
				logger.equals("Player " + not.getPlayerId() + " not found");
			}
		}
	}
	
//	@Scheduled(fixedDelay = 1000 * 60 * 10)
//	private void getNotifications() throws Exception {
//		logger.debug("Reading notifications.");
//		
//		List<Notification> nots = Lists.newArrayList();
//		
//		for (AppInfo appInfo : appSetup.getApps()) {
//			if (appInfo.getGameId() != null && !appInfo.getGameId().isEmpty()) {
//				GameInfo game = gameSetup.findGameById(appInfo.getGameId());
//				if (game.getSend() == null || !game.getSend()) {
//					continue;
//				}
//				nots = getNotifications(appInfo.getAppId());
//				
//				if (!nots.isEmpty()) {
//					logger.info("Read " + nots.size() + " notifications for " + appInfo.getAppId());
//				}
//				
//				for (Notification not: nots) {
//					Player p = playerRepository.findByPlayerIdAndGameId(not.getPlayerId(), not.getGameId());
//					
//					if (p != null) {
//						eu.trentorise.smartcampus.communicator.model.Notification notification = null;
//
//						try {
//							notification = buildNotification(p.getLanguage(), not);
//						} catch (Exception e) {
//							logger.error("Error building notification", e);
//						}
//						if (notification != null) {
//							logger.info("Sending '" + not.getClass().getSimpleName() + "' notification to " + not.getPlayerId());
//							try {
//							notificatioHelper.notify(notification, not.getPlayerId(), appInfo.getMessagingAppId());
//							} catch (Exception e) {
//								logger.error("Error sending notification", e);
//							}								
//						}
//					}
//				}				
//				
//			}
//		}
//	}
//	
//	private <T> List<Notification> getNotifications(String appId) throws Exception {
//		logger.debug("Reading notifications for " + appId);
//		
//		List<Notification> nots = Lists.newArrayList();
//		
//		for (Class clz: notificationClasses) {
//		nots.addAll(getNotifications(appId, clz));
//		}
//		
//		return nots;
//	}
//	
//	private <T> List<Notification> getNotifications(String appId, Class<T> clz) throws Exception {
//		logger.debug("Reading notifications for type " + ((Class)clz).getSimpleName());
//		
//		String gameId = getGameId(appId);
//
//		Criteria criteria = new Criteria("gameId").is(gameId).and("type").is(((Class)clz).getSimpleName());
//		Query query = new Query(criteria);
//		Timestamp old = template.findOne(query, Timestamp.class);
//
//		long from = -1;
//		long to = System.currentTimeMillis();
//
//		if (old != null) {
//			from = old.getTimestamp() + 1;
//		} else {
//			from = to - 1000 * 60 * 60 * 24;
//			old = new Timestamp(gameId, ((Class)clz).getSimpleName(), to);
//		}
//
//		List<Notification> nots = getNotifications(appId, 0, to, clz);
//
//		old.setTimestamp(to);
//		template.save(old);
//
//		return nots;
//	}
//	
//	private <T> List<Notification> getNotifications(String appId, long from, long to, Class<T> clz) throws Exception {
//		try {
//		logger.debug("Reading notifications from " + from + " to " + to);
//		
//		String gameId = getGameId(appId);
//		
//		RestTemplate restTemplate = new RestTemplate();
//		ResponseEntity<String> res = null;
//		
//		String url = gamificationUrl + "/notification/game/" + gameId + "?includeTypes=" + ((Class)clz).getSimpleName() + "&fromTs=" + from + "&toTs=" + to + "&size=1000";
//		
//		try {
//			res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(null, createHeaders(appId)), String.class);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		logger.debug("Result: " + res.getStatusCodeValue());
//		
//		TypeFactory factory = mapper.getTypeFactory();
//		JavaType listOfT = factory.constructCollectionType(List.class, clz);
//		List<Notification> nots = mapper.readValue(res.getBody(), listOfT);		
//		
//		logger.debug("Reading " + nots.size() + " notifications.");
//		
//		return nots;
//		} catch (Exception e) {
//			logger.error("Error retrieving notifications", e);
//			return Lists.newArrayList();
//		}
//	}
	
	private eu.trentorise.smartcampus.communicator.model.Notification buildNotification(String lang, Notification not) {
		String type = not.getClass().getSimpleName();
		Map<String, String> extraData = buildExtraData(not);
		
		eu.trentorise.smartcampus.communicator.model.Notification result = new eu.trentorise.smartcampus.communicator.model.Notification();
		
		NotificationMessage message = notificationsMessages.get(type);
			
		fillNotification(result, lang, message, extraData);
		
		return result;
	}
	
	private eu.trentorise.smartcampus.communicator.model.Notification buildSimpleNotification(String lang, String type, Map<String, String> extraData) {
		eu.trentorise.smartcampus.communicator.model.Notification result = new eu.trentorise.smartcampus.communicator.model.Notification();
		
		NotificationMessage message = notificationsMessages.get(type);
			
		fillNotification(result, lang, message, extraData);
		
		return result;
	}	
	
	private Map<String, String> buildExtraData(Notification not) {
		Map<String, String> result = Maps.newTreeMap();

		switch (not.getClass().getSimpleName()) {
		case "LevelGainedNotification": {
			result.put("levelName", ((LevelGainedNotification) not).getLevelName());
			result.put("levelIndex", ((LevelGainedNotification) not).getLevelIndex() != null ? ((LevelGainedNotification) not).getLevelIndex().toString() : "");
			break;
		}
		case "ChallengeInvitationAcceptedNotification": {
			Player guest = playerRepository.findByPlayerIdAndGameId(((ChallengeInvitationAcceptedNotification) not).getGuestId(), not.getGameId());
			result.put("assigneeName", guest.getNickname());
			break;
		}
		case "ChallengeInvitationRefusedNotification": {
			Player guest = playerRepository.findByPlayerIdAndGameId(((ChallengeInvitationRefusedNotification) not).getGuestId(), not.getGameId());
			result.put("assigneeName", guest.getNickname());
			break;
		}
		case "ChallengeInvitationCanceledNotification": {
			Player proposer = playerRepository.findByPlayerIdAndGameId(((ChallengeInvitationCanceledNotification) not).getProposerId(), not.getGameId());
			result.put("challengerName", proposer.getNickname());
			break;
		}
		case "ChallengeAssignedNotification":
			result.put("challengeId", ((ChallengeAssignedNotification)not).getChallengeName());
			break;		
		case "ChallengeCompletedNotication":
			result.put("challengeId", ((ChallengeCompletedNotication)not).getChallengeName());
			break;
		case "ChallengeFailedNotication": {
			result.put("challengeId", ((ChallengeFailedNotication)not).getChallengeName());
			break;
		}		
		}

		return result;
	}
	
	private void fillNotification(eu.trentorise.smartcampus.communicator.model.Notification notification, String lang, NotificationMessage message, Map<String, String> extraData) {
		if (message != null) {
			notification.setTitle(message.getTitle().get(lang));
			notification.setDescription(fillDescription(lang, message, extraData));
			Map<String, Object> content = Maps.newTreeMap();
			content.put("type", message.getType());
			notification.setContent(content);
		}
	}
	
	private String fillDescription(String lang, NotificationMessage message, Map<String, String> extraData) {
		StringBuilder descr = new StringBuilder(message.getDescription().get(lang));
		String result = null;

		if (message.getExtras() != null && message.getExtras().get(lang) != null && extraData != null) {

			List<NotificationMessageExtra> extras = message.getExtras().get(lang);

			List<NotificationMessageExtra> append = extras.stream().filter(x -> "APPEND".equals(x.getType())).collect(Collectors.toList());

			for (NotificationMessageExtra extra : append) {
				boolean ok = true;
				if (extra.getValue() != null) {
					String keyValue = extraData.get(extra.getKey());
					if (keyValue != null && !keyValue.equals(extra.getValue())) {
						ok = false;
					}
				}
				if (ok) {
					descr.append(extra.getString());
				}
			}

			ST st = new ST(descr.toString());

			List<NotificationMessageExtra> replace = extras.stream().filter(x -> "REPLACE".equals(x.getType())).collect(Collectors.toList());

			for (NotificationMessageExtra extra : replace) {
				boolean ok = true;
				if (extra.getValue() != null) {
					String keyValue = extraData.get(extra.getKey());
					if (keyValue != null && !keyValue.equals(extra.getKey())) {
						ok = false;
					}
				}
				if (ok) {
					st.add(extra.getKey(), extraData.get(extra.getString()));
				}
			}

			result = st.render();
		} else {
			result = descr.toString();
		}

		return result;
	}
	
	private String getGameId(String appId) {
		if (appId != null) {
			AppInfo ai = appSetup.findAppById(appId);
			if (ai == null) {
				return null;
			}
			String gameId = ai.getGameId();
			return gameId;
		}
		return null;
	}	
	
	HttpHeaders createHeaders(String appId) {
		return new HttpHeaders() {
			{
				AppInfo app = appSetup.findAppById(appId);
				GameInfo game = gameSetup.findGameById(app.getGameId());
				String auth = game.getUser() + ":" + game.getPassword();
				byte[] encodedAuth = Base64.encode(auth.getBytes(Charset.forName("UTF-8")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}	
	
}
