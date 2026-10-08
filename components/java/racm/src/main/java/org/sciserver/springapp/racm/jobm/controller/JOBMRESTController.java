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
import org.sciserver.racm.jobm.model.RootVolumeOnComputeDomainModel;
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
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
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
import edu.jhu.job.RootVolumeOnComputeDomain;
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
@Tag(name = "Jobs and compute domains",
     description = "Submit and query jobs, and discover the compute domains and images available "
                 + "to the caller.")
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
    @Operation(
        summary = "List the caller's jobs, optionally only open ones or those within a time "
                  + "range.",
        description = "Returns every job the caller has submitted, as COMPMJobModel entries "
                      + "covering both docker and relational database jobs.",
        parameters = {
            @Parameter(name = "open", in = ParameterIn.QUERY,
                       description = "Present with any value to return only jobs that have not "
                                     + "yet finished. Omit to return all."),
            @Parameter(name = "top", in = ParameterIn.QUERY,
                       description = "Maximum number of jobs to return. -1, the default, returns "
                                     + "all of them."),
            @Parameter(name = "start", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted at or after this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no lower bound is "
                                     + "applied."),
            @Parameter(name = "end", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted strictly before this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no upper bound is "
                                     + "applied.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The caller's jobs, both docker and relational database, as COMPMJobModel "
                                   + "entries."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
     * Return the total number of DockerJob records.
     *
     * @return
     */
    @Operation(
        summary = "Count the caller's docker jobs.",
        description = "Returns a single count field holding the total number of docker jobs the "
                      + "caller has submitted.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "An object with a single count field, holding the number of docker jobs the "
                                   + "caller has submitted."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping("/jobs/count")
    public ResponseEntity<JsonNode> queryDockerJobsCount(@AuthenticationPrincipal UserProfile up) {
        try {
            long count = jobm.queryUserDockerJobsCount(up);
            ObjectNode result = om.createObjectNode();
            result.put("count", count);
            return jsonAPIHelper.success(result);
        } catch (Exception e) {
            return jsonAPIHelper.logAndReturnJsonExceptionEntity(
                    "Error querying docker jobs count", Optional.of(up), e, true);
        }
    }

    /**
     * Return list of "my" jobs with status.<br/>
     * Use ...?all or ...?open t ask for open jobs or all jobs
     * filtering, e.g. between two times.
     *
     * @return
     */
    @Operation(
        summary = "List the caller's docker jobs, with optional label filtering.",
        description = "Returns the caller's docker jobs as COMPMDockerJobModel entries, which "
                      + "carry container and image detail that the general job listing omits.",
        parameters = {
            @Parameter(name = "open", in = ParameterIn.QUERY,
                       description = "Present with any value to return only jobs that have not "
                                     + "yet finished. Omit to return all."),
            @Parameter(name = "top", in = ParameterIn.QUERY,
                       description = "Maximum number of jobs to return. -1, the default, returns "
                                     + "all of them."),
            @Parameter(name = "start", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted at or after this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no lower bound is "
                                     + "applied."),
            @Parameter(name = "end", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted strictly before this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no upper bound is "
                                     + "applied."),
            @Parameter(name = "labelReg", in = ParameterIn.QUERY,
                       description = "Regular expression matched against job labels; only "
                                     + "matching jobs are returned.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The caller's docker jobs, each with the container and image detail the "
                                   + "general job listing omits."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Summarise the caller's job outcomes over a recent period.",
        description = "Returns a map of job status to the number of the caller's jobs in that "
                      + "status, covering jobs that finished within the window and any still "
                      + "running.",
        parameters = {
            @Parameter(name = "since", in = ParameterIn.QUERY,
                       description = "Number of hours before the present to report on. Defaults "
                                     + "to 24; a negative value is treated as 24.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "A map of job status to the number of the caller's jobs in that status over "
                                   + "the window."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "List the caller's docker jobs. Superseded by /dockerjobs; retained for "
                         + "compatibility.",
        description = "Retained for compatibility with older clients. Behaves like /dockerjobs "
                      + "but offers no label filtering. Prefer /dockerjobs.",
        parameters = {
            @Parameter(name = "open", in = ParameterIn.QUERY,
                       description = "Present with any value to return only jobs that have not "
                                     + "yet finished. Omit to return all."),
            @Parameter(name = "top", in = ParameterIn.QUERY,
                       description = "Maximum number of jobs to return. -1, the default, returns "
                                     + "all of them."),
            @Parameter(name = "start", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted at or after this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no lower bound is "
                                     + "applied."),
            @Parameter(name = "end", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted strictly before this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no upper bound is "
                                     + "applied.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The caller's docker jobs. Prefer /dockerjobs, which returns the same jobs."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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

    
    @Operation(
        summary = "List the caller's docker jobs using a faster native query.",
        description = "Selects the same jobs as /dockerjobs but runs a native query and returns a "
                      + "NativeQueryResult of columns and rows rather than model objects. Faster "
                      + "for large result sets.",
        parameters = {
            @Parameter(name = "open", in = ParameterIn.QUERY,
                       description = "Present with any value to return only jobs that have not "
                                     + "yet finished. Omit to return all."),
            @Parameter(name = "top", in = ParameterIn.QUERY,
                       description = "Maximum number of jobs to return. -1, the default, returns "
                                     + "all of them."),
            @Parameter(name = "start", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted at or after this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no lower bound is "
                                     + "applied."),
            @Parameter(name = "end", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted strictly before this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no upper bound is "
                                     + "applied."),
            @Parameter(name = "labelReg", in = ParameterIn.QUERY,
                       description = "Regular expression matched against job labels; only "
                                     + "matching jobs are returned.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The caller's docker jobs, as a result set of columns and rows rather than "
                                   + "model objects."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "List the caller's relational database jobs.",
        description = "Returns the caller's relational database jobs.",
        parameters = {
            @Parameter(name = "open", in = ParameterIn.QUERY,
                       description = "Present with any value to return only jobs that have not "
                                     + "yet finished. Omit to return all."),
            @Parameter(name = "top", in = ParameterIn.QUERY,
                       description = "Maximum number of jobs to return. -1, the default, returns "
                                     + "all of them."),
            @Parameter(name = "start", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted at or after this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no lower bound is "
                                     + "applied."),
            @Parameter(name = "end", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted strictly before this time. Two "
                                     + "formats are accepted: yyyy-MM-dd HH:mm:ss Z, for example "
                                     + "2026-10-07 14:30:00 -0400, and yyyy-MM-dd, which is read "
                                     + "in the time zone of the service. The zone is not "
                                     + "optional: a value carrying a time but no zone falls back "
                                     + "to the date alone, so 2026-10-07 14:30:00 and "
                                     + "2026-10-07T14:30:00Z both mean midnight. A value not "
                                     + "beginning with a date is ignored, and no upper bound is "
                                     + "applied.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The caller's relational database jobs."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping("/rdbjobs")
    public ResponseEntity<JsonNode> queryUserRdbJobs(@RequestParam(required = false) String open,
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

    @Operation(
        summary = "Search jobs using a structured query.",
        description = "Returns the jobs matching a structured query. Use this when the query "
                      + "parameters on the listing endpoints are not expressive enough.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "Criteria selecting which jobs to return."))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The jobs matching the query."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                   + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PostMapping("/jobs/query")
    public ResponseEntity<JsonNode> queryJob(@RequestBody JobQuery query,
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
    @Operation(
        summary = "Report queue depth per compute domain and where a job submitted now would "
                  + "land.",
        description = "Returns, for every compute domain, the jobs currently running, the jobs "
                      + "waiting, and the position a job submitted now by the caller would take "
                      + "in the queue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Queue depth per compute domain, as a result set of columns and rows, with "
                                   + "the position a job submitted now by the caller would take."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Get the status of one job, if the caller may view it.",
        description = "Returns the full status of one job. The caller must be permitted to view "
                      + "it.",
        parameters = {
            @Parameter(name = "jobId", in = ParameterIn.PATH,
                       description = "Identifier of the job, as returned by the job listing "
                                     + "endpoints.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The job, with its full status."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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

    @Operation(
        summary = "Cancel one of the caller's jobs.",
        description = "Requests cancellation of one of the caller's jobs and returns its updated "
                      + "status.",
        parameters = {
            @Parameter(name = "jobId", in = ParameterIn.PATH,
                       description = "Identifier of the job to cancel.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The job, with the cancellation recorded. Cancelling is a request: the "
                                   + "COMPM running the job stops it when it next polls."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                   + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "List the docker compute domains the caller can use, with the images and "
                         + "volumes available to them.",
        description = "Returns the docker compute domains the caller may use, each with the "
                      + "images, volume containers and user volumes available to them. "
                      + "Interactive domains only, unless batch is requested.",
        parameters = {
            @Parameter(name = "batch", in = ParameterIn.QUERY,
                       description = "Set to true to include batch domains, that is, domains "
                                     + "served by a registered COMPM."),
            @Parameter(name = "interactive", in = ParameterIn.QUERY,
                       description = "Only consulted when batch is true. Set to true to include "
                                     + "interactive domains alongside batch ones; otherwise batch "
                                     + "domains are returned on their own.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The docker compute domains the caller may use, each with the images, "
                                   + "volume containers and user volumes available to them."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "List all registered docker compute domains. Requires an administrator.",
        description = "Returns every registered docker compute domain with its images, volume "
                      + "containers and root volumes, regardless of the caller's access to them. "
                      + "Administrators only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "Every registered docker compute domain, with its images, volume containers "
                                   + "and root volumes."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The caller is not permitted to perform this action."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Register a docker compute domain, or replace an existing one. Omitted images, "
                         + "volumes and root volumes are deleted.",
        description = "Registers a docker compute domain, or replaces an existing one when the "
                      + "body carries both id and racmUUID. Replacement is destructive: images, "
                      + "volume containers and root volumes absent from the body are deleted. To "
                      + "add a single root volume without that risk, use POST "
                      + "/computedomains/docker/{racmUUID}/rootvolumes instead.",
        parameters = {
            @Parameter(name = "admins", in = ParameterIn.QUERY,
                       description = "Comma-separated group names to be granted administrator "
                                     + "rights over the domain's images and volume containers.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "DockerComputeDomainModel as JSON. Include id and racmUUID "
                                     + "to replace an existing domain; omit both to register a "
                                     + "new one."))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The compute domain as registered or replaced, including its racmUUID and "
                                   + "the images, volume containers and root volumes it now holds."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                   + "reason."),
        @ApiResponse(responseCode = "403",
                     description = "The caller is not permitted to perform this action."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
     * Attach a single root volume to an existing docker compute domain.
     *
     * <p>Unlike POST /computedomains/docker, this does not require resending the whole domain
     * document and will not remove images, volume containers or other root volumes that are
     * omitted from the request.
     *
     * @param racmUUID uuid of the compute domain's resource context
     * @param rvm the root volume to attach
     * @param up the calling user
     * @return the created entry, including the id assigned by RACM
     */
    @Operation(
        summary = "Attach one root volume to a docker compute domain, leaving its other contents "
                         + "untouched.",
        description = "Attaches one root volume to the domain without resending the whole "
                      + "registration document, so its existing images, volume containers and "
                      + "root volumes are left untouched. The root volume must not already be "
                      + "mounted on the domain, and its path and display name must each be unused "
                      + "there.",
        parameters = {
            @Parameter(name = "racmUUID", in = ParameterIn.PATH,
                       description = "Identifier of the compute domain's resource context, "
                                     + "returned as racmUUID by GET /dockercomputedomains.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "Root volume to attach. rootVolumeId, pathOnCD and "
                                     + "displayName are required; publisherDID is optional and "
                                     + "recorded as given; id must not be supplied."))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The attachment as created, linking the root volume to the compute domain "
                                   + "at the path given."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                   + "reason."),
        @ApiResponse(responseCode = "403",
                     description = "The caller is not permitted to perform this action."),
        @ApiResponse(responseCode = "404",
                     description = "No compute domain exists with that racmUUID."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PostMapping("/computedomains/docker/{racmUUID}/rootvolumes")
    public ResponseEntity<JsonNode> addRootVolumeToDockerComputeDomain(@PathVariable String racmUUID,
            @RequestBody RootVolumeOnComputeDomainModel rvm,
            @AuthenticationPrincipal UserProfile up) {
        try {
            RootVolumeOnComputeDomain rv =
                    dockerComputeDomainManager.addRootVolume(racmUUID, rvm, up);
            up.getTom().persist();

            RootVolumeOnComputeDomainModel created =
                    jobmModelFactory.newRootVolumeOnComputeDomainModel(rv);

            LogUtils.buildLog()
                .forJOBM()
                .user(up)
                .showInUserHistory()
                .sentence()
                    .subject(up.getUsername())
                    .verb("registered")
                    .predicate("root volume %d at '%s' on docker compute domain '%s'",
                            created.getRootVolumeId(), created.getPathOnCD(), racmUUID)
                .extraField("dockerComputeDomain", racmUUID)
                .extraField("rootVolume", created.getRootVolumeId())
                .extraField("rootVolumeOnComputeDomain", created.getId())
                .log();

            return jsonAPIHelper.success(created);
        } catch (ResourceNotFoundException e) {
            return jsonAPIHelper.logAndReturnJsonExceptionEntity(
                    "No docker compute domain with racmUUID " + racmUUID,
                    Optional.of(up), e, HttpStatus.NOT_FOUND, true);
        } catch (Exception e) {
            return jsonAPIHelper.logAndReturnJsonExceptionEntity(
                    "Error adding root volume to docker compute domain " + racmUUID,
                    Optional.of(up), e, true);
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
    @Operation(
        summary = "Submit a docker job.",
        description = "Submits a docker job to a compute domain. The caller must be permitted to "
                      + "create a container from the requested image and to mount every volume "
                      + "the job asks for.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "COMPMDockerJobModel as JSON, naming the image, compute "
                                     + "domain, command and volumes to mount."))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The submitted job, as RACM now holds it, including the identifier the "
                                   + "caller uses to follow its progress."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                   + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
    @Operation(
        summary = "Register a compute domain manager and return any jobs still assigned to it.",
        description = "Registers a compute domain manager. A COMPM that has registered before "
                      + "must send the uuid it was given; a new one is assigned one. The response "
                      + "lists any jobs still outstanding for that COMPM.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "Registration details for the COMPM, including its uuid "
                                     + "when re-registering."))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The COMPM as registered, carrying the uuid it must present on later calls "
                                   + "and any jobs still outstanding for it."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                   + "reason."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PostMapping("/compm/register")
    public ResponseEntity<JsonNode> registerCOMPM(@AuthenticationPrincipal UserProfile up,
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
    @Operation(
        summary = "List the caller's CasJobs jobs, proxied from SkyServer.",
        description = "Proxies a CasJobs job listing for the caller from SkyServer, using the "
                      + "caller's token. On failure the upstream status code is passed through.",
        parameters = {
            @Parameter(name = "submittedFrom", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted at or after this time. The "
                                     + "value is passed to CasJobs unchanged, so CasJobs defines "
                                     + "the format. Supply both: a parameter left out reaches "
                                     + "CasJobs as the literal text null."),
            @Parameter(name = "submittedTo", in = ParameterIn.QUERY,
                       description = "Only return jobs submitted at or before this time. The "
                                     + "value is passed to CasJobs unchanged, so CasJobs defines "
                                     + "the format. Supply both: a parameter left out reaches "
                                     + "CasJobs as the literal text null.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The CasJobs job listing, as CasJobs returned it. RACM passes the body "
                                   + "through without interpreting it."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
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
