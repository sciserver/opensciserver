package compm.application;

//import java.util.ArrayList;
import org.sciserver.racm.jobm.model.COMPMJobModel;


/**
 * The `JobWorker` class is an abstract class that represents a worker responsible for running jobs
 * assigned by a `JobMaster` instance in a multithreaded way.
 */
public abstract class JobWorker implements Runnable {

    public JobMaster master;
    public int workerId;
    public long idleTimeBetweenJobs;
    protected boolean isCanceled;
    protected COMPMJobModel job;
    public boolean exit;
    public boolean isRunningJob;


    /**
     * Constructor method for the `JobWorker` class.
     *
     * @param master the JobMaster instance.
     * @param workerId the worker id. 
     * @param idleTimeBetweenJobs idle time (in seconds) to sleep when no new jobs are available.
     */
    public JobWorker(JobMaster master, int workerId, long idleTimeBetweenJobs) {
        this.master = master;
        this.workerId = workerId;
        this.idleTimeBetweenJobs = idleTimeBetweenJobs;
        this.isCanceled =  false;
        this.job = null;
        this.exit = false;
        this.isRunningJob = false;
    }

    /**
     * Stops the running of a job in this worker.
     */
    public void stop() {
        this.exit = true;
    }

    /**
     * Runs a job. Consists of a loop where a worker retrieves a new job from JobMaster and runs the job, 
     * or waits if no new jobs are available.
     */
    public void run() {

        while (!exit) {
            try {
                //System.out.println("worker " + workerId);
                this.job = master.getNewJobForWorker(workerId);
                if (this.job != null) {
                    //System.out.println("Worker " + workerId + " running job " + job.getId());
                    //   master.convertObjectToJsonString(job));
                    this.isCanceled = false;
                    this.isRunningJob = true;
                    runJob(this.job);
                    this.isRunningJob = false;
                } else {
                    Thread.sleep((long) (this.idleTimeBetweenJobs * 1000)); //sleep time in milliseconds.
                }
            } catch (Exception e) {
                master.LogMessage(
                    "JobWorker error when running job " + job.getId().toString() 
                    + " in worker " + workerId + ".", e
                );
            }
        }
    }

    public abstract void runJob(COMPMJobModel job) throws Exception;

    /**
     * Sets the isCancled flag to `true` for the job being run in this worker.
     */
    public void cancelJob() {
        this.isCanceled = true;
    }


    /**
     * Returns a boolean indicating whether the job is canceled or not.
     *
     * @return Returning the value of the `isCanceled` instance variable (boolean).
     */
    public boolean isJobCanceled() {
        return this.isCanceled;
    }

    /*
     * Makes the master to update the job definition in the JOBM database. 
     */
    public COMPMJobModel updateJob(COMPMJobModel job) {
        return master.updateJob(job);
    }
}