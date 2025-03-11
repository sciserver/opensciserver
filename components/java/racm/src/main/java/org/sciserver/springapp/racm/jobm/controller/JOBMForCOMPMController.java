package org.sciserver.springapp.racm.jobm.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.jobm.model.COMPMDockerJobModel;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.JobStatus;
import org.sciserver.springapp.racm.jobm.application.COMPMRequired;
import org.sciserver.springapp.racm.jobm.application.COMPMTokenUtils;
import org.sciserver.springapp.racm.jobm.application.JOBM;
import org.sciserver.springapp.racm.jobm.application.JOBMModelFactory;
import org.sciserver.springapp.racm.jobm.application.COMPMRequiredInjector.COMPMInfo;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.jhu.job.COMPM;
import edu.jhu.job.Job;

@CrossOrigin
@RestController
@COMPMRequired
@RequestMapping("jobm/rest")
public class JOBMForCOMPMController {
	public static final String X_SERVICE_ID = "X-Service-Auth-ID";
	private static final Logger LOG = LogManager.getLogger();
	private final ObjectMapper om = RACMUtil.newObjectMapper();
	private final JsonAPIHelper jsonAPIHelper;
	private final COMPMTokenUtils tokenUtils;
	private final JOBM jobm;
	private final JOBMModelFactory jobmModelFactory;
	private final VOURPContext vourpContext;

	@Autowired
	public JOBMForCOMPMController(JsonAPIHelper jsonAPIHelper, COMPMTokenUtils tokenUtils, JOBM jobm,
			JOBMModelFactory jobmModelFactory, VOURPContext vourpContext) {
		this.jsonAPIHelper = jsonAPIHelper;
		this.tokenUtils = tokenUtils;
		this.jobm = jobm;
		this.jobmModelFactory = jobmModelFactory;
		this.vourpContext = vourpContext;
	}

	/**
	 *
	 * Status plus potentially messages goes into JSON body.
	 *
	 * @param jobId
	 * @param request
	 * @return
	 */
	@PostMapping("/compmdockerjob/{jobId}")
	public ResponseEntity<JsonNode> updateDockerJobStatus(@PathVariable Long jobId, @RequestBody String body,
			@ModelAttribute COMPMInfo compm) {
		COMPMDockerJobModel jobModel = null;
		try {
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);

			if (node != null) {
				jobModel = mapper.convertValue(node, COMPMDockerJobModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
						"Invalid Json posted to /compmdockerjob endpoint");
			}
			if (jobModel != null) {
				String token = jobModel.getSubmitterToken(); // need to remember this, for VO-URP JOb does not have a
																// token, but COMPM needs it still
				COMPMDockerJobModel newJobModel = jobmModelFactory.updateDockerJob(jobModel, compm.id());
				LOG.info("JobStatus updated to '{}' for jobid {}", newJobModel.getStatus(),
						newJobModel.getId());
				tokenUtils.checkSubmitterToken(token, jobModel);
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
		JsonNode j = om.valueToTree(jobModel);
		return new ResponseEntity<>(j, HttpStatus.OK);
	}

	/**
	 * Returns a Json message concerning whether X-Service-Auth-ID field in the
	 * request header corresponds to a valid registered COMPM. Returns a 200 code is
	 * it is, or a 403 code if it's not.<br/>
	 *
	 * @return
	 */
	@GetMapping("/compm/checkId")
	public ResponseEntity<JsonNode> checkServiceId(@ModelAttribute COMPMInfo compm) {
		Map<String, String> h = new HashMap<>();
		JsonNode json = null;
		if (compm == null) {
			h.put("Message", "COMPM X-Service-Auth-ID is not authorized");
			json = om.valueToTree(h);
			return new ResponseEntity<>(json, HttpStatus.FORBIDDEN);
		} else {
			h.put("Message", "COMPM X-Service-Auth-ID is authorized");
			return jsonAPIHelper.success(h);
		}
	}

	/**
	 * Return status of specified Job.<br/>
	 * Only if compm is allowed
	 *
	 * @param jobId
	 * @return
	 */
	@GetMapping("/compmjob/{jobId}")
	public ResponseEntity<JsonNode> compmJobStatus(@PathVariable Long jobId,
			@ModelAttribute COMPMInfo compm) {
		COMPMJobModel jm = jobm.queryCOMPMJob(jobId, compm.id());
		JsonNode json = om.valueToTree(jm);
		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	/**
	 *
	 * Status plus potentially messages goes into JSON body.
	 *
	 * @param jobId
	 * @param request
	 * @return
	 */
	@PostMapping("/compmjob/{jobId}")
	public ResponseEntity<JsonNode> updateJobStatus(@PathVariable Long jobId, @RequestBody COMPMJobModel jobModel,
			@ModelAttribute COMPMInfo compm) {
		try {
			if (jobModel != null) {
				String token = jobModel.getSubmitterToken(); // need to remember this before the next line, for VO-URP
																// Job does not have a token, but COMPM needs it still
				jobModel = jobmModelFactory.updateJob(jobModel, compm.id());
				tokenUtils.checkSubmitterToken(token, jobModel);
			}
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Failed to update job status",
					Optional.empty(),
					e, true
			);
		}
		return jsonAPIHelper.success(jobModel);
	}

	@GetMapping("/compmjobs/canceled")
	public ResponseEntity<JsonNode> queryCanceledCOMPMJobs(@ModelAttribute COMPMInfo compm) {
		try {
			Long[] jobIds = jobm.queryCanceledCOMPMJobs(compm.uuid());

			return jsonAPIHelper.success(jobIds);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Failed getting canceled jobs", Optional.empty(),
					e, HttpStatus.BAD_REQUEST, true);
		}
	}

