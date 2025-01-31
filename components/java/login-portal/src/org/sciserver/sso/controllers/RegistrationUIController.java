package org.sciserver.sso.controllers;

import static org.sciserver.sso.controllers.UiController.LOGIN_INFO_MESSAGE;

import java.time.Instant;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.sciserver.sso.AppConfig;
import org.sciserver.sso.AuthenticationService;
import org.sciserver.sso.DatabaseService;
import org.sciserver.sso.AuthenticationService.CreatedUserWithSameCredientialsException;
import org.sciserver.sso.AuthenticationService.InvalidUserException;
import org.sciserver.sso.EmailService;
import org.sciserver.sso.EncryptedMessageService;
import org.sciserver.sso.Utility;
import org.sciserver.sso.model.ApprovalRequest;
import org.sciserver.sso.model.ApprovalStatus;
import org.sciserver.sso.model.EmailRegistrationPayload;
import org.sciserver.sso.model.ErrorContent;
import org.sciserver.sso.model.RegistrationInfo;
import org.sciserver.sso.model.RuleType;
import org.sciserver.sso.model.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Controller
public class RegistrationUIController {
	private static final String ACTION_FOR_LOGGING = "Register";
	private final AppConfig appConfig;
	private final AuthenticationService authenticationService;
	private final EncryptedMessageService encryptedMessages;
	private final EmailService emailService;
	private final UIUtils uiUtils;
	private final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	DatabaseService databaseService;
	
	@Autowired
	ServletContext context;
	
	RegistrationUIController(AppConfig appConfig,
			AuthenticationService authenticationService,
			EncryptedMessageService encryptedMessages,
			EmailService emailService,
			UIUtils uiUtils) {
		this.appConfig = appConfig;
		this.authenticationService = authenticationService;
		this.encryptedMessages = encryptedMessages;
		this.emailService = emailService;
		this.uiUtils = uiUtils;
	}

	@GetMapping({"/register","/Account/Register"})
	public String registrationForm(Model model, @RequestParam(required = false) String callbackUrl) {
		model.addAttribute("registrationInfo", new RegistrationInfo());
		return "register";
	}
	
