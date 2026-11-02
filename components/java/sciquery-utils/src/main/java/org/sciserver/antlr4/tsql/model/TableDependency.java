package org.sciserver.antlr4.tsql.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableDependency {

    private String database, schema, tableName;
    private UsageMode usage;
    private String alias;
    private Table _table = null;

    public TableDependency(@JsonProperty("database") String database, @JsonProperty("schema") String schema,
            @JsonProperty("table_name") String table, @JsonProperty("usage") UsageMode usage) {
        this.database = database;
        this.schema = schema;
        this.tableName = table;
        this.usage = usage;
    }

    public TableDependency(Table table) {
        this(table.getDatabase().getName().getNameUsedInQuery(), table.getSchema().getName().getNameUsedInQuery(), table.getName().getNameUsedInQuery(),
                usageModeForTable(table));
        this._table = table;
    }

    public static UsageMode usageModeForTable(Table table) {
        UsageMode rw = table.isInWriteMode() ? UsageMode.W : UsageMode.R;
        return rw;
    }

    @Override
    public String toString() {
        return String.format("%s.%s.%s [%s]", this.database, this.schema, this.tableName, this.usage);
    }

    public Table asTable() {
        if (_table == null)
            _table = new Table(database, schema, tableName);
        return _table;
    }

    public String getDatabase() {
        return database;
    }

    public String getSchema() {
        return schema;
    }

    public String getTableName() {
        return tableName;
    }

    public UsageMode getUsage() {
        return usage;
    }

    /**
     * Test whether two instances are equal based on contents.
     */
    public boolean equals(Object other) {
        if (!(other instanceof TableDependency))
            return false;
        TableDependency o = (TableDependency) other;
        return this.database.equals(o.getDatabase()) && this.schema.equals(o.getSchema())
                && this.tableName.equals(o.getTableName()) && o.getUsage() == this.usage;
    }

    public void setUsage(UsageMode usage) {
        this.usage = usage;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

}
