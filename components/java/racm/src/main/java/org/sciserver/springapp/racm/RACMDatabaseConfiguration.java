
package org.sciserver.springapp.racm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.sql.DataSource;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;
import org.springframework.transaction.aspectj.JtaAnnotationTransactionAspect;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableLoadTimeWeaving(aspectjWeaving=AspectJWeaving.ENABLED)
@EnableTransactionManagement(mode=AdviceMode.ASPECTJ)
public class RACMDatabaseConfiguration {
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
		PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();
		propertyConfigurer.setLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:/jpa-config.properties"));
		return propertyConfigurer;
	}

	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory);
		return transactionManager;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			DataSource dataSource,
			@Value("${eclipselink.logging.level}") String loggingLevel) {
		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
		emf.setDataSource(dataSource);
		emf.setPersistenceXmlLocation("classpath*:/META-INF/persistence.xml");

		Map<String, String> jpaProperties = new HashMap<>();
		jpaProperties.put(PersistenceUnitProperties.LOGGING_LEVEL, loggingLevel);
		jpaProperties.put(PersistenceUnitProperties.PERSISTENCE_CONTEXT_FLUSH_MODE, FlushModeType.COMMIT.name());
		emf.setJpaPropertyMap(jpaProperties);

		emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
		
		return emf;
	}

	/* For Aspectj configuration */
	@Bean
	public JtaAnnotationTransactionAspect jtaAnnotationTransactionAspect(PlatformTransactionManager transactionManager) {
		JtaAnnotationTransactionAspect annotationTransactionAspect = JtaAnnotationTransactionAspect.aspectOf();
		annotationTransactionAspect.setTransactionManager(transactionManager);
		return annotationTransactionAspect;
	}

	@Bean
	public AnnotationTransactionAspect annotationTransactionAspect(PlatformTransactionManager transactionManager) {
		AnnotationTransactionAspect annotationTransactionAspect = AnnotationTransactionAspect.aspectOf();
		annotationTransactionAspect.setTransactionManager(transactionManager);
		return annotationTransactionAspect;
	}

	/* Can't use JpaBaseConfiguration for this due to spring-projects/spring-boot#1327
	 * and because we need this as a filter with high precedence (since it needs to happen
	 * before spring security's filter chain) */
	@ConditionalOnWebApplication
	@ConditionalOnClass(WebMvcConfigurer.class)
	@Bean
	public OpenEntityManagerInViewFilter openEntityManagerInViewFilter() {
		return new OpenEntityManagerInViewFilter();
	}

	/*
	 * based on https://stackoverflow.com/a/34771470/239003
	 */
	@ConditionalOnWebApplication
	@ConditionalOnClass(WebMvcConfigurer.class)
	@Bean
	public FilterRegistrationBean<OpenEntityManagerInViewFilter> openEntityManagerInViewFilterRegistration(
			SecurityProperties properties) {
		FilterRegistrationBean<OpenEntityManagerInViewFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(openEntityManagerInViewFilter());
		registration.addUrlPatterns("/*");
		registration.setOrder(properties.getFilter().getOrder() - 1);
		registration.setName("openEntityManagerInViewFilter");
		return registration;
	}

	/*
	 * This makes a single-threaded, transactionable entity manager available.
	 * This assumes that RACM, when not used as a web app, is closed after a short time
	 * (as this entitymanager will stay open until the app terminates) and that multiple
	 * threads are not used. This entity manager is used by the initializer codes
	 */
	@Bean
	@Primary
	@ConditionalOnNotWebApplication
	public EntityManager entityManager(EntityManagerFactory emf) {
		return emf.createEntityManager();
	}
}
