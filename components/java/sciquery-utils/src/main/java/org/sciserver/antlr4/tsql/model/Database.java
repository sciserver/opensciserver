package org.sciserver.antlr4.tsql.model;

import java.util.HashSet;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.IdContext;

public class Database extends DatabaseEntity {

    public static final String READ = "read_database"; // what does this mean?
    public static final String WRITE = "write_database";
    public static final String CREATE_SCHEMA = "create_schema";
    public static final String CREATE_TABLE = "create_table";
    public static final String CREATE_PROCEDURE = "create_procedure";
    private static final HashSet<String> validActions = new HashSet<String>();
    static {
        validActions.add(READ);
        validActions.add(WRITE);
        validActions.add(CREATE_SCHEMA);
        validActions.add(CREATE_TABLE);
        validActions.add(CREATE_PROCEDURE);
    }

    @Override
    public boolean isValidAction(String action) {
        return validActions.contains(action);
    }

    public Database(IdContext name) {
        super(name);
    }

    public Database(String name) {
        super(name);
    }

}
