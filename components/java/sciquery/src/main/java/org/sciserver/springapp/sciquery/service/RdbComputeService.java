package org.sciserver.springapp.sciquery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.authentication.client.User;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.JobStatus;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.springapp.sciquery.SciQueryApiException;
import org.sciserver.springapp.sciquery.model.DbContext;
import org.sciserver.springapp.sciquery.model.DbContextRepository;
import org.sciserver.springapp.sciquery.model.DbDomain;
import org.sciserver.springapp.sciquery.model.DbDomainRepository;
import org.sciserver.springapp.sciquery.model.DbServer;
import org.sciserver.springapp.sciquery.model.DbServerRepository;
import org.sciserver.springapp.sciquery.model.JsonModelViews;
import org.sciserver.springapp.sciquery.model.SciQueryRACMClient2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles operations related to RDB compute domains and database contexts, 
 * including interactions with RACM and the internal SciQuery database.
 */
@Service
public class RdbComputeService {
    private static final Logger LOG = LogManager.getLogger();

    private @Value("#{new Long('${dbContext.default.server}')}") Long dbContextDefaultServer;
    private @Value("#{new Long('${mydb.default.domain}')}") Long mydbDefaultDomain;
    private @Value("#{new Long('${mydb.default.server}')}") Long mydbDefaultServer;
    private @Value("${mydb.readonly.username}") String mydbReadonlyUsername;
    private @Value("${mydb.readonly.password}") String mydbReadonlyPassword;
    private @Value("${mydb.readwrite.username}") String mydbReadwriteUsername;
    private @Value("${mydb.readwrite.password}") String mydbReadwritePassword;

    @Autowired
    RACMClient racmClient;
    @Autowired
    SciQueryRACMClient2 sciqueryRacmClient2;

    @Autowired
    DbDomainRepository dbDomainRepository;
    @Autowired
    DbContextRepository dbContextRepository;
    @Autowired
    DbServerRepository dbServerRepository;

    @Autowired
    CacheService cacheService;
    @Autowired
    SqlService sqlService;

    /**
     * Gets the RDB compute domains and their respective database contexts that a user has access to.
     *
     * @param userId ID of the user.
     * @return A JSON array string of the RDB compute domains and database contexts that the user has access to.
     */
    @Transactional
    public StringJoiner getUserRdbDomains(String userId) {
        Map<Long, Set<String>> rdbComputeDomainUserMap = cacheService.getRdbComputeDomainUserMap();
        Map<Long, Set<String>> databaseContextUserMap = cacheService.getDatabaseContextUserMap();
        ObjectMapper objectMapper = new ObjectMapper();

        StringJoiner dbDomainJsonJoiner = new StringJoiner(",", "[", "]");
        DbDomain allowedDbDomain;

        try {
            for (DbDomain dbDomain : cacheService.getDbDomains()) {
                if (rdbComputeDomainUserMap.get(dbDomain.getRacmId()) != null 
                    && rdbComputeDomainUserMap.get(dbDomain.getRacmId()).contains(userId)) {
                    allowedDbDomain = objectMapper.readValue(objectMapper.writeValueAsString(dbDomain), DbDomain.class);
                    allowedDbDomain.getDbContexts().clear();

                    // Remove dbContexts that are not allowed for this user
                    for (DbContext dbContext : dbDomain.getDbContexts().values()) {
                        if (databaseContextUserMap.get(dbContext.getRacmId()) != null 
                            && databaseContextUserMap.get(dbContext.getRacmId()).contains(userId)) {
                            allowedDbDomain.getDbContexts().put(dbContext.getContextName(), dbContext);
                        } else {
                            LOG.debug(" (Removed dbContext " + dbContext.getRacmId() + ")");
                        }
                    }

                    dbDomainJsonJoiner.add(objectMapper.writerWithView(JsonModelViews.Public.class)
                            .writeValueAsString(allowedDbDomain));
                }
            }
        } catch (JsonProcessingException jpe) {
            LOG.error(jpe.getMessage());
            return null;
        }

        return dbDomainJsonJoiner;
    }

