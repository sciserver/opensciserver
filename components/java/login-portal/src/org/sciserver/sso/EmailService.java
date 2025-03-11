package org.sciserver.sso;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.sciserver.sso.model.EmailRegistrationPayload;
import org.sciserver.sso.model.PasswordResetPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
	@Autowired
	private EncryptedMessageService encryptedMessageService;

	@Autowired
	private TemplateEngine templateEngine;

	public void sendRegistrationEmail(EmailRegistrationPayload payload, String callback)
			throws EmailException, GeneralSecurityException, IOException {
		AppSettings appSettings = AppConfig.getInstance().getAppSettings();
		HtmlEmail email = new HtmlEmail();
		email.setHostName(appSettings.getSmtpHost());
		email.setSmtpPort(appSettings.getSmtpPort());
		email.setFrom(appSettings.getSmtpFrom());
		email.setSubject("Welcome to " + appSettings.getApplicationName() + "!");
		final Context ctx = new Context();
		ctx.setVariable("username", payload.getAttemptedUserCredentials().getUserName());
		ctx.setVariable("emailCallback", appSettings.getEmailCallback());
		ctx.setVariable("callbackUrl", callback);
		ctx.setVariable("code", encryptedMessageService.generateEncryptedString(payload));
		ctx.setVariable("expirationTime", appSettings.getValidationCodeLifetimeMinutes().toString());
		ctx.setVariable("helpdeskEmail", appSettings.getHelpdeskEmail());
		ctx.setVariable("appName", appSettings.getApplicationName());
		email.setTextMsg(templateEngine.process("text/registration.txt", ctx));
		email.setHtmlMsg(templateEngine.process("html/registration.html", ctx));
		email.addTo(payload.getAttemptedUserCredentials().getEmail());
		email.send();
	}

	public void sendResetPasswordEmail(String userName, String userEmail,
			PasswordResetPayload payload)
					throws EmailException, GeneralSecurityException, IOException {
		AppSettings appSettings = AppConfig.getInstance().getAppSettings();
		HtmlEmail email = new HtmlEmail();
		email.setHostName(appSettings.getSmtpHost());
		email.setSmtpPort(appSettings.getSmtpPort());
		email.setFrom(appSettings.getSmtpFrom());
		email.setSubject(appSettings.getApplicationName() + " password reset");
		final Context ctx = new Context();
		ctx.setVariable("username", userName);
		ctx.setVariable("emailCallback", appSettings.getEmailCallback());
		ctx.setVariable("code", encryptedMessageService.generateEncryptedString(payload));
		ctx.setVariable("expirationTime", appSettings.getValidationCodeLifetimeMinutes().toString());
		ctx.setVariable("helpdeskEmail", appSettings.getHelpdeskEmail());
		ctx.setVariable("appName", appSettings.getApplicationName());
		email.setTextMsg(templateEngine.process("text/reset_password.txt", ctx));
		email.setHtmlMsg(templateEngine.process("html/reset_password.html", ctx));
		email.addTo(userEmail);
		email.send();
	}
	
	public void sendRegistrationApprovalEmail(String userEmail, boolean approve) throws EmailException, IOException {
		AppSettings appSettings = AppConfig.getInstance().getAppSettings();
		HtmlEmail email = new HtmlEmail();
		email.setHostName(appSettings.getSmtpHost());
		email.setSmtpPort(appSettings.getSmtpPort());
		email.setFrom(appSettings.getSmtpFrom());
		final Context ctx = new Context();
		ctx.setVariable("emailCallback", appSettings.getEmailCallback());
		ctx.setVariable("helpdeskEmail", appSettings.getHelpdeskEmail());
		ctx.setVariable("appName", appSettings.getApplicationName());
		if (approve ) {
			email.setSubject(appSettings.getApplicationName() + " registration approved");
			email.setTextMsg(templateEngine.process("text/approve.txt", ctx));
			email.setHtmlMsg(templateEngine.process("html/approve.html", ctx));
		} else {
			email.setSubject(appSettings.getApplicationName() + " registration rejected");
			email.setTextMsg(templateEngine.process("text/reject.txt", ctx));
			email.setHtmlMsg(templateEngine.process("html/reject.html", ctx));
		}
		email.addTo(userEmail);
		email.send();
	}
}
