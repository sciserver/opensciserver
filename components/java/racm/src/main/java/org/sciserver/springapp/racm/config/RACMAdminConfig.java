package org.sciserver.springapp.racm.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "org.sciserver.racm.admin")
public class RACMAdminConfig {
	private String username;
	private String password;
	private String email;
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
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	@PostConstruct
	public void fillFromBuiltIn() {
		if (username == null) {
			Properties props = new Properties();

			try {
				props.load(getClass().getResourceAsStream("/racm.properties"));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			username = props.getProperty("racm.admin.user");
			password = props.getProperty("racm.admin.password");
			email = props.getProperty("racm.admin.email");
		}
	}
}
