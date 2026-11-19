package org.sciserver.sciquery.output;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import org.json.JSONArray;

public class OutputWriterToCSV extends OutputWriter{
	
	private final static String RESULTSET_BASE_NAME = "resultset";
	private OutputStream outputStream;
	private PrintStream writer;
	private Long numRowsPerFlush;
	private Long numRowsWritten = 0L;
	
	public OutputWriterToCSV(OutputStream outputStream, Long numRowsPerFlush){
		super();
		this.outputStream = outputStream;
		this.writer = new PrintStream(outputStream);
		this.numRowsPerFlush = numRowsPerFlush == null ? 1000L : numRowsPerFlush;
	}
	
	@Override
	public void open() throws Exception  {
		
	}
	
	@Override
	public void writeBeginning(){
		
	}
	
	@Override
	public void writeBeginningOfResultSet(){
		
	}
	
	@Override
	public void writeResultSetMetadata(ResultSetMetaData resultSetMetaData, LinkedHashMap<String,Object> extraMetadata) throws SQLException, IOException{
		
		String resultsetIndex = extraMetadata.get("ResultSetIndex") != null ? extraMetadata.get("ResultSetIndex").toString() : "1";
        writer.print("#" + RESULTSET_BASE_NAME + resultsetIndex);
        writer.println();

		int n = resultSetMetaData.getColumnCount();
		
		//write column names in header
		for (int i=1; i<=n; i++) {
			String columnName = resultSetMetaData.getColumnName(i);
			if ("?column?".equals(columnName)) columnName = "column" + i;
			writer.print(columnName);
			if (i != n) writer.print(",");
		}
		writer.println();
		
		flush();
	}
		
	@Override
	public void writeResultSetRow(ResultSet resultSet) throws SQLException, IOException{
		int n = resultSet.getMetaData().getColumnCount();
		for (int i=1; i<=n; i++) {
			writer.print(getValueAsString(resultSet, i));
			if (i != n) writer.print(",");
		}
		writer.println();
		numRowsWritten++;

		// write rows every numRowsPerFlush number of rows 
		if(numRowsWritten % numRowsPerFlush == 0){
			flush();
		}
	}

	@Override
	public void writeEndOfResultSet() throws IOException{
		flush();
	}
	
	@Override
	public void writeEnd(){

	}
	
	@Override
	public void flush() throws IOException{
		writer.flush();
		outputStream.flush();
	}
	
	@Override
	public void close(){
		try{writer.close();}catch(Exception e){}
		try{outputStream.close();}catch(Exception e){}
	}

	@Override
	public void cleanup(){
		close();
	}
	
	
	private static String getValueAsString(ResultSet rs, int column) throws SQLException {
		Object obj = rs.getObject(column);
		JDBCType columnType = JDBCType.valueOf(rs.getMetaData().getColumnType(column));
		if (obj == null) return "NULL";
		if ("String".equals(obj.getClass().getSimpleName())) {
			
			/* Enclose the string in double quotes. Escape double quotes and back slashes. */ 
			StringBuilder builder = new StringBuilder();
			builder
			  .append("\"")
			  .append(obj.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\"", "\\\\\""))
			  .append("\"");
			
			return builder.toString();
			
		}else if(JDBCType.ARRAY.equals(columnType)){
			/* switching from comma delimiter to space delimiter. */
			Array array = (Array)obj;
			return new JSONArray(array.getArray()).toString().replaceAll(",", " ");
		}
		else { 
			return obj.toString();
		}
	}

	
	

}