    /**
     * Submits a new RDB job to RACM.
     *
     * @param domainId The ID of the RDB compute domain.
     * @param dbContextName The name of the database context.
     * @param rdbJobModel The RDB job model.
     * @param userId The ID of the user.
     * @param userToken The user's token for RACM authentication.
     * @return The submitted RDB job model.
     * @throws SciServerClientException If there is an error querying RACM.
     * @throws SciQueryApiException If the db context the job is submitted to is invalid.
     */
    public RDBJobModel submitRdbJob(Long domainId, String dbContextName, RDBJobModel rdbJobModel, String userId,
                                    String userToken) throws SciServerClientException, SciQueryApiException {
        DbDomain dbDomain = dbDomainRepository.findByRacmId(domainId);

        List<DbContext> dbContexts = dbContextRepository.findByDbDomainAndContextNameIgnoreCase(dbDomain, 
                                                                                                dbContextName);
        if (dbContexts == null || dbContexts.isEmpty()) {
            throw new SciQueryApiException("Invalid database context!");
        }
        DbContext dbContext = dbContexts.get(0);
        if (dbContext == null) {
            throw new SciQueryApiException("Invalid database context!");
        }

        Boolean canDoAction = null;
        Map<String, String> dbContextActionMap = null;
        String logStr = "submitRdbJob:  user=" + userId;

        if (dbContext.getDbServer().getVendor().equalsIgnoreCase("SQLSERVER")) {
            // Parse the query to determine the tables and actions requested
            try {
                dbContextActionMap = sqlService.parseInputSqlForAction(rdbJobModel.getInputSql(), dbContextName);
            } catch (IOException | ParseCancellationException ex) {
                throw new SciQueryApiException("Invalid SQL!");
            }

            // Using the racmUUID of any requested dbContexts, see if the user can perform the requested actions; 
            // if not, return 400
            for (Map.Entry<String, String> dbContextActionEntry : dbContextActionMap.entrySet()) {
                canDoAction = cacheService.canUserDoResourceActionCache(userId, dbDomain, 
                                                                        dbContextActionEntry.getKey(), 
                                                                        dbContextActionEntry.getValue(), userToken);

                logStr += "\nAction:  SqlS-canUserDo-" + dbContextActionEntry.getKey() 
                       + "+" + dbContextActionEntry.getValue() + "=" + canDoAction;
                if (! canDoAction) {
                    LOG.info(logStr);
                    throw new SciQueryApiException("Action forbidden!");
                }
            }

        // SciQuery API does not currently support SQL parsing or cross-database queries for PostgreSQL, etc.
        } else {
            canDoAction = cacheService.canUserDoResourceActionCache(userId, dbDomain, dbContext.getContextName(), 
                                                                    "QUERY", userToken);
            dbContextActionMap = new HashMap<String, String>();
            dbContextActionMap.put(dbContext.getContextName(), "QUERY");

            logStr += "\nAction:  Pg-canUserDo-" + dbContext.getContextName() + "+" + "QUERY" + "=" + canDoAction;
            if (! canDoAction) {
                LOG.info(logStr);
                throw new SciQueryApiException("Action forbidden!");
            }
        }

        // Update RDBJobModel object as necessary and submit to JOBM
        rdbJobModel.setRdbDomainId(dbDomain.getRacmId());
        rdbJobModel.setStatus(JobStatus.STATUS_PENDING);
        // Dealing w/lack of support for case-sensitivity in JOBMModelFactory.newRDBJob()
        rdbJobModel.setDatabaseContextName(dbContextName);

        // ?TaskName=SciQueryAPI.Jobs.submitRDBQueryJob
        RDBJobModel rdbJobModelResponse = sciqueryRacmClient2.submitRdbJob(userToken, 
                                                                           dbDomain.getServiceToken(), rdbJobModel);

        cacheService.getJobTableDependencyMap().put(rdbJobModelResponse.getId(), dbContextActionMap);

        LOG.debug(logStr);
        return rdbJobModelResponse;
    }


