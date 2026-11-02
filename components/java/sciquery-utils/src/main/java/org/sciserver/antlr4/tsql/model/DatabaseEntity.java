package org.sciserver.antlr4.tsql.model;

import java.util.HashSet;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser.*;

/**
 * Represents a database object that a query tries to access and the actions it
 * tries to perform on it.<br/>
 * 
 * @author Gerard Lemson (glemson1@jhu.edu)
 *
 */
public abstract class DatabaseEntity {

    private ID name;
    protected boolean inWriteMode = false;
    private HashSet<String> actions = new HashSet<String>();

    public DatabaseEntity(IdContext idc) {
        this.name = new ID(idc);
    }

    public DatabaseEntity(String name) {
        this.name = new ID(name);
    }

    public ID getName() {
        return name;
    }

    /**
     * Return a safe name for this entity. <br/>
     */
    @Override
    public String toString() {
        return name.getBracketedName();
    }

    public boolean isInWriteMode() {
        return inWriteMode;
    }

    public void setInWriteMode(boolean inWriteMode) {
        this.inWriteMode = inWriteMode;
    }

    public void addAction(String action) {
        if (isValidAction(action))
            this.actions.add(action);
    }

    public boolean hasAction(String action) {
        return this.actions.contains(action);
    }

    public HashSet<String> getActions() {
        return actions;
    }

    /**
     * Return true if the action exist for the current database entity, false otherwise.<br/>
     * To be implemented  by sub classes.
     * 
     * @param action the action that is proposed.
     * @return
     */
    public abstract boolean isValidAction(String action);

}
