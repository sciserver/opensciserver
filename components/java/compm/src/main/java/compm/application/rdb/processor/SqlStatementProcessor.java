
package compm.application.rdb.processor;

import compm.application.Config;
import compm.application.FileService;
import compm.application.rdb.database.DBConnectionBuilder;
import compm.application.rdb.database.JDBCConnectionInfo;
import compm.application.rdb.database.ParsedDBTargetLocation;
import compm.application.rdb.output.Output;
import compm.application.rdb.output.OutputWriter;
import compm.application.rdb.output.OutputWriterToCSVStream;
import compm.application.rdb.output.OutputWriterToDatabase;
import compm.application.rdb.output.OutputWriterToJSONStream;
import compm.application.rdb.stream.StreamChannel;
import compm.application.rdb.stream.StreamChannelForHTTPRequest;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.racm.jobm.model.RDBTargetModel;
import org.springframework.http.HttpHeaders;


/**
 * Processes the SQL statements, handles result sets, and manages
 * output writing to databases or to files in the SciServer file system.
 */
public class SqlStatementProcessor {

    FileService fileService = new FileService();
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private RDBJobModel rdbJob;
    private boolean isQueryReadOnly;
    ResultSetProcessor[] resultSetProcessors = null;
    HashMap<String, OutputWriter> outputWriters = null;

    /**
     * This constructor initializes the resultset processors that needed to write
     * the SQL statement resultset(s).
     */
    public SqlStatementProcessor(RDBJobModel rdbJob) throws Exception {
        this.rdbJob = rdbJob;
        setResultSetProcessors();
    }

