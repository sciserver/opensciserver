package org.sciserver.springapp.racm.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "org.sciserver.racm.login")
public class LoginConfig {
	private String loginPortalUrl;

	private User loginAdmin = new User();

	public static class User {
		private String username;
		private String password;

		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
	}

	public String getLoginPortalUrl() {
		return loginPortalUrl;
	}
	public void setLoginPortalUrl(String loginPortalUrl) {
		this.loginPortalUrl = loginPortalUrl;
	}
	public User getLoginAdmin() {
		return loginAdmin;
	}
	public void setLoginAdmin(User loginAdmin) {
		this.loginAdmin = loginAdmin;
	}

	@PostConstruct
	public void fillFromBuiltIn() {
		if (loginPortalUrl == null) {
			Properties props = new Properties();

			try {
				props.load(getClass().getResourceAsStream("/login.properties"));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			loginPortalUrl = props.getProperty("login.url").trim();

			loginAdmin = new User();
			loginAdmin.setUsername(props.getProperty("login.admin.user").trim());
			loginAdmin.setPassword(props.getProperty("login.admin.password").trim());
		}

		if (loginPortalUrl.charAt(loginPortalUrl.length() - 1) != '/')
			loginPortalUrl = loginPortalUrl + '/';
	}
}
