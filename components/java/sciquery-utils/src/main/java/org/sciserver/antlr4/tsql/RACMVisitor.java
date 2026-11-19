package org.sciserver.antlr4.tsql;

import java.util.ArrayList;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParserBaseVisitor;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.BatchContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Column_elemContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Common_table_expressionContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Ddl_objectContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Delete_statementContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Drop_tableContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.IdContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Insert_statementContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.SCALAR_FUNCTIONContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Sql_clauseContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.TV_UDF_CALLContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Table_aliasContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Table_alias_usageContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Table_nameContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Table_sourceContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Table_source_itemContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.TsqlContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Update_statementContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Use_statementContext;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.Write_table_nameContext;
import org.sciserver.antlr4.tsql.model.Database;
import org.sciserver.antlr4.tsql.model.SQLClause;
import org.sciserver.antlr4.tsql.model.SQLStatement;
import org.sciserver.antlr4.tsql.model.Table;


/**
 * This visitor extracts RACM-like resources used in a SQL script to support
 * checking user privileges.
 *
 * Also replaces MyDB references with user specific MyDB.
 *
 * @author Gerard Lemson (glemson1@jhu.edu)
 *
 */
public class RACMVisitor extends SciQueryTSqlParserBaseVisitor<String> {
    private static final Logger LOG = LogManager.getLogger();

    private SQLStatement statement;
    private SQLClause currentClause;
    private Database contextDB;
    private boolean inWriteMode = false;
    private Column_elemContext currentColumn = null;
    private Stack<StringBuffer> tokenizers;
    private StringBuffer tokens;
    private String lastId;
    private String lastTable;

    public RACMVisitor(String sql, Database contextDB) {
        this.contextDB = contextDB;
        this.tokenizers = new Stack<StringBuffer>();
        this.statement = new SQLStatement(sql);

        push();
    }

    public void printTokens() {
        LOG.debug("printTokens: " + tokens.toString());
    }

    /*******************************************************/
    /**
     * Manage the stack of rewrite buffers
     * 
     * @return
     */
    private StringBuffer push() {
        this.tokens = new StringBuffer();
        this.tokenizers.push(this.tokens);
        return this.tokens;
    }

    private StringBuffer pop() {
        this.tokenizers.pop();
        this.tokens = this.tokenizers.peek();
        return this.tokens;
    }

    /*******************************************************/
    public static String input(ParserRuleContext ctx) {
        int a = ctx.start.getStartIndex();
        int b = ctx.stop.getStopIndex();
        Interval interval = new Interval(a, b);
        CharStream input = ctx.start.getInputStream();
        return input.getText(interval);
    }

    public ArrayList<SQLClause> getStatements() {
        return statement;
    }

    /**
     * Entry point, though method is currently just turning back to bae class, this
     * allows a breakpoint to be set.
     */
    @Override
    public String visitTsql(TsqlContext ctx) {
        return super.visitTsql(ctx);
    }

    @Override
    public String visitUse_statement(Use_statementContext ctx) {
        super.visitUse_statement(ctx);
        this.contextDB = new Database(ctx.database);
        return null;
    }

    @Override
    public String visitSql_clause(Sql_clauseContext ctx) {
        String input = input(ctx);
        currentClause = new SQLClause(contextDB, input);
        statement.add(currentClause);
        super.visitSql_clause(ctx);
        return null;
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        this.tokens.append(" ").append(node.getText());
        return node.getText();
    }

    /*******************************************************/
    @Override public String visitId(IdContext ctx) {
        this.lastId = ctx.getText();
        return visitChildren(ctx);
    }

    @Override public String visitTable_source(Table_sourceContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public String visitTable_name(Table_nameContext ctx) {
        LOG.debug("visitTable_name: " + ctx.getText() + "  (lastId=" + this.lastId + ", lastTable=" + this.lastTable + ")");

        StringBuffer sb = push();
        super.visitTable_name(ctx);
        Table table = currentClause.addTable(ctx, this.inWriteMode);
        pop();

        if (table != null && currentColumn == null) {
            this.tokens.append(" ").append(table.getQualifiedName());
        } else {
            this.tokens.append(sb);
        }
        printTokens();

        this.lastTable = ctx.getText();
        return sb.toString();
    }

    @Override public String visitTable_alias(Table_aliasContext ctx) { 
        LOG.debug("visitTable_alias: " + ctx.getText() + "  (lastId=" + this.lastId + ", lastTable=" + this.lastTable + ")");
        currentClause.addTableAlias(ctx.getText(), this.lastId, this.lastTable);
        String r = super.visitTable_alias(ctx);
        return r; 
    }

    @Override public String visitTable_alias_usage(Table_alias_usageContext ctx) {
        LOG.debug("visitTable_alias_usage: " + ctx.getText());
        return visitChildren(ctx);
    }

    @Override
    public String visitWrite_table_name(Write_table_nameContext ctx) {
        LOG.debug("visitWrite_table_name: " + ctx.getText());
        this.inWriteMode = true;
        String r = super.visitWrite_table_name(ctx);
        this.inWriteMode = false;
        return r;
    }

    @Override
    public String visitCommon_table_expression(Common_table_expressionContext ctx) {
        super.visitCommon_table_expression(ctx);
        currentClause.addCte(ctx);
        return null;
    }

    @Override
    public String visitColumn_elem(Column_elemContext ctx) {
        this.currentColumn = ctx;
        String s = super.visitColumn_elem(ctx);
        this.currentColumn = null;
        return s;
    }

    @Override
    public String visitDdl_object(Ddl_objectContext ctx) {
        LOG.debug("visitDdl_object: " + ctx.getText());
        this.inWriteMode = true;
        String r = super.visitDdl_object(ctx);
        this.inWriteMode = false;
        return r;
    }

    @Override
    public String visitInsert_statement(Insert_statementContext ctx) {
        // TODO Auto-generated method stub
        return super.visitInsert_statement(ctx);
    }

    @Override
    public String visitUpdate_statement(Update_statementContext ctx) {
        // TODO Auto-generated method stub
        return super.visitUpdate_statement(ctx);
    }

    @Override
    public String visitDelete_statement(Delete_statementContext ctx) {
        // TODO Auto-generated method stub
        return super.visitDelete_statement(ctx);
    }

    @Override
    public String visitBatch(BatchContext ctx) {
        // TODO Auto-generated method stub
        return super.visitBatch(ctx);
    }

    @Override
    public String visitDrop_table(Drop_tableContext ctx) {
        return super.visitDrop_table(ctx);
    }

    /**
     * TODO add code to properly handle table source items. Currently too much
     * handled by the tablename contexts.<br/>
     */
    @Override
    public String visitTable_source_item(Table_source_itemContext ctx) {
        String r = super.visitTable_source_item(ctx);
        return r;
    }

    @Override
    public String visitSCALAR_FUNCTION(SCALAR_FUNCTIONContext ctx) {
        // TODO Auto-generated method stub
        return super.visitSCALAR_FUNCTION(ctx);
    }

    @Override
    public String visitTV_UDF_CALL(TV_UDF_CALLContext ctx) {
        // TODO Auto-generated method stub
        return super.visitTV_UDF_CALL(ctx);
    }

}
