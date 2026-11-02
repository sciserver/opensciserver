package org.sciserver.antlr4.tsql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.sciserver.antlr4.tsql.TableListener;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Common_table_expressionContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Table_nameContext;

public class SQLClause {
    private static final Logger LOG = LogManager.getLogger();

    private Database contextDB;
    private String clause;
    private String fullyQualifiedClause;

    private Map<String, TableDependency> tableMap;
    private Set<String> tableAliasSet;
    private Map<String, Common_table_expressionContext> ctes;

    public SQLClause(Database contextDB, String clause) {
        this.contextDB = contextDB;
        this.clause = clause;

        this.tableMap = new HashMap<String, TableDependency>();
        this.tableAliasSet = new HashSet<String>();
        this.ctes = new HashMap<String, Common_table_expressionContext>();
    }

    public Database getContextDB() {
        return contextDB;
    }

    public void setContextDB(Database contextDB) {
        this.contextDB = contextDB;
    }

    public String getClause() {
        return clause;
    }

    public String getFullyQualifiedClause() {
        return fullyQualifiedClause;
    }

    public void setFullyQualifiedClause(String fullyQualifiedClause) {
        this.fullyQualifiedClause = fullyQualifiedClause;
    }

    public List<TableDependency> getTables() {
        return new ArrayList<TableDependency>(tableMap.values());
    }

    public Table addTable(Table_nameContext ctx) {
        return addTable(ctx, false);
    }

    public Table addTable(Table_nameContext ctx, boolean inWriteMode) {
        String tn = ctx.table.getText();
        LOG.debug("addTable: " + tn + "  [" + inWriteMode + "]");

        if (tn != null && ctes.get(tn) == null) {
            Table table = TableListener.newTable(ctx, contextDB);
            table.setInWriteMode(inWriteMode);

            if (! tableAliasSet.contains(tn)) {
                TableDependency tableDependency = new TableDependency(table);

                // Add table to map if not present or if inWriteMode is true (retain highest requested action)
                if (! tableMap.containsKey(tn) || inWriteMode) {
                    tableMap.put(tn, tableDependency);
                }
            }

            return table;
        } else {
            return null;
        }
    }

    public Set<String> getTableAliasSet() {
        return tableAliasSet;
    }

    public void addTableAlias(String tableAlias, String lastId, String lastTable) {
        LOG.debug("addTableAlias: " + lastId + " - " + lastTable);
        if (lastId.equals(lastTable)) {
            TableDependency tableDependency = this.tableMap.get(lastTable);
            if (tableDependency != null) {
                tableDependency.setAlias(tableAlias);
                LOG.debug("addTableAlias: " + tableDependency.getTableName() + " - " + tableAlias);
            }

            if (tableMap.containsKey(tableAlias)) {
                // If previously created alias table dependency is write mode, need to assign write mode to the table proper
                if (tableMap.get(tableAlias).getUsage().equals(UsageMode.W)) {
                    LOG.debug("addTableAlias: UsageMode.W");
                    tableDependency.setUsage(UsageMode.W);
                }
                tableMap.remove(tableAlias);
            }

        // If this alias was previously established as a table, it is an anonymous sub-select and cannot be evaluated
        // for permissions, so delete it from tables
        } else {
            if (tableMap.containsKey(tableAlias)) {
                tableMap.remove(tableAlias);
            }
        }

        this.tableAliasSet.add(tableAlias);
    }

    public Map<String, Common_table_expressionContext> getCtes() {
        return ctes;
    }

    public void setCtes(Map<String, Common_table_expressionContext> ctes) {
        this.ctes = ctes;
    }

    public void addCte(Common_table_expressionContext ctx) {
        this.ctes.put(ctx.expression_name.getText(), ctx);
    }
}
