package org.sciserver.sso.controllers;

import static org.sciserver.sso.controllers.UiController.LOGIN_INFO_MESSAGE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.sciserver.sso.AppConfig;
import org.sciserver.sso.EmailService;
import org.sciserver.sso.EncryptedMessageService;
import org.sciserver.sso.Utility;
import org.sciserver.sso.keystone.User;
import org.sciserver.sso.model.ErrorContent;
import org.sciserver.sso.model.PasswordInfo;
import org.sciserver.sso.model.PasswordResetPayload;
import org.sciserver.sso.model.ResetPasswordInfo;
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
public class ResetPasswordController {
	private final AppConfig appConfig;
	private final ObjectMapper mapper = new ObjectMapper();
	private final EmailService emailService;
	private final UIUtils uiUtils;
	private final EncryptedMessageService encryptedMessageService;

	ResetPasswordController(AppConfig appConfig,
			EmailService emailService, UIUtils uiUtils,
			EncryptedMessageService encryptedMessageService) {
		this.appConfig = appConfig;
		this.emailService = emailService;
		this.uiUtils = uiUtils;
		this.encryptedMessageService = encryptedMessageService;
	}

	@GetMapping("/reset-password")
	public String initialForm(Model model) {
		model.addAttribute("resetPasswordInfo", new ResetPasswordInfo());
		return "reset-password";
	}

	@PostMapping("/reset-password")
	public String submitRequest(@ModelAttribute ResetPasswordInfo resetPasswordInfo, HttpServletRequest request,
			HttpServletResponse response, RedirectAttributes redirectAttrs) throws Exception {
		String userName = null;
		String userId = null;

		try {
			List<User> users = new ArrayList<>();
			if (!StringUtils.isEmpty(resetPasswordInfo.getUsername())) {
				userName = resetPasswordInfo.getUsername();
				userId = appConfig.getKeystoneService()
						.tryGettingUserId(resetPasswordInfo.getUsername())
						.orElseThrow(() -> new Exception("User name not found"));
				User user = appConfig.getKeystoneService().getUserById(userId);
				users.add(user);
			} else if (StringUtils.contains(resetPasswordInfo.getUserEmail(), '@')) {
				// keystone might have multiple emails per users:
				users = appConfig.getKeystoneService()
						.getUsersFromEmail(resetPasswordInfo.getUserEmail());
				if (users.isEmpty()) {
					throw new Exception("User email not found");
				}
			} else {
				redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE,
						"Error: Undefined user name or user email");
				return "redirect:reset-password";
			}

			for (User user : users) {
				userName = user.getUserName();
				userId = user.getUserId();
				emailService.sendResetPasswordEmail(userName, user.getEmail(),
						new PasswordResetPayload(userId, Instant.now()));

				String subject = userName;
				String verb = "requested";
				String predicate = "resetting SciServer account password";
				ObjectNode sentence = mapper.createObjectNode()
						.put("subject", subject)
						.put("verb", verb)
						.put("predicate", predicate);
				Utility.LogMessage(subject + " " + verb + " " + predicate, "RequestPasswordChange", userName, userId,
						null, request, sentence, null);
			}

			redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE,
					"An email has been sent with a link to reset your password. It will expire in "
							+ appConfig.getAppSettings().getValidationCodeLifetimeMinutes().toString()
							+ " minutes. For additional assistance, please email "
							+ appConfig.getAppSettings().getHelpdeskEmail() + ".");

			return "redirect:login";
		} catch (Exception e) {
			ObjectNode info = mapper.createObjectNode()
					.put("userName", userName)
					.put("verb", "request password reset");
			Utility.LogExceptionMessageFull("RequestPasswordChange", userName, userId, null, request, e, info,
					HttpStatus.INTERNAL_SERVER_ERROR, 4);

			redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE, "Error: " + e.getMessage());
			return "redirect:reset-password";
		}
	}

	@GetMapping("/reset-password-validation")
	public String formWithCode(Model model,
			HttpServletRequest request, HttpServletResponse response,
			@RequestParam("code") String code,
			RedirectAttributes redirectAttrs) {
		String userName = null;
		String userEmail = null;
		try {
			PasswordResetPayload payload =
					encryptedMessageService.decryptString(code, PasswordResetPayload.class);
			User user = appConfig.getKeystoneService().getUserById(payload.getUserId());
			userName = user.getUserName();
			userEmail = user.getEmail();
			uiUtils.verifyCodeUnexpired(payload.getCreationTime(),
					"Password Reset Link expired");
			PasswordInfo info = new PasswordInfo();
			info.setValidationCode(code);
			model.addAttribute("passwordInfo", info);
			return "reset-password-validation";
		} catch (Exception e) {
			Utility.LogExceptionMessage("ResettingPassword", userName, null, null, request, e,
					fillLoggedInfo(userName, userEmail),
					HttpStatus.INTERNAL_SERVER_ERROR);

			redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE,
					"Error" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
			return "redirect:reset-password";
		}
	}

	@PostMapping("/reset-password-validation")
	public String submitPasswordResetWithCode(@ModelAttribute PasswordInfo passwordInfo,
			HttpServletRequest request,
			HttpServletResponse response,
			RedirectAttributes redirectAttrs,
			Model model) {

		String userId = null;
		String userName = null;
		try {
			PasswordResetPayload payload =
					encryptedMessageService.decryptString(
							passwordInfo.getValidationCode(), PasswordResetPayload.class);
			uiUtils.verifyCodeUnexpired(payload.getCreationTime(),
					"Password Reset Link expired");
			userId = payload.getUserId();
			User user = appConfig.getKeystoneService().getUserById(payload.getUserId());
			userName = user.getUserName();

			appConfig.getKeystoneService()
				.changePassword(userId, passwordInfo.getPassword());

			ObjectNode sentence = mapper.createObjectNode();
			sentence.put("subject", userName);
			sentence.put("verb", "changed");
			sentence.put("predicate", "SciServer account password");
			Utility.LogMessage(userName + " changed SciServer password",
					"ChangePasswordWithValidation", userName,
					userId, null, request, sentence, null);

			redirectAttrs.addFlashAttribute(LOGIN_INFO_MESSAGE, "Password successfully changed.");
			return "redirect:login";
		} catch (Exception e) {
			ObjectNode info = mapper.createObjectNode()
					.put("userName", userName)
					.put("verb", "change password");
			Utility.LogExceptionMessageFull("ChangePasswordWithValidation",
					userName, userId, null, request, e, info,
					HttpStatus.INTERNAL_SERVER_ERROR, 4);

			model.addAttribute(LOGIN_INFO_MESSAGE,
					"Error" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
			passwordInfo.setPassword("");
			passwordInfo.setConfirmPassword("");
			return (e instanceof ExpiredCodeException) ?
					"reset-password" :
					"reset-password-validation";
		}
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public String exceptionHandler(Model model, Exception ex) {
		Utility.LogExceptionLocally(ex);
		model.addAttribute("error", new ErrorContent(ex));
		return "error";
	}

	private ObjectNode fillLoggedInfo(String userName, String userEmail) {
		return mapper.createObjectNode()
				.put("userName", userName)
				.put("email", userEmail);
	}
}
