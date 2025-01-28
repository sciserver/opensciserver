package org.sciserver.racm.jobm.model;

import java.util.Collections;
import java.util.List;

public class JobQueryResult {
	private List<COMPMJobModel> jobs;

	public JobQueryResult(List<COMPMJobModel> jobs) {
		this.jobs = Collections.unmodifiableList(jobs);
	}

	public List<COMPMJobModel> getJobs() {
		return jobs;
	}

}
