package eu.trentorise.smartcampus.mobility.syncronization;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@Component
public class SynchronizationManager {

	private static final String EXCHANGE_NAME = "MANAGEMENT";

	private static transient final Logger logger = LoggerFactory.getLogger(SynchronizationManager.class);
	
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
	
	private Channel rabbitMQChannel; 
	
	private ObjectMapper mapper = new ObjectMapper(); {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}	
	
	@PostConstruct
	public void init() throws Exception {
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
						rabbitMQChannel = connection.createChannel();
						rabbitMQChannel.basicQos(1);

						String topics = "PING,MESSAGE";

						rabbitMQChannel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
						String queueName = rabbitMQChannel.queueDeclare().getQueue();
						for (String topic: topics.split(",")) {
							rabbitMQChannel.queueBind(queueName, EXCHANGE_NAME, topic);
						}						

						DefaultConsumer consumer = new DefaultConsumer(rabbitMQChannel) {
							@Override
							public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] b) throws IOException {
								String body = new String(b);
								try {
									 processMessage(envelope.getRoutingKey(), body);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						};
						
						rabbitMQChannel.basicConsume(queueName, true, "",  consumer);

						ok = true;
						logger.info("Connected to RabbitMQ topics");
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

		timer.schedule(tt, 1000);
	}

	private void processMessage(String topic, String body) throws Exception {
		System.err.println(topic + " => " + body);
	}
	
}
