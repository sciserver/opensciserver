package org.sciserver.sciquery.output;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public abstract class ColumnHandler {
	
	public void TableColumnTranslator(){
	}
	
	public abstract String getColumnDefinition(String columnName, JDBCType columnType, Integer precision);
	public abstract void setColumnValue(PreparedStatement statement, ResultSet resultSet, ArrayList<JDBCType> jdbcColumnTypes, ArrayList<Integer> sqlType, ArrayList<Integer> columnPrecision, int columnIndex) throws SQLException;

}
