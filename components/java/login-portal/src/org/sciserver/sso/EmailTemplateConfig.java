package org.sciserver.sso;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

/*
 * Based on https://www.thymeleaf.org/doc/articles/springmail.html#spring-configuration
 */
@Configuration
public class EmailTemplateConfig {
	@Bean
	public TemplateEngine emailTemplateEngine(ServletContext context) {
		final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		// Resolver for TEXT emails
		templateEngine.addTemplateResolver(textTemplateResolver(context));
		// Resolver for HTML emails
		templateEngine.addTemplateResolver(htmlTemplateResolver(context));
		return templateEngine;
	}

	private ITemplateResolver textTemplateResolver(ServletContext context) {
		final ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(context);
		templateResolver.setOrder(Integer.valueOf(1));
		templateResolver.setResolvablePatterns(Collections.singleton("text/*"));
		templateResolver.setPrefix("/WEB-INF/mail/");
		templateResolver.setSuffix(".txt");
		templateResolver.setTemplateMode(TemplateMode.TEXT);
		templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
		templateResolver.setCacheable(false);
		return templateResolver;
	}

	private ITemplateResolver htmlTemplateResolver(ServletContext context) {
		final ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(context);
		templateResolver.setOrder(Integer.valueOf(2));
		templateResolver.setResolvablePatterns(Collections.singleton("html/*"));
		templateResolver.setPrefix("/WEB-INF/mail/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
		templateResolver.setCacheable(false);
		return templateResolver;
	}
}
