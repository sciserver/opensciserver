package org.sciserver.springapp.racm.jobm.controller;

import static java.util.stream.Collectors.toList;
import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.AUTH_HEADER;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.util.TextUtils;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.json.JSONArray;
import org.sciserver.racm.jobm.model.COMPMDockerJobModel;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.COMPMModel;
import org.sciserver.racm.jobm.model.DockerComputeDomainModel;
import org.sciserver.racm.jobm.model.JobQuery;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.jobm.application.COMPMManager;
import org.sciserver.springapp.racm.jobm.application.DockerComputeDomainManager;
import org.sciserver.springapp.racm.jobm.application.JOBM;
import org.sciserver.springapp.racm.jobm.application.JOBMModelFactory;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.ugm.application.UsersAndGroupsManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.sciserver.springapp.racm.utils.http.HttpRequest;
import org.sciserver.springapp.racm.utils.http.HttpResponseResult;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.jhu.job.DockerComputeDomain;
import edu.jhu.job.DockerJob;
import edu.jhu.user.UserGroup;

@CrossOrigin
@RestController
@RequestMapping("jobm/rest")
public class JOBMRESTController {
	public static final String X_SERVICE_ID = "X-Service-Auth-ID";

	private final COMPMManager compmManager;
	private final JOBM jobm;
	private final JsonAPIHelper jsonAPIHelper;
	private final ObjectMapper om;
	private final JOBMModelFactory jobmModelFactory;
	private final DockerComputeDomainManager dockerComputeDomainManager;
	private final UsersAndGroupsManager usersAndGroupsManager;

	@Autowired
	public JOBMRESTController(COMPMManager compmManager, JOBM jobm, JsonAPIHelper jsonAPIHelper,
			JOBMModelFactory jobmModelFactory, DockerComputeDomainManager dockerComputeDomainManager,
			UsersAndGroupsManager usersAndGroupsManager) {
		this.compmManager = compmManager;
		this.jobm = jobm;
		this.om = RACMUtil.newObjectMapper();
		this.jsonAPIHelper = jsonAPIHelper;
		this.jobmModelFactory = jobmModelFactory;
		this.dockerComputeDomainManager = dockerComputeDomainManager;
		this.usersAndGroupsManager = usersAndGroupsManager;
	}

	/**
	 * Return list of "my" jobs with status.<br/>
	 * Use ...?all or ...?open t ask for open jobs or all jobs
	 * filtering, e.g. between two times.
	 *
	 * @return
	 */
	@GetMapping("/jobs")
	public ResponseEntity<JsonNode> queryUserJobs(@RequestParam(required = false) String open,
			@RequestParam(required = false, defaultValue="-1") int top, @RequestParam(required = false) String start,
			@RequestParam(required = false) String end, @AuthenticationPrincipal UserProfile up) {
		try {

			List<COMPMJobModel> jms = jobm.queryUserJobs(up, open != null, top, start, end);
			return jsonAPIHelper.success(jms);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying user jobs", Optional.of(up), e, true);
		}
	}

	/**
	 * Return list of "my" jobs with status.<br/>
	 * Use ...?all or ...?open t ask for open jobs or all jobs
	 * filtering, e.g. between two times.
	 *
	 * @return
	 */
	@GetMapping("/dockerjobs")
	public ResponseEntity<JsonNode> queryUserDockerJobs(@RequestParam(required = false) String open,
			@RequestParam(required = false, defaultValue="-1") int top,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end, @RequestParam(required=false) String labelReg, @AuthenticationPrincipal UserProfile up) {
		try {
			List<COMPMDockerJobModel> jms = jobm.queryUserDockerJobs(up, open != null, top, start, end,labelReg);
			return jsonAPIHelper.success(jms);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying docker jobs", Optional.of(up), e, true);
		}
	}

