package org.sciserver.springapp.racm.utils.logging;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggingInitializer {
	@Value("${logging.jobm.application:JOBM}")
	private String jobmApplicationName;

	@PostConstruct
	public void init() {
		LogUtils.setJobmApplicationName(jobmApplicationName);
	}
}
