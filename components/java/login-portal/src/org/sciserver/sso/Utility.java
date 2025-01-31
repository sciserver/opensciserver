package org.sciserver.sso;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.sso.keystone.KeystoneService.NotAuthorizedException;
import org.sciserver.sso.model.Rule;
import org.sciserver.sso.model.RuleType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sciserver.logging.Message;

public class Utility {
	
	private static final Logger LOG = LogManager.getLogger(Utility.class);	
	private static final sciserver.logging.Logger sciServerLogger = new sciserver.logging.Logger(
			Utility.getApplicationHostName(), 
			AppConfig.getInstance().getAppSettings().getLogApplicationName(),
			AppConfig.getInstance().getAppSettings().getLogMessagingHost(), 
			AppConfig.getInstance().getAppSettings().getLogDatabaseQueueName(), 
			AppConfig.getInstance().getAppSettings().getLogExchangeName(), 
			AppConfig.getInstance().getAppSettings().getLogEnabled());
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static void LogExceptionMessage(String action, String userName, String userID, String token, HttpServletRequest request, Exception error, ObjectNode loggedInfo, HttpStatus http, Integer levelUpStack) {
		
		String msg = getErrorMessage(error);
		Message message = sciServerLogger.createErrorMessage(error, loggedInfo == null ? "" : loggedInfo.toString());
		Utility.fillLoggingMessage(message, userName, userID, token, request, "LoginPortal."+action, levelUpStack);
		try {
			sciServerLogger.SendMessage(message);
		} catch (Exception loggingException) {
			LOG.error("While attempting to log {}, another error was encounted", action, loggingException);
		}
		LogExceptionLocally(error);
	}
	
	public static void LogExceptionMessage(String action, String userName, String userID, String token, HttpServletRequest request, Exception error, ObjectNode loggedInfo, HttpStatus http) {
		LogExceptionMessage(action, userName, userID, token, request, error, loggedInfo, http, 4);
		LogExceptionLocally(error);
	}
	
	public static void LogExceptionMessageFull(String action, String userName, String userID, String token, HttpServletRequest request, Exception error, ObjectNode loggedInfo, HttpStatus http, Integer levelUpStack) {
		LogExceptionMessage(action, userName, userID, token, request, error, loggedInfo, http, levelUpStack); 
		LogExceptionLocally(error);
	}
	
	public static void LogExceptionMessageFull(String action, String userName, String userID, String token, HttpServletRequest request, Exception error, ObjectNode loggedInfo, HttpStatus http) {
		LogExceptionMessage(action, userName, userID, token, request, error, loggedInfo, http, 4); 
		LogExceptionLocally(error);
	}
	
	public static String getErrorMessage(Exception error){
		String msg = "";
		try{
			String exceptionStackTrace = "";
			String exceptionMessage = "";
			if(error != null){
				StringWriter sw = new StringWriter();
				error.printStackTrace(new PrintWriter(sw));
				exceptionStackTrace = sw.toString();
				exceptionMessage = error.getMessage();
			}
			if(exceptionStackTrace != null && !exceptionStackTrace.isEmpty()){
				msg = msg + exceptionStackTrace;
			}else{
				if(exceptionMessage != null && !exceptionMessage.isEmpty())
					msg = msg + exceptionMessage + "\n";
			}
		}catch(Exception ignored){}
		return msg;
	}
	
	public static void LogExceptionLocally(Exception error) {
		if (error instanceof NotAuthorizedException) {
			LOG.info(error.getMessage());
		} else {
			LOG.error(error.getMessage(), error);
		}
	}
	
	protected JsonNode jsonException(Exception e){
		ObjectMapper om = new ObjectMapper();
		ObjectNode on = om.createObjectNode();
		on.put("status", "error");
		on.put("error", e.getMessage());
		return on;
	}
	protected ResponseEntity<JsonNode> jsonExceptionEntity(Exception e, HttpStatus http){
		JsonNode json = jsonException(e);
		return new ResponseEntity<>(json, http);
	}
	
