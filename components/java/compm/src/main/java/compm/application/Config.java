package compm.application;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.springframework.cache.annotation.Cacheable;


/**
 * The `Config` class in Java is a singleton class that loads configuration properties from a file and
 * provides getter methods for accessing these properties.
 */
public class Config {

    private static final Config instance = new Config();
    FileService fileService = new FileService();
    
    private String jobType;
    
    private String jobmUrl;
    private String jobmUrlGetNextJob;
    private String jobmUrlAcceptJob;
    private String jobmUrlRefuseJob;
    private String jobmUrlUpdateJobStatus;
    private String jobmUrlCheckForMessages;
    private String jobmUrlRegisterCompm;
    private String jobmUrlGetCanceledJobs;

    private String computeUrl;
    private String computeUrlSubmitJob;
    private String computeUrlCheckJobStatus;
    private String computeUrlGetJobOutput;
    private String computeUrlDeleteJob;
    private String computeUrlGetContainers;

    private String sciqueryDBJdbcURL;
    private int resultFetchSize;
    private long numRowsPerFlush;
    private HikariDataSource sciQueryDBDataSource;

    private String fileServiceUrl;
    private String fileServiceUrlUploadFile;
    private String fileServiceBasePath;
    private String fileServiceRootVolume;
    private String fileServiceUserVolume;
    private String fileServiceJobsFolderRelativePath;
    private LinkedHashMap<String, MinimalFileServiceModel> fileServices;
    private String storemUrl;    

    private String logApplicationHost;
    private String logApplicationName;
    private String logMessagingHost;
    private String logDatabaseQueueName;
    private String logExchangeName;
    private Boolean logEnabled;

    private String compmId; // should be obtained from the config file.
    private Integer maxNumberOfJobs; // maximum number of jobs asked to JOBM for COMPM to manage at a time.
    private Integer minNumberOfJobs; // minimum number of jobs that COMPM shall reach as stored in its local queue, 
    // before COMPM asking JOBM for a new batch of available jobs.
    private Integer numWorkers;

    private Long idleTimeBetweenJobs;
    private Long idleTimeBetweenJobmRequests;
    private Long idleTimeBetweenFailedRequests;

    private Long maxTries;
    private Integer httpWriterTimeout;
    private Integer databaseWriterTimeout;

    private String casjobsApiBaseUrl;
    private String loginPortalApiBaseUrl;

    private static final Logger LOG = LogManager.getLogger(Config.class);


    private Config() { }

    public static Config getInstance() {
        return instance;
    }
    
