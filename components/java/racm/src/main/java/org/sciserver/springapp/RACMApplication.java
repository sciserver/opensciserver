package org.sciserver.springapp;

import org.sciserver.springapp.racm.config.LoginConfig;
import org.sciserver.springapp.racm.login.LoginPortalService;
import org.sciserver.springapp.racm.utils.ControllerMethodLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@SpringBootApplication
public class RACMApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(RACMApplication.class, args);
    }
    
	@Bean
	public MeterBinder processMemoryMetrics() {
		return new ProcessMemoryMetrics();
	}

	@Bean
	public MeterBinder processThreadMetrics() {
		return new ProcessThreadMetrics();
	}

	/*
	 * Needed by racm.Config to obtain details about the current request for logging.
	 */
	@Bean
	@ConditionalOnWebApplication
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}

	@Bean
	@ConditionalOnWebApplication
	FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
		FilterRegistrationBean<ForwardedHeaderFilter> filterRegistrationBean =
				new FilterRegistrationBean<>();
		filterRegistrationBean.setFilter(new ForwardedHeaderFilter());
		filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);

		return filterRegistrationBean;
	}

	@Bean
	@Autowired
	public LoginPortalService loginPortalService(LoginConfig loginConfig) {
		return new Retrofit.Builder()
				.addConverterFactory(JacksonConverterFactory.create())
				.baseUrl(loginConfig.getLoginPortalUrl())
				.build()
				.create(LoginPortalService.class);
	}

	@Bean
	public WebMvcConfigurer registerLoggerHelperConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				WebMvcConfigurer.super.addInterceptors(registry);
				registry.addInterceptor(new ControllerMethodLogger());
			}
		};
	}
}
