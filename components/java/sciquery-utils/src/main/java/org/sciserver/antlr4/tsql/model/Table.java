package org.sciserver.antlr4.tsql.model;

import java.util.HashSet;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.*;

public class Table extends DatabaseEntity {

    private Database database;
    private Schema schema;

    public static final String READ = "read_table"; // use of table in select statement
    public static final String WRITE = "write_table"; // creating/updating/deleting index, constraint etc. Also other
                                                      // metadata we will support
    private static final HashSet<String> validActions = new HashSet<String>();
    static {
        validActions.add(READ);
        validActions.add(WRITE);
    }

    @Override
    public boolean isValidAction(String action) {
        return validActions.contains(action);
    }

    public Table(String database, String schema, String name) {
        super(name);
        this.database = new Database(database);
        this.schema = new Schema(schema);
    }

    public Table(IdContext database, IdContext schema, IdContext name) {
        super(name);
        this.database = new Database(database);
        this.schema = new Schema(schema);
    }

    public Table(Database database, Schema schema, IdContext name) {
        super(name);
        this.database = database;
        this.schema = schema;
    }

    public Table(Table_nameContext ctx) {
        this(ctx.database, ctx.schema, ctx.table);
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public boolean isInWriteMode() {
        return hasAction(WRITE);
    }

    @Override
    public void setInWriteMode(boolean inWriteMode) {
        if (inWriteMode) {
            addAction(WRITE);
        }
    }

    public String getQualifiedName() {
        return String.format("%s.%s.%s", database, schema, this);
    }
}
