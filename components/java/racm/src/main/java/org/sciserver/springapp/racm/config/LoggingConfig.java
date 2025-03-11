package org.sciserver.springapp.racm.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "org.sciserver.racm.logging")
public class LoggingConfig {
	private static final String DEFAULT_JOBM_APPLICATION_NAME = "JOBM";

	private String applicationHost;
	private String applicationName;
	private String jobmApplicationNameForLogger = DEFAULT_JOBM_APPLICATION_NAME;
	private boolean enabled;

	private String messagingHost;
	private String databaseQueueName;
	private String exchangeName;

	public String getApplicationHost() {
		return applicationHost;
	}
	public void setApplicationHost(String applicationHost) {
		this.applicationHost = applicationHost;
	}
	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	public String getJobmApplicationNameForLogger() {
		return jobmApplicationNameForLogger;
	}
	public void setJobmApplicationNameForLogger(String jobmApplicationNameForLogger) {
		this.jobmApplicationNameForLogger = jobmApplicationNameForLogger;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getMessagingHost() {
		return messagingHost;
	}
	public void setMessagingHost(String messagingHost) {
		this.messagingHost = messagingHost;
	}
	public String getDatabaseQueueName() {
		return databaseQueueName;
	}
	public void setDatabaseQueueName(String databaseQueueName) {
		this.databaseQueueName = databaseQueueName;
	}
	public String getExchangeName() {
		return exchangeName;
	}
	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	@PostConstruct
	public void setupLogging() {
		if (applicationName == null) {
			Properties props = new Properties();
			try {
				props.load(getClass().getResourceAsStream("/login.properties"));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			this.applicationHost = props.getProperty("Log.ApplicationHost");
			this.applicationName = props.getProperty("Log.ApplicationName");
			this.jobmApplicationNameForLogger = props.getProperty(
					"Log.JOBMApplicationName", DEFAULT_JOBM_APPLICATION_NAME);
			this.enabled = "true".equals(props.getProperty("Log.Enabled"));
			this.messagingHost = props.getProperty("Log.MessagingHost");
			this.databaseQueueName = props.getProperty("Log.DatabaseQueueName");
			this.exchangeName = props.getProperty("Log.ExchangeName");
		}
	}
}