	@PostMapping({ "/register", "/Account/Register" })
	public String registrationSubmit(
			@ModelAttribute RegistrationInfo registrationInfo,
			Model model,
			HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(required = false) String callbackUrl,
			RedirectAttributes redirectAttrs) throws TooManyLoginAttemptsException {
		try {
			String userName = registrationInfo.getUsername();
			String userPassword = registrationInfo.getPassword();
			String userEmail = registrationInfo.getEmail();
			String keycloakUserId = registrationInfo.getKeycloakUserId();
			String keycloakUsername = registrationInfo.getKeycloakUsername();
			Optional<String> directTo = initialRegistrationAttempt(request, response, userName, userEmail, userPassword );
			
			if (directTo.isPresent()) {
				return directTo.get();
			}
			
			UserCredentials userCredentials = new UserCredentials();
			userCredentials.setUserName(userName);
			userCredentials.setPassword(userPassword);
			userCredentials.setEmail(userEmail);
			userCredentials.setKeycloakUserId(keycloakUserId);
			userCredentials.setKeycloakUsername(keycloakUsername);
			
			if (!appConfig.getAppSettings().isValidationCodeEnabled()) {
				return attemptRegistration(request, response, model, callbackUrl, userName, userEmail, userPassword, keycloakUserId, keycloakUsername, redirectAttrs);
			}
			
			EmailRegistrationPayload payload = new EmailRegistrationPayload(userCredentials, Instant.now());
			emailService.sendRegistrationEmail(payload, callbackUrl);

			redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE,
					"An email has been sent to validate your account. It will expire in "
							+ appConfig.getAppSettings().getValidationCodeLifetimeMinutes()
							+ " minutes. For additional assistance, please email "
							+ appConfig.getAppSettings().getHelpdeskEmail() + ".");

			ObjectNode sentence = mapper.createObjectNode();
			sentence.put("subject", userName);
			sentence.put("verb", "requested");
			sentence.put("predicate", "creating a SciServer account");
			Utility.LogMessage(userName + " requested creating a SciServer account", "RequestRegistration", userName,
					null, null, request, sentence, null);

			return "redirect:login";
		} catch (Exception e) {
			Utility.LogExceptionMessage("RequestRegistration", null, null, null, request, e,
					fillLoggedInfo(registrationInfo.getUsername(), registrationInfo.getEmail()),
					HttpStatus.INTERNAL_SERVER_ERROR);

			redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE, "Error: " + e.getMessage());
			return "redirect:login";
		}
	}
	
	@GetMapping("/register-validation")
	public String registrationSubmitValidation(HttpServletRequest request, HttpServletResponse response,
			Model model,
			@RequestParam(value="callbackUrl", required = false) String callbackUrl,
			@RequestParam("code") String code,
			RedirectAttributes redirectAttrs) throws Exception {

		String userName = null;
		String userEmail = null;
		try {
			EmailRegistrationPayload payload = encryptedMessages
					.decryptString(code, EmailRegistrationPayload.class);
			userName = payload.getAttemptedUserCredentials().getUserName();
			userEmail = payload.getAttemptedUserCredentials().getEmail();
			String userPassword = payload.getAttemptedUserCredentials().getPassword();
			String keycloakUserId = payload.getAttemptedUserCredentials().getKeycloakUserId();
			String keycloakUsername = payload.getAttemptedUserCredentials().getKeycloakUsername();
			uiUtils.verifyCodeUnexpired(payload.getCreationTime(),
					"Email validation expired. Please register again.");

			return attemptRegistration(request, response, model, callbackUrl, userName, userEmail, userPassword, keycloakUserId, keycloakUsername, redirectAttrs);
		} catch (Exception e) {
			Utility.LogExceptionMessage(ACTION_FOR_LOGGING, userName, null, null, request, e,
					fillLoggedInfo(userName, userEmail),
					HttpStatus.INTERNAL_SERVER_ERROR);

			redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE,
					"Error" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
			return "redirect:login";

		}
	}

	/**
	 * Does an initial check if the user can be created later.
	 * 
	 * In special cases (e.g., the user already exists with this username and password),
	 * a String with the name of the view or redirect: link will be returned. The response
	 * object is needed to set cookies in this case.
	 */
	private Optional<String> initialRegistrationAttempt(HttpServletRequest request, HttpServletResponse response,
			String userName, String userEmail, String userPassword) throws Exception {
		try {
			authenticationService.verifyUser(userName, userPassword, userEmail, context);
		} catch (CreatedUserWithSameCredientialsException e) {
			Utility.LogExceptionMessage(ACTION_FOR_LOGGING, userName, null, null, request,
					new Exception(userName + " tried to register with an existing account credentials", e),
					fillLoggedInfo(userName, userEmail),
					HttpStatus.INTERNAL_SERVER_ERROR);
			return Optional.of(uiUtils.login(userName, userPassword, request, response, null));
		}
		return Optional.empty();
	}

	private String attemptRegistration(HttpServletRequest request, HttpServletResponse response, Model model,
			String callbackUrl, String userName, String userEmail, String userPassword, String keycloakUserId, String keycloakUsername, RedirectAttributes redirectAttrs) throws Exception {
		String userId;
		try {
			userId = authenticationService.createUser(userName, userPassword, userEmail, context);
			
			if (keycloakUserId != null && !StringUtils.isBlank(keycloakUserId)) {
				authenticationService.linkKeycloakUser(userName, userPassword, keycloakUserId, keycloakUsername, request);
			}
		} catch (TooManyLoginAttemptsException e) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			throw new TooManyLoginAttemptsException("Cannot create an account, this username has already been used."
					+ "Additionally, attempting to login failed: " + e.getMessage(), e);
		} catch (CreatedUserWithSameCredientialsException e) {
			Utility.LogExceptionMessage(ACTION_FOR_LOGGING, userName, null, null, request,
					new Exception(userName + " tried to register with an existing account credentials after validating their email", e),
					fillLoggedInfo(userName, userEmail),
					HttpStatus.INTERNAL_SERVER_ERROR);
			return uiUtils.login(userName, userPassword, request, response, callbackUrl);
		} catch (InvalidUserException e) {
			RegistrationInfo info = new RegistrationInfo();
			info.setUsername(userName);
			info.setEmail(userEmail);
			model.addAttribute("registrationInfo", info);
			model.addAttribute(LOGIN_INFO_MESSAGE,
					"Your account can no longer be created: "
					+ e.getMessage());
			response.setStatus(HttpStatus.CONFLICT.value());

			Utility.LogExceptionMessage(ACTION_FOR_LOGGING, userName, null, null, request,
					new Exception("Cannot create user after email validation", e),
					fillLoggedInfo(userName, userEmail),
					HttpStatus.INTERNAL_SERVER_ERROR);

			return "register";
		}

		ObjectNode sentence = mapper.createObjectNode()
				.put("subject", userName)
				.put("verb", "registered")
				.put("predicate", "a SciServer account");
		Utility.LogMessage(userName + " registered a SciServer account", ACTION_FOR_LOGGING, userName, userId, null,
				request, sentence, null);

		if (appConfig.getAppSettings().isEmailFilteringEnabled()) {
			// Check if account requires manual approval
			RuleType ruleType = Utility.getEmailRuleType(userEmail, databaseService.getRules(), context);
			if (ruleType.equals(RuleType.NEEDS_APPROVAL)) {
				appConfig.getKeystoneService().setUserEnabled(userId, false);
				ApprovalRequest approvalRequest = new ApprovalRequest(userId, userName, userEmail, Utility.getClientIpAddress(request), null, ApprovalStatus.REQUESTED);
				databaseService.addApprovalRequest(approvalRequest);
				ObjectNode sentence2 = mapper.createObjectNode()
						.put("subject", userName)
						.put("verb", "needs")
						.put("predicate", "registration approval");
				Utility.LogMessage(userName + " needs registration approval", ACTION_FOR_LOGGING, userName, userId, null,
						request, sentence2, null);
				
				model.addAttribute("userId", userId);
				
				return "extra";
			}
		}
		
		return uiUtils.login(userName, userPassword, request, response, callbackUrl);
	}

	@PostMapping("/extra-submit")
	public String extraSubmit(@RequestParam String userId, 
			@RequestParam String extraFullName,
			@RequestParam String extraAffiliation,
			@RequestParam String extraComments, HttpServletRequest request, HttpServletResponse response,
			RedirectAttributes redirectAttrs) throws Exception {
		
		ObjectNode extra = mapper.createObjectNode()
			.put("fullName", extraFullName)
			.put("affiliation", extraAffiliation)
			.put("comments", extraComments);
		
		databaseService.setApprovalRequestExtra(userId, extra.toString());
		
		redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE,
				"Your registration approval request has been successfully submitted." +
				"We will contact you once the account has been approved or if we need more information from you.");
		
		return "redirect:login";
	}
	
	private ObjectNode fillLoggedInfo(String userName, String userEmail) {
		return mapper.createObjectNode()
				.put("userName", userName)
				.put("email", userEmail);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public String exceptionHandler(Model model, Exception ex, HttpServletResponse response) {
		Utility.LogExceptionLocally(ex);
		model.addAttribute("error", new ErrorContent(ex));
		return "error";
	}
}
