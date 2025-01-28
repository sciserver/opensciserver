package org.sciserver.springapp.racm.utils.logging;

import javax.annotation.PostConstruct;

import org.sciserver.springapp.racm.config.LoggingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoggingInitializer {
	@Autowired
	LoggingConfig loggingConfig;

	@PostConstruct
	public void init() {
		LogUtils.setupLogger(loggingConfig);
	}
}
