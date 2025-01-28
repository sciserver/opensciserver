package org.sciserver.springapp.racm.jobm.controller;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ivoa.dm.VOURPException;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.springapp.racm.jobm.application.COMPMRequired;
import org.sciserver.springapp.racm.jobm.application.COMPMTokenUtils;
import org.sciserver.springapp.racm.jobm.application.JOBMModelFactory;
import org.sciserver.springapp.racm.jobm.application.COMPMRequiredInjector.COMPMInfo;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@CrossOrigin
@RestController
@COMPMRequired
@RequestMapping("jobm/rest")
public class RDBJobForCOMPMController {
	private static final Logger LOG = LogManager.getLogger();

	private final JsonAPIHelper jsonAPIHelper;
	private final COMPMTokenUtils compmTokenUtils;
	private final JOBMModelFactory jobmModelFactory;
	@Autowired
	public RDBJobForCOMPMController(JsonAPIHelper jsonAPIHelper, COMPMTokenUtils compmTokenUtils,
			JOBMModelFactory jobmModelFactory) {
		this.jsonAPIHelper = jsonAPIHelper;
		this.compmTokenUtils = compmTokenUtils;
		this.jobmModelFactory = jobmModelFactory;
	}
	/**
	 *
	 * Status plus potentially messages goes into JSON body.
	 *
	 * @param jobId
	 * @param request
	 * @return
	 */
	@PostMapping("/rdbjob/{jobId}")
	public ResponseEntity<JsonNode> updateRDBJobStatus(@PathVariable Long jobId,
			@RequestBody String body,
			@ModelAttribute COMPMInfo compm) {
		RDBJobModel jobModel = null;
		try {
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);

			if (node != null) {
				jobModel = mapper.convertValue(node, RDBJobModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "Invalid Json posted to /rdbjob endpoint");
			}
			if (jobModel != null) {
				String token = jobModel.getSubmitterToken(); // need to remember this, for VO-URP JOb does not have a
																// token, but COMPM needs it still
				RDBJobModel newJobModel = jobmModelFactory.updateRDBJob(jobModel, compm.id());
				if (LOG.isInfoEnabled())
					LOG.info(String.format("JobStatus updated to '%s' for jobid %d", newJobModel.getStatus(),
							newJobModel.getId()));
				compmTokenUtils.checkSubmitterToken(token, jobModel);
			}
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					String.format(
							"Failed to update JobStatus for jobid %s",
							(jobModel == null ? "UNKNOWN" : jobModel.getId())),
						Optional.empty(),
						e, true
			);
		}
		return jsonAPIHelper.success(jobModel);
	}
}