    /**
     * The `load()` function reads and parses properties from a configuration file, setting various
     * instance variables based on the properties read.
     */
    public static void load() throws Exception {
        try {
            
            Properties properties = new Properties();
            String configFilename = System.getenv("SCISERVER_PROPERTIES_FILE");
            InputStream input;
            if (configFilename != null) {
                LOG.info("Loading config from {}", configFilename);
                input = new FileInputStream(configFilename);
            } else {
                LOG.info("Loading config from built-in config.properties");
                input = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
            }
            properties.load(input);
            
            instance.jobType = properties.getProperty("Compm.jobType");
            instance.compmId = properties.getProperty("Compm.compmId");
            instance.maxNumberOfJobs = Integer.parseInt(properties.getProperty("Compm.maxNumberOfJobs"));
            instance.minNumberOfJobs = Integer.parseInt(properties.getProperty("Compm.minNumberOfJobs"));
            instance.numWorkers = Integer.parseInt(properties.getProperty("Compm.numWorkers"));
            
            instance.idleTimeBetweenJobs = ((Double) Double.parseDouble(
                                                            properties.getProperty("Compm.idleTimeBetweenJobs"))
                                           ).longValue();
            instance.idleTimeBetweenJobmRequests = ((Double) Double.parseDouble(
                                                                    properties
                                                                    .getProperty("Compm.idleTimeBetweenJobmRequests"))
                                                   ).longValue();
            instance.idleTimeBetweenFailedRequests = ((Double) Double.parseDouble(
                                                                      properties
                                                                      .getProperty(
                                                                        "Compm.idleTimeBetweenFailedRequests"
                                                                      ))
                                                      ).longValue();
            instance.maxTries = Long.parseLong(properties.getProperty("Compm.maxTries"));
            
            instance.jobmUrl = properties.getProperty("RACM.baseUrl") + "jobm/rest/";
            instance.jobmUrlAcceptJob = instance.jobmUrl + "compmjob/{jobId}";
            instance.jobmUrlCheckForMessages = instance.jobmUrl + "compmjob/{jobId}";
            instance.jobmUrlGetNextJob = instance.jobmUrl + "compmjob/next";
            instance.jobmUrlRefuseJob = instance.jobmUrl + "compmjob/{jobId}";
            instance.jobmUrlRegisterCompm = instance.jobmUrl + "compmjobs/open" + "";
            instance.jobmUrlGetCanceledJobs = instance.jobmUrl + "compmjobs/canceled";
            instance.jobmUrlUpdateJobStatus = instance.jobmUrl + "compmjob/{jobId}";
            
            instance.computeUrl = properties.getProperty("Compute.domainUrl");
            instance.computeUrlCheckJobStatus = instance.computeUrl + "containers/{jobId}/json";
            instance.computeUrlDeleteJob = instance.computeUrl + "containers/{jobId}";        
            instance.computeUrlGetJobOutput = instance.computeUrl + "containers/{jobId}/logs";
            instance.computeUrlSubmitJob = instance.computeUrl + "containers";
            instance.computeUrlGetContainers = instance.computeUrl + "containers?description=API%20container";
            
            instance.numRowsPerFlush = Long.parseLong(properties.getProperty("OUTPUT.WRITER.numRowsPerFlush"));
            
            instance.fileServiceUrl = properties.getProperty("FileService.baseUrl");
            instance.fileServiceUrlUploadFile = instance.fileServiceUrl + "api/data";

            instance.httpWriterTimeout = 1000 * Integer.parseInt(properties.getProperty(
                                                             "OUTPUT.WRITER.httpRequestTimeout"));
            instance.databaseWriterTimeout = Integer.parseInt(properties.getProperty(
                                                          "OUTPUT.WRITER.dbConnectionTimeout"));            

            //make sure fileServiceBasePath starts and ends with a slash:
            instance.fileServiceBasePath = properties.getProperty("FileService.basePath");
            instance.fileServiceBasePath = "/" + instance.fileServiceBasePath.replaceAll("^/+", "")
                                                                             .replaceAll("/+$", "") + "/";
            //make sure fileServiceRootVolume and fileServiceUserVolume don't contain a slash
            instance.fileServiceRootVolume = properties.getProperty("FileService.rootVolume");
            instance.fileServiceUserVolume = properties.getProperty("FileService.userVolume");
            if (instance.fileServiceUserVolume.contains("/") || instance.fileServiceRootVolume.contains("/")) {
                throw new Exception("FileService.userVolume or FileService.rootVolume cannot contain a '/' character");
            }
            // fileServiceJobsFolderRelativePath should end with slash, but not begin with a slash: 
            instance.fileServiceJobsFolderRelativePath = properties.getProperty("FileService.jobsFolderRelativePath");
            instance.fileServiceJobsFolderRelativePath = instance.fileServiceJobsFolderRelativePath
                                                                 .replaceAll("^/+", "").replaceAll("/+$", "") + "/";
            instance.storemUrl = properties.getProperty("RACM.baseUrl") + "storem/";
            instance.fileServices = new LinkedHashMap<String, MinimalFileServiceModel>();

            instance.logApplicationHost = properties.getProperty("Log.ApplicationHost");
            instance.logApplicationName = properties.getProperty("Log.ApplicationName");
            instance.logMessagingHost = properties.getProperty("Log.MessagingHost");
            instance.logDatabaseQueueName = properties.getProperty("Log.DatabaseQueueName");
            instance.logExchangeName = properties.getProperty("Log.ExchangeName");
            instance.logEnabled = Boolean.parseBoolean(properties.getProperty("Log.Enabled"));
            
            instance.casjobsApiBaseUrl = properties.getProperty("CasJobs.apiBaseUrl");

            instance.loginPortalApiBaseUrl = properties.getProperty("Login.apiBaseUrl");


            //load DB connection, only needed by rdb COMPMs
            if ("rdb".equals(instance.jobType)) {
                instance.resultFetchSize = Integer.parseInt(properties.getProperty("DB.result_fetch_size"));
                instance.sciqueryDBJdbcURL = properties.getProperty("DB.sciquery_db_jdbc_url");
                int connectionPoolSize = Integer.parseInt(properties.getProperty("DB.sciquery_db_conn_pool_size"));

                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setRegisterMbeans(true);
                hikariConfig.setJdbcUrl(instance.sciqueryDBJdbcURL);
                hikariConfig.setMaximumPoolSize(connectionPoolSize);
                instance.sciQueryDBDataSource = new HikariDataSource(hikariConfig);
            }

            
        } catch (Exception e) {
            
            String errorMessage = "Could not successfully read/parse properties from file config.properties.\n";
            LOG.fatal(errorMessage);
            throw(e);
        }
        
    }

