package eu.trentorise.smartcampus.mobility.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import eu.trentorise.smartcampus.mobility.security.CustomAuthenticationProvider;
import eu.trentorise.smartcampus.mobility.security.CustomResourceAuthenticationProvider;
import eu.trentorise.smartcampus.mobility.security.CustomTokenExtractor;

@Configuration
@ComponentScan("eu.trentorise.smartcampus.resourceprovider")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	@Order(1)
	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
	    auth
	    .authenticationProvider(getCustomAuthenticationProvider())
	    .authenticationProvider(getCustomResourceAuthenticationProvider());

	}	
	
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}		
	
	@Bean
	public CustomResourceAuthenticationProvider getCustomResourceAuthenticationProvider() {
		CustomResourceAuthenticationProvider rap = new CustomResourceAuthenticationProvider();
		return rap;
	}
	
	@Bean
	public CustomAuthenticationProvider getCustomAuthenticationProvider() {
		return new CustomAuthenticationProvider();
	}
	
	public SavedRequestAwareAuthenticationSuccessHandler getSavedRequestAwareAuthenticationSuccessHandler() {
		SavedRequestAwareAuthenticationSuccessHandler h = new SavedRequestAwareAuthenticationSuccessHandler();
		h.setUseReferer(true);
		return h;
		
	}
	
    @Bean(name="oauthAuthenticationEntryPoint")
    public OAuth2AuthenticationEntryPoint getOAuth2AuthenticationEntryPoint() {
    	return new OAuth2AuthenticationEntryPoint();
    }
    
    @Bean(name="oAuth2AccessDeniedHandler")
    public OAuth2AccessDeniedHandler getOAuth2AccessDeniedHandler() {
    	return new OAuth2AccessDeniedHandler();
    }    
    
    @Configuration
    @Order(20)
	public static class OAuthSecurityConfig1 extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public OAuth2AuthenticationProcessingFilter getResourceFilter() throws Exception {
        	OAuth2AuthenticationProcessingFilter rf = new OAuth2AuthenticationProcessingFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	rf.setTokenExtractor(new CustomTokenExtractor());
        	rf.setStateless(false);
        	return rf;
        }    	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    		http.antMatcher("/gamification/freetracking/**").authorizeRequests().antMatchers("/gamification/freetracking/**").fullyAuthenticated().and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	    		
    	}        	
    }    
    
    @Configuration
    @Order(21)
	public static class OAuthSecurityConfig2 extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public OAuth2AuthenticationProcessingFilter getResourceFilter() throws Exception {
        	OAuth2AuthenticationProcessingFilter rf = new OAuth2AuthenticationProcessingFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	rf.setTokenExtractor(new CustomTokenExtractor());
        	rf.setStateless(false);
        	return rf;
        }      	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    		
    		http.antMatcher("/gamification/journey/**").authorizeRequests().antMatchers("/gamification/journey/**").fullyAuthenticated().and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	     		
    	}        	
    }       
    
    
    @Configuration
    @Order(22)
	public static class OAuthSecurityConfig3 extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public OAuth2AuthenticationProcessingFilter getResourceFilter() throws Exception {
        	OAuth2AuthenticationProcessingFilter rf = new OAuth2AuthenticationProcessingFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	rf.setTokenExtractor(new CustomTokenExtractor());
        	rf.setStateless(false);
        	return rf;
        }        	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		
    		http.antMatcher("/itinerary/**").authorizeRequests().antMatchers("/itinerary/**").fullyAuthenticated().and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	

    	}        	
    }      
    
    @Configuration
    @Order(24)
	public static class OAuthSecurityConfig5 extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public OAuth2AuthenticationProcessingFilter getResourceFilter() throws Exception {
        	OAuth2AuthenticationProcessingFilter rf = new OAuth2AuthenticationProcessingFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	rf.setTokenExtractor(new CustomTokenExtractor());
        	rf.setStateless(false);
        	return rf;
        }        	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    		
    		http.antMatcher("/gamification/statistics/**").authorizeRequests().antMatchers("/gamification/statistics/**").fullyAuthenticated().and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	

    	}        	
    }     
    
    @Configuration
    @Order(25)
	public static class OAuthSecurityConfig6 extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public OAuth2AuthenticationProcessingFilter getResourceFilter() throws Exception {
        	OAuth2AuthenticationProcessingFilter rf = new OAuth2AuthenticationProcessingFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	rf.setTokenExtractor(new CustomTokenExtractor());
        	rf.setStateless(false);
        	return rf;
        }        	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    		
    		http.antMatcher("/diary/**").authorizeRequests().antMatchers("/diary/**").fullyAuthenticated().and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	

    	}        	
    }     
    
    
    @Configuration
    @Order(26)
	public static class OAuthSecurityConfig7 extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public OAuth2AuthenticationProcessingFilter getResourceFilter() throws Exception {
        	OAuth2AuthenticationProcessingFilter rf = new OAuth2AuthenticationProcessingFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	rf.setTokenExtractor(new CustomTokenExtractor());
        	rf.setStateless(false);
        	return rf;
        }      	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    		
    		http.antMatcher("/gamification/temporary/**").authorizeRequests().antMatchers("/gamification/temporary/**").fullyAuthenticated().and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	     		
    	}        	
    }    
    
    @Configuration
    @Order(27)
	public static class OAuthSecurityConfig8 extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public OAuth2AuthenticationProcessingFilter getResourceFilter() throws Exception {
        	OAuth2AuthenticationProcessingFilter rf = new OAuth2AuthenticationProcessingFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	rf.setTokenExtractor(new CustomTokenExtractor());
        	rf.setStateless(false);
        	return rf;
        }      	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    		
    		http.antMatcher("/gamification/geolocations/**").authorizeRequests().antMatchers("/gamification/geolocations/**").fullyAuthenticated().and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	     		
    	}        	
    }        
    
    
    @Configuration
    @Order(30)                                                        
    public static class HttpSecurityConfig1 extends WebSecurityConfigurerAdapter {
    
    	@Override
    	protected void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.rememberMe();		

    		http.authorizeRequests().antMatchers("/policies/console/**","/web/notification/**","/gamification/console/**").hasAnyAuthority("ROLE_CONSOLE").and()
    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();	    		
    	}    	
    	
    }       
    
	
}
