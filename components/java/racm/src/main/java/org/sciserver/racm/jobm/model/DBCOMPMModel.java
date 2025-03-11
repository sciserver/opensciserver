package org.sciserver.racm.jobm.model;

import java.util.List;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class DBCOMPMModel extends RACMBaseModel{

	private String uuid;
	private String description;
	private String creatorUsername;
	private String creatorUserid;
	private String label;
    private long defaultJobTimeout = 8L * 60 * 60;
    private long defaultJobsPerUser = 1;
    
	private List<RDBJobModel> outstandingJobs;
	private Long rdbComputeDomainId;
	public DBCOMPMModel(){
	}

	public DBCOMPMModel(String id){
		super(id);
	}
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<RDBJobModel> getOutstandingJobs() {
		return outstandingJobs;
	}

	public void setOutstandingJobs(List<RDBJobModel> outstandingJobs) {
		this.outstandingJobs = outstandingJobs;
	}



	public String getCreatorUsername() {
		return creatorUsername;
	}

	public void setCreatorUsername(String creator) {
		this.creatorUsername = creator;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Long getRdbComputeDomainId() {
		return rdbComputeDomainId;
	}

	public void setComputeDomain(Long computeDomain) {
		this.rdbComputeDomainId = computeDomain;
	}

	public String getCreatorUserid() {
		return creatorUserid;
	}

	public void setCreatorUserid(String createrUserid) {
		this.creatorUserid = createrUserid;
	}

    public long getDefaultJobTimeout() {
        return defaultJobTimeout;
    }

    public void setDefaultJobTimeout(long defaultJobTimeout) {
        this.defaultJobTimeout = defaultJobTimeout;
    }

    public long getDefaultJobsPerUser() {
        return defaultJobsPerUser;
    }

    public void setDefaultJobsPerUser(long defaultJobsPerUser) {
        this.defaultJobsPerUser = defaultJobsPerUser;
    }}
