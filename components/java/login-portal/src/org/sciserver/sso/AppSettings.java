/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso;

import java.util.Base64;
import java.util.Properties;

public class AppSettings {
	
	private final String keystoneUrl;
	private final String keystoneAdminToken;
	private final String keystoneAdminUser;
	private final String keystoneAdminPassword;
	private final String keystoneAdminProject;
	private final String defaultDomainId;
	private final int maxLoginAttempts;
	private final int waitMinutes;
	
	private final String cjBaseUrl;
	private final String cjAdminUser;
	private final String cjAdminProject;
	private final String cjAdminPassword;
	private final boolean cjEnabled;
	
	private final String logApplicationHost;
	private final String logApplicationName;
	private final String logDatabaseQueueName;
	private final String logMessagingHost;
	private final String logExchangeName;
	private final Boolean logEnabled;
	private final String defaultCallback;
	private final String emailCallback;
	private final String[] allowedHosts;
	
	private final String smtpHost;
	private final String smtpFrom;
	private final int smtpPort;
	private final String helpdeskEmail;
	private final Double validationCodeLifetimeMinutes;
	private final byte[] secretKey;
	private final boolean validationCodeEnabled;
	
	private final String sciserverVersion;
	private String databaseDriver;
	private String databaseConnString;
	private String keystoneTrusteeUser;
	private String keystoneTrusteeProject;
	private String keystoneTrusteePassword;
	
	private final boolean keycloakEnabled;

	private final String disclaimerInfo;
	private final boolean displaySciserverLogin;
	private final String keycloakLoginButtonText;
	private final String keycloakLoginInfoText;
	private final boolean showGlobusSignout;
	private final boolean isDisclaimerInfo;

	private final boolean emailFilteringEnabled;

	private final String policiesUrl;

	private final String navbarColor;
	private final String fontFamily;
	private final String applicationName;
	private final String applicationTagline;
	private final String applicationHomeUrl;
	
	private final String racmUrl;
	private final String racmSystemRcUuid;
	
	public AppSettings(Properties properties) {
		keystoneUrl = properties.getProperty("keystone.url");
		keystoneAdminToken = properties.getProperty("keystone.admin_token");
		keystoneAdminUser = properties.getProperty("keystone.admin_user");
		keystoneAdminPassword = properties.getProperty("keystone.admin_password");
		keystoneAdminProject = properties.getProperty("keystone.admin_project");
		keystoneTrusteeUser = properties.getProperty("keystone.trustee_user");
		keystoneTrusteeProject = properties.getProperty("keystone.trustee_project");
		keystoneTrusteePassword = properties.getProperty("keystone.trustee_password");
		defaultDomainId = properties.getProperty("keystone.default_domain_id");
		
		maxLoginAttempts = Integer.parseInt(properties.getProperty("login.max_unsuccessful_attempts", "3"));
		waitMinutes = Integer.parseInt(properties.getProperty("login.wait_minutes", "10"));
		
		cjBaseUrl = properties.getProperty("casjobs.base_url");
		cjAdminUser = properties.getProperty("casjobs.admin_user");
		cjAdminProject = properties.getProperty("casjobs.admin_project");
		cjAdminPassword = properties.getProperty("casjobs.admin_password");
		cjEnabled = Boolean.parseBoolean(properties.getProperty("casjobs.enabled", "false"));
		
		logApplicationHost = properties.getProperty("Log.ApplicationHost");
		logApplicationName = properties.getProperty("Log.ApplicationName");
		logMessagingHost = properties.getProperty("Log.MessagingHost");
		logDatabaseQueueName = properties.getProperty("Log.DatabaseQueueName");
		logExchangeName = properties.getProperty("Log.ExchangeName");
		logEnabled = Boolean.parseBoolean(properties.getProperty("Log.Enabled", "false"));
		allowedHosts = properties.getProperty("allowed_hosts").split(";");
		defaultCallback = properties.getProperty("default_callback");
		emailCallback = properties.getProperty("email_callback");
		
		smtpHost = properties.getProperty("smtp.host");
		smtpFrom = properties.getProperty("smtp.from");
		smtpPort = Integer.parseInt(properties.getProperty("smtp.port", "25"));
		
		helpdeskEmail = properties.getProperty("helpdesk.email"); 
		
		validationCodeEnabled = Boolean.parseBoolean(
			properties.getProperty("validation_code.enabled", "false"));
		validationCodeLifetimeMinutes = Double.parseDouble(
			properties.getProperty("validation_code.lifetime_minutes", "5"));
		secretKey = Base64.getDecoder().decode(
			properties.getProperty("validation_code.secret_key", "Y2hhbmdlbWV0b3NvbWV0aGluZzMyYnl0ZXNsb25nZ2c="));

		keycloakEnabled = Boolean.parseBoolean(properties.getProperty("keycloak.enabled", "false"));
		databaseDriver = properties.getProperty("database.driver");
		databaseConnString = properties.getProperty("database.conn_string");

		sciserverVersion = properties.getProperty("sciserver.version");

		disclaimerInfo =  properties.getProperty("disclaimerInfo");
		isDisclaimerInfo = disclaimerInfo.length() > 0 ? true : false;
		displaySciserverLogin = Boolean.parseBoolean(properties.getProperty("displaySciserverLogin"));
		keycloakLoginButtonText = properties.getProperty("keycloakLoginButtonText");
		keycloakLoginInfoText = properties.getProperty("keycloakLoginInfoText");
		showGlobusSignout = Boolean.parseBoolean(properties.getProperty("showGlobusSignout"));
		
		emailFilteringEnabled = Boolean.parseBoolean(properties.getProperty("email_filtering.enabled", "false"));

		policiesUrl = properties.getProperty("support.policiesUrl", "https://www.sciserver.org/support/policies/");

		navbarColor = properties.getProperty("theme.navbarColor", "#003466");
		fontFamily = properties.getProperty("theme.fontFamily", "Helvetica Neue, Helvetica, Arial, sans-serif");
		applicationName = properties.getProperty("theme.applicationName", "SciServer");
		applicationTagline = properties.getProperty("theme.applicationTagline", "Data, Collaboration, Compute");
		applicationHomeUrl = properties.getProperty("theme.applicationHomeUrl", "https://www.sciserver.org/");
		
		racmUrl = properties.getProperty("RACM.Url");
		racmSystemRcUuid = properties.getProperty("RACM.systemRcUuid");
	}

