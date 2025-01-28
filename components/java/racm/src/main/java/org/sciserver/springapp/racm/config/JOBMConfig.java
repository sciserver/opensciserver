package org.sciserver.springapp.racm.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "org.sciserver.racm.jobm")
public class JOBMConfig {
	private String adminUser;
	private String adminPassword;
	private String adminEmail;

	public String getAdminUser() {
		return adminUser;
	}

	public void setAdminUser(String adminUser) {
		this.adminUser = adminUser;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getAdminEmail() {
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}

	@PostConstruct
	public void fillFromBuiltIn() {
		if (adminUser == null && adminPassword == null && adminEmail == null) {
			Properties jobmprops = new Properties();
			try {
				jobmprops.load(getClass().getResourceAsStream("/jobm.properties"));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			adminUser = jobmprops.getProperty("jobm.admin.user");
			adminPassword = jobmprops.getProperty("jobm.admin.password");
			adminEmail = jobmprops.getProperty("jobm.admin.email");
		}
	}
}
