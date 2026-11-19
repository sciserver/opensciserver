package org.sciserver.springapp.sciquery.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.authentication.client.AuthenticationClient;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.authentication.client.User;
import org.sciserver.casjobs.client.CasJobsClient;
import org.sciserver.clientutils.Client;
import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.racm.storem.model.MinimalFileServiceModel;
import org.sciserver.racm.ugm.model.GroupInfo;
import org.sciserver.racm.ugm.model.MemberUserModel;
import org.sciserver.sciquery.metadata.CasJobsPrivileges;
import org.sciserver.sciquery.metadata.MetadataManager;
import org.sciserver.springapp.auth.Auth;
import org.sciserver.springapp.sciquery.Config;
import org.sciserver.springapp.sciquery.SciQueryApiException;
import org.sciserver.springapp.sciquery.controller.UploadedFile.WriteModeEnum;
import org.sciserver.springapp.sciquery.model.DatabaseVendor;
import org.sciserver.springapp.sciquery.model.DbContext;
import org.sciserver.springapp.sciquery.model.DbContextRepository;
import org.sciserver.springapp.sciquery.model.DbDomain;
import org.sciserver.springapp.sciquery.model.DbServer;
import org.sciserver.springapp.sciquery.model.Metadata;
import org.sciserver.springapp.sciquery.model.Metadata.MetadataType;
import org.sciserver.springapp.sciquery.model.Output;
import org.sciserver.springapp.sciquery.model.Output.OutputFormat;
import org.sciserver.springapp.sciquery.service.CacheService;
import org.sciserver.springapp.sciquery.service.RdbComputeService;
import org.sciserver.tableingestor.stil.SQLServerJDBCFormatter;
import org.sciserver.tableingestor.stil.WriteMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;


/**
 * Supports the interaction and operation of Relational Database Management Systems (RDBMS) 
 * within SciServer.
 */
@CrossOrigin
@RestController
@Validated
@RequestMapping("api")
@Tag(name = "SciServer SciQuery REST API ", 
     description = "Supports the interaction and operation of "
                 + "Relational Database Management Systems (RDBMS) within SciServer.")
public class APIController {
    private static final Logger LOG = LogManager.getLogger();

    private @Value("#{new Long('${RACM.userGroup.admin}')}") Long racmUserGroupAdmin;

    private final Config config;
    private final ResponseEntity<String> rdbComputeDomainForbiddenResponse;
    private final ResponseEntity<String> sciQueryUnauthorizedResponse;

    @Autowired
    AuthenticationClient authenticationClient;
    @Autowired
    CasJobsClient casJobsClient;
    @Autowired
    RACMClient racmClient;
    // @Autowired
    // SciQueryRACMClient sciqueryRacmClient;

    @Autowired
    DbContextRepository dbContextRepository;

    @Autowired
    RdbComputeService rdbComputeService;
    @Autowired
    CacheService cacheService;

    private MetadataManager metadataManager;


    @Autowired
    APIController(Config config, MetadataManager metadataManager) {
        this.config = config;
        this.rdbComputeDomainForbiddenResponse = new ResponseEntity<String>("No matching RDB Compute Domain!",
                                                                            HttpStatus.BAD_REQUEST);
        this.sciQueryUnauthorizedResponse = new ResponseEntity<String>("Not authorized to perform this operation!",
                                                                       HttpStatus.UNAUTHORIZED);

        this.metadataManager = metadataManager;
    }

    private String createLogStr(AuthenticatedUser user, String method, String message) {
        return method + ":  " + message + "; User:  id=" + user.getUserId() + ", name=" + user.getUserName();
    }

    private Boolean isUserSciserverAdmin(String userToken) throws SciServerClientException {
        GroupInfo groupInfo = null;
        try {
            groupInfo = racmClient.queryMyGroup(racmUserGroupAdmin, userToken, null);
        } catch (SciServerClientException ssce) {
            groupInfo = null;
        }

        if (groupInfo == null || groupInfo.getMemberUsers().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Verifies that this API is up and running.
     *
     * @return a message with HTTP 200 status
     */
    @Operation(summary = "Ping this API", description = "Verifies that this API is up and running.")
    @ApiResponses({ @ApiResponse(responseCode = "200") })
    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public ResponseEntity<String> getPing() {
        return new ResponseEntity<>("Service is alive", HttpStatus.OK);
    }

    /**
     * Get health report of this API.
     *
     * @return a health report with HTTP 200 status
     */
    @Operation(summary = "Get health report of this API", 
               description = "TODO: Verifies that registered databases are up and running.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Databases are healthy"),
        @ApiResponse(responseCode = "500", description = "Health problem with registered databases")
    })
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public ResponseEntity<String> getHealth() {
        return new ResponseEntity<>("Health is OK", HttpStatus.OK);
    }

