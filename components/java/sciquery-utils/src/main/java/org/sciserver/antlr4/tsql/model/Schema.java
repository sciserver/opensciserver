package org.sciserver.antlr4.tsql.model;

import java.util.HashMap;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.*;

public class Schema extends DatabaseEntity {

    public static final String READ = "read_schema"; // ?
    public static final String WRITE = "write_schema"; // includes delete, updatng metadata, NOT creating content
    public static final String CREATE_TABLE = "create_table";
    public static final String CREATE_VIEW = "create_view";
    public static final String CREATE_PROCEDURE = "create_procedure";
    private static final HashMap<String, String> validActions = new HashMap<String, String>();
    static {
        validActions.put(READ, READ);
        validActions.put(WRITE, WRITE);
        validActions.put(CREATE_TABLE, CREATE_TABLE);
        validActions.put(CREATE_VIEW, CREATE_VIEW);
        validActions.put(CREATE_PROCEDURE, CREATE_PROCEDURE);
    }

    @Override
    public boolean isValidAction(String action) {
        return validActions.containsKey(action);
    }

    public Schema(IdContext name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    public Schema(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }
}
