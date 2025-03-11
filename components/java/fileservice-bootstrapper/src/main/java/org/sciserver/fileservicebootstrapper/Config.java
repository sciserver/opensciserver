package org.sciserver.fileservicebootstrapper;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class Config {
	private String racmUrl;
	private String loginPortalUrl;

	private String fileServiceURL;
	private String fileServiceIdentifier;
	private String fileServiceServiceToken;

	private User admin;
	public String getRacmUrl() {
		return racmUrl;
	}
	public void setRacmUrl(String racmUrl) {
		this.racmUrl = racmUrl;
	}
	public String getLoginPortalUrl() {
		return loginPortalUrl;
	}
	public String getFileServiceURL() {
		return fileServiceURL;
	}
	public void setFileServiceURL(String fileServiceURL) {
		this.fileServiceURL = fileServiceURL;
	}
	public String getFileServiceIdentifier() {
		return fileServiceIdentifier;
	}
	public void setFileServiceIdentifier(String fileServiceIdentifier) {
		this.fileServiceIdentifier = fileServiceIdentifier;
	}
	public String getFileServiceServiceToken() {
		return fileServiceServiceToken;
	}
	public void setFileServiceServiceToken(String fileServiceServiceToken) {
		this.fileServiceServiceToken = fileServiceServiceToken;
	}
	public User getAdmin() {
		return admin;
	}
	public void setAdmin(User admin) {
		this.admin = admin;
	}
	public void setLoginPortalUrl(String loginPortalUrl) {
		this.loginPortalUrl = loginPortalUrl;
	}
	public static class User {
		private String name;
		private String password;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		@Override
		public String toString() {
			return "User [name=" + name + ", password=" + password + "]";
		}
	}
	@Override
	public String toString() {
		return "Config [racmUrl=" + racmUrl + ", loginPortalUrl=" + loginPortalUrl + ", fileServiceURL="
				+ fileServiceURL + ", fileServiceIdentifier=" + fileServiceIdentifier + ", fileServiceServiceToken="
				+ fileServiceServiceToken + ", admin=" + admin + "]";
	}
}