	/**
	 * Return statistics about jobs finished less than 'since' hours before the present, or that have not yet finished.<br/>
	 * @param since number of hours before current time over which statistics is desired. default=24
	 * @param up
	 * @return
	 */
	@GetMapping("/jobsstats")
	public ResponseEntity<JsonNode> queryUserJobsStats(@RequestParam(required=false) Integer since, @AuthenticationPrincipal UserProfile up) {
		try {
			if(since == null || since < 0) since=24;
			Map<Integer,Integer> js = jobm.queryUserJobsStats(up, since);
			return jsonAPIHelper.success(js);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying jobs stats", Optional.of(up), e, true);
		}
	}
	@GetMapping("/dockerjobsold")
	public ResponseEntity<JsonNode> queryUserDockerJobsOld(@RequestParam(required = false) String open,
			@RequestParam(required = false, defaultValue="-1") int top,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end, @AuthenticationPrincipal UserProfile up) {
		try {
			List<COMPMDockerJobModel> jms = jobm.queryUserDockerJobs(up, open != null, top, start, end);
			return jsonAPIHelper.success(jms);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying docker jobs", Optional.of(up), e, true);
		}
	}

	
	@GetMapping("/dockerjobs/quick")
	public ResponseEntity<JsonNode> queryUserDockerJobsNative(@RequestParam(required = false) String open,
			@RequestParam(required = false, defaultValue="-1") int top,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end, @RequestParam(required=false) String labelReg, @AuthenticationPrincipal UserProfile up) {
		try {
			NativeQueryResult jms = jobm.queryUserDockerJobsNative(up, open != null, top, start, end, labelReg);
			return jsonAPIHelper.success(jms);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying docker jobs (natively)", Optional.of(up), e, true);
		}
	}
	/**
	 * Return list of "my" rdb jobs with status.<br/>
	 * Use ...?open t ask for only open jobs
	 * filtering, e.g. between two times.
	 *
	 */
	@GetMapping("/rdbjobs")
	public ResponseEntity<JsonNode> queryUserRdbJobs(
			@RequestParam(required = false) String open,
			@RequestParam(defaultValue = "-1", required = false) int top,
			@RequestParam(required = false) String start,
			@RequestParam(required = false) String end,
			@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(
					jobm.queryUserRDBJobs(up, open != null, top, start, end)
					);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error obtaining rdb jobs", Optional.of(up), e, true);
		}
	}

	@PostMapping("/jobs/query")
	public ResponseEntity<JsonNode> queryJob(
			@RequestBody JobQuery query,
			@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(
					jobm.performJobQuery(up, query));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying jobs", Optional.of(up), e, true);
		}
	}

	/**
	 * Return info about all queues for all domains.<br/>
	 * Current jobs, pending jobs and locaton where a job submitted now by the specified user would end up in the queue.
	 * @param compmuuid
	 * @param up
	 * @return
	 */
    @GetMapping("/jobs/queues")
    public NativeQueryResult queryJobsQueues(@AuthenticationPrincipal UserProfile up) {
        return jobm.queryJobsQueues(up);
    }
	/**
	 * Return status of specified Job.<br/>
	 * Only if user is allowed
	 *
	 * @param jobId
	 * @return
	 */
	@GetMapping("/jobs/{jobId}")
	public ResponseEntity<JsonNode> jobStatus(@PathVariable Long jobId, @AuthenticationPrincipal UserProfile up) {
		COMPMJobModel jm = jobm.queryUserJob(jobId, up);
		JsonNode json = om.valueToTree(jm);

		LogUtils.buildLog()
			.forJOBM()
			.user(up)
			.sentence()
				.subject(up.getUsername())
				.verb("viewed")
				.predicate("job %d", jm.getId())
			.extraField("job", jm.getId())
			.log();
		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	@PostMapping("/jobs/{jobId}/cancel")
	public ResponseEntity<JsonNode> cancelJob(@PathVariable Long jobId, @AuthenticationPrincipal UserProfile up)
			throws VOURPException {
		try {
			COMPMJobModel jm = jobm.cancelUserJob(jobId, up);

			LogUtils.buildLog()
				.forJOBM()
				.user(up)
				.showInUserHistory()
				.sentence()
					.subject(up.getUsername())
					.verb("canceled")
					.predicate("job %d", jm.getId())
				.extraField("job", jm.getId())
				.log();
			JsonNode json = om.valueToTree(jm);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (VOURPException e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error canceling job", Optional.of(up), e, true);
		}
	}

	/**
	 * Return information about the docker compute domain where a user can at least
	 * access one docker image.<br/>
	 * Return the visible images, volume containers and uservolumes that can be
	 * mountes on the compute domain for the user.
	 *
	 * @param batch
	 * @param interactive
	 * @param request
	 * @param response
	 * @return
	 * @throws VOURPException
	 */
	@GetMapping("/computedomains")
	public ResponseEntity<JsonNode> queryComputeDomains(@RequestParam(required = false) String batch,
			@RequestParam(required = false) String interactive, @AuthenticationPrincipal UserProfile up) {
		try {
			// rule: if batch is not invluded, only interactive.
			// if batch is included, only include interactive if explicitly requested
			boolean includeBatch = (batch != null && "true".equals(batch));
			boolean includeInteractive = !includeBatch || (interactive != null && "true".equals(interactive));
			Collection<UserDockerComputeDomainModel> jms = dockerComputeDomainManager.queryUserDockerComputeDomains(up,
					includeBatch, includeInteractive);

			JsonNode json = om.valueToTree(jms);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying compute domains", Optional.of(up), e);
		}
	}
	/**
	 * Return an admin view of all the registered DockerComputeDomains.<br/>
	 * User must be an admin to be allowed to access this information.
	 * @param up
	 * @return
	 */
	@GetMapping("/dockercomputedomains")
	public ResponseEntity<JsonNode> queryDockerComputeDomains(@AuthenticationPrincipal UserProfile up) {
		try {
			if(!up.isAdmin())
				throw new InsufficientPermissionsException("Illegal request made for admin information about Docker Compute Domains.");
			List<DockerComputeDomainModel> dcdms = dockerComputeDomainManager.queryDockerComputeDomains(up);

			JsonNode json = om.valueToTree(dcdms);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying docker compute domains", Optional.of(up), e);
		}
	}

	/**
	 *
	 * @param body
	 * @param admin
	 *            if set, give admin privileges to the groups in the comma-separated
	 *            value
	 * @param read
	 *            if set, give read privileges to the groups in the comma-separated
	 *            value
	 * @param request
	 * @param response
	 * @return
	 * @throws VOURPException
	 */
	@PostMapping("/computedomains/docker")
	public ResponseEntity<JsonNode> registerDockerComputeDomain(@RequestBody String body,
			@RequestParam(required = false) String admins, @AuthenticationPrincipal UserProfile up) {
		try {
			jobm.buildTrustIfNeeded(up.getUser(), up.getToken());
			DockerComputeDomainModel dcdm;
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);

			if (node != null) {
				dcdm = mapper.convertValue(node, DockerComputeDomainModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "No valid Json posted to groups endpoint");
			}

			TransientObjectManager tom = up.getTom();
			UserGroup[] adminGroups = usersAndGroupsManager.findGroups(admins, tom);

			DockerComputeDomain dcd = dockerComputeDomainManager.manageDockerComputeDomain(dcdm, up, adminGroups);
			tom.persist();

			dcdm = jobmModelFactory.newDockerComputeDomainModel(dcd, true);
			JsonNode json = om.valueToTree(dcdm);

			LogUtils.buildLog()
				.forJOBM()
				.user(up)
				.showInUserHistory()
				.sentence()
					.subject(up.getUsername())
					.verb("registered")
					.predicate("docker compute domain '%s'", dcdm.getName())
				.extraField("dockerComputeDomain", dcdm.getId())
				.log();
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error register a docker compute domain", Optional.of(up), e, true);
		}
	}

	/**
	 * Submit a docker job
	 *
	 * @param jobModel
	 * @param request
	 * @param response
	 * @return
	 */
	@PostMapping("/jobs/docker")
	public ResponseEntity<JsonNode> submitJob(@RequestBody String body, @AuthenticationPrincipal UserProfile up) {
        String username = up==null?"null":up.getUsername();
		try {
			jobm.buildTrustIfNeeded(up.getUser(), up.getToken());
		} catch(Exception e) {
            return jsonAPIHelper.logAndReturnJsonExceptionEntity(
                    "Error retrieving trust ID for user "+username, Optional.of(up), e, true);
		}		    

		try {
			// every user can in principle *try* to submit a job.
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);
			COMPMDockerJobModel jobModel = null;

			if (node != null) {
				jobModel = mapper.convertValue(node, COMPMDockerJobModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
						"Invalid Json posted to /jobs/docker endpoint");
			}
			
			DockerJob job = null;
			
			try {
			    job = jobm.newDockerJob(jobModel, up);
			} catch(Exception e) {
			    throw new Exception("Error creating DockerJob\n"+e.getMessage(),e);
			}
			COMPMJobModel jm = jobmModelFactory.newCOMPMJobModel(job, true);

			String jobDescription;
			if (!TextUtils.isEmpty(job.getPublisherDID())) {
				jobDescription = job.getPublisherDID();
			} else if (!TextUtils.isEmpty(job.getScriptURI())) {
				jobDescription = "notebook '" + job.getScriptURI() + "'";
			} else {
				jobDescription = "'" + job.getCommand() + "'";
			}

			LogUtils.buildLog()
				.forJOBM()
				.user(up)
				.showInUserHistory()
				.sentence()
					.subject(up.getUsername())
					.verb("submitted")
					.predicate(jobDescription)
				.extraField("userVolumes", new JSONArray(
						job.getUserVolume().stream()
							.map(uv -> uv.getUserVolume().getId()).collect(toList())
				))
				.extraField("volumeContainers", new JSONArray(
						job.getRequiredVolume().stream()
							.map(vc -> vc.getVolume().getId()).collect(toList())
				))
				.extraField("computeDomain", job.getComputeDomain().getId())
				.extraField("job", job.getId())
				.extraField("image", job.getImage().getId())
				.log();
			return jsonAPIHelper.success(jm);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error in job submission for user "+username+"\n\t"+e.getMessage(), Optional.of(up), e, true);
		}
	}

	/**
	 * A COMPM registers itself. Must do so with a valid token of a user with the
	 * right to register COMPMs. Will be assigned a new UUID if it does not include
	 * this in the registration. Will be known by that uuid from then on and MUST
	 * use it in future registrations. If the COMPM had been registered before, the
	 * call will return a JSON message with the jobs owned by the COMPM that are
	 * still outstanding.
	 *
	 * @param request
	 * @return
	 */
	@PostMapping("/compm/register")
	public ResponseEntity<JsonNode> registerCOMPM(
			@AuthenticationPrincipal UserProfile up,
			@RequestBody COMPMModel compmModel) {
		try {
			COMPMModel cm = compmManager.createCOMPM(compmModel, up);

			LogUtils.buildLog()
				.forJOBM()
				.user(up)
				.showInUserHistory()
				.sentence()
					.subject(up.getUsername())
					.verb("registered")
					.predicate("compm '%s'", cm.getDescription())
				.extraField("compm", cm.getId())
				.extraField("computeDomain", cm.getComputeDomainId())
				.log();
			return jsonAPIHelper.success(cm);
		} catch (VOURPException e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error registering a compm", Optional.of(up), e, true);
		}
	}

	/**
	 * Return list of "casjobs" jobs<br/>
	 *
	 * @return
	 */
	@GetMapping("/casjobs")
	public ResponseEntity<JsonNode> queryUserCasJobs(@RequestParam(required = false) String submittedFrom,
			@RequestParam(required = false) String submittedTo, @AuthenticationPrincipal UserProfile up) {
		try {
			String token = up.getToken();
			HttpRequest casJobsRequest = new HttpRequest();
			HttpResponseResult casJobsResult;

			Map<String, String> extraHeaderFields = new HashMap<>();
			extraHeaderFields.put(AUTH_HEADER, token);

			String queryString = "submittedFrom=" + submittedFrom + "&submittedTo=" + submittedTo;

			casJobsResult = casJobsRequest.executeGet("http://skyserver.sdss.org/CasJobs/RestApi/jobs?" + queryString,
					extraHeaderFields);

			if (casJobsResult != null && casJobsResult.getResponseCode() >= 200 && casJobsResult.getResponseCode() <= 299) {
				LogUtils.buildLog()
					.forJOBM()
					.user(up)
					.sentence()
						.subject(up.getUsername())
						.verb("queried")
						.predicate("CasJobs")
					.log();

				String jsonString = casJobsResult.getMessage();
				JsonNode jsonNode = null;

				if (jsonString != null && !jsonString.isEmpty()) {
					jsonNode = om.readTree(jsonString);
				}
				return new ResponseEntity<>(jsonNode, HttpStatus.OK);
			} else {
				ObjectNode on = om.createObjectNode();
				on.put("status", "error");
				if(casJobsResult != null)
					on.put("error", casJobsResult.getMessage());
				return new ResponseEntity<>(on,
						casJobsResult != null ? HttpStatus.valueOf(casJobsResult.getResponseCode()) : HttpStatus.NOT_FOUND);
			}

		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying casjobs jobs", Optional.of(up), e, true);
		}
	}

}
