package org.sciserver.springapp.racm.jobm.controller;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.DBCOMPMModel;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.springapp.racm.jobm.application.JOBM;
import org.sciserver.springapp.racm.jobm.application.JOBMModelFactory;
import org.sciserver.springapp.racm.jobm.application.RDBDomainManager;
import org.sciserver.springapp.racm.ugm.application.UsersAndGroupsManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.jhu.job.COMPM;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.RDBComputeDomain;
import edu.jhu.job.RDBJob;
import edu.jhu.user.UserGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin
@RestController
@RequestMapping("jobm/rest")
@Tag(name = "Relational database jobs",
     description = "Submit and query SQL jobs against registered database contexts.")
public class RDBJobRESTController {

	public static final String X_SERVICE_ID = "X-Service-Auth-ID";
	private final ObjectMapper om;
	private final JOBM jobm;
	private final JsonAPIHelper jsonAPIHelper;
	private final RDBDomainManager rdbDomainManager;
	private final JOBMModelFactory jobmModelFactory;
	private final UsersAndGroupsManager usersAndGroupsManager;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public RDBJobRESTController(JOBM jobm, JsonAPIHelper jsonAPIHelper, RDBDomainManager rdbDomainManager,
			JOBMModelFactory jobmModelFactory, UsersAndGroupsManager usersAndGroupsManager) {
		this.om = RACMUtil.newObjectMapper();
		this.jobm = jobm;
		this.jsonAPIHelper = jsonAPIHelper;
		this.rdbDomainManager = rdbDomainManager;
		this.jobmModelFactory = jobmModelFactory;
		this.usersAndGroupsManager = usersAndGroupsManager;
	}

	/**
	 * Submit a rdb job
	 *
	 * @param jobModel
	 * @param request
	 * @param response
	 * @return
	 * @todo use injector pattern to check for proper computedomain?
	 */
	@Operation(
	    summary = "Submit a relational database job.",
	    description = "Queues a SQL job against a registered database context. The caller must be "
	                  + "permitted to query that context.",
	    parameters = {
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
	                   description = "Service token of the service submitting on a user's behalf. "
                    + "Omit for an ordinary user request.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "RDBJobModel as JSON, naming the database context and the "
	                                 + "SQL to run."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The submitted job, as RACM now holds it, including the identifier the "
	                               + "caller uses to follow its progress."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller may not query the named database context."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/jobs/rdb")
	public ResponseEntity<JsonNode> submitJob(@RequestBody String body, @AuthenticationPrincipal UserProfile up, 
	        @RequestHeader(name = X_SERVICE_ID,required = true) String serviceAccountToken) {
		RDBJobModel jobModel = null;
		try {
	        RDBComputeDomain rdbcd = rdbDomainManager.getRDBComputeDomain(serviceAccountToken);
			jobm.buildTrustIfNeeded(up.getUser(), up.getToken());
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);
			if (node != null) {
				jobModel = mapper.convertValue(node, RDBJobModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "No valid Json posted to /jobs/rdb endpoint");
			}
			// TODO decide whether the rdbcomputedomainid should even be provided by the jobs submission. the serviceaccount should suffice.
			if(rdbcd == null || !rdbcd.getId().equals(jobModel.getRdbDomainId()))
                throw new VOURPException(VOURPException.UNAUTHORIZED, "This job must have a service account corresponding to the domain that is targeted");

			RDBJob job = jobm.newRDBJob(jobModel, up);
			COMPMJobModel jm = jobmModelFactory.newCOMPMJobModel(job, true);

			LogUtils.buildLog()
				.forJOBM()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("submitted")
					.predicate("'%s' on '%s'", job.getSql(), job.getDatabaseContext().getName())
				.extraField("job", job.getId())
				.log();
			return jsonAPIHelper.success(jm);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error in creating an rdb job", Optional.of(up), e, true);
		}
	}

	/**
	 *
	 * @param body
	 * @param admins
	 *            if set, give admin privileges to the groups in the comma-separated
	 *            value
	 * @return
	 * @throws VOURPException
	 */
	@Operation(
	    summary = "Register a relational database compute domain, or replace an existing one.",
	    description = "Registers a domain and its database contexts. As with the docker "
	                  + "equivalent, replacing an existing domain is destructive: database "
	                  + "contexts absent from the body are deleted.",
	    parameters = {
	        @Parameter(name = "admins", in = ParameterIn.QUERY,
	                   description = "Comma-separated group names to be granted administrator "
                    + "rights over the domain's database contexts.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "RDBComputeDomainModel as JSON. Include the id and racmUUID "
                    + "to replace an existing domain; omit both to register a new one."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The compute domain as registered or replaced, including the identifier "
	                               + "RACM assigned it and the database contexts it now holds."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller is not permitted to register a compute domain."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/computedomains/rdb")
	public ResponseEntity<JsonNode> registerRDBComputeDomain(@RequestBody String body,
			@RequestParam(required = false) String admins, @AuthenticationPrincipal UserProfile up)
			throws VOURPException {
		RDBComputeDomainModel rcdm;
		try {
			jobm.buildTrustIfNeeded(up.getUser(), up.getToken());
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);

			if (node != null) {
				rcdm = mapper.convertValue(node, RDBComputeDomainModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
						"No valid JSON posted to /computedomains/rdb endpoint");
			}

			TransientObjectManager tom = up.getTom();
			UserGroup[] adminGroups = usersAndGroupsManager.findGroups(admins, tom);

			RDBComputeDomain dcd = rdbDomainManager.manageRDBComputeDomain(rcdm, up, adminGroups);
			tom.persist();

			rcdm = jobmModelFactory.newRDBComputeDomainModel(dcd, true);
			LogUtils.buildLog()
				.forJOBM()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("registered")
					.predicate("rdb compute domain '%s'", rcdm.getName())
				.extraField("rdbComputeDomain", rcdm.getId())
				.log();
			return jsonAPIHelper.success(rcdm);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
				"Error registering an RDB compute domain", Optional.of(up), e, true);
		}
	}

