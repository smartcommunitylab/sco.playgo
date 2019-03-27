package eu.trentorise.smartcampus.mobility.security;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import eu.trentorise.smartcampus.profileservice.BasicProfileService;
import eu.trentorise.smartcampus.profileservice.model.BasicProfile;

public class CustomResourceAuthenticationProvider implements AuthenticationProvider {

//	@Autowired
//	private Environment env;	
	
	@Value("${aacURL}")
	private String aacURL;	
	
	private BasicProfileService profileService;

	@PostConstruct
	private void init() {
		profileService = new BasicProfileService(aacURL);
	}	
	
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		PreAuthenticatedAuthenticationToken rcAuth = (PreAuthenticatedAuthenticationToken) authentication;

		String token = (String) rcAuth.getPrincipal();
		
		BasicProfile basicProfile;
		try {
			basicProfile = profileService.getBasicProfile(token);
		} catch (Exception e) {
			throw new InvalidTokenException("Invalid token: " + token, e);
		}

		if (basicProfile == null) {
			throw new InvalidTokenException("Invalid token: " + token);
		}
		
		PreAuthenticatedAuthenticationToken authRes = new PreAuthenticatedAuthenticationToken(basicProfile.getUserId(), null);
		authRes.setAuthenticated(true);
		authRes.setDetails(token);
		
		return authRes;
		
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return (PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication));
	}		

}
