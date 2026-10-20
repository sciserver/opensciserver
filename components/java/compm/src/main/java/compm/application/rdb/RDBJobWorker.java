package compm.application.rdb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import compm.application.Config;
import compm.application.JobMaster;
import compm.application.JobWorker;
import compm.application.rdb.database.DBConnectionBuilder;
import compm.application.rdb.processor.SqlStatementProcessor;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Date;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.JobMessageModel;
import org.sciserver.racm.jobm.model.JobStatus;
import org.sciserver.racm.jobm.model.RDBJobModel;


/**
 * The `RDBJobWorker` class extends `JobWorker` and contains methods for
 * managing the lifecycle of a SQL query that runs in a Relational Database as a Job.*
 * 
 */
public class RDBJobWorker extends JobWorker {
    private PreparedStatement sqlStatement;
    private SqlStatementProcessor processor;
    private RDBJobModel rdbJob;

    public RDBJobWorker(JobMaster master, int workerId, long idleTimeBetweenJobs) {
        super(master, workerId, idleTimeBetweenJobs);
    }

    /**
     * Runs and manages the query job's life cycle.
     */
    @Override
    public void runJob(COMPMJobModel job) {
        rdbJob = (RDBJobModel) job;
        try {

            // create sql statement
            String sqlQuery;
            Boolean isQueryReadOnly;

            if (rdbJob.getSql() == null) {
                sqlQuery = rdbJob.getInputSql();
                isQueryReadOnly = true;
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(rdbJob.getSql());
                sqlQuery = jsonNode.get("outputSql").asText();
                isQueryReadOnly = jsonNode.get("isQueryReadOnly").asBoolean();
            }

            String schema = null; // known after query parsing, to be added later.
            Integer queryTimeout = Integer.parseInt(rdbJob.getTimeout().toString());
            Integer fetchSize = Config.getInstance().getResultFetchSize();

            processor = new SqlStatementProcessor(rdbJob);
            sqlStatement = DBConnectionBuilder.getNewStatement(sqlQuery, rdbJob.getDatabaseContextName(),
                                                               rdbJob.getRdbDomainName(), schema, 
                                                               isQueryReadOnly, queryTimeout, fetchSize);

            // updating job status in JOBM to STARTED
            rdbJob.setStartTime(new Date());
            rdbJob.setStatus(JobStatus.STATUS_STARTED);
            rdbJob = (RDBJobModel) master.updateJob(rdbJob);
            // do not run query if cancelJob() was triggered by a user cancellation before
            // reaching this block of code.
            if (!isCanceled) {
                // executing query statement:
                processor.processSqlStatement(sqlStatement);
                sqlStatement.getConnection().commit();
                rdbJob.setStatus(JobStatus.STATUS_SUCCESS);
            } else {
                // will be caught as triggered by a user cancellation of this job
                throw new Exception();
            }
        } catch (SQLTimeoutException e) {
            rdbJob.setStatus(JobStatus.STATUS_ERROR);
            rdbJob.setMessages(master.putNewMessageInMessagesList(rdbJob, JobMessageModel.TIMEOUT,
                       "The query has timed out.", false));
            try {
                sqlStatement.getConnection().rollback();
            } catch (Exception ex) {
            }
        } catch (Exception e) {
            try {
                sqlStatement.getConnection().rollback();
            } catch (Exception ex) {
            }
            if (!isCanceled) {
                rdbJob.setStatus(JobStatus.STATUS_ERROR);
                String errorMessage = "Job failed. " + e.getMessage() != null ? e.getMessage() : "";
                rdbJob.setMessages(master.putNewMessageInMessagesList(rdbJob, JobMessageModel.ERROR, 
                                                                      errorMessage, false));
                JobMaster.LogMessage(
                        "Error when running rdbJob " + job.getId() + " by workerId = " + this.workerId + ".", e);
            } else {
                rdbJob.setStatus(JobStatus.STATUS_CANCELED);
                rdbJob.setMessages(master.putNewMessageInMessagesList(rdbJob, JobMessageModel.CANCEL,
                           "SQL query was cancelled by the user.", false));
            }
        } finally {
            if (rdbJob.getStartTime() == null) {
                rdbJob.setStartTime(new Date());
            }
                
            rdbJob.setEndTime(new Date());
            double duration = (rdbJob.getEndTime().getTime() - rdbJob.getStartTime().getTime()) / 1000.0;
            rdbJob.setDuration(duration); // in seconds
            // close statement and connection before updating status to jobm.
            try {
                sqlStatement.close();
            } catch (Exception e) {
            }
            try {
                sqlStatement.getConnection().close();
            } catch (Exception e) {
            }
            // last update of the job status to JOBM:
            rdbJob = (RDBJobModel) updateJob(rdbJob);
        }
    }

    /**
     * Cancels SQL statement.
     */
    @Override
    public void cancelJob() {
        if (job.getStatus() <= JobStatus.STATUS_FINISHED) {
            super.cancelJob();
            try {
                if (sqlStatement != null) {
                    // sqlStatement.cancel() makes processSqlStatement throw a SQLException
                    sqlStatement.cancel();
                }
            } catch (SQLException e) {
            }
        }
    }
}
