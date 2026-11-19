package org.sciserver.sciquery.metadata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.sciserver.clientutils.SciServerClientException;
import org.sciserver.racm.client.RACMClient;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.resources.model.UserResourceModel;
import org.sciserver.sciquery.metadata.model.ConnectionAuthData;

/**
 * This class supports metadata handling for different sciserver actions.<br/>
 * For example checks from RACM metadata whether user has rights to upload a table to a database, or data into a table.
 * ALso can find the access URL for a database context.  
 *  
 * @author Gerar
 *
 */
public class MetadataManager {
    
    public static final String RACM_ENDPOINT = "racm.endpoint";
    public static final String CASJOBS_UUID = "casjobs.uuid";
    public static final String SCIQUERY_SERVICE_TOKEN = "sciquery.service.token";
    
    private String casjobsUUID;
    private SciQueryMetadataDatabase sciQueryMDDB;
    
    private final RACMClient racmClient;

    /**
     * Constructor taking appropriate properties and a datasource for connecting to the SciQuery metadata database.
     * Latter is created for it by 'master' user code.
     * 
     * TBD should master define the racmclient as well? and maybe even instantiate the SciQueryMetadataDatabase?
     * 
     * @param properties
     * @param sciqueryMDDataSource data source to connect to SciQuery Metadata Database
     * @throws SQLException
     */
    public MetadataManager(String racmEndpoint, String serviceToken, String casJobsUUID, DataSource sciqueryMDDataSource) throws SQLException, SciQueryException{
        this.racmClient = new RACMClient(racmEndpoint,serviceToken);    
        this.casjobsUUID = casJobsUUID;
        this.sciQueryMDDB = new SciQueryMetadataDatabase(sciqueryMDDataSource);
    }
    
    /**
     * Create a url for connecting to a database
     * 
     * @param domainUUID
     * @param context name of database context, unique within domain
     * @param forWrite true if the connection is made for an update
     * @return
     * @throws SciServerClientException
     */
    public ConnectionAuthData getConnectionData(String domainUUID, String context, boolean forWrite) {
        return sciQueryMDDB.jdbcAuthData(domainUUID, context, forWrite);
    }
    


    /**
     * Find all resource/action combinations for the user with the specified token and cache them
     * @param token
     */
    public List<SciQueryPrivileges> sciQueryPrivilegesForUser(String usertoken) throws SciServerClientException{
        List<SciQueryPrivileges> privs = new ArrayList<SciQueryPrivileges>();
        List<RDBComputeDomainModel> o = this.racmClient.queryRDBComputeDomainsCall(usertoken);
        if(o != null) {
            for (RDBComputeDomainModel r:o) {
                privs.add(domainPrivilegesForUser(usertoken, r.getRacmUUID()));
            }
        }
        return privs;
    }
    /**
     * Find all resource/action combinations for the user with the specified token and cache them
     * @param token
     */
    public SciQueryPrivileges domainPrivilegesForUser(String usertoken, String domainUUID) throws SciServerClientException{
        List<UserResourceModel> u = racmClient.queryUserRCResourcesAsTable(usertoken, domainUUID);
        return new SciQueryPrivileges(domainUUID, u);
    }
    
    /**
     * Find all resource/action combinations for the user with the specified token and cache them
     * @param token
     */
    public CasJobsPrivileges casjobsPrivilegesForUser(String usertoken) throws SciServerClientException{
        List<UserResourceModel> u = racmClient.queryUserRCResourcesAsTable(usertoken, casjobsUUID);
        return new CasJobsPrivileges(u);
    }

    public SciQueryMetadataDatabase getSciQueryMDDB() {
        return sciQueryMDDB;
    }

    public RACMClient getRacmClient() {
        return racmClient;
    }
    
    
}
