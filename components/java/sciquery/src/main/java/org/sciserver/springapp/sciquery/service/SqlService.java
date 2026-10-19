package org.sciserver.springapp.sciquery.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.antlr4.tsql.ParserFacade;
import org.sciserver.antlr4.tsql.RACMVisitor;
import org.sciserver.antlr4.tsql.model.Database;
import org.sciserver.antlr4.tsql.model.SQLClause;
import org.sciserver.antlr4.tsql.model.TableDependency;
import org.sciserver.antlr4.tsql.model.UsageMode;
import org.springframework.stereotype.Service;

/**
 * Handles the SQL query parsing.
 */
@Service
public class SqlService {
    private static final Logger LOG = LogManager.getLogger();

    /**
     * Parses the input SQL query to determine the database contexts 
     * involved and the user actions requested.
     *
     * @param sql The SQL query to parse.
     * @param dbContextName The name of the database context.
     * @return A map of database contexts to user actions.
     * @throws IOException If an I/O error occurs.
     * @throws ParseCancellationException If the SQL query cannot be parsed.
     */
    public Map<String, String> parseInputSqlForAction(String sql, String dbContextName) throws IOException,
            ParseCancellationException {
        // Parse the query to determine the tables and actions requested
        UsageMode usageMode = null;
        String action = null;

        ParserFacade pf = new ParserFacade();
        RACMVisitor visitor = new RACMVisitor(sql, new Database(dbContextName));
        RuleContext rc = pf.parse(sql);
        visitor.visit(rc);

        ArrayList<TableDependency> inferredTables = new ArrayList<TableDependency>();
        for (SQLClause s : visitor.getStatements()) {
            LOG.debug(s.getClause() + " (" + s.getTables() + ")");
            inferredTables.addAll(s.getTables());
        }

        Map<String, String> dbContextActionMap = new HashMap<String, String>();
        for (TableDependency tableDependency : inferredTables) {
            usageMode = tableDependency.getUsage();
            action = (usageMode == UsageMode.R) ? "QUERY" : "UPDATE";
            LOG.debug(tableDependency.getDatabase() + "|" + tableDependency.getSchema() 
                      + "|" + tableDependency.getTableName());

            // Currently, with only QUERY and UPDATE for actions, do not overwrite existing cache permission if it's
            // already set to UPDATE, since that is the highest permission available.
            if (! dbContextActionMap.containsKey(tableDependency.getDatabase()) || usageMode == UsageMode.W) {
                dbContextActionMap.put(tableDependency.getDatabase(), action); 
            }
        }

        return dbContextActionMap;
    }
}