    @Cacheable(value = "fileservices", key = "#fileServiceUUID")
    public String getFileServiceUrl(String fileServiceUUID, String token) throws Exception {
        return fileService.getFileServiceUrl(fileServiceUUID, token);
    }

    public String getFileServiceUrl() {
        return fileServiceUrl;
    }

    public String getJobmUrl() {
        return jobmUrl;
    }

    public String getJobmUrl_getNextJob() {
        return jobmUrlGetNextJob;
    }

    public String getJobmUrl_acceptJob() {
        return jobmUrlAcceptJob;
    }

    public String getJobmUrl_refuseJob() {
        return jobmUrlRefuseJob;
    }

    public String getJobmUrl_updateJobStatus() {
        return jobmUrlUpdateJobStatus;
    }

    public String getJobmUrl_checkForMessages() {
        return jobmUrlCheckForMessages;
    }

    public String getJobmUrl_registerCompm() {
        return jobmUrlRegisterCompm;
    }
    
    public String getJobmUrl_getCanceledJobs() {
        return jobmUrlGetCanceledJobs;
    }

    public String getComputeUrl_submitJob() {
        return computeUrlSubmitJob;
    }
    
    public String getComputeUrl_checkJobStatus() {
        return computeUrlCheckJobStatus;
    }
    
    public String getComputeUrl_getJobOutput() {
        return computeUrlGetJobOutput;
    }
    
    public String getComputeUrl_deleteJob() {
        return computeUrlDeleteJob;
    }

    public String getComputeUrl_getContainers() {
        return computeUrlGetContainers;
    }
    
    public Integer getResultFetchSize() {
        return resultFetchSize;
    }

    public Integer gethttpWriterTimeout() {
        return httpWriterTimeout;
    }

    public Integer getDatabaseWriterTimeout() {
        return databaseWriterTimeout;
    }
    
    public long getNumRowsPerFlush() {
        return numRowsPerFlush;
    }

    public String getSciqueryDBJdbcURL() {
        return sciqueryDBJdbcURL;
    }

    public HikariDataSource getSciQueryDBDataSource() {
        return sciQueryDBDataSource;
    }

    public String getFileServiceUrl_uploadFile() {
        return fileServiceUrlUploadFile;
    }
    
    public String getFileServiceBasePath() {
        return fileServiceBasePath;
    }

    public String getFileServiceRootVolume() {
        return fileServiceRootVolume;
    }

    public String getFileServiceUserVolume() {
        return fileServiceUserVolume;
    }
    
    public String getFileServiceJobsFolderRelativePath() {
        return fileServiceJobsFolderRelativePath;
    }

    public String getStoremUrl() {
        return storemUrl;
    }

    public LinkedHashMap<String, MinimalFileServiceModel> getFileServices() {
        return fileServices;
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

    public boolean getLogEnabled() {
        return logEnabled;
    }

    public String getJobType() {
        return jobType;
    }
    
    public String getCompmId() {
        return compmId;
    }

    public int getMaxNumberOfJobs() {
        return maxNumberOfJobs;
    }

    public int getMinNumberOfJobs() {
        return minNumberOfJobs;
    }

    public int getNumWorkers() {
        return numWorkers;
    }
    
    public Long getIdleTimeBetweenJobs() {
        return idleTimeBetweenJobs;
    }

    public Long getIdleTimeBetweenJobmRequests() {
        return idleTimeBetweenJobmRequests;
    }
    
    public Long getIdleTimeBetweenFailedRequests() {
        return idleTimeBetweenFailedRequests;
    }

    public Long getMaxTries() {
        return maxTries;
    }

    public String getCasjobsApiBaseUrl() {
        return casjobsApiBaseUrl;
    }

    public String getLoginPortalApiBaseUrl() {
        return loginPortalApiBaseUrl;
    }
}