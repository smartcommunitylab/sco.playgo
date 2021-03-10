package eu.trentorise.smartcampus.mobility.config;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.google.common.io.Resources;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;

@Configuration
@EnableWebMvc
@ComponentScan("eu.trentorise.smartcampus.mobility")
@PropertySource("classpath:application.yml")
@EnableAsync
@EnableScheduling
@Order(value = 0)
public class MobilityConfig implements WebMvcConfigurer {

	
	private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
		"classpath:/META-INF/resources/", "classpath:/resources/",
		"classpath:/static/", "classpath:/public/" };	

	@Value("${mail.host}")
	private String host;
	@Value("${mail.port}")
	private String port;
	@Value("${mail.protocol}")
	private String protocol;
	@Value("${mail.username}")
	private String username;
	@Value("${mail.password}")
	private String password;	

	@Value("${spring.data.mongodb.url}")
	private String mongoUri;	

//	@Value("${imagesDir}")
//	private String imagesDir;		

	
	@Autowired
	private MongoClient mongoClient;
	
	public MobilityConfig() {
		super();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	@Bean
	public JavaMailSender getJavaMailSender() throws IOException {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(Integer.parseInt(port));
		sender.setProtocol(protocol);
		sender.setUsername(username);
		sender.setPassword(password);
		
		Properties props = new Properties();
		props.load(Resources.asByteSource(Resources.getResource("javamail.properties")).openBufferedStream());
		
		sender.setJavaMailProperties(props);
		return sender;
	}

	@Bean
	public MongoClient getMongoClient() {
		return new MongoClient(new MongoClientURI(mongoUri));
	}
	
	@Bean(name = "mongoTemplate")
	@Primary
	public MongoTemplate getDomainMongoTemplate() throws UnknownHostException {
//		MongoTemplate template = new MongoTemplate(new Mongo("localhost", 17017), "mobility-domain");
		MongoTemplate template = new MongoTemplate(mongoClient, new MongoClientURI(mongoUri).getDatabase());
		template.indexOps("trackedInstances").ensureIndex(new Index("day", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("time", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("userId", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("appId", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("clientId", Direction.ASC));
		
		template.indexOps("trackedInstances").ensureIndex(new Index("changedValidity", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("validationResult.validationStatus.validationOutcome", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("validationResult.validationStatus.distance", Direction.ASC));
		
		
		template.setWriteConcern(new WriteConcern(1).withWTimeout(200, TimeUnit.MILLISECONDS));
		return template;
	}
	
	@Bean(name = "messageSource")
	public ResourceBundleMessageSource getResourceBundleMessageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("Messages");
		return source;
	}
	
	@Bean
	public ExecutorService getExecutors() {
		return Executors.newCachedThreadPool();
	}


	 @Override
	 public void addResourceHandlers(ResourceHandlerRegistry registry) {
		 registry.addResourceHandler("/**").addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);	
	 }
	 
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	} 	 
		 
	@Bean
	public LocaleResolver localeResolver()
	{
	    SessionLocaleResolver slr = new SessionLocaleResolver();
	    slr.setDefaultLocale(Locale.ITALIAN);
	    return slr;
	}
	
	@Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
	
}
