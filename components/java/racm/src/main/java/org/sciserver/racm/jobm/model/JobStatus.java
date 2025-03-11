package org.sciserver.racm.jobm.model;
/**
 * Utility methods to be used to set status on Job or JobModel.
 *
 * Workflow of job:
 * PENDING -> QUEUED -> STARTED -> FINISHED + (SUCCESS | ERROR)
 *
 * @author gerard
 *
 */
public final class JobStatus {
	/** status components */
	/** important, keep this order, so that the constraint < STATUS_FINISHED can be used to indicate not-yet completed jobs! */
	public static final int STATUS_PENDING = 1;
	public static final int STATUS_QUEUED = STATUS_PENDING<<1; // 2
	public static final int STATUS_ACCEPTED = STATUS_QUEUED<<1; // 4
	public static final int STATUS_STARTED = STATUS_ACCEPTED<<1; // 8
	public static final int STATUS_FINISHED = STATUS_STARTED<<1; // 16
	public static final int STATUS_SUCCESS = STATUS_FINISHED<<1; // 32
	public static final int STATUS_ERROR = STATUS_SUCCESS<<1; // 64
	public static final int STATUS_CANCELED = STATUS_ERROR<<1; // 128

	// ~~~ STATUS ~~~
	private static boolean hasStatus(int status, int test){
		return (status & test) == test;
	}
	// ~~~ STARTED ~~~
	public static boolean isStarted(int status){
		return hasStatus(status, STATUS_STARTED);
	}
	// ~~~ FINISHED ~~~
	/**
	 * Return true if the job has finished, and may be completed, canceled or in error.<br/>
	 * @param status
	 * @return
	 */
	public static boolean hasFinished(int status){
		return status >= STATUS_FINISHED;
	}
	/**
	 * Return true if the job has finished, and may be completed, canceled or in error.<br/>
	 * @param status
	 * @return
	 */
	public static boolean hasStarted(int status){
		return status >= STATUS_STARTED;
	}
	public static boolean isCompleted(int status){
		return status > STATUS_FINISHED;
	}
}