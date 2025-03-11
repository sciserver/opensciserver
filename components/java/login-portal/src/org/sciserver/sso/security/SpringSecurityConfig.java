package org.sciserver.sso.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;

@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
public class SpringSecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(keycloakAuthenticationProvider());
	}

	@Bean
	@Override
	protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception
	{
		super.configure(http);
		http
				.csrf().disable()
				.authorizeRequests()
				.antMatchers("/sso/login*").permitAll()
				.antMatchers("/keystone/**").permitAll()
				.antMatchers("/api/**").permitAll()
				.antMatchers("/keycloak-*").fullyAuthenticated()
				.anyRequest().permitAll();
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		super.configure(web);
		web.httpFirewall(notStrictHttpFirewall());
		
	}
	
	private static HttpFirewall notStrictHttpFirewall() {
		return new HttpFirewall() {

			@Override
			public FirewalledRequest getFirewalledRequest(HttpServletRequest request) {
				return new FirewalledRequest(request) {
					@Override
					public void reset() {
					}};
			}

			@Override
			public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
				return response;
			}
		};
	}
}
