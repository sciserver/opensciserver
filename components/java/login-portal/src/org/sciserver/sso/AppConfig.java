/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.sso.keystone.KeystoneService;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class AppConfig {
	private static final Logger logger = LogManager.getLogger(AppConfig.class);
	
	private static AppConfig instance = new AppConfig();
	private AppSettings appSettings;
	private KeystoneService keystoneService;
	private ConcurrentMap<String, LoginStats> loginStats = new ConcurrentHashMap<>();
	private String version;
	private HikariDataSource dataSource;
	
	public static AppConfig getInstance() {
		return instance;
	}
	
	private AppConfig() {};
	
	public void loadSettings(InputStream input) throws IOException {
		Properties properties = new Properties();
		properties.load(input);
		appSettings = new AppSettings(properties);
	}
	
	public void startup() throws ClassNotFoundException, SQLException {
		keystoneService = new KeystoneService(appSettings);

		if (appSettings.isKeycloakEnabled() || appSettings.isEmailFilteringEnabled()) {
			Class.forName(appSettings.getDatabaseDriver());
			HikariConfig hikariConfig = new HikariConfig();
			hikariConfig.setRegisterMbeans(true);
			hikariConfig.setJdbcUrl(appSettings.getDatabaseConnString());
			hikariConfig.setMaximumPoolSize(8);
			HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
			dataSource = hikariDataSource;
			initilizeDatabase();
		}
	}
	
	public void shutdown() {
		dataSource.close();
	}
	
	private void initilizeDatabase() throws SQLException {
		logger.info("Initializing database");
		try (Connection conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `user_mapping` ("
						+ "`row_id` BIGINT NOT NULL AUTO_INCREMENT, "
						+ "`external_user_id` VARCHAR(36) NOT NULL, "
						+ "`keystone_user_id` VARCHAR(64) NOT NULL , "
						+ "`keystone_trust_id` VARCHAR(64) NOT NULL, "
						+ "`external_username` VARCHAR(255) NOT NULL, "
						+ "PRIMARY KEY (`row_id`), "
						+ "INDEX `external_user_id_IX` (`external_user_id`))");
				
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `rules` ("
						+ "`id` INT NOT NULL, "
						+ "`type` INT NOT NULL, "
						+ "`reg_ex` VARCHAR(255) NOT NULL, "
						+ "PRIMARY KEY (`id`))");
				
				stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `approval_request` ("
						+ "`row_id` BIGINT NOT NULL AUTO_INCREMENT,"
						+ "`created_at` DATETIME NOT NULL,"
						+ "`keystone_user_id` VARCHAR(64) NOT NULL,"
						+ "`name` VARCHAR(255) NOT NULL,"
						+ "`email` VARCHAR(320) NOT NULL,"
						+ "`ip_address` VARCHAR(45) NULL,"
						+ "`extra` TEXT NULL,"
						+ "`status` TINYINT NOT NULL DEFAULT 0,"
						+ "PRIMARY KEY (`row_id`),"
						+ "INDEX `keystone_user_id_IX` (`keystone_user_id`))");
			}
			conn.commit();
		}
	}
	
	public AppSettings getAppSettings() {
		return appSettings;
	}
	
	public KeystoneService getKeystoneService() {
		return keystoneService;
	}
	
	public ConcurrentMap<String, LoginStats> getLoginStats() {
		return loginStats;
	}

	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public HikariDataSource getDataSource() {
		return dataSource;
	}
}
