package org.sciserver.antlr4.tsql;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser;

public class AstPrinter {

    public static void print(RuleContext ctx) {
        explore(ctx, 0);
    }

    private static void explore(RuleContext ctx, int indentation) {
        String ruleName = SciQueryTSqlParser.ruleNames[ctx.getRuleIndex()];
        for (int i = 0; i < indentation; i++) {
            System.out.print("  ");
        }
        System.out.println(ruleName);
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree element = ctx.getChild(i);
            if (element instanceof RuleContext) {
                explore((RuleContext) element, indentation + 1);
            } else {
///            	System.out.printf("=======>>>  Found non RuleContext thingy %s\n",element);
            }
        }
    }

}