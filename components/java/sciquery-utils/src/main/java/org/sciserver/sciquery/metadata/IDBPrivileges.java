package org.sciserver.sciquery.metadata;

public interface IDBPrivileges {

    public boolean canQueryDatabase(String db);

    public boolean canCreateTable(String db);

    public boolean canReadTable(String db, String table);

    public boolean canWriteTable(String db, String table);

}
