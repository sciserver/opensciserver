package org.sciserver.racm.utils.model;

import java.util.Arrays;
import java.util.List;

public class NativeQueryResult {

    /** original column names and human readable ones */
    private String[] columns = null;
    private String[] hrNames = null;
    /** rows, assumed to arise from a native query to the TransientObjectManager */
    private List<Object[]> rows = null;

    public NativeQueryResult() {
    }

    public NativeQueryResult(String columnsCSV, List<?> rows) {
        if(columnsCSV != null)
            this.setColumns(columnsCSV);
        this.setRows(rows);
    }

    public void setColumns(String csv) {
        this.columns = Arrays.stream(csv.split("[,]")).map(String::trim).toArray(String[]::new);
        this.hrNames = new String[columns.length];
        int i = 0;
        for (String c : columns) {
            String hrName = splitCamelCase(c);
            hrName = hrName.substring(0, 1).toUpperCase() + hrName.substring(1);
            hrNames[i++] = hrName;
        }
    }

    static String splitCamelCase(String s) {
        return s.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
    }

    public List<Object[]> getRows() {
        return rows;
    }

    /*
     * Since these rows come from native queries, we can't make use of Java's type
     * system to ensure that the results are type-safe. All code using this class
     * uses Object arrays though
     */
    @SuppressWarnings("unchecked")
    public void setRows(List<?> rows) {
        this.rows = (List<Object[]>) rows;
    }

    public String[] getColumns() {
        return columns;
    }

    public String[] getHrNames() {
        return hrNames;
    }

}