	public String getKeystoneTrusteeUser() {
		return keystoneTrusteeUser;
	}

	public String getKeystoneTrusteeProject() {
		return keystoneTrusteeProject;
	}

	public String getKeystoneTrusteePassword() {
		return keystoneTrusteePassword;
	}

	public int getMaxLoginAttempts() {
		return maxLoginAttempts;
	}

	public int getWaitMinutes() {
		return waitMinutes;
	}

	public String getKeystoneUrl() {
		return keystoneUrl;
	}

	public String getKeystoneAdminToken() {
		return keystoneAdminToken;
	}

	public String getKeystoneAdminUser() {
		return keystoneAdminUser;
	}

	public String getKeystoneAdminPassword() {
		return keystoneAdminPassword;
	}

	public String getKeystoneAdminProject() {
		return keystoneAdminProject;
	}

	public String getDefaultDomainId() {
		return defaultDomainId;
	}
	
	public String[] getAllowedHosts() {
		return allowedHosts;
	}

	public String getCjBaseUrl() {
		return cjBaseUrl;
	}

	public String getCjAdminUser() {
		return cjAdminUser;
	}

	public String getCjAdminProject() {
		return cjAdminProject;
	}

	public String getCjAdminPassword() {
		return cjAdminPassword;
	}

	public boolean isCjEnabled() {
		return cjEnabled;
	}

	public String getLogApplicationHost() {
		return logApplicationHost;
	}

	public String getLogApplicationName() {
		return logApplicationName;
	}

	public String getLogDatabaseQueueName() {
		return logDatabaseQueueName;
	}

	public String getLogMessagingHost() {
		return logMessagingHost;
	}

	public String getLogExchangeName() {
		return logExchangeName;
	}

	public Boolean getLogEnabled() {
		return logEnabled;
	}

	public String getDefaultCallback() {
		return defaultCallback;
	}

	public String getEmailCallback() {
		return emailCallback;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public String getSmtpFrom() {
		return smtpFrom;
	}

	public int getSmtpPort() {
		return smtpPort;
	}
	
	public String getHelpdeskEmail() {
		return helpdeskEmail;
	}
	
	public Double getValidationCodeLifetimeMinutes(){
		return validationCodeLifetimeMinutes;
	}

	public byte[] getSecretKey() {
		return secretKey;
	}

	public String getSciserverVersion() {
		return applicationName + " " + sciserverVersion;
	}

	public String getDatabaseDriver() {
		return databaseDriver;
	}

	public String getDatabaseConnString() {
		return databaseConnString;
	}
	
	public boolean isValidationCodeEnabled() {
		return validationCodeEnabled;
	}

	public boolean isKeycloakEnabled() {
		return keycloakEnabled;
	}

	public String getDisclaimerInfo() {
		return disclaimerInfo;
	}

	public boolean isDisplaySciserverLogin() {
		return displaySciserverLogin;
	}

	public String getKeycloakLoginButtonText() {
		return keycloakLoginButtonText;
	}

	public String getKeycloakLoginInfoText() {
		return keycloakLoginInfoText;
	}

	public boolean showGlobusSignout() {
		return showGlobusSignout;
	}

	public boolean showDisclaimerInfo() {
		return isDisclaimerInfo;
	}
	
	public boolean isEmailFilteringEnabled() {
		return emailFilteringEnabled;
	}

	public String getPoliciesUrl() {
		return policiesUrl;
	}

	public String getNavbarColor() {
		return navbarColor;
	}

	public String getFontFamily() {
		return fontFamily;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getApplicationTagline() {
		return applicationTagline;
	}

	public String getApplicationHomeUrl() {
		return applicationHomeUrl;
	}

	public String getRacmUrl() {
		return racmUrl;
	}

	public String getRacmSystemRcUuid() {
		return racmSystemRcUuid;
	}
}
