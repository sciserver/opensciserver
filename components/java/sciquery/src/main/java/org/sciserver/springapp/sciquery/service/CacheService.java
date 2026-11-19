package org.sciserver.springapp.sciquery.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.sciserver.springapp.sciquery.model.DbContext;
import org.sciserver.springapp.sciquery.model.DbContextRepository;
import org.sciserver.springapp.sciquery.model.DbDomain;
import org.sciserver.springapp.sciquery.model.DbDomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Responsible for caching and reducing the number of calls to RACM.
 */
@Service
public class CacheService {
    @Autowired
    RACMClient racmClient;

    @Autowired
    DbDomainRepository dbDomainRepository;
    @Autowired
    DbContextRepository dbContextRepository;

    private final List<DbDomain> dbDomains;

    private final Map<String, MinimalFileServiceModel> fileServiceIdMap;
    private final Map<String, Set<String>> fileServiceUserMap;

    private final Map<Long, Set<String>> rdbComputeDomainUserMap;
    private final Map<Long, Set<String>> databaseContextUserMap;

    private final Map<String, Boolean> userDbContextPermissionMap;
    private final Map<Long, Map<String, String>> jobTableDependencyMap; 

    /**
     * Constructor for CacheService. Initializes several cache data structures.
     */
    public CacheService() {
        dbDomains = new ArrayList<DbDomain>();

        fileServiceIdMap = new HashMap<>();
        fileServiceUserMap = new HashMap<>();

        rdbComputeDomainUserMap = new HashMap<>();
        databaseContextUserMap = new HashMap<>();

        userDbContextPermissionMap = new HashMap<>();
        jobTableDependencyMap = new HashMap<>();
    }

    /**
     * Gets the list of database domains.
     *
     * @return List of database domains.
     */
    public List<DbDomain> getDbDomains() {
        // Can't be initialized in constructor
        if (dbDomains.size() == 0) {
            dbDomainRepository.findAll().forEach(dbDomains::add);
            // Attempt to force a fetch of the DbContexts, which are always used when this method is invoked
            for (DbDomain dbDomain : dbDomains) {
                dbDomain.getDbContexts().size();
            }
        }
        return dbDomains;
    }

    public List<DbDomain> refreshDbDomains() {
        dbDomains.clear();
        return getDbDomains();
    }

    public Map<String, Set<String>> getFileServiceUserMap() {
        return fileServiceUserMap;
    }

    private void invalidateFileServiceUserCache(String userId) {
        for (Set<String> cacheSet : fileServiceUserMap.values()) {
            cacheSet.remove(userId);
        }
    }

    private void syncFileServiceUserCache(String userId, String userToken) throws SciServerClientException {
        // Clear any existing cache entries for this user
        invalidateFileServiceUserCache(userId);

        // Query RACM for the user's accessible file services
        List<MinimalFileServiceModel> fileServiceList = racmClient.getFileServices(userToken);

        // Repopulate cache with the user's file services
        String minFileSvcModelUuid;
        for (MinimalFileServiceModel minFileServiceModel : fileServiceList) {
            minFileSvcModelUuid = minFileServiceModel.getIdentifier();

            // If this file service has previously been cached, add user to set
            if (fileServiceIdMap.containsKey(minFileSvcModelUuid)) {
                fileServiceUserMap.get(minFileSvcModelUuid).add(userId);
            // Else it's a new file service, so add it to cache with an entry for this user
            } else {
                fileServiceIdMap.put(minFileSvcModelUuid, minFileServiceModel);
                fileServiceUserMap.put(minFileSvcModelUuid, new HashSet<String>(Stream.of(userId)
                                                                                      .collect(Collectors.toList())));
            }
        }
    }

    /**
     * Gets the cached file service for a user, querying RACM if necessary.
     *
     * @param userId The ID of the user.
     * @param fileServiceUuid The UUID of the file service.
     * @param userToken The user's token for RACM authentication.
     * @return The cached file service model, or null if the user does not have access.
     * @throws SciServerClientException If there is an error querying RACM.
     */
    public MinimalFileServiceModel getFileServiceUserCache(String userId, String fileServiceUuid, String userToken)
            throws SciServerClientException {

        MinimalFileServiceModel fileService = fileServiceIdMap.get(fileServiceUuid);
        // If the file service has not been previously encountered, or the user does not have access, then check w/RACM
        if (fileService == null || ! fileServiceUserMap.get(fileServiceUuid).contains(userId)) {
            syncFileServiceUserCache(userId, userToken);
        }

        // Check again to see if file service has been added
        fileService = fileServiceIdMap.get(fileServiceUuid);

        // If the file service is in the cache and the user is now in the set for the file service, 
        // return the file service, else null
        return (fileService != null && fileServiceUserMap.get(fileServiceUuid).contains(userId)) ? fileService : null;
    }

    public Map<Long, Set<String>> getRdbComputeDomainUserMap() {
        return rdbComputeDomainUserMap;
    }

    public Map<Long, Set<String>> getDatabaseContextUserMap() {
        return databaseContextUserMap;
    }

    private void invalidateRdbComputeDomainUserCache(String userId) {
        for (Set<String> cacheSet : rdbComputeDomainUserMap.values()) {
            cacheSet.remove(userId);
        }
        for (Set<String> cacheSet : databaseContextUserMap.values()) {
            cacheSet.remove(userId);
        }
    }

