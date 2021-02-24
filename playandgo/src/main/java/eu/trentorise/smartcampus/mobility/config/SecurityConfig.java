package eu.trentorise.smartcampus.mobility.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.filter.OncePerRequestFilter;

import eu.trentorise.smartcampus.mobility.security.AppInfo;
import eu.trentorise.smartcampus.mobility.security.AppSetup;
import eu.trentorise.smartcampus.mobility.security.CustomAuthenticationProvider;
import eu.trentorise.smartcampus.mobility.security.CustomResourceAuthenticationProvider;
import eu.trentorise.smartcampus.mobility.security.CustomTokenExtractor;

@Configuration
@ComponentScan("eu.trentorise.smartcampus.resourceprovider")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private AppSetup appSetup;

	@Autowired
	@Order(1)
	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
	    auth
	    .authenticationProvider(getCustomAuthenticationProvider())
	    .authenticationProvider(getCustomResourceAuthenticationProvider());

	}	
	
	@Bean
	public OncePerRequestFilter noContentFilter() {
		return new CheckHeaderFilter();
	}		
	
	private class CheckHeaderFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

			String appId = request.getHeader("appId");
			if (appId != null && !appId.isEmpty()) {
				AppInfo app = appSetup.findAppById(appId);
				if (app == null) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
			}

			filterChain.doFilter(request, response);
		}
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
    @Order(28)
	public static class OAuthSecurityConfig9 extends WebSecurityConfigurerAdapter {
    	
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
    		
    		http.antMatcher("/initiatives/**").authorizeRequests()
    			.antMatchers("/initiatives/mgmt/**").fullyAuthenticated()
    			.antMatchers("/initiatives/web/**").permitAll()
    		.and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);	     		
    	}        	
    }   
    
    @Configuration
    @Order(29)
	public static class OAuthSecurityConfig10 extends WebSecurityConfigurerAdapter {   	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    		
    		http.antMatcher("/gamification/trackingdata").authorizeRequests()
    			.antMatchers("/gamification/trackingdata").hasAnyAuthority("ROLE_CONSOLE")
    			.and().httpBasic();	     		
    	}        	
    } 
    
    @Configuration
    @Order(30)                                                        
    public static class HttpSecurityConfig1 extends WebSecurityConfigurerAdapter {
    
    	@Override
    	protected void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.rememberMe();		

    		http.authorizeRequests()
				.antMatchers("/swagger**").permitAll()
				.antMatchers("/v2/**").permitAll()
    			.antMatchers("/announcements/**").permitAll()
    			.antMatchers("/policies/console/**","/web/notification/**","/gamification/console/**").hasAnyAuthority("ROLE_CONSOLE")
    			.and()
    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();	    		
    	}    	
    	
    }       
    
	
}
