package org.sciserver.springapp.racm.jobm.application;

import java.io.IOException;

import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class COMPMTokenUtils {
	private final JOBM jobm;

	@Autowired
	public COMPMTokenUtils(JOBM jobm) {
		this.jobm = jobm;
	}

	public void checkSubmitterToken(String token, COMPMJobModel jobModel) throws IOException {
		if (jobModel.getSubmitterToken() == null) {
			if (token == null || token.trim().length() == 0) {
				// create new token from trustid
				try {
					token = jobm.getTrustToken(jobModel.getSubmitterTrustId());
				} catch (IOException e) {
					throw new IOException(String.format("Error retrieving token for trustId=%s: %s",
							jobModel.getSubmitterTrustId(), e.getMessage()), e);
				}
			}
			jobModel.setSubmitterToken(token);
		}
	}
}