	public static void LogMessage(String messageString, String action, String userName, String userID, String token, HttpServletRequest request, ObjectNode loggedSentence, Boolean doShowInUserHistory, Integer levelUpStack){
		try{
			
			//logInfo(user, request.getRequestURI(), messageString);
			
			ObjectNode oNode = mapper.createObjectNode();
			oNode.put("action", messageString);
			if(loggedSentence != null)
				oNode.put("sentence", loggedSentence);
			
			if(doShowInUserHistory == null)
				doShowInUserHistory = request.getParameter("doShowInUserHistory") == null ? true : (request.getParameter("doShowInUserHistory").toLowerCase().equals("true") ? true : false);
			
			Message message = sciServerLogger.createAuthenticationMessage(oNode.toString(), doShowInUserHistory);
			Utility.fillLoggingMessage(message, userName, userID, token, request, "LoginPortal."+action, levelUpStack);
			sciServerLogger.SendMessage(message);
			//return message;
		}catch(Exception ex){
			LOG.error(ex.getMessage());
			//return null;
		}
	}

	public static void LogMessage(String messageString, String action, String userName, String userID, String token, HttpServletRequest request, ObjectNode loggedSentence, Boolean doShowInUserHistory){
		LogMessage(messageString, action, userName, userID, token, request, loggedSentence, doShowInUserHistory, 4);
	}

	public static void fillLoggingMessage(Message message, String userName, String userID, String token, HttpServletRequest request, String taskName, Integer levelUpStack) {
		try {
			message.ClientIP = getClientIpAddress(request);
			message.TaskName = request.getParameter("TaskName") != null ? request.getParameter("TaskName") : taskName;
			message.UserToken = token;
			message.UserId = userID;
			message.UserName = userName;
			
			if(levelUpStack == null) {
				levelUpStack = 4;
			}
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			sciServerLogger.setMethod(stackTraceElements[levelUpStack].getClassName() + "." + stackTraceElements[levelUpStack].getMethodName());
		
		} catch(Exception e) {};
	}
	
	public static boolean isHostAllowed(String host, String[] allowedHosts) {
		
		for (String h : allowedHosts) {
			Pattern pattern = Pattern.compile(h);
			if (pattern.matcher(host).matches()) return true;
		}
		
		return false;
	}
	
	public static String getStackTrace(Exception ex) {
		StringWriter buffer = new StringWriter();
		PrintWriter writer = new PrintWriter(buffer);
		ex.printStackTrace(writer);
		writer.close();
		
		return buffer.toString();
	}

	public static String getApplicationHostName(){
		String hostName = "";
		try{
			hostName = InetAddress.getLocalHost().getCanonicalHostName();
		}catch(Exception ex){
			hostName = AppConfig.getInstance().getAppSettings().getLogApplicationHost();
		}
		return hostName;
	}
	
	public static String getTokenFromHeader(HttpServletRequest request, HttpServletResponse response) throws Exception {
		AppConfig appConfig = AppConfig.getInstance();
		String tokenFromHeader = request.getHeader("X-Auth-Token");
		if (tokenFromHeader != null) {
			appConfig.getKeystoneService().checkToken(tokenFromHeader);
			return tokenFromHeader;
		}
		
		return null;
	}

	public static RuleType getEmailRuleType(String email, List<Rule> rules, ServletContext context) throws IOException {
		RuleType lastFired = RuleType.DENY;
		
		for(Rule r: rules) {
			if (r.getRegEx().startsWith("file:")) {
				String filename = r.getRegEx().substring("file:".length());
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8));
				try {
					if (br.lines().anyMatch((regex) -> Pattern.matches(regex, email))) {
						lastFired = r.getType();
					}
				} finally {
					br.close();
				}
			} else {
				Pattern pattern = Pattern.compile(r.getRegEx());
				Matcher matcher = pattern.matcher(email);
				if (matcher.find()) {
					lastFired = r.getType();
				}
			}
		}
		
		return lastFired;
	}
	
	public static String getClientIpAddress(HttpServletRequest request) {
		String ipAddress = "";
		
		if (request.getHeader("X-FORWARDED-FOR") != null) {
			ipAddress = request.getHeader("X-FORWARDED-FOR");
		} else if(request.getHeader("X-Forwarded-For") != null) {
			ipAddress = request.getHeader("X-Forwarded-For");
		} else if(request.getHeader("HTTP_CLIENT_IP") != null) {
			ipAddress = request.getHeader("X-Forwarded-For");
		} else{
			ipAddress = request.getRemoteAddr();
		}
		
		return ipAddress.split(",")[0];
	}
	
	public static void ensureSuccessStatusCode(CloseableHttpResponse response) throws Exception {
		int code = response.getStatusLine().getStatusCode();
		if (code == 401 || code == 404) {
			throw new NotAuthorizedException();
		}
		if (code < 200 || code >= 300) {
			throw new Exception(response.getStatusLine().toString());
		}
	}
}
