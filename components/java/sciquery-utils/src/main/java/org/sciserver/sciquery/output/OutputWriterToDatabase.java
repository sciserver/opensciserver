package org.sciserver.sciquery.output;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.JsonGenerator;

import org.sciserver.racm.jobm.model.RDBComputeDomainModel;

public class OutputWriterToDatabase extends OutputWriter{
	
	JsonGenerator jg;
	Integer [] columnTypes;
	
	private Connection connection;
	private PreparedStatement statement;
	private ResultSetMetaData metadata;
	private Integer timeout;
	private String tableName;
	private String insertQuery = "";
	private Long numRowsWritten = 0L;
	private Long numRowsPerFlush;
	private ArrayList<JDBCType> jdbcColumnTypes = new ArrayList<JDBCType>();
	private ArrayList<Integer> sqlTypes = new ArrayList<Integer>();
	private ArrayList<Integer> columnPrecisions = new ArrayList<Integer>();
	private ColumnHandler columnHandler;
	
	private String vendor;
	
	public OutputWriterToDatabase(String vendor, Connection connection, Integer timeout, String tableName, Long numRowsPerFlush){
		super();
		if(vendor.equals(RDBComputeDomainModel.SQLSERVER)){
			columnHandler = new ColumnHandlerSQLSERVER();
		}else{
			throw(new IllegalArgumentException("Unsupported target database vendor " + vendor));
		}
		
		this.vendor = vendor;
        this.connection = connection;
		this.timeout = timeout;
		this.tableName = tableName; // needs to be checked against sql injection
		this.numRowsPerFlush = numRowsPerFlush == null ? 1000 : numRowsPerFlush;
		
		
	}
	
	private String getSafeTableName(String tableName){
		return tableName;
	}
	
	
	@Override
	public void open() throws Exception  {
		
	}
	
	@Override
	public void writeBeginning() throws Exception  {
		
	}
	
	@Override
	public void writeBeginningOfResultSet(){
		
	}
	
	
	@Override
	public void writeResultSetMetadata(ResultSetMetaData resultSetMetaData, LinkedHashMap<String,Object> extraMetadata) throws IOException, SQLException{
		// creates an empty database table
		try{
			metadata = resultSetMetaData;
			if(resultSetMetaData.getColumnCount() > 0){
		        String columnName;
		        JDBCType columnType;
		        Integer type;
		        Integer precision;
		        String tableName_ = getSafeTableName(tableName);
		        String query = "CREATE TABLE " + tableName_ + "(";
		        for(int i=1; i<= resultSetMetaData.getColumnCount(); i++){
		        	if(i > 1)
		        		query += ", ";
		        	columnName = resultSetMetaData.getColumnLabel(i);
					if ("?column?".equals(columnName))
						columnName = "column" + i;

					columnType = JDBCType.valueOf(resultSetMetaData.getColumnType(i));
					precision  = resultSetMetaData.getPrecision(i);
					type = resultSetMetaData.getColumnType(i);
		        	jdbcColumnTypes.add(columnType);
					columnPrecisions.add(precision);
					sqlTypes.add(type);
		        	
		        	query += columnHandler.getColumnDefinition(columnName, columnType, precision);
		        }
		        query += ")";
		        System.out.println(query);
		        statement = connection.prepareStatement(query);
		        statement.setQueryTimeout(timeout);
		        statement.execute();
		        statement.close();
			}
		}catch(SQLException ex){
			cleanup();
			throw(ex);
		}
	}
	
	@Override
	public void writeResultSetRow(ResultSet resultSet) throws SQLException, IOException{
		if(metadata == null)
			metadata  = resultSet.getMetaData();
		
		insertQuery = "INSERT INTO " + tableName + " VALUES (";
		for(int i=1; i<= metadata.getColumnCount(); i++){
        	if(i > 1)
        		insertQuery += ", ?";
        	else{
        		insertQuery += " ?";
        	}
		}
		insertQuery += " )";
		
		if(numRowsWritten == 0){
			statement = connection.prepareStatement(insertQuery);
			statement.setQueryTimeout(timeout);
		}
		
		//injecting the row values:
		for(int i=1; i<= metadata.getColumnCount(); i++){
			columnHandler.setColumnValue(statement, resultSet, jdbcColumnTypes, sqlTypes, columnPrecisions, i);  
		}
		statement.addBatch();
		numRowsWritten++;

		// write rows to table every numRowsPerFlush rows
		if(numRowsWritten % numRowsPerFlush == 0){
			flush();
		}
	}
	
	@Override
	public void writeEndOfResultSet() throws SQLException{
		flush();
	}
	
	
	@Override
	public void writeEnd() throws SQLException{

	}
	
	@Override
	public void flush() throws SQLException{
		statement.executeBatch();
	}
	
	@Override
	public void close(){
		try{statement.close();}catch(Exception e){}
		try{connection.commit();}catch(Exception e){};
		try{connection.close();}catch(Exception e){}
	}
	
	@Override
	public void cleanup(){
		try{statement.close();}catch(Exception e){}
		try{connection.rollback();}catch(Exception e){};
		try{connection.close();}catch(Exception e){}
	}

}


