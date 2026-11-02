package org.sciserver.sciquery.metadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sciserver.sciquery.metadata.tap.TAP_COLUMN;
import org.sciserver.sciquery.metadata.tap.TAP_TABLE;

/**
 * This class implements querying a TAP_SCHEMA stored in a database.<br/>
 * It follows the metadata standard from the specification at http://www.ivoa.net/documents/TAP/20190927/
 * We need to take evolution of the schema into account.
 * 
 * This class does *not* do its own connection handling. Connection is passd in from control code that also deals with transaction management.
 * Will close result sets etc.
 * 
 * @author Gerard
 *
 */
public class TAPMetadataManager {

    public List<TAP_TABLE> queryTAP_TABLES(Connection conn){
        String sql = "select * from TAP_SCHEMA.Tables";
        return null;
    }
    public List<TAP_COLUMN> queryTAP_COLUMNS(Connection conn, String table_schema, String table_name) throws SQLException {
        String sql = "select column_name, description, datatype from TAP_SCHEMA.columns where table_schema=? and table_name=?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList<TAP_COLUMN> result = new ArrayList<TAP_COLUMN>();
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, table_schema);
            ps.setString(2, table_name);
            ps.closeOnCompletion();
            rs = ps.executeQuery();
            while(rs.next()) {
                TAP_COLUMN c = new TAP_COLUMN(table_schema,table_name);
                int ix=0;
                c.setColumn_name(rs.getString(ix++));
                c.setDescription(rs.getString(ix++));
                c.setDatatype(rs.getString(ix++));
                result.add(c);
            }
            return result;
        }finally {
            try {
                if (rs != null)
                    rs.close();
            } catch(SQLException se) {
                // TODO log, don't throw
            }
        }
    }
}
