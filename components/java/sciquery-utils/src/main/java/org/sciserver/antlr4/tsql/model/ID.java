package org.sciserver.antlr4.tsql.model;

import org.antlr.v4.runtime.tree.ParseTree;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.*;

public class ID {

    private String name;
    private boolean isBracketed;

    public ID(String name) {
        setName(name);
    }
    
    public ID(IdContext ctx) {
        for (ParseTree child : ctx.children) {
            String text = child.getText();
            setName(text);
        }
    }

    private void setName(String text) {
        text=text.trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            name = text.substring(1, text.length() - 1);
            isBracketed = true;
        } else {
            name = text;
            isBracketed = false;
        }
    }
    
    public String getNameUsedInQuery() {
        return isBracketed ? getBracketedName() : name;
    }

    /**
     * return name as it will have been represented in RACM or SciQuery repo.<br/>
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get safe name that can be used even if name contains characters that are illegal to use in simple, unbracketed names.<br/>
     * @return
     */
    public String getBracketedName() {
        return "[" + name + "]";
    }

}
