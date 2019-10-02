package eu.trentorise.smartcampus.mobility.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.common.collect.Lists;

public class AppDetails implements UserDetails, Serializable {

	private static final long serialVersionUID = 4681692038979149760L;
	
	private AppInfo app;
	List<GrantedAuthority> authorities;
	
	public AppDetails() {
		super();
	}

	public AppDetails(AppInfo app) {
		super();
		this.app = app;
	}


	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		if (authorities == null) {
			authorities = Lists.newArrayList();
			authorities.add(new SimpleGrantedAuthority(app.getAppId()));
		}
		return authorities;
	}

	@Override
	public String getPassword() {
		return app.getPassword();
	}

	@Override
	public String getUsername() {
		return app.getAppId();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	public AppInfo getApp() {
		return app;
	}

	public void setApp(AppInfo app) {
		this.app = app;
	}
}
