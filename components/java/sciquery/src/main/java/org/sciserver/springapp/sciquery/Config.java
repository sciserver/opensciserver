package org.sciserver.springapp.sciquery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Configuration class for SciQuery application.
 * Manages loading and providing access to application properties and database configurations.
 */
public class Config {
    public static final boolean IS_DEBUG = true;
    public static final int CONNECTION_TIMEOUT = (IS_DEBUG) ? 100000 : 60;
    public static final int READ_TIMEOUT = (IS_DEBUG) ? 100000 : 60;
    public static final int WRITE_TIMEOUT = (IS_DEBUG) ? 100000 : 60;

    private static final Logger LOG = LogManager.getLogger();

    @Autowired
    private DataSource sciQueryDBDataSource;

    private Properties properties;

    private String authPortalUrl;

    private String racmEndpoint;
    private Long racmUserGroupAdmin;
    private String racmServiceToken;

    private Long dbContextDefaultServer;

    private Long mydbDefaultDomain;
    private Long mydbDefaultServer;
    private String mydbReadonlyUsername;
    private String mydbReadonlyPassword;
    private String mydbReadwriteUsername;
    private String mydbReadwritePassword;

    private String casJobsEndpoint;
    private String casJobsUUID;

    private String fileUploadBasePath;
    private long maxFileUploadSize;
    private String fileServiceToken;

    private int resultFetchSize;
    private int queryTimeout;
    private int numRowsPerFlush;

    public Config() { }

    /**
     * Loads configuration properties.
     *
     * @throws IOException if an I/O error occurs when reading the properties file.
     * @throws Exception if any other error occurs during property loading.
     */
    public void load() throws IOException, Exception {
        this.properties = new Properties();
        String configFilename = System.getenv("SCISERVER_PROPERTIES_FILE");

        InputStream input;
        if (configFilename != null) {
            LOG.info("Loading config from {}", configFilename);
            input = new FileInputStream(configFilename);
        } else {
            LOG.info("Loading config from built-in application.properties");
            input = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties");
        }
        this.properties.load(input);

        this.authPortalUrl = this.properties.getProperty("auth.portal.url");

        this.racmEndpoint = this.properties.getProperty("RACM.endpoint");
        this.racmUserGroupAdmin = Long.valueOf(this.properties.getProperty("RACM.userGroup.admin"));
        this.racmServiceToken = this.properties.getProperty("RACM.resourceContext.serviceToken");

        this.dbContextDefaultServer = Long.valueOf(this.properties.getProperty("dbContext.default.server"));

        this.mydbDefaultDomain = Long.valueOf(this.properties.getProperty("mydb.default.domain"));
        this.mydbDefaultServer = Long.valueOf(this.properties.getProperty("mydb.default.server"));
        this.mydbReadonlyUsername = this.properties.getProperty("mydb.readonly.username");
        this.mydbReadonlyPassword = this.properties.getProperty("mydb.readonly.password");
        this.mydbReadwriteUsername = this.properties.getProperty("mydb.readwrite.username");
        this.mydbReadwritePassword = this.properties.getProperty("mydb.readwrite.password");

        this.casJobsEndpoint = this.properties.getProperty("CasJobs.endpoint");
        this.casJobsUUID = this.properties.getProperty("CasJobs.UUID");
        this.fileServiceToken = this.properties.getProperty("File-service.serviceId");
        this.resultFetchSize = Integer.parseInt(this.properties.getProperty("DB.result_fetch_size"));
        this.queryTimeout = Integer.parseInt(this.properties.getProperty("DB.query_timeout"));
        this.numRowsPerFlush = Integer.parseInt(this.properties.getProperty("HTTP.RESPONSE.BODY.numRowsPerFlush"));
        this.maxFileUploadSize = Long.valueOf(this.properties.getProperty("file.upload.maxFileSize"));
        this.fileUploadBasePath = StringUtils.removeEnd(this.properties.getProperty("file.upload.basePath"), 
                                                        File.separator) + File.separator;
        if (this.fileUploadBasePath == null || this.fileUploadBasePath.isEmpty() 
            || !new File(this.fileUploadBasePath).exists()) {
            throw new Exception("Invalid file upload base path: " + this.fileUploadBasePath);
        }
    }

    public DataSource getSciQueryDBDataSource() {
        return sciQueryDBDataSource;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getAuthPortalUrl() {
        return authPortalUrl;
    }

    public String getRacmEndpoint() {
        return racmEndpoint;
    }

    public Long getRacmUserGroupAdmin() {
        return racmUserGroupAdmin;
    }

    public String getRacmServiceToken() {
        return racmServiceToken;
    }

    public Long getDbContextDefaultServer() {
        return dbContextDefaultServer;
    }

    public Long getMydbDefaultDomain() {
        return mydbDefaultDomain;
    }

    public Long getMydbDefaultServer() {
        return mydbDefaultServer;
    }

    public String getMydbReadonlyUsername() {
        return mydbReadonlyUsername;
    }

    public String getMydbReadonlyPassword() {
        return mydbReadonlyPassword;
    }

    public String getMydbReadwriteUsername() {
        return mydbReadwriteUsername;
    }

    public String getMydbReadwritePassword() {
        return mydbReadwritePassword;
    }

    public String getCasJobsEndpoint() {
        return casJobsEndpoint;
    }

    public String getCasJobsUUID() {
        return casJobsUUID;
    }

    public String getFileUploadBasePath() {
        return fileUploadBasePath;
    }

    public long getMaxFileUploadSize() {
        return maxFileUploadSize;
    }

    public String getFileServiceToken() {
        return fileServiceToken;
    }

    public int getResultFetchSize() {
        return resultFetchSize;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public int getNumRowsPerFlush() {
        return numRowsPerFlush;
    }
}
