package org.sciserver.antlr4.tsql;

import java.util.ArrayList;

import org.sciserver.antlr4.tsql.model.TableDependency;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a
 * @author Gerard Lemson (glemson1@jhu.edu)
 *
 */
public class DependencyTest {
    private String comments;
    private String sql;
    private String context;
    private ArrayList<TableDependency> tablesUsed;
    
    DependencyTest(@JsonProperty("comments") String comments
            , @JsonProperty("sql") String sql
            , @JsonProperty("context") String context
            , @JsonProperty("tables-used") ArrayList<TableDependency> tablesUsed){
        this.comments = comments;
        this.sql = sql;
        this.context = context;
        this.tablesUsed = tablesUsed;
    }

    public String getComments() {
        return comments;
    }

    public String getSql() {
        return sql;
    }

    public String getContext() {
        return context;
    }

    public ArrayList<TableDependency> getTablesUsed() {
        return tablesUsed;
    }
}
