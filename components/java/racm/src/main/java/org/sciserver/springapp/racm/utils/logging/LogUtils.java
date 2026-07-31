package org.sciserver.springapp.racm.utils.logging;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;
import org.sciserver.springapp.loginterceptor.Log;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.ControllerMethodLogger;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import sciserver.logging.Logger;
import sciserver.logging.Message;

public class LogUtils {
	private static String jobmApplicationName = "JOBM";

	private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger();

	public static void setJobmApplicationName(String name) {
		jobmApplicationName = name;
	}

	private static Logger getLogger() {
		return Log.getLogger();
	}

	private static void wrapSendingMessage(Message message) {
		try {
			getLogger().SendMessage(message);
		} catch (Exception ex) {
			LOG.error("Failed to send message to sciserver logger", ex);
		}
	}

	static void logError(String text, Optional<UserProfile> up, Exception exc, boolean isJOBM) {
		Message message = getLogger().createErrorMessage(exc, text);
		fillMessageWithUserInfo(message, up);
		if (isJOBM) {
			message.Application = jobmApplicationName;
		}
		wrapSendingMessage(message);
	}

	static void logJobm(JSONObject content, boolean doShowInUserHistory, Optional<UserProfile> up) {
		Message message = getLogger().createJOBMMessage(
				content.toString(), doShowInUserHistory);

		fillMessageWithUserInfo(message, up);
		message.Application = jobmApplicationName;
		wrapSendingMessage(message);
	}

	static void logRACM(JSONObject content, boolean doShowInUserHistory, Optional<UserProfile> up) {
		Message message = getLogger().createRACMMessage(content.toString(), doShowInUserHistory);

		fillMessageWithUserInfo(message, up);
		wrapSendingMessage(message);
	}

	public static LogBuilder buildLog() {
		return new LogBuilder();
	}

	static void logFileService(JSONObject content, boolean doShowInUserHistory, Optional<UserProfile> up) {
		Message message = getLogger().createFileServiceMessage(
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
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
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
