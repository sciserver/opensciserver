package org.sciserver.antlr4.tsql.model;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.*;

public class Procedure extends DatabaseEntity {

    public Procedure(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    public Procedure(IdContext idc) {
        super(idc);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isValidAction(String action) {
        return true;
    }

}
