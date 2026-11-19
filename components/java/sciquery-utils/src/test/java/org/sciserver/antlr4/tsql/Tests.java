package org.sciserver.antlr4.tsql;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.antlr.v4.runtime.RuleContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import org.sciserver.antlr4.tsql.model.Database;
import org.sciserver.antlr4.tsql.model.SQLClause;
import org.sciserver.antlr4.tsql.model.TableDependency;

/**
 * Should contain unit tests for this project, but it is somewhat non-trivial to
 * create these as intrinsically a realistic SQL Server database connection is
 * required form the beginning. <br/>
 * TODO add some useful ones.
 * 
 * @author Gerard
 *
 */
public class Tests {
    private static final Logger LOG = LogManager.getLogger();

    public static final String folder = "/sql-statement-examples/";

    private ObjectMapper om;

    @Before
    public void init() {
        om = JsonMapper.builder().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS).build();
    }

    @Test
    public void insert_columns_values() {
        assertTrue(testForTables("insert_columns_values.sql", true));
    }

    @Test
    public void insert_select_cte() {
        assertTrue(testForTables("insert_select_cte.sql", true));
    }

    @Test
    public void insert_select() {
        assertTrue(testForTables("insert_select.sql", true));
    }

    @Test
    public void insert_values() {
        assertTrue(testForTables("insert_values.sql", true));
    }

    @Test
    public void select_cte_use() {
        assertTrue(testForTables("select_cte_use.sql", true));
    }

    @Test
    public void select_cte() {
        assertTrue(testForTables("select_cte.sql", true));
    }

    @Test
    public void select_no_tables() {
        assertTrue(testForTables("select_no_tables.sql", true));
    }

    @Test
    public void select_one_table() {
        assertTrue(testForTables("select_one_table.sql", true));
    }

    @Test
    public void select_scalar_function() {
        assertTrue(testForTables("select_scalar_function.sql", true));
    }

    @Test
    public void select_table_function() {
        assertTrue(testForTables("select_table_function.sql", true));
    }

    @Test
    public void select_two_tables_alias() {
        assertTrue(testForTables("select_two_tables_alias.sql", true));
    }

    @Test
    public void select_use() {
        assertTrue(testForTables("select_use.sql", true));
    }

    @Test
    public void update_table() {
        assertTrue(testForTables("update_table.sql", true));
    }

    /**
     * run tests for all the .sql files in the examples folder.<br/>
     * Fail if an error is found, print all failed files.
     */
    @Test
    public void testAllForTables() {
        String[] testFiles = null;

        URL url = Tests.class.getResource(folder);
        if (url != null) {
            try {
                File dir = new File(url.toURI());
                if (dir.isDirectory()) {
                    testFiles = dir.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".sql");
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> failed = new ArrayList<String>();
        for (String testFile : testFiles) {
            System.out.printf("\nTESTING %s \n", testFile);
            if (! testForTables(testFile, false)) {
                failed.add(testFile);
                System.out.println("XXXXXX  FAIL " + testFile);
            } else {
                System.out.println("VVVVVV  PASS " + testFile);
            }
        }

        assert(failed.size() == 0);
    }

    public boolean testForTables(String filename, boolean verbose) {
        ParserFacade pf = new ParserFacade();
        DependencyTest test = null;

        URL url = Tests.class.getResource(folder + filename);
        if (url != null) {
            try {
                File file = new File(url.toURI());
                test = om.readValue(file, DependencyTest.class);
            } catch (Exception e) {
                System.out.println("JSON PARSE EXCEPTION: " + e.getMessage());
                return false;
            }
        }

        if (test == null) {
            return false;
        }

        try {
            RACMVisitor visitor = new RACMVisitor(test.getSql(), new Database(test.getContext()));
            RuleContext rc = pf.parse(test.getSql());
            visitor.visit(rc);

            ArrayList<TableDependency> inferredTables = new ArrayList<TableDependency>();
            if (visitor.getStatements().size() == 0) {
                return test.getTablesUsed().size() == 0;
            } else {
                for (SQLClause s : visitor.getStatements()) {
                    LOG.info(s.getClause());
                    inferredTables.addAll(s.getTables());
                }
            }

            return compare(test.getTablesUsed(), inferredTables, verbose);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private boolean testForTables2(String filename, boolean verbose) {
        ParserFacade pf = new ParserFacade();
        JsonNode json = null;
        URL url = Tests.class.getResource(folder + filename);
        if (url != null) {
            try {
                File file = new File(url.toURI());
                json = om.readTree(file);
            } catch (Exception e) {
                System.out.println("JSON PARSE EXCEPTION: " + e.getMessage());
                return false;
            }
        }
        String sql = json.get("sql").asText();
        sql = sql == null ? "" : sql.trim();
        String context = json.get("context").asText();
        ArrayNode tablesUsed = (ArrayNode) json.get("tables-used");
        ArrayList<TableDependency> expectedTables = new ArrayList<TableDependency>();
        ArrayList<TableDependency> inferredTables = new ArrayList<TableDependency>();
        try {
            for (JsonNode m : tablesUsed) {
                expectedTables.add(om.treeToValue(m, TableDependency.class));
            }
        } catch (JsonProcessingException jpe) {
            System.out.println("JSON PARSE EXCEPTION: " + jpe.getMessage());
            return false;
        }

        try {
            RACMVisitor visitor = new RACMVisitor(sql, new Database(context));
            RuleContext rc = pf.parse(sql);
            visitor.visit(rc);

            if (visitor.getStatements().size() == 0) {
                return expectedTables.size() == 0;
            } else {
                for (SQLClause s : visitor.getStatements()) {
                    inferredTables.addAll(s.getTables());
                }
            }

            return compare(expectedTables, inferredTables, verbose);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Return true if the two lists contain all equal elements, false
     * otherwise.<br/>
     * 
     * @param expected
     * @param inferred
     * @return
     */
    private boolean compare(ArrayList<TableDependency> expected, ArrayList<TableDependency> inferred, boolean verbose) {
        boolean ok = true;
        
        if(verbose) {
            System.out.println("--------------- COMPARE -------------------");
            System.out.println("EXPECTED");
            for (TableDependency td : expected)
                System.out.println(td);
            System.out.println("INFERRED:");
            for (TableDependency td : inferred)
                System.out.println(td);
            System.out.println("--------------- COMPARE -------------------");
        }
        
        for (TableDependency e : expected) {
            boolean found = false;
            for (TableDependency i : inferred) {
                if (i.equals(e)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("NOT FOUND: " + e);
                ok = false;
            }
        }
        if (expected.size() != inferred.size()) {
            System.out.printf("expected %d tables, found %d\n", expected.size(), inferred.size());
            ok = false;
        }
        return ok;
    }

}
