package org.sciserver.antlr4.tsql;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Common_table_expressionContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Dml_clauseContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Sql_clausesContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Table_nameContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParserBaseListener;
import org.sciserver.antlr4.tsql.model.Database;
import org.sciserver.antlr4.tsql.model.Schema;
import org.sciserver.antlr4.tsql.model.Table;

public class TableListener extends SciQueryTSqlParserBaseListener {

    private static Database defaultDB = new Database("MyDB");
    private static Schema defaultSchema = new Schema("dbo");
    private Database contextDB = defaultDB;

    HashMap<String, Common_table_expressionContext> ctes = new HashMap<String, Common_table_expressionContext>();
    HashMap<String, Table> tables = new HashMap<String, Table>();

    public TableListener(Database contextDB) {
        this.contextDB = contextDB;
    }

    @Override
    public void exitSelect_statement(SciQueryTSqlParser.Select_statementContext ctx) {
        System.out.printf("Found select statement inside %s\n", ctx.parent.getClass());
    }

    @Override
    public void exitTable_name(Table_nameContext ctx) {
        Table tbl = newTable(ctx, contextDB);
        tables.put(tbl.getQualifiedName(), tbl);

        super.exitTable_name(ctx);
    }

    @Override
    public void exitCommon_table_expression(Common_table_expressionContext ctx) {
        ctes.put(ctx.expression_name.getText(), ctx);
    }

    public static Table newTable(Table_nameContext ctx, Database currentDB) {
        Database database = ctx.database != null ? new Database(ctx.database) : currentDB;
        Schema schema = ctx.schema != null ? new Schema(ctx.schema) : defaultSchema;
        Table table = new Table(database, schema, ctx.table);
        return table;
    }

    public List<Table> getTables() {
        List<Table> r = new ArrayList<Table>();
        for (String key : tables.keySet()) {
            Table t = tables.get(key);
            if (ctes.get(t.getName().getName()) == null)
                r.add(t);
        }
        return r;
    }
}