    /**
     * Syncs with RACM the cache of RDB compute domains and database contexts that a user has access to.
     *
     * @param userId The ID of the user.
     * @param userToken The user's token for RACM authentication.
     * @throws SciServerClientException If there is an error querying RACM.
     */
    public void syncRdbComputeDomainUserCache(String userId, String userToken) throws SciServerClientException {
        // Clear any existing cache entries for this user
        invalidateRdbComputeDomainUserCache(userId);

        // Query RACM for the user's accessible RDB compute domains
        List<RDBComputeDomainModel> rdbList = racmClient.queryRDBComputeDomainsCall(userToken);

        // Repopulate cache with the user's RDB compute domains
        Long rdbCompDomainModelId;
        Long databaseContextModelId;
        for (RDBComputeDomainModel rdbComputeDomainModel : rdbList) {
            rdbCompDomainModelId = rdbComputeDomainModel.getId();

            // If this RDB compute domain has previously been cached, add user to set
            if (rdbComputeDomainUserMap.containsKey(rdbCompDomainModelId)) {
                rdbComputeDomainUserMap.get(rdbCompDomainModelId).add(userId);
            // Else it's a new RDB compute domain, so add it to cache with an entry for this user
            } else {
                rdbComputeDomainUserMap.put(rdbCompDomainModelId, 
                                            new HashSet<String>(Stream.of(userId).collect(Collectors.toList())));
            }

            for (DatabaseContextModel databaseContextModel : rdbComputeDomainModel.getDatabaseContexts()) {
                databaseContextModelId = databaseContextModel.getId();
    
                // If this RDB compute domain has previously been cached, add user to set
                if (databaseContextUserMap.containsKey(databaseContextModelId)) {
                    databaseContextUserMap.get(databaseContextModelId).add(userId);
                // Else it's a new RDB compute domain, so add it to cache with an entry for this user
                } else {
                    databaseContextUserMap.put(databaseContextModelId, 
                                               new HashSet<String>(Stream.of(userId).collect(Collectors.toList())));
                }
            }
        }
    }

    /**
     * Gets directly from RACM the RDB compute domain that a user has access to, given the domain's ID.
     *
     * @param domainId  The ID of the RDB compute domain.
     * @param userToken The user's token for RACM authentication.
     * @return The RDB compute domain model.
     * @throws SciServerClientException If there is an error querying RACM.
     */
    public RDBComputeDomainModel getRdbComputeDomainUserDirect(Long domainId, String userToken)
            throws SciServerClientException {
        // Query RACM for the user's accessible RDB compute domains
        List<RDBComputeDomainModel> rdbComputeDomainList = racmClient.queryRDBComputeDomainsCall(userToken);

        RDBComputeDomainModel rdbComputeDomainModel = rdbComputeDomainList.stream().filter(rdbComputeDomain ->
                rdbComputeDomain.getId().equals(domainId)).findAny().orElse(null);

        return rdbComputeDomainModel;
    }

    /**
     * Gets the cached RDB compute domain for a user, querying RACM if it is not found in the cache.
     *
     * @param userId    The ID of the user.
     * @param domainId  The ID of the RDB compute domain.
     * @param userToken The user's token for RACM authentication.
     * @return True if the user has access to the RDB compute domain, false otherwise.
     * @throws SciServerClientException If there is an error querying RACM.
     */
    public Boolean getRdbComputeDomainUserCache(String userId, Long domainId, String userToken)
            throws SciServerClientException {
        // If RDB compute domain has not been previously encountered, 
        // or the user does not have access, then check w/RACM
        if (rdbComputeDomainUserMap.get(domainId) == null || ! rdbComputeDomainUserMap.get(domainId).contains(userId)) {
            syncRdbComputeDomainUserCache(userId, userToken);
        }

        return (rdbComputeDomainUserMap.get(domainId) != null 
                && rdbComputeDomainUserMap.get(domainId).contains(userId));
    }

    public Map<Long, Map<String, String>> getJobTableDependencyMap() {
        return jobTableDependencyMap;
    }

    /**
     * Refers to the cache to determine if the user has permission to perform 
     * the requested action on the specified database context.  If there is no matching element in the cache, 
     * or if the user is not permitted, query RACM to obtain an update or an answer.
     *
     * @param userId ID of the user.
     * @param dbDomain The database domain.
     * @param dbContextName The name of the database context.
     * @param action The action to be performed.
     * @param userToken The user's token for RACM authentication.
     * @return True if the user has permission, false otherwise.
     * @throws SciServerClientException If there is an error querying RACM.
     */
    public Boolean canUserDoResourceActionCache(String userId, DbDomain dbDomain, String dbContextName,
            String action, String userToken) throws SciServerClientException {
        List<DbContext> dbContexts = dbContextRepository.findByDbDomainAndContextNameIgnoreCase(dbDomain, 
                                                                                                dbContextName);
        if (dbContexts == null || dbContexts.isEmpty()) {
            return false;
        }
        DbContext dbContext = dbContexts.get(0); 
        if (dbContext == null) {
            return false;
        }

        return canUserDoResourceActionCache(userId, dbContext.getRacmUuid(), action, userToken);
    }

    private Boolean canUserDoResourceActionCache(String userId, String dbContextUuid, String action, String userToken)
            throws SciServerClientException {
        String cacheKey = userId + "|" + dbContextUuid + "|" + action;

        Boolean canDoAction = userDbContextPermissionMap.get(cacheKey);
        if (canDoAction == null || ! canDoAction) {
            canDoAction = racmClient.canUserDoResourceAction(dbContextUuid, action, userToken);
            userDbContextPermissionMap.put(cacheKey, canDoAction);
        }

        return canDoAction;
    }

    /**
     * Invalidates all cache entries for a user. 
     *
     * @param userId ID of the user.
     */
    public void invalidateUserCache(String userId) {
        invalidateFileServiceUserCache(userId);

        invalidateRdbComputeDomainUserCache(userId);

        for (String cacheKey : userDbContextPermissionMap.keySet()) {
            if (userId.equals(cacheKey.split("|")[0])) {
                userDbContextPermissionMap.remove(cacheKey);
            }
        }
    }
}
