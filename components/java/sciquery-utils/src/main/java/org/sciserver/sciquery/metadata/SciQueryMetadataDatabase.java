package org.sciserver.sciquery.metadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.sciserver.sciquery.metadata.model.ConnectionAuthData;
import org.sciserver.sciquery.metadata.model.DBContext;
import org.sciserver.sciquery.metadata.model.DBDomain;

public class SciQueryMetadataDatabase {

    private DataSource dataSource;
    protected Map<String,DBDomain> domains;
    private TAPMetadataManager tapManager;

    public SciQueryMetadataDatabase(DataSource ds) {
        this.dataSource = ds;
        this.tapManager = new TAPMetadataManager();
    }

    public Map<String,DBDomain> getDomains() {
        if(domains == null) {
            try {
                this.refreshCache(dataSource);
            } catch(Exception e) {
                // TODO log error
                domains = null;
            }
        }
        return domains;
    }
    /**
     * Create a cached representation of the sciquery databases metadata.
     */
    public void refreshCache(DataSource dataSource) throws SciQueryException, SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            String sql1 = "select domain_id, racm_domain_uuid, displayName, description "
                    + "from dbdomain";
            
            String sql2 = "select domain_id, id, context_name, racm_resource_uuid, description"
                    + ", host, port, db_name, vendor, jdbcURL"
                    + ", readonly_user_name, readonly_user_password"
                    + ", readwrite_user_name, readwrite_user_password"
                    + " from DBContext order by domain_id";
            
            PreparedStatement ps = conn.prepareStatement(sql1+";"+sql2);
            HashMap<Long,DBDomain> domainsById = new HashMap<Long, DBDomain>();
            HashMap<String,DBDomain> domainsByUUID = new HashMap<String, DBDomain>();
            
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                int ix= 1;
                DBDomain d = new DBDomain(rs.getLong(ix++), rs.getString(ix++));
                domainsById.put(d.getDomain_id(), d);
                domainsByUUID.put(d.getRacm_domain_uuid(), d);
                d.setDisplayName(rs.getString(ix++));
                d.setDescription(rs.getString(ix++));
            }
            ps.getMoreResults(PreparedStatement.CLOSE_CURRENT_RESULT);
            rs = ps.getResultSet();
            while(rs.next()) {
                int ix= 1;
                long domain_id=rs.getLong(ix++);
                DBDomain domain = domainsById.get(domain_id); // assuming FK constraint this should not be null
                DBContext c = new DBContext( domain, rs.getLong(ix++),rs.getString(ix++));
                c.setRacm_resource_uuid(rs.getString(ix++));
                c.setDescription(rs.getString(ix++));
                c.setHost(rs.getString(ix++));
                c.setPort(rs.getInt(ix++));
                c.setDb_name(rs.getString(ix++));
                c.setVendor(rs.getString(ix++));
                c.setJdbcURL(rs.getString(ix++));
                c.setReaderusername(rs.getString(ix++));
                c.setReaderuserpassword(rs.getString(ix++));
                c.setWriterusername(rs.getString(ix++));
                c.setWriteruserpassword(rs.getString(ix++));
            }
            rs.close();
            ps.close();
            
            this.domains = domainsByUUID;
        } catch(Exception e){
            e.printStackTrace();
        }
            finally {
            if(conn != null)
                conn.close();
        }
        
    }
    
    public DBDomain getDomain(String uuid){
        return getDomains().get(uuid);
    }
    
    public ConnectionAuthData jdbcAuthData(String domainUUID, String context, boolean forWrite) {
        try {
            DBDomain domain = getDomain(domainUUID);
            DBContext dbc = domain.getContext(context);
            return new ConnectionAuthData(dbc.getJdbcURL(),
                    forWrite?dbc.getWriterusername():dbc.getReaderusername(),
                    forWrite?dbc.getWriteruserpassword():dbc.getReaderuserpassword());
        } catch(NullPointerException e) {
            return null;
        }
    }
    
    /**
     * admin method to allow adding new database metadata.<mr/>
     * 
     * TODO implement
     * 
     * @param domainUUID
     * @param db
     */
    public void addDatabaseContext(String domainUUID, DBContext db) {
        throw new UnsupportedOperationException();
    }
}