    /**
     * Registers a new RDB compute domain in RACM and the SciQuery database, 
     * along with its associated database contexts.
     *
     * @param dbDomain The RDB compute domain to register.
     * @param apiEndpoint The API endpoint for the RDB compute domain.
     * @param vendor The database vendor of the RDB compute domain.
     * @param admins Name(s) of user group(s) composed by admins of this domain (comma-separated).
     * @param userToken The user token for authentication with RACM.
     * @param userId The ID of the user.
     * @return The registered DbDomain object with IDs populated, or null if the RDB compute domain 
     *         already exists or if any referenced database server does not exist.
     * @throws SciServerClientException If there is an error querying RACM.
     */
    public DbDomain registerDbDomain(DbDomain dbDomain, String apiEndpoint, String vendor, String admins,
                                     String userToken, String userId) throws SciServerClientException {
        // See if RDB compute domain is already defined; if yes, return 400
        List<DbDomain> existingDbDomains = dbDomainRepository.findByDisplayName(dbDomain.getDisplayName());
        if (existingDbDomains != null && ! existingDbDomains.isEmpty()) {
            // Return value of null is used to indicate that the submitted RDB compute domain already exists
            return null;
        }

        RDBComputeDomainModel rdbComputeDomainModel = dbDomain.toRDBComputeDomainModel(apiEndpoint, vendor);
        RDBComputeDomainModel rdbComputeDomainModelResponse = racmClient
                                                              .registerRDBComputeDomain(rdbComputeDomainModel, 
                                                                                        admins, userToken);

        // Propagate RACM information into DbDomain
        dbDomain.setRacmId(rdbComputeDomainModelResponse.getId());
        dbDomain.setRacmUuid(rdbComputeDomainModelResponse.getRacmUUID());

        // Propagate RACM information into DbContexts
        DbContext dbContext = null;
        DbServer dbServerFromId = null;
        for (DatabaseContextModel databaseContextModelResponse : rdbComputeDomainModelResponse.getDatabaseContexts()) {
            dbContext = dbDomain.getDbContexts().get(databaseContextModelResponse.getName());
            dbContext.setRacmId(databaseContextModelResponse.getId());
            dbContext.setRacmUuid(databaseContextModelResponse.getRacmUUID());

            dbServerFromId = dbServerRepository.findById(dbContext.getServerId().longValue());
            if (dbServerFromId == null) {
                // Return value of null is used to indicate that the submitted database server does not exists
                LOG.debug("DbServer referenced by serverId = '" + dbContext.getServerId() + "' does not exist!");
                return null;
            }
            LOG.debug("Attached dbServer.serverName=" + dbServerFromId.getServerName());
            dbContext.setDbServer(dbServerFromId);
        }

        dbDomain = dbDomainRepository.save(dbDomain);

        // Ensure we have object with IDs set
        DbDomain newDbDomain = dbDomainRepository.findByRacmId(dbDomain.getRacmId());

        // Update DbDomain and RDB compute domain cache
        cacheService.refreshDbDomains();
        cacheService.syncRdbComputeDomainUserCache(userId, userToken);

        return newDbDomain;
    }

    /**
     * Creates for a user a new MyDB DbContext in a particular RDB compute domain.
     *
     * @param user The user for whom to create the MyDB context.
     * @param mydbName The name of the MyDB database.
     * @param domainId The ID of the RDB compute domain. 
     *                 If null, the default domain specified in the application properties will be used.
     * @return The created DbContext object, or null if the specified RDB compute domain cannot be found.
     */
    public DbContext createMyDbContext(User user, String mydbName, Long domainId) {
        DbDomain dbDomain = null;
        // If no domain is specified, use the one from the application properties
        if (domainId == null) {
            dbDomain = dbDomainRepository.findByRacmId(mydbDefaultDomain);
        } else  {
            dbDomain = dbDomainRepository.findByRacmId(domainId);
        }

        // If the specified domain can't be found, return null
        if (dbDomain == null) {
            return null;
        }

        DbContext myDbContext = new DbContext(dbDomain, null, null, "mydb:" + user.getUserName(),
                "Default MyDB database of user " + user.getUserName(), mydbName,
                mydbReadonlyUsername, mydbReadonlyPassword);

        // Set read/write credentials
        myDbContext.setReadwriteUserName(mydbReadwriteUsername);
        myDbContext.setReadwriteUserPassword(mydbReadwritePassword);

        // Set up DbServer to the default for the mydb's
        myDbContext.setServerId(mydbDefaultServer);

        return myDbContext;
    }

