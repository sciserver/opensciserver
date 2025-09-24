package org.sciserver.springapp.racm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter;
import org.sciserver.springapp.racm.auth.SciserverCookieAuthenticationFilter;
import org.sciserver.springapp.racm.auth.SciserverQueryParamAuthenticationFilter;
import org.sciserver.springapp.racm.auth.SciserverUserDetailsService;
import org.sciserver.springapp.racm.auth.HeaderAuthenticationAuthorizationHandlers.ReturnUnauthenticatedJsonMessage;
import org.sciserver.springapp.racm.auth.HeaderAuthenticationAuthorizationHandlers.ReturnUnauthorizedJsonMessage;
import org.sciserver.springapp.racm.auth.WebAuthenticationAuthorizationHandlers.RedirectToLoginPortal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;

@Configuration
@ConditionalOnWebApplication
public class SecurityConfiguration {
	private static final String ACCESS_DENIED_PAGE = "/accessdenied";

	// These endpoints use cookies for authentication
	private static final String[] webPaths = new String[] {
			"/", "/index_list.html", "/static/**", ACCESS_DENIED_PAGE,
			"/compm/mvc/**", "/query/**", "/cctree/**", "/rctree/**"};
	// These endpoints are used by compm's, file services, etc and handle their own authentication
	private static final String[] NON_USER_ENDPOINTS = new String[] {
			"/jobm/rest/compmdockerjob/*", "/jobm/rest/compm/checkId", "/jobm/rest/compmjob/*", "/jobm/rest/compmjobs/*",
			"/jobm/rest/rdbjob/*", "/storem/fileservice/*", "/swagger-ui/*"
	};

	@Bean
	public AuthenticationProvider preAuthenticatedAuthenticationProvider(
			SciserverUserDetailsService sciserverUserDetailsService) {
		PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
		provider.setPreAuthenticatedUserDetailsService(sciserverUserDetailsService);

		return provider;
	}

	@Order(2)
	@ConditionalOnWebApplication
	public static class HeaderConfigurationAdapter extends WebSecurityConfigurerAdapter {
		@Autowired
		private SciServerHeaderAuthenticationFilter sciServerHeaderAuthenticationFilter;
		@Autowired
		private ReturnUnauthenticatedJsonMessage returnUnauthenticatedJsonMessage;
		@Autowired
		private ReturnUnauthorizedJsonMessage returnUnauthorizedJsonMessage;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.cors().and()
				.csrf().disable()
				.logout().disable()
				.authorizeRequests()
					.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class, PrometheusScrapeEndpoint.class, MetricsEndpoint.class))
						.permitAll()
					.requestMatchers(EndpointRequest.toAnyEndpoint())
						.hasRole("ADMIN")
					.mvcMatchers("/config")
						.permitAll()
					.mvcMatchers(NON_USER_ENDPOINTS)
						.permitAll()
					.anyRequest()
						.authenticated()
					.and()
				.addFilter(sciServerHeaderAuthenticationFilter)
				.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
					.and()
				.exceptionHandling()
					.accessDeniedHandler(returnUnauthorizedJsonMessage)
					.authenticationEntryPoint(returnUnauthenticatedJsonMessage);
		}

		@Bean(name="headerAuthenticationManager")
		public AuthenticationManager headerAuthenticationManager() throws Exception {
			return super.authenticationManagerBean();
		}

		/*
		 * This is needed to avoid using this filter for all requests. Spring security is explicitly
		 * configured to use it for the endpoints requested in the configure() method.
		 */
		@Bean
		public FilterRegistrationBean<SciServerHeaderAuthenticationFilter> avoidRegisteringHeaderFilter(
				SciServerHeaderAuthenticationFilter sciServerHeaderAuthenticationFilter) {
			FilterRegistrationBean<SciServerHeaderAuthenticationFilter> registration
				= new FilterRegistrationBean<>(sciServerHeaderAuthenticationFilter);
			registration.setEnabled(false);
			return registration;
		}

		@Override
		public void configure(WebSecurity web) throws Exception {
			super.configure(web);
			web.httpFirewall(notStrictHttpFirewall());
		}
	}

	@Order(1)
	@ConditionalOnWebApplication
	public static class CookieConfigurationAdapter extends WebSecurityConfigurerAdapter {
		@Autowired
		private SciserverCookieAuthenticationFilter sciserverCookieAuthenticationFilter;
		@Autowired
		private SciserverQueryParamAuthenticationFilter sciserverQueryParamAuthenticationFilter;
		@Autowired
		private RedirectToLoginPortal redirectToLoginPortal;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.requestMatchers()
					.mvcMatchers(webPaths)
					.and()
				.logout().disable()
				.csrf().disable()
				.addFilter(sciserverCookieAuthenticationFilter)
				.addFilterAfter(sciserverQueryParamAuthenticationFilter, SciserverCookieAuthenticationFilter.class)
				.authorizeRequests()
					.mvcMatchers(ACCESS_DENIED_PAGE).authenticated()
					.anyRequest().hasRole("ADMIN")
					.and()
				.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
					.and()
				.exceptionHandling()
					.accessDeniedPage(ACCESS_DENIED_PAGE)
					.authenticationEntryPoint(redirectToLoginPortal);
		}

		@Bean(name="cookieAuthenticationManager")
		public AuthenticationManager cookieAuthenticationManager() throws Exception {
			return super.authenticationManagerBean();
		}

		/*
		 * This is needed to avoid using this filter for all requests. Spring security is explicitly
		 * configured to use it for the endpoints requested in the configure() method.
		 */
		@Bean
		public FilterRegistrationBean<SciserverCookieAuthenticationFilter> avoidRegisteringCookieFilter() {
			FilterRegistrationBean<SciserverCookieAuthenticationFilter> registration
				= new FilterRegistrationBean<>(sciserverCookieAuthenticationFilter);
			registration.setEnabled(false);
			return registration;
		}

		@Override
		public void configure(WebSecurity web) throws Exception {
			super.configure(web);
			web.httpFirewall(notStrictHttpFirewall());
		}
	}

	/* We want to allow everything, for better or for worse, because users
	 * can use arbitrary names for the user volume folders that are used in urls */
	private static HttpFirewall notStrictHttpFirewall() {
		return new HttpFirewall() {

			@Override
			public FirewalledRequest getFirewalledRequest(HttpServletRequest request) {
				return new FirewalledRequest(request) {
					@Override
					public void reset() {
						// No Need to modify the request
					}};
			}

			@Override
			public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
				return response;
			}
		};
	}
}
