package org.sciserver.springapp.racm.utils.logging;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;
import org.sciserver.springapp.racm.config.LoggingConfig;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.ControllerMethodLogger;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import sciserver.logging.Logger;
import sciserver.logging.Message;

public class LogUtils {
	private static final LogUtils instance = new LogUtils();
	private Logger logger;
	private String jobmApplicationNameForLogger;

	private LogUtils() {}

	public static void setupLogger(LoggingConfig loggingConfig) {
		if (!loggingConfig.isEnabled()) return;
		instance.logger = new Logger(loggingConfig.getApplicationHost(),
				loggingConfig.getApplicationName(),
				loggingConfig.getMessagingHost(),
				loggingConfig.getDatabaseQueueName(),
				loggingConfig.getExchangeName(),
				loggingConfig.isEnabled());
		instance.jobmApplicationNameForLogger = loggingConfig.getJobmApplicationNameForLogger();
	}

	private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger();

	private static void wrapSendingMessage(Message message) {
		try {
			instance.logger.SendMessage(message);
		} catch (Exception ex) {
			LOG.error("Failed to send message to sciserver logger", ex);
		}
	}

	private static boolean isLogInfoEnabled() {
		return instance.logger != null && instance.logger.enabled;
	}

	private static boolean isLogErrorEnabled() {
		return instance.logger != null && instance.logger.enabled;
	}

	static void logError(String text, Optional<UserProfile> up, Exception exc, boolean isJOBM) {
		if (isLogErrorEnabled()) {
			Message message = instance.logger.createErrorMessage(exc, text);
			fillMessageWithUserInfo(message, up);
			if (isJOBM) {
				message.Application = instance.jobmApplicationNameForLogger;
			}
			wrapSendingMessage(message);
		}
	}

	static void logJobm(JSONObject content, boolean doShowInUserHistory, Optional<UserProfile> up) {
		if(!isLogInfoEnabled()) {
			return;
		}
		Message message = instance.logger.createJOBMMessage(
				content.toString(), doShowInUserHistory);

		fillMessageWithUserInfo(message, up);
		message.Application = instance.jobmApplicationNameForLogger;
		wrapSendingMessage(message);
	}

	static void logRACM(JSONObject content, boolean doShowInUserHistory, Optional<UserProfile> up) {
		if(!isLogInfoEnabled()) {
			return;
		}
		Message message = instance.logger.createRACMMessage(content.toString(), doShowInUserHistory);

		fillMessageWithUserInfo(message, up);
		wrapSendingMessage(message);
	}

	public static LogBuilder buildLog() {
		return new LogBuilder();
	}

	static void logFileService(JSONObject content, boolean doShowInUserHistory, Optional<UserProfile> up) {
		if(!isLogInfoEnabled()) {
			return;
		}
		Message message = instance.logger.createFileServiceMessage(
				content.toString(), doShowInUserHistory);

		fillMessageWithUserInfo(message, up);
		wrapSendingMessage(message);
	}

	private static void fillMessageWithUserInfo(Message message, Optional<UserProfile> up) {
		up.ifPresent(userProfile -> {
			message.UserId = userProfile.getUserid();
			message.UserName = userProfile.getUsername();
			message.UserToken = userProfile.getToken();
		});
		HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes())
				.getRequest();

		String clientIp = request.getHeader("X-FORWARDED-FOR");
		if (clientIp == null)
			clientIp = request.getRemoteAddr();
		if (request.getAttribute(ControllerMethodLogger.METHOD_NAME_ATTRIBUTE) != null) {
			message.Method = request.getAttribute(ControllerMethodLogger.METHOD_NAME_ATTRIBUTE).toString();
		}
		message.ClientIP = clientIp;
		message.TaskName = request.getParameter("TaskName");
	}

}