	@GetMapping("/compmjobs/open")
	public ResponseEntity<JsonNode> queryOpenCOMPMJobs(@ModelAttribute COMPMInfo compm) {
		try {
			List<COMPMJobModel> jobs = jobm.queryOpenCOMPMJobs(compm.id());

			for (COMPMJobModel job : jobs) {
				attemptCreatingNewToken(job.getSubmitterTrustId())
					.ifPresent(job::setSubmitterToken);
			}
			return jsonAPIHelper.success(jobs);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Failed getting compm jobs", Optional.empty(),
					e, HttpStatus.BAD_REQUEST, true);
		}
	}

	private Optional<String> attemptCreatingNewToken(String submitterTrustId) {
		try {
			return Optional.of(jobm.getTrustToken(submitterTrustId));
		} catch (Exception e) {
			LogUtils.buildLog()
				.logError()
				.forJOBM()
				.errorText("Unable to obtain trust token")
				.exception(e)
				.log();
			return Optional.empty();
		}
	}

	/**
	 * Get a new job for the COMPM instance identified by the X_SERVICE_ID. MAY use
	 * ...?jobType=<job-type-name> to ask for specific job.
	 *
	 * @param request
	 * @return
	 * @throws VOURPException
	 * @todo add specific call for dockerjob rather than generic jobs.
	 */
	@GetMapping("/compmjob/next")
	public ResponseEntity<JsonNode> nextJob(@RequestParam(required = false) Short maxNumberOfJobs,
			@ModelAttribute COMPMInfo compm) throws VOURPException {
		TransientObjectManager tom = vourpContext.newTOM();
		if (maxNumberOfJobs == null || maxNumberOfJobs < 1)
			maxNumberOfJobs = 1;
		List<Job> js = jobm.nextJobs(compm.uuid(), compm.defaultJobTimeout(),
				compm.defaultJobsPerUser(), maxNumberOfJobs.shortValue());
		List<COMPMJobModel> jms = new ArrayList<>();
		if (js != null) {
			for (Job j : js) {
				String token = null;
				String trustId = j.getSubmitter().getTrustId();
				try {
					token = jobm.getTrustToken(trustId);
				} catch (Exception e) {
					return jsonAPIHelper.logAndReturnJsonExceptionEntity(
							String.format("Error retrieving token for trustId=%s", trustId),
							Optional.empty(), e);
				}
				if (StringUtils.isEmpty(token)) {
					return jsonAPIHelper.logAndReturnJsonExceptionEntity(
						String.format("Cannot find token for trustId=%s", trustId),
						Optional.empty(), new Exception());
				}

				j.setStatus(JobStatus.STATUS_QUEUED);
				j.setTimeout(compm.defaultJobTimeout());
				j.setRunBy(tom.find(COMPM.class, compm.id()));

				COMPMJobModel jm = jobmModelFactory.newCOMPMJobModel(j, true);
				jm.setSubmitterToken(token);
				jms.add(jm);
			}
			try {
				if (!js.isEmpty())
					tom.persist();
			} catch (Exception e) {
				return jsonAPIHelper.logAndReturnJsonExceptionEntity(
						"Error when saving changes to jobs in nextJob call",
						Optional.empty(), new Exception(), true);
			}
			return jsonAPIHelper.success(jms);
		}
		return null;
	}
}
