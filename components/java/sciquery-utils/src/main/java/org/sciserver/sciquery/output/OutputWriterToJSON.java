package org.sciserver.sciquery.output;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.sql.Array;
import java.sql.JDBCType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;


import org.sciserver.sciquery.output.Output.OutputFormat;

public class OutputWriterToJSON extends OutputWriter{
	
	private static ObjectMapper objectMapper = new ObjectMapper();
	private final static String RESULTSET_BASE_NAME = "resultset";
	private OutputStream outputStream;
	private Long numRowsPerFlush;
	private Long numRowsWritten = 0L;
	JsonGenerator jg;
	JDBCType [] columnTypes;
	
	public OutputWriterToJSON(OutputStream outputStream, Long numRowsPerFlush) throws IOException{
		super();
		this.outputStream = outputStream;
		this.jg = objectMapper.getFactory().createGenerator(outputStream).useDefaultPrettyPrinter();
		this.numRowsPerFlush = numRowsPerFlush == null ? 1000 : numRowsPerFlush; 
	}
	
	@Override
	public void open() throws Exception  {
		
	}
	
	@Override
	public void writeBeginning() throws IOException{
		jg.writeStartObject();
        jg.writeFieldName("Result");
        jg.writeStartArray();
        flush();
	}
	
	@Override
	public void writeBeginningOfResultSet() throws IOException{
		// start the resultset object inside the result array
		jg.writeStartObject();
		flush();
	}
	
	@Override
	public void writeResultSetMetadata(ResultSetMetaData resultSetMetaData, LinkedHashMap<String,Object> extraMetadata) throws IOException, SQLException{

		String resultsetIndex = extraMetadata.get("ResultSetIndex") != null ? extraMetadata.get("ResultSetIndex").toString() : "1";
		
        jg.writeStringField("TableName", RESULTSET_BASE_NAME + resultsetIndex);
        for(String key : extraMetadata.keySet()){
        	jg.writeStringField(key, extraMetadata.get(key).toString());// only strings for now
        }
        
        // starting column names array
        jg.writeFieldName("ColumnNames");
        jg.writeStartArray();
        
		int n = resultSetMetaData.getColumnCount();
		this.columnTypes = new JDBCType[n];
		for (Integer i=1; i<=n; i++) {
			String columnName = resultSetMetaData.getColumnName(i);
			columnTypes[i-1] =  JDBCType.valueOf(resultSetMetaData.getColumnType(i));
			if ("?column?".equals(columnName))
				columnName = "column" + i;
			jg.writeString(columnName);
		}
		// ending column names
		jg.writeEndArray();

        // starting column types array
        jg.writeFieldName("ColumnTypes");
        jg.writeStartArray();
        
		for (Integer i=0; i<n; i++) {
			String typeName = columnTypes[i].toString();
			jg.writeString(typeName);
		}
		// ending column types array
		jg.writeEndArray();
		
		// starting data array:
        jg.writeFieldName("Data");
        jg.writeStartArray();
        
        flush();
		
	}
	
	@Override
	public void writeResultSetRow(ResultSet resultSet) throws SQLException, IOException{
		int n = resultSet.getMetaData().getColumnCount();
		jg.writeStartArray();	
		for (Integer i=1; i<=n; i++) {
			//jg.writeString(getValueAsString(resultSet, i));
			writeJsonField(jg, resultSet, i, columnTypes);
		}
		jg.writeEndArray();
		numRowsWritten++;
		// write rows every numRowsPerFlush number of rows 
		if(numRowsWritten % numRowsPerFlush == 0){
			flush();
		}
		
	}
	
	@Override
	public void writeEndOfResultSet() throws IOException{
		//ending the data array of the current resultset object
		jg.writeEndArray();
		//ending the current resultset object		
		jg.writeEndObject();
		flush();
	}
	
	
	
	@Override
	public void writeEnd() throws IOException{
		//ending the "Results" object:
		jg.writeEndArray();
		
		//ending the json response object
		jg.writeEndObject();
		
		flush();
	}
	
	@Override
	public void flush() throws IOException{
		jg.flush();
		outputStream.flush();
	}
	
	@Override
	public void close(){
		try{jg.close();}catch(Exception e){}
		try{outputStream.close();}catch(Exception e){}
	}
	
	@Override
	public void cleanup(){
		close();
	}
	
	
	private static void writeJsonField(JsonGenerator jg, ResultSet rs, Integer columnIndex, JDBCType [] columnTypes) throws SQLException, IOException {
		
		JDBCType columnType = columnTypes[columnIndex-1];

		try{
			//see https://stackoverflow.com/questions/6514876/most-efficient-conversion-of-resultset-to-json
			Object obj = rs.getObject(columnIndex );
			if (obj == null){
				jg.writeNull();
				return;
			}
		
			switch(columnType){
				// TODO: test types
				case ARRAY:
					Array array = rs.getArray(columnIndex);
					jg.writeObject((array.getArray()));
					break;
				case BOOLEAN:
					jg.writeBoolean(rs.getBoolean(columnIndex));break;
				case BIT:
					jg.writeNumber(rs.getInt(columnIndex));break;
				case TINYINT:
				case SMALLINT:
				case INTEGER:
				case BIGINT:
				case DOUBLE:
				case REAL:
				case DECIMAL:
				case NUMERIC:
					jg.writeNumber(obj.toString());break;
				case DATE:
					jg.writeString(rs.getDate(columnIndex).toString());break;				
				case TIME:
					jg.writeString(rs.getTime(columnIndex).toString());break;
				case TIMESTAMP:
					jg.writeString(rs.getTimestamp(columnIndex).toString());break;
				case BINARY:
				case VARBINARY:
					jg.writeString(rs.getBytes(columnIndex).toString());break;				
				case BLOB:
					jg.writeString(rs.getBlob(columnIndex).toString());break;
				case CLOB:
					jg.writeString(rs.getClob(columnIndex).toString());break;
				case NCLOB:
					jg.writeString(rs.getNClob(columnIndex).toString());break;
				case SQLXML:
					jg.writeString(rs.getSQLXML(columnIndex).toString());break;				
				case REF:
					jg.writeString(rs.getRef(columnIndex).toString());break;
				default:
					jg.writeString(obj.toString());break;
			}
		}catch(Exception e){
			jg.writeString("UNKNOWN_VALUE");
		}
	}
	
	
	

}


