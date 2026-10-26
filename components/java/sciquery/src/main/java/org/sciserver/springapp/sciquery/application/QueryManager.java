package org.sciserver.springapp.sciquery.application;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * In charge of creating and managing database connections.
 */
public class QueryManager {

    /**
     * Returns a new HikariDataSource with specific configuration settings for
     * database connection pooling.
     *
     * @return A new instance of HikariDataSource.
     */
    public HikariDataSource getNewDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc_url");
        hikariConfig.setPassword("database_password");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setRegisterMbeans(true);
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setAutoCommit(false);
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        return dataSource;
    }

    public void closeDataSource(HikariDataSource dataSource) {
        dataSource.close();
    }

    public void runQuery(String statement, HikariDataSource dataSource, String[] formats) {

    }

    public String prepareStatement(String statement) {
        return statement;
    }

}
