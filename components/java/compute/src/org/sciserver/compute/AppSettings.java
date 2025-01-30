/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.compute.core.registry.Registry;


public class AppSettings {
    private static final Logger logger = LogManager.getLogger(AppSettings.class);

    private Registry registry;

    private final String loginPortalUrl;
    private final String loginCallback;
    private final String logoutCallback;
    private final String registryDriver;
    private final String registryConnString;
    private final boolean registryMigrate;
    private final String registryChangelogPath;
    private final String certificatePath;
    private final String settingsTable;
    private final String configVolume;
    // if true, do a cleanup (by stopping containers) based on activity time set in database (as opposed
    // to docker/configurable proxy specific method)
    private final boolean enableDBBasedCleanup;
    private final int sessionInactiveTimeoutSecs;

    private final String logApplicationHost;
    private final String logApplicationName;
    private final String logMessagingHost;
    private final String logDatabaseQueueName;
    private final String logExchangeName;
    private final boolean logEnabled;

    private final String racmUrl;
    private final String dashboardUrl;
    private final String casJobsUrl;
    private final String sciDriveUrl;
    private final String skyServerUrl;
    private final String skyQueryUrl;
    private final String helpUrl;
    private final String sciserverVersion;
    private final String[] privilegedGroups;
    private final String[] developerGroups;

    private final int daskWorkers;
    private final String daskMemory;
    private final int daskThreads;

    private final Set<SciServerLink> uiSciServerLinks;
    private final boolean uiJobsEnabled;
    private final boolean uiInformationEnabled;

    private final String policiesUrl;

    private final String navbarColor;
    private final String fontFamily;
    private final String applicationName;
    private final String applicationTagline;
    private final String applicationHomeUrl;

    public AppSettings(Properties properties) {
        loginPortalUrl = properties.getProperty("login_portal.url");
        loginCallback = properties.getProperty("login_portal.login_callback");
        logoutCallback = properties.getProperty("login_portal.logout_callback");
        certificatePath = properties.getProperty("certificate_path");
        settingsTable = properties.getProperty("settings_table");
        registryDriver = properties.getProperty("registry.driver");
        registryConnString = properties.getProperty("registry.conn_string");
        registryMigrate = Boolean.parseBoolean(properties.getProperty("registry.migrate", "false"));
        configVolume = properties.getProperty("config.volume-name");
        enableDBBasedCleanup = Boolean.parseBoolean(properties.getProperty("enableDBBasedCleanup", "false"));
        sessionInactiveTimeoutSecs = Integer.parseInt(properties.getProperty("sessionInactiveTimeoutSecs", "3600"));

        logApplicationHost = properties.getProperty("Log.ApplicationHost");
        logApplicationName = properties.getProperty("Log.ApplicationName");
        logMessagingHost = properties.getProperty("Log.MessagingHost");
        logDatabaseQueueName = properties.getProperty("Log.DatabaseQueueName");
        logExchangeName = properties.getProperty("Log.ExchangeName");
        logEnabled = Boolean.parseBoolean(properties.getProperty("Log.Enabled"));

        racmUrl = properties.getProperty("RACM.Url");
        dashboardUrl = properties.getProperty("Dashboard.Url");
        casJobsUrl = properties.getProperty("CasJobs.Url");
        sciDriveUrl = properties.getProperty("SciDrive.Url");
        skyServerUrl = properties.getProperty("SkyServer.Url");
        skyQueryUrl = properties.getProperty("SkyQuery.Url");
        helpUrl = properties.getProperty("ComputeHelp.Url");
        sciserverVersion = properties.getProperty("sciserver.version");
        privilegedGroups = properties.getProperty("privileged_groups", "").split(",");
        developerGroups = properties.getProperty("developer_groups", "").split(",");
        registryChangelogPath =
                properties.getProperty("registry.changelog_path", "db/changelog/db.changelog-master.xml");

        daskWorkers = Integer.parseInt(properties.getProperty("dask.workers", "1"));
        daskMemory = properties.getProperty("dask.memory", "1e9");
        daskThreads = Integer.parseInt(properties.getProperty("dask.threads", "4"));

        uiJobsEnabled = Boolean.parseBoolean(properties.getProperty("ui.jobs.enabled", "true"));
        uiInformationEnabled = Boolean.parseBoolean(properties.getProperty("ui.information.enabled", "true"));

        uiSciServerLinks = parseSciServerLinks();

        policiesUrl = properties.getProperty("support.policiesUrl", "https://www.sciserver.org/support/policies/");

        navbarColor = properties.getProperty("theme.navbarColor", "#003466");
        fontFamily = properties.getProperty("theme.fontFamily", "Helvetica Neue, Helvetica, Arial, sans-serif");
        applicationName = properties.getProperty("theme.applicationName", "SciServer");
        applicationTagline = properties.getProperty("theme.applicationTagline", "Data, Collaboration, Compute");
        applicationHomeUrl = properties.getProperty("theme.applicationHomeUrl", "https://www.sciserver.org/");
    }

