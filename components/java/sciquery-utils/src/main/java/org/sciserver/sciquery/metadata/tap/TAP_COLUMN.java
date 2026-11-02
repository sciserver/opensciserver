package org.sciserver.sciquery.metadata.tap;

public class TAP_COLUMN {
    private String table_schema;
    private String table_name;
    private String column_name;
    private String description;
    private String datatype;
    
    public TAP_COLUMN(String table_schema, String table_name) {
        this.table_schema = table_schema;
        this.table_name = table_name;
    }
    public String getColumn_name() {
        return column_name;
    }
    public void setColumn_name(String column_name) {
        this.column_name = column_name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDatatype() {
        return datatype;
    }
    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
    public String getTable_schema() {
        return table_schema;
    }
    public String getTable_name() {
        return table_name;
    }

}