    /**
     * Registers a new database context within a specified RDB compute domain in RACM and in the SciQuery database.
     *
     * @param domainId ID of the RDB compute domain to register the database context in.
     * @param dbContext The database context to register.
     * @param admins Name(s) of user group(s) composed by admins of this database (comma-separated).
     * @param userToken The user token for authentication.
     * @param userId The ID of the user.
     * @return The registered DbContext object, or null if the registration fails.
     * @throws SciServerClientException If there is an error calling RACM.
     */
    public DbContext registerDbContext(Long domainId, DbContext dbContext, String admins,
                                       String userToken, String userId) throws SciServerClientException {
        DbDomain dbDomain = dbDomainRepository.findByRacmId(domainId);

        // See if dbContext is in the compute domain; if yes, return 400
        List<DbContext> existingDbContexts = dbContextRepository
                                             .findByDbDomainAndContextNameIgnoreCase(dbDomain, 
                                                                                     dbContext.getContextName());
        if (existingDbContexts != null && ! existingDbContexts.isEmpty()) {
            // Return value of null is used to indicate that the submitted database context already exists
            return null;
        }

        // Convert the user's supplied DbContext to a RACM model object for registration
        DatabaseContextModel dbContextModel = dbContext.toDatabaseContextModel();

        // RACM requires the domain's service account token to register a context against it.
        DatabaseContextModel dbContextModelResponse = racmClient.registerRDBComputeDbContext(dbDomain.getRacmId(),
                dbContextModel, admins, userToken, dbDomain.getServiceToken());

        // Update our SciQuery API DbContext with response from RACM
        dbContext.setDbDomain(dbDomain);
        dbContext.setRacmId(dbContextModelResponse.getId());
        dbContext.setRacmUuid(dbContextModelResponse.getRacmUUID());

        DbServer dbServerFromId = dbServerRepository.findById(dbContext.getServerId().longValue());
        if (dbServerFromId == null) {
            // Return value of null is used to indicate that the submitted database server does not exists
            LOG.debug("DbServer referenced by serverId = '" + dbContext.getServerId() + "' does not exist!");
            return null;
        }
        LOG.debug("Attached dbServer.serverName=" + dbServerFromId.getServerName());
        dbContext.setDbServer(dbServerFromId);

        dbDomain.getDbContexts().put(dbContext.getContextName(), dbContext);
        dbDomain = dbDomainRepository.save(dbDomain);

        // Update DbDomain and RdbComputeDomainUserCache
        cacheService.refreshDbDomains();
        if (userId != null) {
            cacheService.syncRdbComputeDomainUserCache(userId, userToken);
        }

        DbContext newDbContext = dbDomain.getDbContexts().get(dbContext.getContextName());
        return newDbContext;
    }

    /**
     * Registers a new database server in the SciQuery database.
     *
     * @param dbServer The database server object to be registered.
     * @return The registered DbServer object, or null if the db server already exists.
     */
    public DbServer registerDbServer(DbServer dbServer) throws SciServerClientException {
        DbServer dbServerCheck = dbServerRepository.findByHost(dbServer.getHost());
        if (dbServerCheck != null) {
            // Return value of null is used to indicate that the submitted database server already exists
            return null;
        }

        dbServer = dbServerRepository.save(dbServer);

        return dbServer;
    }

    /**
     * Refreshes in the local cache the RDB compute domains and database contexts 
     * that a user has access to, by querying directly from RACM.
     *
     * @param userToken The user's token for RACM authentication.
     * @param userId The ID of the user.
     * @return A list of RDBComputeDomainModel objects representing the refreshed domains.
     * @throws SciServerClientException If there is an error calling RACM.
     */
    public List<RDBComputeDomainModel> refreshRdbComputeDomains(String userToken, String userId)
            throws SciServerClientException {
        // Obtain from RACM the current set of RDB compute domains and DB contexts
        List<RDBComputeDomainModel> rdbComputeDomains = racmClient.queryRDBComputeDomainsCall(userToken);

        DbServer defaultDbServer = dbServerRepository.findById(dbContextDefaultServer.longValue());

        Set<Long> syncedDomainsFromRacm = new HashSet<>();
        DbDomain matchingDbDomain = null;
        for (RDBComputeDomainModel rdbComputeDomain : rdbComputeDomains) {
            LOG.debug("1: RDBDomain=" + rdbComputeDomain.getId());
            // Look up matching DbDomain from SciQuery DB
            matchingDbDomain = cacheService.getDbDomains().stream().filter(dbDomain ->
                    rdbComputeDomain.getId().equals(dbDomain.getRacmId())).findAny().orElse(null);

            // If the RACM RDB compute domain exists in SciQuery
            if (matchingDbDomain != null) {
                LOG.debug("2: LocalDomain=" + matchingDbDomain.getRacmId());
                matchingDbDomain.updateFromRDBComputeDomainModel(rdbComputeDomain, null);

            // Else the RACM RDB compute domain is new to SciQuery
            } else {
                LOG.debug("3: NewLocalDomain");
                matchingDbDomain = new DbDomain(rdbComputeDomain, defaultDbServer);
            }

            dbDomainRepository.save(matchingDbDomain);
            syncedDomainsFromRacm.add(rdbComputeDomain.getId());
        }

        // Remove RDB compute domain from SciQuery DB and the cache if not returned from RACM
        for (DbDomain dbDomain : cacheService.getDbDomains()) {
            if (! syncedDomainsFromRacm.contains(dbDomain.getRacmId())) {
                dbDomainRepository.delete(dbDomain);

                // TODO: Remove from user caches
            }
        }

        // Update DbDomain and RDB compute domain cache
        cacheService.refreshDbDomains();
        cacheService.syncRdbComputeDomainUserCache(userId, userToken);

        return rdbComputeDomains;
    }
}