    private Set<SciServerLink> parseSciServerLinks() {
        Set<SciServerLink> result = new HashSet<SciServerLink>();

        if (!StringUtils.isEmpty(getDashboardUrl())) {
            result.add(SciServerLink.DASHBOARD);
            result.add(SciServerLink.ACTIVITYLOG);
        }
        if (!StringUtils.isEmpty(getCasJobsUrl())) {
            result.add(SciServerLink.CASJOBS);
        }
        if (!StringUtils.isEmpty(getSciDriveUrl())) {
            result.add(SciServerLink.SCIDRIVE);
        }
        if (!StringUtils.isEmpty(getSkyServerUrl())) {
            result.add(SciServerLink.SKYSERVER);
        }
        if (!StringUtils.isEmpty(getSkyQueryUrl())) {
            result.add(SciServerLink.SKYQUERY);
        }
        if (isUiJobsEnabled()) {
            result.add(SciServerLink.COMPUTEJOBS);
        }

        if (result.isEmpty()) {
            result.add(SciServerLink.NONE);
        } else {
            result.add(SciServerLink.COMPUTE);
        }

        return result;
    }

    private long[] parseLongs(String s) throws Exception {
        try {
            String[] a = Stream.of(s.split("[ ,;]")).filter(t -> !t.isEmpty()).toArray(String[]::new);
            long[] result = new long[a.length];
            for (int i = 0; i < a.length; i++) {
                result[i] = Long.parseLong(a[i]);
            }
            Arrays.sort(result);
            return result;
        } catch (Exception ex) {
            throw new Exception("Could not parse default images from string: \"" + s + "\".", ex);
        }
    }

    public String getRegistryChangelogPath() {
        return registryChangelogPath;
    }

    public String getLoginPortalUrl() {
        return loginPortalUrl;
    }

    public String getLoginCallback() {
        return loginCallback;
    }

    public String getLogoutCallback() {
        return logoutCallback;
    }

    public String getRegistryDriver() {
        return registryDriver;
    }

    public String getRegistryConnString() {
        return registryConnString;
    }

    public boolean isRegistryMigrate() {
        return registryMigrate;
    }

    public String getConfigVolume() {
        return configVolume;
    }

    public boolean getEnableDBBasedCleanup() {
        return enableDBBasedCleanup;
    }

    public int getSessionInactiveTimeoutSecs() {
        return sessionInactiveTimeoutSecs;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public String getSettingsTable() {
        return settingsTable;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public long[] getDefaultImages() throws Exception {
        try {
            return parseLongs(getRegistry().getSettings("DefaultImages"));
        } catch (Exception ex) {
            logger.warn(ex);
            return new long[] {};
        }
    }

    public boolean isMaintenanceMode() throws Exception {
        return Boolean.parseBoolean(getRegistry().getSettings("MaintenanceMode"));
    }

    public boolean isCleanupEnabled() throws Exception {
        return Boolean.parseBoolean(getRegistry().getSettings("Cleanup.Enabled"));
    }

    public int getCleanupInactiveHours() throws Exception {
        return Integer.parseInt(getRegistry().getSettings("Cleanup.InactiveHours"));
    }

    public int getCleanupIntervalHours() throws Exception {
        return Integer.parseInt(getRegistry().getSettings("Cleanup.IntervalHours"));
    }

    public int getMaxContainersPerUser() throws Exception {
        return Integer.parseInt(getRegistry().getSettings("MaxContainersPerUser"));
    }

    public String getMaxContainerSize() throws Exception {
        return getRegistry().getSettings("MaxContainerSize");
    }

    public long getDefaultDomainId() throws Exception {
        return Long.parseLong(getRegistry().getSettings("DefaultDomainId"));
    }

    public String getAlertMessage() throws Exception {
        return getRegistry().getSettings("AlertMessage");
    }

    public String getLogApplicationHost() {
        return logApplicationHost;
    }

    public String getLogApplicationName() {
        return logApplicationName;
    }

    public String getLogMessagingHost() {
        return logMessagingHost;
    }

    public String getLogDatabaseQueueName() {
        return logDatabaseQueueName;
    }

    public String getLogExchangeName() {
        return logExchangeName;
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public String getRacmUrl() {
        return racmUrl;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public String getCasJobsUrl() {
        return casJobsUrl;
    }

    public String getSciDriveUrl() {
        return sciDriveUrl;
    }

    public String getSkyServerUrl() {
        return skyServerUrl;
    }

    public String getSkyQueryUrl() {
        return skyQueryUrl;
    }

    public String getHelpUrl() {
        return helpUrl;
    }

    public String getSciserverVersion() {
        return applicationName + " " + sciserverVersion;
    }

    public List<String> getPrivilegedGroups() {
        return Arrays.asList(privilegedGroups);
    }

    public int getDaskWorkers() {
        return daskWorkers;
    }

    public String[] getDeveloperGroups() {
        return developerGroups;
    }

    public String getDaskMemory() {
        return daskMemory;
    }

    public int getDaskThreads() {
        return daskThreads;
    }

    public Set<SciServerLink> getUiSciServerLinks() {
        return uiSciServerLinks;
    }

    public boolean isUiJobsEnabled() {
        return uiJobsEnabled;
    }

    public boolean isUiInformationEnabled() {
        return uiInformationEnabled;
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
}