    /**
     * Returns metadata associated to the RDB domains a user is able to access.
     *
     * @param userToken the user's authentication token.
     * @return a ResponseEntity containing the JSON representation of the user's RDB domains with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with RACM.
     */
    @Operation(
        summary = "Gets RDB domains for user", 
        description = "Returns metadata associated to the RDB domains a user is able to access.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "500", description = "Internal Error when retrieving the RDB domain metadata"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/info/domain", method = RequestMethod.GET)
    public ResponseEntity<?> getUserRdbDomains(
            @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) String userToken)
            throws UnauthenticatedException, SciServerClientException {
        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "getUserRdbDomains", null);

        // Get the user's potential rdbComputeDomains
        cacheService.syncRdbComputeDomainUserCache(user.getUserId(), userToken);

        StringJoiner dbDomainJsonJoiner = rdbComputeService.getUserRdbDomains(user.getUserId());

        LOG.debug(logStr);
        if (dbDomainJsonJoiner == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<String>(dbDomainJsonJoiner.toString(), HttpStatus.OK);
    }

    /**
     * Submits a SQL query job to an RDB Compute domain.
     *
     * @param domainId the ID of the RDB Compute Domain.
     * @param rdbJobModel the RDB job model to submit.
     * @param userToken the user's authentication token.
     * @return a ResponseEntity containing the submitted job ID with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid.
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Submits an RDB query job", description = "Submits a SQL query job to an RDB Compute domain.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job was submited successfully"),
        @ApiResponse(responseCode = "500", description = "Internal Error when submitting job"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "400", description = "User does not have access to File Service specified "
                                                       + "in request body, or no access to RDB Compute Domain.")
    })
    @RequestMapping(value = "/jobs/{domainId}", method = RequestMethod.POST)
    public ResponseEntity<?> submitRdbJob(
            @Parameter(description = "Unique ID of the RDB Compute Domain.", required = true)
            @PathVariable Long domainId,
            @RequestBody(required = true, description = "Description of the RDB Job.")
            @org.springframework.web.bind.annotation.RequestBody RDBJobModel rdbJobModel,
            @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true)
            String userToken) throws UnauthenticatedException, SciServerClientException {
        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        // Get the user's potential file services, see if the identifier from the
        // resultsFolderURI is in list;
        // if not, return 400
        // Assumes the format for resultsFolderURI is
        // <file_service_uuid>:<path_to_results_folder>
        MinimalFileServiceModel fileService = cacheService.getFileServiceUserCache(user.getUserId(),
                rdbJobModel.getResultsFolderURI().split(":")[0], userToken);
        if (fileService == null) {
            return new ResponseEntity<String>("No matching File Service!", HttpStatus.BAD_REQUEST);
        }

        // Get the user's potential rdbComputeDomains, see if domainId is in list; if
        // not, return 400
        Boolean isAllowedDomain = cacheService.getRdbComputeDomainUserCache(user.getUserId(), domainId, userToken);
        if (!isAllowedDomain) {
            return rdbComputeDomainForbiddenResponse;
        }

        RDBJobModel rdbJobModelResponse = null;
        try {
            rdbJobModelResponse = rdbComputeService.submitRdbJob(domainId, rdbJobModel.getDatabaseContextName(),
                    rdbJobModel, user.getUserId(), userToken);
        } catch (SciServerClientException ssce) {
            throw ssce;
        } catch (SciQueryApiException sqae) {
            return new ResponseEntity<String>(sqae.getMessage(), HttpStatus.BAD_REQUEST);
        }
        String logStr = createLogStr(user, "submitRdbJob", "domainId=" + domainId);
        LOG.debug(logStr);
        return new ResponseEntity<Long>(rdbJobModelResponse.getId(), HttpStatus.OK);
    }

    /**
     * Registers a new RDB Compute Domain in SciQuery.
     *
     * @param dbDomain description of the RDB domain.
     * @param apiEndpoint the API endpoint for the RDB compute domain.
     * @param vendor the database vendor.
     * @param admins Name(s) of user group(s) composed by admins of this domain (comma-separated)
     * @param userToken the user's authentication token.
     * @return a ResponseEntity containing the newly created RDBDomain with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid.
     * @throws SciServerClientException if an error occurs communicating with SciServer services.
     */
    @Operation(
        summary = "Register an RDB Compute Domain", 
        description = "Registers a new RDB Compute Domain in SciQuery.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", description = "User's auth token.", 
                       required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "400", description = "RDB Compute Domain already exists "
                                                       + "or Invalid Database Server.")
    })
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<?> registerDbDomain(
            @RequestBody(required = true, description = "Description of the RDB domain.")
            @org.springframework.web.bind.annotation.RequestBody DbDomain dbDomain,
            @Parameter(description = "API endpoint for the RDB compute domain.", required = true)
            @RequestParam(required = true) String apiEndpoint, 
            @Parameter(description = "Database vendor.", required = true)
            @RequestParam(required = true) String vendor,
            @Parameter(description = "Name(s) of user group(s) composed by admins of this domain (comma-separated)", 
                       required = false) 
            @RequestParam(required = false) String admins,
            @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
            String userToken) throws UnauthenticatedException, SciServerClientException {
        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "registerDbDomain", "displayName=" + dbDomain.getDisplayName());

        DbDomain newDbDomain = rdbComputeService.registerDbDomain(dbDomain, apiEndpoint, vendor, admins, userToken,
                user.getUserId());
        if (newDbDomain == null) {
            return new ResponseEntity<String>("Existing RDB Compute Domain or Invalid Database Server!",
                    HttpStatus.BAD_REQUEST);
        }
        logStr += "; DbDomain:  " + newDbDomain.getId();

        LOG.debug(logStr);
        return new ResponseEntity<DbDomain>(newDbDomain, HttpStatus.OK);
    }

    /**
     * Registers a new Database Context in an RDB Compute Domain.
     *
     * @param domainId the unique ID of the RDB Compute Domain
     * @param dbContext description of the db context
     * @param admins Name(s) of user group(s) composed by admins of this database (comma-separated).
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing the newly created DbContext with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Register Database Context", 
        description = "Registers a new Database Context in an RDB Compute Domain.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Database Context registered successfully."),
        @ApiResponse(responseCode = "500", description = "Internal Error when registereing Database Context."),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "400", description = "Database Context already exists, "
                                                       + "or no access to RDB Compute Domain.")
    })
    @RequestMapping(value = "/register/{domainId}", method = RequestMethod.POST)
    public ResponseEntity<?> registerDbContext(
            @Parameter(description = "Unique ID of the RDB Compute Domain.", required = true)
            @PathVariable Long domainId,
            @RequestBody(required = true, description = "Description of the db context.")
            @org.springframework.web.bind.annotation.RequestBody DbContext dbContext,
            @Parameter(description = "Name(s) of user group(s) composed by admins of this database (comma-separated)", 
                       required = false) 
            @RequestParam(required = false) String admins,
            @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
            String userToken) throws UnauthenticatedException, SciServerClientException {
        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "registerDbContext", "domainId=" + domainId);

        DbContext newDbContext = rdbComputeService.registerDbContext(domainId, dbContext, admins, userToken,
                user.getUserId());
        if (newDbContext == null) {
            return new ResponseEntity<String>("Existing Database Context or Invalid Database Server!",
                    HttpStatus.BAD_REQUEST);
        }
        logStr += "; DbContext:  " + newDbContext.getId();

        LOG.debug(logStr);
        return new ResponseEntity<DbContext>(newDbContext, HttpStatus.OK);
    }

    /**
     * Registers a CasJobs MyDB database context in SciQuery.
     *
     * @param username the username whose MyDB should be registered
     * @param admins Name(s) of user group(s) composed by admins of this database (comma-separated).
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing the newly created Db Context with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Register MyDB for user", 
        description = "Registers a CasJobs MyDB database context in SciQuery.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MyDB Database Context registered successfully."),
        @ApiResponse(responseCode = "500", description = "Internal Error when registereing Database Context."),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "400", description = "User has no MyDB , user does not exist, "
                                                       + "MyDB was already registered, "
                                                       + "or no access to RDB Compute Domain.")
    })
    @RequestMapping(value = "/register/mydb/user/{username}", method = RequestMethod.POST)
    public ResponseEntity<?> registerMydbContextByUsername(
        @Parameter(description = "Username whose MyDB should be registered.", required = true)
        @PathVariable String username,
        @Parameter(description = "Name of group(s) of admins of this database context (might be comma-separated)", 
                    required = false) 
        @RequestParam(required = false) String admins,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        User authUser;
        String mydbName;
        try {
            authUser = authenticationClient.getUserByName(username);
            mydbName = casJobsClient.getMyDB(authUser.getUserId(), userToken);
        } catch (SciServerClientException ssce) {
            return new ResponseEntity<String>("User " + username + " has no mydb!",
                    HttpStatus.BAD_REQUEST);
        } catch (IndexOutOfBoundsException ioobe) {
            return new ResponseEntity<String>("User " + username + " has no authUser!",
                    HttpStatus.BAD_REQUEST);
        }

        DbContext mydbContext = rdbComputeService.createMyDbContext(authUser, mydbName, null);

        // Get the registering user's potential rdbComputeDomains, see if domainId is in
        // list; if not, return 400
        Boolean isAllowedDomain = cacheService.getRdbComputeDomainUserCache(user.getUserId(),
                mydbContext.getDbDomain().getRacmId(), userToken);
        if (!isAllowedDomain) {
            return rdbComputeDomainForbiddenResponse;
        }

        DbContext newDbContext = rdbComputeService.registerDbContext(mydbContext.getDbDomain().getRacmId(),
                mydbContext, admins, userToken, null);
        if (newDbContext == null) {
            return new ResponseEntity<String>("Existing Database Context!", HttpStatus.BAD_REQUEST);
        }
        String logStr = createLogStr(user, "registerMydbContextByUsername", "username=" + username);
        logStr += "; DbContext:  " + newDbContext.getId();

        LOG.debug(logStr);
        return new ResponseEntity<DbContext>(newDbContext, HttpStatus.OK);
    }

    /**
     * Registers CasJobs MyDB database contexts for a group of users in SciQuery.
     *
     * @param groupId the ID of the group whose users' MyDBs should be registered.
     * @param admins Name(s) of user group(s) composed by admins of this database (comma-separated).
     * @param userToken the user's authentication token
     * @return a ResponseEntity with HTTP 200 status on success.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Register MyDB for group of users", 
        description = "Registers CasJobs MyDB database contexts for a group of users in SciQuery.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MyDB Database Contexts registered successfully."),
        @ApiResponse(responseCode = "500", description = "Internal Error when registereing Database Contexts."),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "400", description = "User has no MyDB , user does not exist, "
                                                       + "MyDB was already registered, "
                                                       + "or no access to RDB Compute Domain.")
    })
    @RequestMapping(value = "/register/mydb/group/{groupId}", method = RequestMethod.POST)
    public ResponseEntity<?> registerMydbContextByGroupId(
        @Parameter(description = "ID of the group whose users' MyDBs should be registered.", required = true)
        @PathVariable Long groupId,
        @Parameter(description = "Name of group(s) of admins of these database contexts (might be comma-separated)", 
                    required = false)
        @RequestParam(required = false) String admins,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "registerMydbContextByGroupId", "groupId=" + groupId);
        int totalMembers = 0;
        int mydbMembers = 0;

        GroupInfo groupInfo = null;
        try {
            groupInfo = racmClient.queryMyGroup(groupId, userToken, null);
        } catch (SciServerClientException ssce) {
            LOG.debug("racmClient.queryMyGroup()", ssce);
            groupInfo = null;
        }
        if (groupInfo == null || groupInfo.getMemberUsers().isEmpty()) {
            return new ResponseEntity<String>("Invalid or empty Group!", HttpStatus.BAD_REQUEST);
        }

        User authUser;
        String mydbName;
        DbContext mydbContext;
        DbContext newDbContext;
        boolean isAllowedDomain = false;
        for (MemberUserModel memberUser : groupInfo.getMemberUsers()) {
            totalMembers++;
            try {
                authUser = authenticationClient.getUserByName(memberUser.getUsername());
                mydbName = casJobsClient.getMyDB(authUser.getUserId(), userToken);
                mydbContext = rdbComputeService.createMyDbContext(authUser, mydbName, null);

                // Get the registering user's potential rdbComputeDomains, see if domainId is in
                // list; if not, return 400. Only needs to be done once, 
                // as all mydb's will be under the same domain (for now).
                if (!isAllowedDomain) {
                    isAllowedDomain = cacheService.getRdbComputeDomainUserCache(user.getUserId(),
                            mydbContext.getDbDomain().getRacmId(), userToken);
                    if (!isAllowedDomain) {
                        return rdbComputeDomainForbiddenResponse;
                    }
                }

                newDbContext = rdbComputeService.registerDbContext(mydbContext.getDbDomain().getRacmId(), mydbContext,
                        admins, userToken, null);
                // If newDbContext == null, indicates that the mydb already exists, continue
                // with other group members
                if (newDbContext == null) {
                    logStr += "\n  " + memberUser.getUsername() + " - mydb EXISTS!";
                    continue;
                }
            } catch (SciServerClientException ssce) {
                LOG.debug(groupInfo.getGroupName() + " user " + memberUser.getUsername() + " has no mydb!");
            } catch (IndexOutOfBoundsException ioobe) {
                LOG.debug(groupInfo.getGroupName() + " user " + memberUser.getUsername() + " has no authUser!");
            }
            mydbMembers++;
        }

        LOG.debug(logStr + "\n" + mydbMembers + "/" + totalMembers + " added.");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Registers a new Database Server to SciQuery. Must be called by an admin user.
     *
     * @param dbServer description of the db server
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing the newly created DbServer with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Register DB Server", 
        description = "Registers a Database Server to SciQuery. Must be called by an admin user.",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Database Server registered successfully."),
        @ApiResponse(responseCode = "500", description = "Internal Error when registereing Database Server."),
        @ApiResponse(responseCode = "401", description = "User's auth token is invalid/user not authorized."),
        @ApiResponse(responseCode = "400", description = "DB Server already exists.")
    })
    @RequestMapping(value = "/register/server", method = RequestMethod.POST)
    public ResponseEntity<?> registerDbServer(
        @RequestBody(required = true, description = "Description of the db server.")
        @org.springframework.web.bind.annotation.RequestBody DbServer dbServer,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "registerDbServer", null);

        // Restrict to admin users
        if (!isUserSciserverAdmin(userToken)) {
            return sciQueryUnauthorizedResponse;
        }

        DbServer newDbServer = rdbComputeService.registerDbServer(dbServer);
        if (newDbServer == null) {
            return new ResponseEntity<String>("Existing Database Server!", HttpStatus.BAD_REQUEST);
        }
        logStr += "; DbServer:  " + newDbServer.getId();

        LOG.debug(logStr);
        return new ResponseEntity<DbServer>(newDbServer, HttpStatus.OK);
    }

    /**
     * Gets a list of RDB jobs in an RDB Compute Domain for the authenticated user.
     *
     * @param domainId unique ID of the RDB Compute Domain
     * @param userToken user's authentication token
     * @return a ResponseEntity containing a list of RDBJobModel objects with HTTP 200 status,
     *         HTTP 400 if no domain access, or HTTP 401 if unauthorized
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Get Jobs list", 
        description = "Gets list of RDB jobs in an RDB Compute Domain for a particular user.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of RDB Jobs returned successfully."),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "400", description = "No access to RDB Compute Domain.")
    })
    @RequestMapping(value = "/jobs/{domainId}", method = RequestMethod.GET)
    public ResponseEntity<?> getUserRdbJobs(
        @Parameter(description = "Unique ID of the RDB Compute Domain.", required = true) 
        @PathVariable Long domainId,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "getUserRdbJobs", "domainId=" + domainId);

        // Get the user's potential rdbComputeDomains, see if domainId is in list; if
        // not, return 400
        Boolean isAllowedDomain = cacheService.getRdbComputeDomainUserCache(user.getUserId(), domainId, userToken);
        if (!isAllowedDomain) {
            return rdbComputeDomainForbiddenResponse;
        }

        List<RDBJobModel> rdbJobModelList = racmClient.queryUserRdbJobs(userToken);
        List<RDBJobModel> rdbJobModelListByDomain = rdbJobModelList.stream()
                .filter(rdbJobModel -> rdbJobModel.getRdbDomainId().equals(domainId))
                .collect(java.util.stream.Collectors.toList());

        LOG.debug(logStr);
        return new ResponseEntity<List<RDBJobModel>>(rdbJobModelListByDomain, HttpStatus.OK);
    }

    /**
     * Gets a particular RDB job in an RDB Compute Domain for a particular user.
     *
     * @param domainId the unique ID of the RDB Compute Domain
     * @param jobId the unique ID of the job
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing the RDBJobModel with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Get Job", 
        description = "Gets a particular RDB job in an RDB Compute Domain for a particular user.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "RDB Job returned successfully."),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid."),
        @ApiResponse(responseCode = "400", description = "No access to RDB Compute Domain.")
    })
    @RequestMapping(value = "/jobs/{domainId}/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<?> getUserRdbJob(
        @Parameter(description = "Unique ID of the RDB Compute Domain.", required = true) 
        @PathVariable Long domainId,
        @Parameter(description = "Unique ID of the Job.", required = true) 
        @PathVariable Long jobId,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "getUserRdbJob", "domainId=" + domainId);

        // Get the user's potential rdbComputeDomains, see if domainId is in list; if
        // not, return 400
        Boolean isAllowedDomain = cacheService.getRdbComputeDomainUserCache(user.getUserId(), domainId, userToken);
        if (!isAllowedDomain) {
            return rdbComputeDomainForbiddenResponse;
        }

        RDBJobModel rdbJobModel = racmClient.rdbJobStatus(userToken, jobId);

        LOG.debug(logStr);
        return new ResponseEntity<RDBJobModel>(rdbJobModel, HttpStatus.OK);
    }

    /**
     * Gets the database context map for a particular RDB job.
     *
     * @param domainId the unique ID of the RDB Compute Domain.
     * @param jobId the unique ID of the job.
     * @param userToken the user's authentication token.
     * @return a ResponseEntity containing a JSON representation of the dbContextMap with HTTP 200 status.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid.
     * @throws SciServerClientException if an error occurs communicating with SciServer services.
     */
    // MTP comment: not working, not sure yet what was its purpose or use case; might remove it.
    @RequestMapping(
        value = "/jobs/{domainId}/{jobId}/context", 
        method = RequestMethod.GET)
    public ResponseEntity<?> getUserRdbJobContextMap(
        @Parameter(description = "Unique ID of the RDB Compute Domain.", required = true)
        @PathVariable Long domainId, 
        @Parameter(description = "Unique ID of the Job.", required = true)
        @PathVariable Long jobId,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "getUserRdbJobContextMap", "domainId=" + domainId);

        // Get the user's potential rdbComputeDomains, see if domainId is in list; if
        // not, return 400
        Boolean isAllowedDomain = cacheService.getRdbComputeDomainUserCache(user.getUserId(), domainId, userToken);
        if (!isAllowedDomain) {
            return rdbComputeDomainForbiddenResponse;
        }

        Map<String, String> dbContextMap = cacheService.getJobTableDependencyMap().get(jobId);

        LOG.debug(logStr);
        return new ResponseEntity<Map<String, String>>(dbContextMap, HttpStatus.OK);
    }

    /**
     * Refreshes the RDB Domains metadata from the SciQuery API's local cache.
     *
     * @param userToken the user's authentication token
     * @return a ResponseEntity with HTTP 200 status on success,
     *         HTTP 401 if not authorized or not admin, or HTTP 400 if error occurs
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Refresh RDB Compute Domains.",
        description = "Refreshes the RDB Domains metadata from the SciQuery API's local cache.",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache refreshed successfully."),
        @ApiResponse(responseCode = "401", 
                     description = "User's auth token is missing or invalid or user not an admin."),
        @ApiResponse(responseCode = "400", description = "Error when refreshing maetadata.")
    })
    @RequestMapping(value = "/refreshDomains/sciquery", method = RequestMethod.GET)
    public ResponseEntity<?> refreshRdbComputeDomainsFromSciQuery(
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "refreshRdbComputeDomainsFromSciQuery", null);

        // Restrict to admin users
        if (!isUserSciserverAdmin(userToken)) {
            return sciQueryUnauthorizedResponse;
        }

        cacheService.refreshDbDomains();

        LOG.debug(logStr);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Refreshes the RDB Compute Domains metadata from RACM.
     * Only callable by admin users.
     *
     * @param userToken Authentication token of an admin.
     * @return a ResponseEntity with HTTP 200 status on success.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Refresh RDB Compute Domains", 
        description = "Refreshes the RDB Compute Domains metadata from RACM. Only callable by admin users.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "Authentication token of an admin.", required = true, 
                       schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache refreshed successfully."),
        @ApiResponse(responseCode = "401", 
                     description = "User's auth token is missing or invalid or user not an admin."),
        @ApiResponse(responseCode = "400", description = "Error when refreshing maetadata.")
    })
    // TODO: This method will pull any new RDB compute domains and database contexts
    // from RACM, and update ones
    // already in SciQuery. For new domains, it does not yet populate the service
    // token, and for contexts, it
    // enters placeholders for the required host, dbName, vendor, readonlyUserName,
    // and readonlyUserPassword fields.
    @RequestMapping(value = "/refreshDomains/racm", method = RequestMethod.GET)
    public ResponseEntity<?> refreshRdbComputeDomainsFromRacm(
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "refreshRdbComputeDomainsFromRacm", null);

        // Restrict to admin users
        if (!isUserSciserverAdmin(userToken)) {
            return sciQueryUnauthorizedResponse;
        }

        rdbComputeService.refreshRdbComputeDomains(userToken, user.getUserId());

        LOG.debug(logStr);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Deletes the metadata cache stored in memory for a particular user.
     * Only callable by admin users.
     *
     * @param userId the user ID whose cache should be invalidated
     * @param userToken Authentication token of an admin
     * @return a ResponseEntity with HTTP 200 status on success.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Deletes cache for a user.", 
        description = "Deletes the metadata cache stored in memory for a particular user. "
                    + "Only callable by admin users.",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "Authentication token of an admin.", required = true, 
                       schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cache refreshed successfully."),
        @ApiResponse(responseCode = "401", 
                     description = "User's auth token is missing or invalid or user not an admin.")
    })
    @RequestMapping(value = "/cache/user/{userId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> invalidateUserCache(
        @Parameter(description = "User ID of the user whose cache should be invalidated.", required = true)
        @PathVariable String userId,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        // Verify user authentication and set up log message
        AuthenticatedUser user = Auth.get();
        String logStr = createLogStr(user, "invalidateUserCache", "userId=" + userId);

        // Restrict to admin users
        if (!isUserSciserverAdmin(userToken)) {
            return sciQueryUnauthorizedResponse;
        }

        cacheService.invalidateUserCache(userId);

        LOG.debug(logStr);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     * Uploads a CSV table in a file to a specified database table in a particular RDB compute domain.
     *
     * @param request the HTTP request
     * @param writeMode write mode enum for upload ("APPEND", "CREATE", "DROP_CREATE"); default is "CREATE"
     * @param domainId the unique identifier of the RDB compute domain
     * @param databaseName the name of the database in the RDB compute domain
     * @param dbTableName the name of the database table
     * @param userToken the user's authentication token
     * @return a ResponseEntity with HttpStatus OK.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     * @throws IOException if an error occurs when creating/deleting the temporary file
     * @throws SQLException if an error occurs when interacting with the database
     * @throws ClassNotFoundException if the database driver class is not found
     * @throws IllegalArgumentException if the writeMode parameter is invalid or 
     *     the uploaded file size is too big or has no data
     */
    @Operation(
        summary = "Uploads CSV table file to database table.", 
        description = "Uploads a CSV table file to a specified database table in a particular RDB compute domain.",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, 
                       schema = @Schema(type = "string"))
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        required = true,
                        description = "CSV file content to upload",
                        content = {@Content(mediaType = "text/csv", 
                                            schema = @Schema(type = "string", format = "binary"))}
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV file uploaded successfully."),
        @ApiResponse(responseCode = "500", description = "Internal Error when uploading data to database table"),
        @ApiResponse(responseCode = "400", description = "User is not allowed to upload data to the specified "
                                                        + "database table or domain, "
                                                       + "or those are not specified correctly."),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/data/upload/{domainId}/{databaseName}/{dbTableName}", method = RequestMethod.POST)
    public ResponseEntity<StreamingResponseBody> uploadDataToDatabaseTable(
        HttpServletRequest request,
        @Parameter(
            description = "Write mode for the CSV upload.",
            required = false,
            schema = @Schema(type = "string", defaultValue = "CREATE", 
                             allowableValues = {"APPEND", "CREATE", "DROP_CREATE"})
        )
        @RequestParam(value = "writeMode", required = false, defaultValue = "CREATE") WriteModeEnum writeMode,
        @Parameter(name = "domainId", description = "Unique identifier of the RDB compute domain.", required = true, 
                   schema = @Schema(type = "integer"))
        @PathVariable(required = true) @NotNull Long domainId,
        @Parameter(name = "databaseName", description = "Name of database in RDB compute domain.", required = true, 
                   schema = @Schema(type = "string"))
        @PathVariable(required = true) @NotBlank String databaseName,
        @Parameter(name = "dbTableName", description = "Name of database table.", required = true, 
                   schema = @Schema(type = "string"))
        @PathVariable(required = true) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_]*$", 
                                                message = "Invalid parameter dbTableName. "
                                                        + "Allowed characters: alphanumeric and underscore. "
                                                        + "Must start with an alphanumeric character.")
        @NotBlank(message = "dbTableName cannot be blank") String dbTableName,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) String userToken) 
        throws UnauthenticatedException, SciServerClientException, IOException, ClassNotFoundException, SQLException {


        AuthenticatedUser user = Auth.get();
        File uploadedFile = null;
        Connection dbConnection = null;

        try {

            // Get the user's potential rdbComputeDomains and check if domainId is in the list.
            RDBComputeDomainModel rdbComputeDomain = cacheService.getRdbComputeDomainUserDirect(domainId,
                                                                                                user.getToken());
            if (rdbComputeDomain == null) {
                return getErrorResponse(
                    "No matching RDB Compute Domain " + domainId, HttpStatus.BAD_REQUEST);
            }

            // Get the database context for the specified database name in the RDB compute domain.
            DatabaseContextModel dbContextModel = rdbComputeDomain.getDatabaseContexts()
                    .stream().filter(databaseContext -> databaseName.equalsIgnoreCase(databaseContext.getName()))
                    .findAny().orElse(null);
            if (dbContextModel == null) {
                return getErrorResponse(
                    "Inaccessible Database Context " + databaseName, HttpStatus.BAD_REQUEST);
            }

            // Get the DbDomain for the RDB compute domain, 
            // which contains the connection information required to upload the data.
            DbDomain domain = cacheService.getDbDomains().stream()
                    .filter(dbDomain -> rdbComputeDomain.getId().equals(dbDomain.getRacmId())).findAny().orElse(null);
            if (domain == null) {
                return getErrorResponse(
                    "No matching DbDomain for RDB Compute Domain " + domainId, HttpStatus.BAD_REQUEST);
            }

            DbContext dbContext = (dbContextRepository.findByDbDomainAndContextName(domain,
                    dbContextModel.getName())).get(0);
            if (dbContext == null) {
                return getErrorResponse(
                    "No matching dbContext for database name " + databaseName, HttpStatus.BAD_REQUEST);
            }

            String fileName = "upload_" + user.getUserId() + "_" + rdbComputeDomain.getId() + "_" 
                            + dbContext.getId() + "_" + UUID.randomUUID().toString().replace("-", "") 
                            + ".csv";
            String pathToFile = config.getFileUploadBasePath() + fileName;

            uploadedFile = new File(pathToFile);
            // check that the generated file path is within the configured upload directory.
            if (!uploadedFile.getCanonicalPath()
                             .startsWith(new File(config.getFileUploadBasePath()).getCanonicalPath())) {
                throw new IOException("Invalid upload file path: " + pathToFile);
            }

            WriteMode selectedWriteMode = UploadedFile.getDatabaseTableWriteMode(writeMode);
            // copy the input stream to a file location.
            try (InputStream inputFileStream = request.getInputStream()) {
                uploadedFile = UploadedFile.writeUploadedFile(inputFileStream, pathToFile,  
                                                         config.getMaxFileUploadSize());
            } catch (IllegalArgumentException ex) {
                return getErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
            }
            StarTable startable = new StarTableFactory(false).makeStarTable(uploadedFile.getPath(), "csv");
            dbConnection = getNewConnection(dbContext, true);
            new SQLServerJDBCFormatter(dbConnection, startable).createJDBCTable(dbTableName, selectedWriteMode);
        } catch (Exception ex) {
            LOG.error("Error uploading data to database table", ex);
            return getErrorResponse("Error uploading data to database table: " + ex.getMessage(), 
                                    HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (uploadedFile != null && uploadedFile.exists()) {
                    uploadedFile.delete();
                }
            } catch (Exception ex) {
                LOG.warn("Error deleting uploaded file. ", ex);
            }
            try {
                if (dbConnection != null) {
                    dbConnection.close();
                }
            } catch (Exception ex) {
                LOG.warn("Error closing database connection. ", ex);
            }
        }
        return new ResponseEntity<StreamingResponseBody>(HttpStatus.OK);
    }



    /**
     * Returns database table metadata in a particular domain's database.
     *
     * @param request the HTTP request
     * @param domainId the unique identifier of the RDB compute domain
     * @param databaseName the name of the database in the RDB compute domain
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing table metadata in JSON format.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Gets database tables", 
        description = "Returns tables metadata in a particular domain's database.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "500", description = "Internal Error when retrieving the table metadata"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/metadata/{domainId}/{databaseName}/tables", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> getTables(
        HttpServletRequest request,
        @Parameter(description = "Unique identifier of the RDB compute domain.", required = true) 
        @PathVariable Long domainId,
        @Parameter(description = "Name of database in RDB compute domain.", required = true) 
        @PathVariable String databaseName,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true)
        String userToken) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            return returnMetadata(user, domainId, databaseName, null, MetadataType.TABLES);
        } catch (Exception e) {
            return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves table views metadata in a particular domain's database.
     *
     * @param request the HTTP request
     * @param domainId unique identifier of the RDB compute domain
     * @param databaseName name of the database in the RDB compute domain
     * @return a ResponseEntity containing view metadata in JSON format.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Gets database views", 
        description = "Retrieves table views metadata in a particular domain's database.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "500", description = "Internal Error when retrieving the views metadata"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/metadata/{domainId}/{databaseName}/views", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> getViews(
        HttpServletRequest request,
        @Parameter(description = "Unique identifier of the RDB compute domain.", required = true) 
        @PathVariable Long domainId,
        @Parameter(description = "Name of database in RDB compute domain.", required = true) 
        @PathVariable String databaseName,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            return returnMetadata(user, domainId, databaseName, null, MetadataType.VIEWS);
        } catch (Exception e) {
            return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves database routine metadata in a particular domain's database.
     *
     * @param request the HTTP request
     * @param domainId the unique identifier of the RDB compute domain
     * @param databaseName the name of the database in the RDB compute domain
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing with routine metadata in JSON format.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Gets database routines", 
        description = "Retrieves database routines metadata in a particular domain's database.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "500", description = "Internal Error when retrieving the routines metadata"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/metadata/{domainId}/{databaseName}/routines", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> getRoutines(
        HttpServletRequest request,
        @Parameter(description = "Unique identifier of the RDB compute domain.", required = true) 
        @PathVariable Long domainId,
        @Parameter(description = "Name of database in RDB compute domain.", required = true) 
        @PathVariable String databaseName,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            return returnMetadata(user, domainId, databaseName, null, MetadataType.ROUTINES);
        } catch (Exception e) {
            return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves column metadata for a table in a particular domain's database.
     *
     * @param request the HTTP request
     * @param domainId the unique identifier of the RDB compute domain
     * @param databaseName the name of the database in the RDB compute domain
     * @param tableName the name of the database table
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing column metadata in JSON format.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Gets table columns", 
        description = "Retrieves columns metadata associated to a table in a particular domain's database.",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "500", description = "Internal Error when retrieving the columns metadata"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/metadata/{domainId}/{databaseName}/{tableName}/columns", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> getColumns(
        HttpServletRequest request,
        @Parameter(description = "Unique identifier of the RDB compute domain.", required = true) 
        @PathVariable Long domainId,
        @Parameter(description = "Name of database in RDB compute domain.", required = true) 
        @PathVariable String databaseName,
        @Parameter(description = "Name of database table.", required = true) 
        @PathVariable String tableName,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            return returnMetadata(user, domainId, databaseName, tableName, MetadataType.COLUMNS);
        } catch (Exception e) {
            return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves constraint metadata for a table in a particular domain's database.
     *
     * @param request the HTTP request
     * @param domainId the unique identifier of the RDB compute domain
     * @param databaseName the name of the database in the RDB compute domain
     * @param tableName the name of the database table
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing constraint metadata in JSON format.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Gets table constraints", 
        description = "Retrieves constraints metadata associated to a table in a particular domain's database.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "500", description = "Internal Error when retrieving the constraints metadata"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/metadata/{domainId}/{databaseName}/{tableName}/constraints", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> getConstraints(
        HttpServletRequest request,
        @Parameter(description = "Unique identifier of the RDB compute domain.", required = true) 
        @PathVariable Long domainId,
        @Parameter(description = "Name of database in RDB compute domain.", required = true) 
        @PathVariable String databaseName,
        @Parameter(description = "Name of database table.", required = true) 
        @PathVariable String tableName,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            return returnMetadata(user, domainId, databaseName, tableName, MetadataType.CONSTRAINTS);
        } catch (Exception e) {
            return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves routine parameter metadata in a particular domain's database.
     *
     * @param request the HTTP request
     * @param domainId the unique identifier of the RDB compute domain
     * @param databaseName the name of the database in the RDB compute domain
     * @param routineName the name of the routine
     * @param userToken the user's authentication token
     * @return a ResponseEntity containing routine parameter metadata in JSON format.
     * @throws UnauthenticatedException if the user's auth token is missing or invalid
     * @throws SciServerClientException if an error occurs communicating with SciServer services
     */
    @Operation(
        summary = "Gets routine parameters", 
        description = "Retrieves routine parameters metadata in a particular domain's database.", 
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Auth-Token", 
                       description = "User's auth token.", required = true, schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "500", description = "Internal Error when retrieving the parameters metadata"),
        @ApiResponse(responseCode = "401", description = "User's auth token is missing or invalid.")
    })
    @RequestMapping(value = "/metadata/{domainId}/{databaseName}/{routineName}/parameters", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> getParameters(
        HttpServletRequest request,
        @Parameter(description = "Unique identifier of the RDB compute domain.", required = true) 
        @PathVariable Long domainId,
        @Parameter(description = "Name of database in RDB compute domain.", required = true) 
        @PathVariable String databaseName,
        @Parameter(description = "Name of routine.", required = true) 
        @PathVariable(required = true) String routineName,
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) 
        String userToken) throws UnauthenticatedException, SciServerClientException {

        AuthenticatedUser user = Auth.get();
        try {
            return returnMetadata(user, domainId, databaseName, routineName, MetadataType.PARAMETERS);
        } catch (Exception e) {
            return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper method to retrieve metadata from the database and return it as a streaming response.
     *
     * @param user the authenticated user making the request
     * @param domainId the unique identifier of the RDB compute domain
     * @param databaseName the name of the database
     * @param resourceName the name of the resource (table, routine, etc.) for database-level metadata
     * @param type the type of metadata to retrieve (TABLES, COLUMNS, ROUTINES, etc.)
     * @return a ResponseEntity containing the metadata in JSON format.
     */
    private ResponseEntity<StreamingResponseBody> returnMetadata(AuthenticatedUser user, Long domainId,
                                                                 String databaseName, 
                                                                 String resourceName, MetadataType type) {
        PreparedStatement sqlStatement = null;
        Connection connection = null;

        try {
            RDBComputeDomainModel rdbComputeDomain = cacheService.getRdbComputeDomainUserDirect(domainId,
                                                                                                user.getToken());
            if (rdbComputeDomain == null) {
                return new ResponseEntity<StreamingResponseBody>(
                        new StreamingHttpErrorBody("No matching RDB Compute Domain " + domainId),
                                                    HttpStatus.BAD_REQUEST);
            }

            DatabaseContextModel dbContextModel = rdbComputeDomain.getDatabaseContexts()
                    .stream().filter(databaseContext -> databaseName.equalsIgnoreCase(databaseContext.getName()))
                    .findAny().orElse(null);
            if (dbContextModel == null) {
                return new ResponseEntity<StreamingResponseBody>(
                        new StreamingHttpErrorBody("Inaccessible Database Context " + databaseName),
                        HttpStatus.BAD_REQUEST);
            }

            DbDomain domain = cacheService.getDbDomains().stream()
                    .filter(dbDomain -> rdbComputeDomain.getId().equals(dbDomain.getRacmId())).findAny().orElse(null);

            DbContext dbContext = (dbContextRepository.findByDbDomainAndContextName(domain,
                    dbContextModel.getName())).get(0);

            connection = getNewConnection(dbContext, false);
            String query = Metadata.getQuery(type, connection);
            sqlStatement = getNewStatement(connection, query);
            if (resourceName != null && (type == MetadataType.COLUMNS || type == MetadataType.CONSTRAINTS
                    || type == MetadataType.PARAMETERS)) {
                sqlStatement.setString(1, resourceName);
                query = query.replace("?", "'" + resourceName + "'");
            }

            boolean isResultSet = sqlStatement.execute();
            StreamingHttpResponseBody responseBody = new StreamingHttpResponseBody(OutputFormat.JSON,
                    sqlStatement, query, isResultSet, config.getNumRowsPerFlush());
            HttpHeaders httpHeaders = createHttpResponseHeaders(OutputFormat.JSON);

            return new ResponseEntity<StreamingResponseBody>(responseBody, httpHeaders, HttpStatus.OK);

        } catch (Exception e) {
            try {
                if (sqlStatement != null) {
                    sqlStatement.close();
                }
            } catch (Exception ex) {
                // ignore
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception ex) {
                // ignore
            }
            return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * This endpoint tests access to the metadata database. should be removed after
     * testing is done.
     *
     * @param userToken the user's authentication token
     * @param database the name of the database to test access for
     * @return true if the user can query the specified database, false otherwise, or null if an error occurs
     */
    @RequestMapping(value = "/test/casjobs/{database}", method = RequestMethod.GET)
    public Boolean getCasJobsResource(
        @RequestHeader(value = Client.AUTH_TOKEN_HEADER, required = true) String userToken,
        @PathVariable String database) {

        try {
            CasJobsPrivileges cjprivs = metadataManager.casjobsPrivilegesForUser(userToken);
            return cjprivs.canQueryDatabase(database);
        } catch (SciServerClientException e) {
            return null;
        }
    }

    private HttpHeaders createHttpResponseHeaders(OutputFormat format) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(Output.getContentTypeHeader(format));
        return httpHeaders;
    }

    private PreparedStatement getNewStatement(Connection connection, String query)
            throws SQLException {
        PreparedStatement sqlStatement = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        sqlStatement.setFetchSize(config.getResultFetchSize());
        sqlStatement.setQueryTimeout(config.getQueryTimeout());
        return sqlStatement;
    }

    private Connection getNewConnection(DbContext dbContext, boolean doWrite) 
        throws SQLException, ClassNotFoundException {

        String schema = ""; // for now
        Boolean doUseSSL = false; // for now
        String userName = doWrite ? dbContext.getReadwriteUserName() : dbContext.getReadonlyUserName();
        String password = doWrite ? dbContext.getReadwriteUserPassword() : dbContext.getReadonlyUserPassword();

        String jdbcUrl = DatabaseVendor.getJDBCUrl(dbContext.getDbServer().getVendor(),
                dbContext.getDbServer().getHost(),
                dbContext.getDbName(),
                userName, password,
                schema, doUseSSL);
        Connection connection = DriverManager.getConnection(jdbcUrl);
        connection.setAutoCommit(false);
        return connection;
    }

    private ResponseEntity<StreamingResponseBody> getErrorResponse(String errorMessage, HttpStatus status) {
        HttpHeaders httpHeaders = createHttpResponseHeaders(OutputFormat.JSON);
        return new ResponseEntity<StreamingResponseBody>(new StreamingHttpErrorBody(errorMessage), 
                                                                                    httpHeaders, status);
    }
}
