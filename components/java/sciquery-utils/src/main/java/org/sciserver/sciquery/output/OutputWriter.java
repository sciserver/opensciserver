package org.sciserver.sciquery.output;


import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;


public abstract class OutputWriter {
	
	public OutputWriter(){
	}
	
	public abstract void open() throws Exception;
	public abstract void writeBeginning() throws Exception;
	public abstract void writeBeginningOfResultSet() throws Exception;
	public abstract void writeResultSetRow(ResultSet resultSet) throws Exception;
	public abstract void writeResultSetMetadata(ResultSetMetaData resultSetMetaData, LinkedHashMap<String,Object> extraMetadata) throws Exception;
	public abstract void writeEndOfResultSet() throws Exception;
	public abstract void writeEnd() throws Exception ;
	public abstract void close() throws Exception ;
	public abstract void flush() throws Exception ;
	public abstract void cleanup();

	

}
