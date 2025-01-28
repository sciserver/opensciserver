/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.Timer;
import javax.sql.DataSource;
import org.sciserver.authentication.client.AuthenticationClient;
import org.sciserver.authentication.client.CachingAuthenticationClient;
import org.sciserver.compute.core.registry.Registry;
import org.sciserver.compute.core.registry.RegistryImpl;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import sciserver.logging.Logger;

public class AppConfig {

    private static AppConfig instance = new AppConfig();
    private AppSettings appSettings;
    private AuthenticationClient authClient;
    private HikariDataSource dataSource;
    private Registry registry;
    private Timer timer;
    private Timer inactiveTimer;
    private String version;
    private Date lastCleanup;
    private Logger logger;
    private RACMClient racmClient;
    private LoadingCache<String, List<String>> userGroupCache;
    private LoadingCache<String, List<UserDockerComputeDomainModel>> interactiveUserDomainsCache;
    private LoadingCache<String, List<UserDockerComputeDomainModel>> jobsUserDomainsCache;

    public static AppConfig getInstance() {
        return instance;
    }

    private AppConfig() {};

    public void loadSettings(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        appSettings = new AppSettings(properties);
    }

    public void startup() throws Exception {
        authClient = new CachingAuthenticationClient(appSettings.getLoginPortalUrl());

        Class.forName(appSettings.getRegistryDriver());

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setRegisterMbeans(true);
        hikariConfig.setJdbcUrl(appSettings.getRegistryConnString());
        hikariConfig.setMaximumPoolSize(8);
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);

        dataSource = hikariDataSource;

        registry = new RegistryImpl(
            dataSource,
            appSettings.getCertificatePath(),
            appSettings.getSettingsTable());

        appSettings.setRegistry(registry);

        if (appSettings.isRegistryMigrate()) {
            registry.migrateToLatestChanges(appSettings.getRegistryChangelogPath());
        }

        if (appSettings.isCleanupEnabled()) {
            int delay = 60 * 1000; // Wait for 1 min before first execution
            int period = appSettings.getCleanupIntervalHours() * 1000 * 60 * 60;
            timer = new Timer();
            timer.schedule(new CleanupTask(), delay, period);
        }
        inactiveTimer = new Timer();
        inactiveTimer.schedule(new InactivityCleanupTask(), 0, 600 * 1000);

        logger = new Logger(
                appSettings.getLogApplicationHost(),
                appSettings.getLogApplicationName(),
                appSettings.getLogMessagingHost(),
                appSettings.getLogDatabaseQueueName(),
                appSettings.getLogExchangeName(),
                appSettings.isLogEnabled());

        racmClient = new RACMClient(appSettings.getRacmUrl());

        userGroupCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<String>>() {
                    @Override
                    public List<String> load(String token) throws Exception {
                        return racmClient.getGroupsUserIsMemberOf(token).stream()
                            .filter(x -> (appSettings.getPrivilegedGroups().contains(x) || x.equals("admin")))
                            .collect(Collectors.toList());
                    }
                });

        interactiveUserDomainsCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<UserDockerComputeDomainModel>>() {
                    @Override
                    public List<UserDockerComputeDomainModel> load(String token) throws Exception {
                        return racmClient.getUserComputeDomainsInteractive(token);
                    }
                });

        jobsUserDomainsCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<UserDockerComputeDomainModel>>() {
                    @Override
                    public List<UserDockerComputeDomainModel> load(String token) throws Exception {
                        return racmClient.getUserComputeDomainsBatch(token);
                    }
                });
    }

    public void shutdown() {
        dataSource.close();

        if (instance.timer != null) {
            instance.timer.cancel();
        }
        if (instance.inactiveTimer != null) {
            instance.inactiveTimer.cancel();
        }

        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.shutdown();
        }
        catch (Exception ex) { }

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            try {
                DriverManager.deregisterDriver(drivers.nextElement());
            }
            catch (Exception ex) { }
        }
    }

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public AuthenticationClient getAuthClient() {
        return authClient;
    }


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Registry getRegistry() {
        return registry;
    }

    public Date getLastCleanup() {
        return lastCleanup;
    }

    public void setLastCleanup(Date lastCleanup) {
        this.lastCleanup = lastCleanup;

    }

    public Logger getLogger() {
        return logger;
    }

    public RACMClient getRACMClient() {
        return racmClient;
    }

    public LoadingCache<String, List<String>> getUserGroupCache() {
        return userGroupCache;
    }

    public LoadingCache<String, List<UserDockerComputeDomainModel>> getInteractiveUserDomainsCache() {
        return interactiveUserDomainsCache;
    }

    public LoadingCache<String, List<UserDockerComputeDomainModel>> getJobsUserDomainsCache() {
        return jobsUserDomainsCache;
    }

}