	/**
	 * register a COMPM for an RDBComputeDomain.<br/>
	 *
	 * @param body
	 * @param request
	 * @param response
	 * @return
	 * @throws VOURPException
	 */
	@Operation(
	    summary = "Register a compute domain manager for a relational database domain.",
	    description = "Registers the COMPM that will run jobs for an RDB compute domain. A COMPM "
	                  + "that has registered before must send the uuid it was given.",
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "DBCOMPMModel as JSON, including the COMPM's uuid when "
	                                 + "re-registering."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The compute domain manager as registered, including the identifier RACM "
	                               + "assigned it."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller is not permitted to register a COMPM."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/dbcompm")
	public ResponseEntity<JsonNode> registerDBCOMPM(@RequestBody String body, HttpServletRequest request,
			@AuthenticationPrincipal UserProfile up) throws VOURPException {
		String serviceId = request.getHeader(X_SERVICE_ID);
		DBCOMPMModel dbcm;
		try {
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);

			if (node != null) {
				dbcm = mapper.convertValue(node, DBCOMPMModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "No valid JSON posted to /dbcompm endpoint");
			}

			TransientObjectManager tom = up.getTom();

			COMPM compm = rdbDomainManager.manageDBCOMPM(serviceId, dbcm, up);
			tom.persist();

			DBCOMPMModel compmm = jobmModelFactory.newDBCOMPMModel(compm);
			JsonNode json = om.valueToTree(compmm);

			LogUtils.buildLog()
				.forJOBM()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("registered")
					.predicate("rdb compm for compute dodmain '%s'", compm.getComputeDomain().getName())
				.extraField("compm", compm.getId())
				.log();
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error registering a dbcompm", Optional.of(up), e, true);
		}
	}

	@Operation(
	    summary = "List the relational database compute domains available to the caller.",
	    description = "Returns each RDB compute domain with the database contexts the caller may "
	                  + "query, so a client can offer the user somewhere to send a job.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The relational database compute domains visible to the caller, each with "
	                               + "the database contexts the caller may use."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/computedomains/rdb")
	public ResponseEntity<JsonNode> queryRDBComputeDomains(@AuthenticationPrincipal UserProfile up)
			throws VOURPException {
		try {
			List<RDBComputeDomainModel> jms = rdbDomainManager.queryVisibleRDBComputeDomains(up);

			return jsonAPIHelper.success(jms);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
					"Error querying RDB compute domains", Optional.of(up), e, true);
		}
	}

	/**
	 *
     * @param domainId  RACM ID of the RDBComputeDomain to which the DatabaseContext is to be added
	 * @param body  JSON representation of a DatabaseContext to be added to the specified domain
	 * @param admins  if set, give admin privileges to the groups in the comma-separated value
     * @param up  representation of the user submitting the requested DatabaseContext
	 * @return
	 * @throws VOURPException
	 */
	@Operation(
	    summary = "Add a database context to an existing relational database compute domain.",
	    description = "Registers one database context on the named domain without resending the "
	                  + "whole domain document, so the domain's other contexts are left "
	                  + "untouched.",
	    parameters = {
	        @Parameter(name = "domainId", in = ParameterIn.PATH,
	                   description = "Identifier of the RDB compute domain the context is added "
	                                 + "to."),
	        @Parameter(name = "admins", in = ParameterIn.QUERY,
	                   description = "Comma-separated group names to be granted administrator "
                    + "rights over the new database context.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "DatabaseContextModel as JSON, describing the context to "
	                                 + "add."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The database context as registered, including the identifier RACM assigned "
	                               + "it."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller is not permitted to modify this compute domain."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("/computedomains/rdb/{domainId}")
	public ResponseEntity<JsonNode> registerRDBComputeDbContext(@PathVariable Long domainId, @RequestBody String body,
			@RequestParam(required = false) String admins, @AuthenticationPrincipal UserProfile up)
			throws VOURPException {
		DatabaseContextModel dbcm = null;

        try {
			jobm.buildTrustIfNeeded(up.getUser(), up.getToken());
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ObjectNode node = mapper.readValue(body, ObjectNode.class);

			if (node != null) {
				dbcm = mapper.convertValue(node, DatabaseContextModel.class);
			} else {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
						"No valid JSON posted to /computedomains/rdb/<domain_id> endpoint");
			}

			TransientObjectManager tom = up.getTom();
			UserGroup[] adminGroups = usersAndGroupsManager.findGroups(admins, tom);

            DatabaseContext dbc = rdbDomainManager.addDbContextToRDBComputeDomain(domainId, dbcm, up, adminGroups);
            
			dbcm = jobmModelFactory.newDatabaseContextModel(dbc);
			LogUtils.buildLog()
				.forJOBM()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("registered")
					.predicate("rdb database context '%s'", dbcm.getName())
				.extraField("rdbDatabaseContext", dbcm.getId())
				.log();
			return jsonAPIHelper.success(dbcm);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(
				"Error registering an rdb database context", Optional.of(up), e, true);
		}
	}

}
