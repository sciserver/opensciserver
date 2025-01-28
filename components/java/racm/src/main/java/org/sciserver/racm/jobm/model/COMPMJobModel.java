package org.sciserver.racm.jobm.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sciserver.racm.utils.model.RACMBaseModel;
/**
 * Model for a Job to be used in interactions with a COMPM.<br/>
 * @author gerard
 *
 */
public class COMPMJobModel extends RACMBaseModel {

	/** Id of job assigned ("designated") by system actually executing the job */
	private String executorDID;

	/** Id of job assigned by submitter */
	private String submitterDID;

	/** userid of the User for whom the job is executed */
	private String submitterId;
	private String submitterTrustId;
	private String submitterToken;

	/** ID of compm running the job */
	private String runByUUID;

	/** time job was submitted */
	private Date submissionTime;
	private Date startTime;
	private Date endTime;

	/** execution time in seconds.
	 * Provided by COMPM
	 */
	private Double duration;

	/* Job timeout in seconds
	 * Set when COMPM gets this job
	 */
	private Long timeout;

	/** result */
	private List<JobMessageModel> messages = new ArrayList<>();

	/** status of job, consists of STATUS_XXX variables */
	private Integer status;

	private String resultsFolderURI;

	// immutable field
	private final String type = getLegacyType(this.getClass());

	private List<RequiredUserVolumeModel> userVolumes;

	private String username;


	public COMPMJobModel(){
	}

	public COMPMJobModel(Long id){
		super(id);
	}

	public int getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getExecutorDID() {
		return executorDID;
	}
	public void setExecutorDID(String executorDID) {
		this.executorDID = executorDID;
	}
	public String getSubmitterDID() {
		return submitterDID;
	}
	public void setSubmitterDID(String submitterDID) {
		this.submitterDID = submitterDID;
	}
	public Date getSubmissionTime() {
		return submissionTime;
	}
	public void setSubmissionTime(Date submissionTime) {
		this.submissionTime = submissionTime;
	}
	public List<JobMessageModel> getMessages() {
		return messages;
	}

	public void setMessages(List<JobMessageModel> messages) {
		if(messages != null)
			this.messages = messages;
		else
			this.messages = new ArrayList<>();
	}
	public String getRunByUUID() {
		return runByUUID;
	}

	public void setRunByUUID(String runById) {
		this.runByUUID = runById;
	}

	public String getSubmitterTrustId() {
		return submitterTrustId;
	}

	public void setSubmitterTrustId(String userid) {
		this.submitterTrustId = userid;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getSubmitterToken() {
		return submitterToken;
	}

	public void setSubmitterToken(String submitterToken) {
		this.submitterToken = submitterToken;
	}

	public Double getDuration() {
		return duration;
	}

	public void setDuration(Double duration) {
		this.duration = duration;
	}

	public String getResultsFolderURI() {
		return resultsFolderURI;
	}

	public void setResultsFolderURI(String resultFolderURI) {
		this.resultsFolderURI = resultFolderURI;
	}

	public String getSubmitterId()  {
		return submitterId;
	}

	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}

	public String getType() {
		return type;
	}

	private static String getLegacyType(Class<? extends COMPMJobModel> cls) {
		if (cls.equals(RDBJobModel.class)) {
			return "jobm.model.RDBJobModel";
		} else if (cls.equals(COMPMDockerJobModel.class)) {
			return "jobm.model.COMPMDockerJobModel";
		}
		return cls.getSimpleName();
	}

	public void setType(String type) {
		// noop
	}

	public List<RequiredUserVolumeModel> getUserVolumes() {
		return userVolumes;
	}

	public void setUserVolumes(List<RequiredUserVolumeModel> userVolumes) {
		this.userVolumes = userVolumes;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setTimeout(Long jobTimeout) {
		this.timeout = jobTimeout;
	}
}
