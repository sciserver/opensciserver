package org.sciserver.antlr4.tsql.model;

import java.util.ArrayList;

public class SQLStatement extends ArrayList<SQLClause> {

    private String sql;

    public SQLStatement(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