    /**
     * Executes a SQL query statement, possibly composed by several queries, and
     * processes/writes the query resultsets into several user-defined outputs.
     *
     * @param sqlStatement SQL query statement, that can consist of multiple
     *                     consecutive sql queries, each of them
     *                     producing its own resultset.
     */
    public void processSqlStatement(PreparedStatement sqlStatement)
                                    throws SQLTimeoutException, SQLException, IOException, Exception {

        try {
            // execute query and loop over result sets:
            boolean isResultSet = sqlStatement.execute();
            int resultSetIndex = 0;
            while (true) {
                if (resultSetProcessors.length > resultSetIndex) { 
                    // only write results of queries for which there is
                    // target set by the user.
                    ResultSet resultSet = null;
                    try {
                        resultSet = sqlStatement.getResultSet();
                        LinkedHashMap<String, Object> metadata = new LinkedHashMap<String, Object>();
                        this.resultSetProcessors[resultSetIndex].processResultSet(resultSet, metadata);
                        resultSetIndex++;

                    } catch (SQLException ex) {
                        throw ex;
                    } catch (Exception e) {
                        throw (new IOException("Unable to successfully write resultset(s). " + e.getMessage()));
                    } finally {
                        try {
                            resultSet.close();
                        } catch (Exception e) {
                        }
                    }
                } else {
                    Integer updateCount = sqlStatement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                }
                isResultSet = sqlStatement.getMoreResults();
            }
        } catch (SQLTimeoutException ex) {
            throw new SQLTimeoutException("The sql query has timed out. ", ex);
        } catch (SQLException ex) {
            throw new SQLException("An error occurred while running the query or fetching query result(s). " 
                                   + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new IOException("An error occurred. " + ex.getMessage(), ex);
        } finally {
            cleanUpWriters(); // making sure all writers are closed, in case of exception is thrown.
            // sqlStatement is closed in rdbworker
        }

    }

    /**
     * Runs the cleanUp() method in all output writers.
     */
    public void cleanUpWriters() {
        for (OutputWriter outputWriter : outputWriters.values()) {
            outputWriter.cleanUp();
        }
    }

    /**
     * Given the output targets defined by the user in the rdbJob, initializes the
     * respective output writers for each of the targets,
     * and assigns them to the corresponding resultset processors.
     */
    private void setResultSetProcessors() throws Exception {

        outputWriters = new HashMap<String, OutputWriter>();
        int maxResultNum = 0;
        // initialize outputWriters, based on respective target models.
        for (RDBTargetModel targetModel : rdbJob.getTargets()) {
            String outputId = getOutputId(targetModel);
            maxResultNum = Math.max(maxResultNum, targetModel.getResultNumber());
            if (!outputWriters.containsKey(outputId)) {
                outputWriters.put(outputId, getOutputWriter(targetModel));
            }
            outputWriters.get(outputId).incrementNumberOfResultSetsToWrite();
        }

        this.resultSetProcessors = new ResultSetProcessor[maxResultNum];
        for (int i = 0; i < maxResultNum; i++) {
            this.resultSetProcessors[i] = new ResultSetProcessor();
        }
        for (RDBTargetModel target : rdbJob.getTargets()) {
            this.resultSetProcessors[target.getResultNumber() - 1]
                .addOutputWriter(outputWriters.get(getOutputId(target)));
        }
    }

    /**
     * Given output target model specified for the resultset of a SQL query, returns
     * its corresponding OutputWriter object used for writing that resultset.
     *
     * @param targetModel represents the output target for a SQL query rsultset.
     * @return returns an `OutputWriter` object needed to write the SQL query
     *         resultset.
     */
    public OutputWriter getOutputWriter(RDBTargetModel targetModel) throws Exception {

        Long numRowsPerFlush = Config.getInstance().getNumRowsPerFlush();
        if (targetModel.getType().equals(RDBTargetModel.TABLE)) {

            // timeout for uploading output table to database
            Integer timeout = Config.getInstance().getDatabaseWriterTimeout();
            
            ParsedDBTargetLocation location = new ParsedDBTargetLocation(targetModel);
            String domainName = location.getDomain();
            String databaseName = location.getDatabase();
            String databaseSchema = location.getSchema();
            String tableName = location.getTable();
            JDBCConnectionInfo info = DBConnectionBuilder.getJDBCConnectionInfo(databaseName, domainName,
                                                                                databaseSchema, isQueryReadOnly);
            String jdbcUrl = info.getJDBCUrl();
            String vendor = info.getVendor();
            // String vendor = DatabaseVendor.getVendorFromJDBCUrl(jdbcUrl);
            return new OutputWriterToDatabase(jdbcUrl, vendor, tableName, databaseSchema, timeout, numRowsPerFlush);

        } else { // output to FileService
            String url = getFileServiceUploadUrl(targetModel.getLocation());
            // timeout for http request to FileService
            Integer timeout = Config.getInstance().gethttpWriterTimeout();
            String requestMethod = "PUT";
            String contentTypeHeader = Output.getContentType(targetModel.getType());
            HttpHeaders httpRequestHeaders = new HttpHeaders();
            httpRequestHeaders.put("Content-Type", new ArrayList<String>(Arrays.asList(contentTypeHeader)));
            httpRequestHeaders.put("X-Auth-Token", new ArrayList<String>(Arrays.asList(rdbJob.getSubmitterToken())));
            StreamChannel streamChannel = new StreamChannelForHTTPRequest(url, requestMethod, httpRequestHeaders, 
                                                                          timeout);
            // job metadata
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<String, Object>();
            metadata.put("Query", rdbJob.getInputSql());
            metadata.put("Database", rdbJob.getDatabaseContextName());
            metadata.put("Domain", rdbJob.getRdbDomainName());
            metadata.put("JobId", rdbJob.getId());

            if (targetModel.getType().equals(RDBTargetModel.FILE_JSON)) {
                return new OutputWriterToJSONStream(streamChannel, numRowsPerFlush, metadata);
            } else if (targetModel.getType().equals(RDBTargetModel.FILE_CSV)) {
                return new OutputWriterToCSVStream(streamChannel, numRowsPerFlush);
            } else {
                throw new IllegalArgumentException("Invalid query results target type " + targetModel.getType());
            }
        }
    }

    /**
     * Generates a SciServer FileService URL for uploading a file into a particualr
     * location in the SciServer file system.
     *
     * @param location determines the location in the SciServer file system wwhere
     *                 the file needs to be uploaded into.
     * @return a SciServer FileService URL for uploading a file to a file service.
     */
    public String getFileServiceUploadUrl(String location) throws Exception {

        String fileServiceUUID = fileService.getFileServiceUUID(location);
        String relativePath = fileService.getResultsFolderRelativePath(location);
        String fileServiceUrl = Config.getInstance().getFileServiceUrl(fileServiceUUID, rdbJob.getSubmitterToken());
        return fileServiceUrl + "api/file/" + relativePath
                              + "?doShowInUserHistory=false&TaskName=RDBComputeManager.UploadFile&quiet=true";
    }

    /**
     * Returns the ID used to uniquely identify the output writer for a particular
     * target of a SQL Query.
     *
     * @param targetModel a target object.
     * @return a unique identifier string based on the target location and type.
     */
    private String getOutputId(RDBTargetModel targetModel) {
        return "(" + targetModel.getLocation() + "," + targetModel.getType() + ")";
    }

}
