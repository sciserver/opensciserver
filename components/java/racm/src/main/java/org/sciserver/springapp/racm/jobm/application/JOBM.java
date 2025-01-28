package org.sciserver.springapp.racm.jobm.application;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.persistence.config.QueryHints;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.ivoa.dm.model.TransientObjectManager.ChangeSet;
import org.sciserver.racm.jobm.model.COMPMDockerJobModel;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.JobQuery;
import org.sciserver.racm.jobm.model.JobQuery.JobType;
import org.sciserver.racm.jobm.model.JobQueryResult;
import org.sciserver.racm.jobm.model.JobStatus;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.config.JOBMConfig;
import org.sciserver.springapp.racm.login.LoginPortalAccess;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMException;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.job.DockerJob;
import edu.jhu.job.Job;
import edu.jhu.job.JobMessage;
import edu.jhu.job.MessageType;
import edu.jhu.job.RDBJob;
import edu.jhu.user.User;

@Service
public class JOBM {
	private static final String SUBMIT_TIME_COLUMN = "submitTime";
	private static final String DATETIME_FORMAT="yyyy-MM-dd HH:mm:ss Z";
	private static final String DATE_FORMAT="yyyy-MM-dd";
	//valid JobType names

	private final JOBMConfig jobmConfig;
	private final VOURPContext vourpContext;
	private final JOBMAccessControl jobmAccessControl;
	private final RDBDomainManager rdbDomainManager;
	private final JOBMModelFactory jobmModelFactory;
	private final LoginPortalAccess loginPortalAccess;

	@Autowired
	public JOBM(VOURPContext vourpContext, JOBMAccessControl jobmAccessControl,
			RDBDomainManager rdbDomainManager, JOBMModelFactory jobmModelFactory,
			JOBMConfig jobmConfig, LoginPortalAccess loginPortalAccess) {
		this.jobmConfig = jobmConfig;
		this.vourpContext = vourpContext;
		this.jobmAccessControl = jobmAccessControl;
		this.rdbDomainManager = rdbDomainManager;
		this.jobmModelFactory = jobmModelFactory;
		this.loginPortalAccess = loginPortalAccess;
	}

	/**
	 * Creates a trust relation between user and JOBM, determines a trust token and returns that.<br/>
	 * @param up
	 * @return
	 * @throws VOURPException
	 */
	private String checkUserTrust(UserProfile up) throws VOURPException {
		String trustId = up.getTrustId();
		try {
			if(trustId == null || trustId.trim().length() == 0){
					trustId = loginPortalAccess.getTrustId(up.getToken(), jobmConfig.getAdminUser());
					User u = up.getUser();
					u.setTrustId(trustId);
			}
		} catch(Exception e){
				throw new VOURPException(e);
		}
		return trustId;
	}

	public String getTrustToken(String trustId) throws IOException {
			return loginPortalAccess.getTrustedToken(jobmConfig.getAdminUser(), jobmConfig.getAdminPassword(), trustId);
	}

	public void buildTrustIfNeeded(User user, String token) throws IOException {
		if (user.getTrustId() == null || user.getTrustId().trim().length() == 0) {
			loginPortalAccess.getTrustId(token, user.getUsername());
		}
	}

	/**
	 * Create a new job for a user uploaded jobmodel.<br/>
	 * Assumption is that JOBMAccessControl has NOT YET been checked already that user is allowed to submit a job.
	 * @param model
	 * @param up
	 * @return
	 * @throws VOURPException
	 * @throws RACMException
	 */
	public DockerJob newDockerJob(COMPMDockerJobModel model, UserProfile up) throws VOURPException, RACMException {
		// check whether user is allowed to submit jobs
		jobmAccessControl.tryUserSubmitJob(up);
		TransientObjectManager tom = up.getTom();
		DockerJob job = jobmModelFactory.newDockerJob(model, up);
		// ensure user has been assigned a trustid.
		checkUserTrust(up);
		job.setSubmitter(up.getUser());
		tom.persist();
		return job;
	}

	/**
	 * Create a new job for a user uploaded jobmodel.<br/>
	 * Assumption is that JOBMAccessControl has NOT YET been checked already that user is allowed to submit a job.
	 * @param model
	 * @param up
	 * @return
	 * @throws VOURPException
	 * @throws RACMException
	 */
	public RDBJob newRDBJob(RDBJobModel model, UserProfile up) throws VOURPException, RACMException {
		// check whether user is allowed to submit jobs
		TransientObjectManager tom = up.getTom();

		// check whether user is allowed to submit jobs
		jobmAccessControl.tryUserSubmitJob(up);
		// check whether user is allowed to connect to this database and query it
  	boolean ok = rdbDomainManager.canUserQueryDatabaseContext(up, model.getRdbDomainId(), model.getDatabaseContextName());
  	if(!ok)
  		throw new VOURPException(VOURPException.UNAUTHORIZED,String.format("User '%s' is not allowed to query database context '%s'",up.getUsername(), model.getDatabaseContextName()));

  	RDBJob job = jobmModelFactory.newRDBJob(model, up);

		// ensure user has been assigned a trustid.
		checkUserTrust(up);
		job.setSubmitter(up.getUser());
        ChangeSet changeSet = tom.newChangeSet();
        changeSet.add(job);
        tom.persistChangeSet(changeSet, true);
		return job;
	}


	public COMPMJobModel queryUserJob(Long jobId, UserProfile up){
		TransientObjectManager tom = vourpContext.newTOM();
		Job job = getJobBySubmitter(tom, up.getId(), jobId);
 		return jobmModelFactory.newCOMPMJobModel(job, true);

	}
	/**
	 * Add a cancel
	 * @param jobId
	 * @param up
	 * @return
	 * @throws VOURPException
	 */
	public COMPMJobModel cancelUserJob(Long jobId, UserProfile up) throws VOURPException{
		TransientObjectManager tom = vourpContext.newTOM();
		Job job = getJobBySubmitter(tom, up.getId(), jobId);
		if(job == null)
			throw new VOURPException(String.format("User '%s' has no access to a job with id %d",up.getUsername(), jobId));
		if(job.getStatus() < JobStatus.STATUS_FINISHED)
		{
			if(job.getStatus() == JobStatus.STATUS_PENDING)
				job.setStatus(JobStatus.STATUS_CANCELED);
			boolean wasCancelled = false;
			for(JobMessage m: job.getMessage())
				if(m.getLabel() == MessageType.CANCEL){
					wasCancelled = true;
					break;
				}
			if(!wasCancelled){
				JobMessage m = new JobMessage(job);
				m.setLabel(MessageType.CANCEL);
				tom.persist();
			}
		}
 		return jobmModelFactory.newCOMPMJobModel(job, true);

	}

	public COMPMJobModel queryCOMPMJob(Long jobId, long compmId) {
		TransientObjectManager tom = vourpContext.newTOM();
		Job job = getJobByCOMPM(tom, compmId, jobId);
 		return jobmModelFactory.newCOMPMJobModel(job, true);
	}

	private Job getJobByCOMPM(TransientObjectManager tom, long compmId, long jobId) {
		Query q = tom.createQuery("select j from Job j where j.id = :jobId and j.runBy.id=:compmid")
				.setParameter("compmid", compmId)
				.setParameter("jobId", jobId);
		return tom.queryOne(q, Job.class);
	}

	private Job getJobBySubmitter(TransientObjectManager tom, long userId, long jobId) {
		Query q = tom.createQuery("select j from Job j where j.id = :jobId and j.submitter.id=:userId")
				.setParameter("userId", userId)
				.setParameter("jobId", jobId);
		return tom.queryOne(q, Job.class);
	}

	/**
	 * Return a Date for the input string. If any exception is thrown while parsing the string null will be returned.<br/>
	 * @param sd
	 * @return
	 */
	private static Date toDate(String sd) {
		 if(sd == null)
			 return null;
		SimpleDateFormat formatter = new SimpleDateFormat(DATETIME_FORMAT);
		try {
			return formatter.parse(sd);
		} catch (ParseException e) {
			formatter = new SimpleDateFormat(DATE_FORMAT);
			try {
				return formatter.parse(sd);
			} catch (ParseException e2) {
				return null;
			}
		}
	}

	/**
	 * Find all jobs for a user, with specified constraints.
	 * @param user The user for whom the jobs are queried.
	 * @param open If true, only open jobs, i.e. status <= FINISHED (FINISHED means not yet confirmed that job is properly destroyed).
	 * @param top If specified as a positive integer the maximum number of jobs that should be returned. If not specified of <= 0, all jobs are returned
	 * @param start The earliest date (inclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS. If not specified, no lower bound on date.
	 * @param end The latest date (exclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS, If not specified, no upper bound on the submit time.
	 * @return
	 * @throws VOURPException
	 */
	public List<COMPMJobModel> queryUserJobs(UserProfile user, boolean open, int top, String start, String end) throws VOURPException{
		List<COMPMJobModel> jobs = CollectionUtils.collate(
				queryUserDockerJobs(user, open, -1, start, end),
				queryUserRDBJobs(user, open, -1, start, end),
				(dockerJob, rdbJob) ->
					-dockerJob.getSubmissionTime().compareTo(rdbJob.getSubmissionTime())
				);
		if (top > 0 && top < jobs.size())
			jobs = jobs.subList(0, top);

		return jobs;
	}

	public JobQueryResult performJobQuery(
			UserProfile up, JobQuery jobQuery) {
		CriteriaBuilder builder = up.getTom().getEntityManager().getCriteriaBuilder();
		CriteriaQuery<Job> cQuery = builder.createQuery(Job.class);
		Class<? extends Job> clazz = jobQuery.getType().equals(JobQuery.JobType.DOCKER) ?
				DockerJob.class : RDBJob.class;
		Root<? extends Job> job = cQuery.from(clazz);
		Optional<Instant> submitTimeStart = jobQuery.getSubmitTimeStart();
		Optional<Instant> submitTimeEnd = jobQuery.getSubmitTimeEnd();

		Predicate predicate = builder.conjunction();
		predicate = builder.and(predicate,
				builder.equal(job.get("submitter"), up.getUser()));
		if (!jobQuery.getJobIds().isEmpty()) {
			predicate = builder.and(predicate,
					job.get("id").in(jobQuery.getJobIds()));
		}
		if (!jobQuery.getJobStatuses().isEmpty()) {
			predicate = builder.and(predicate,
					job.get("status").in(jobQuery.getJobStatuses()));
		}
		if (submitTimeStart.isPresent()) {
			predicate = builder.and(predicate,
					builder.greaterThan(
							job.get(SUBMIT_TIME_COLUMN),
							Date.from(submitTimeStart.get())));
		}
		if (submitTimeEnd.isPresent()) {
			predicate = builder.and(predicate,
					builder.lessThan(
							job.get(SUBMIT_TIME_COLUMN),
							Date.from(submitTimeEnd.get())));
		}
		cQuery.select(job).where(predicate);

		jobQuery.getOrderBy().ifPresent(queryOrder -> {
			switch (queryOrder) {
			case ASC_JOB_ID:
				cQuery.orderBy(builder.asc(job.get("id")));
				break;
			case ASC_SUBMIT_TIME:
				cQuery.orderBy(builder.asc(job.get(SUBMIT_TIME_COLUMN)));
				break;
			case DESC_JOB_ID:
				cQuery.orderBy(builder.desc(job.get("id")));
				break;
			case DESC_SUBMIT_TIME:
				cQuery.orderBy(builder.desc(job.get(SUBMIT_TIME_COLUMN)));
				break;
			}
		});

		TypedQuery<Job> q = up.getTom().getEntityManager().createQuery(cQuery)
			.setFirstResult(jobQuery.getPageNumber() * jobQuery.getLimit())
			.setMaxResults(jobQuery.getLimit());

		if (jobQuery.includeDetails()) {
			job.fetch("message", JoinType.LEFT);
		}

		job.fetch("message", JoinType.LEFT);
		job.fetch("userVolume", JoinType.LEFT);
		job.fetch("computeDomain");
		job.fetch("submitter");
		job.fetch("runBy", JoinType.LEFT);

		if (jobQuery.getType() == JobType.DOCKER) {
			job.fetch("image", JoinType.LEFT);
			job.fetch("requiredVolume");
		} else if (jobQuery.getType() == JobType.RDB) {
			job.fetch("databaseContext", JoinType.LEFT);
			job.fetch("target", JoinType.LEFT);
		} else {
			throw new IllegalStateException("Unknown job type: " + jobQuery.getType());
		}

		return new JobQueryResult(q.getResultList().stream()
			.map(retrievedJob ->
				jobmModelFactory.newCOMPMJobModel(retrievedJob, jobQuery.includeDetails()))
			.collect(toList()));
	}

	/**
	 * Return statistics about user's jobs still running or finished in the last 'since' hours.<br/>
	 * @param up
	 * @param since
	 * @return
	 */
	public Map<Integer, Integer> queryUserJobsStats(UserProfile up, int since){
		TransientObjectManager tom = up.getTom();
		
		HashMap<Integer, Integer> r = new HashMap<Integer,Integer>();
		String sql = String.format("select status, count(*) as num " + 
				"  from DockerJob where submitterId=%d " + 
				"    and (status <= %d or DATEDIFF(HOUR,finishedTime,CURRENT_TIMESTAMP)<=%d) " + 
				"group by status",up.getId(), JobStatus.STATUS_FINISHED,since);
		Query q = tom.createNativeQuery(sql);
		
		List<?> rows = tom.executeNativeQuery(q);
		for (Object o : rows) {
			Object[] row = (Object[])o;
			r.put((Integer)row[0], (Integer)row[1]);
		}
		return r;
	}
	/**
	 * Find all docker jobs for a user, with specified constraints.
	 * @param user The user for whom the jobs are queried.
	 * @param open If true, only open jobs, i.e. status <= FINISHED (FINISHED means not yet confirmed that job is properly destroyed).
	 * @param top If specified as a positive integer the maximum number of jobs that should be returned. If not specified of <= 0, all jobs are returned
	 * @param start The earliest date (inclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS. If not specified, no lower bound on date.
	 * @param end The latest date (exclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS, If not specified, no upper bound on the submit time.
	 * @return
	 * @throws VOURPException
	 */
	public List<COMPMDockerJobModel> queryUserDockerJobs(UserProfile user, boolean open, int top, String start, String end) throws VOURPException{
		TransientObjectManager tom = vourpContext.newTOM();
		StringBuilder where = new StringBuilder();
		if(open)
			where.append(" AND j.status <= :finished ");
		Date dstart = toDate(start);
		if(dstart != null)
			where.append(" AND j.submitTime >= :start ");
		Date dend = toDate(end);
		if(dend != null)
			where.append(" AND j.submitTime < :end ");

		Query q = tom.createQuery(
				"select j from DockerJob j where j.submitter.id=:id "
				+ where.toString()
				+ " order by j.submitTime desc")
				.setParameter("id", user.getId());

		q.setHint(QueryHints.LEFT_FETCH, "j.message");
		q.setHint(QueryHints.LEFT_FETCH, "j.userVolume.userVolume");
		q.setHint(QueryHints.LEFT_FETCH, "j.computeDomain.resourceContext");
		q.setHint(QueryHints.LEFT_FETCH, "j.submitter");
		q.setHint(QueryHints.LEFT_FETCH, "j.runBy");
		q.setHint(QueryHints.LEFT_FETCH, "j.image");
		q.setHint(QueryHints.LEFT_FETCH, "j.requiredVolume.volume.resource");

		if(open)
			q.setParameter("finished", JobStatus.STATUS_FINISHED);
		if(dstart != null)
			q.setParameter("start", dstart);
		if(dend != null)
			q.setParameter("end", dend);

		List<DockerJob> l = tom.queryJPA(q, DockerJob.class);
		if (top > 0 && top < l.size())
			l = l.subList(0, top);

		List<COMPMDockerJobModel> jms = new ArrayList<>();
		if(l != null){
			for(DockerJob job : l){
				COMPMDockerJobModel jm = (COMPMDockerJobModel) jobmModelFactory.newCOMPMJobModel(job, true);
				jms.add(jm);
			}
		}
		return jms;
	}

	public List<COMPMDockerJobModel> queryUserDockerJobs(UserProfile user, boolean open, int top, String start, String end, String labelReg) throws VOURPException{
		NativeQueryResult r = queryUserDockerJobsNative(user, open, top, start, end, labelReg);
		// id,status,publisherDID,submitterDID,submitTime,startedTime,finishedTime,duration,resultsFolderURI,command,scriptURI,image,computeDomain
		ArrayList<COMPMDockerJobModel> jobs = new ArrayList<>();
		
		for(Object[] row: r.getRows()) {
			int i = 0;
			COMPMDockerJobModel job = new COMPMDockerJobModel((Long)row[i++]);
			job.setStatus((Integer)row[i++]);
			job.setPublisherDID((String)row[i++]);
			job.setSubmitterDID((String)row[i++]);
			job.setSubmissionTime((Date)row[i++]);
			job.setStartTime((Date)row[i++]);
			job.setEndTime((Date)row[i++]);
			job.setDuration((Double)row[i++]);
			job.setResultsFolderURI((String)row[i++]);
			job.setCommand((String)row[i++]);
			job.setScriptURI((String)row[i++]);
			job.setDockerImageName((String)row[i++]);
			job.setDockerComputeEndpoint((String)row[i++]);
			job.setDockerComputeResourceContextUUID((String)row[i++]);
			jobs.add(job);
		}
		return jobs;
	}
	/**
	 * Find all rdb jobs for a user, with specified constraints.
	 * @param user The user for whom the jobs are queried.
	 * @param open If true, only open jobs, i.e. status <= FINISHED (FINISHED means not yet confirmed that job is properly destroyed).
	 * @param top If specified as a positive integer the maximum number of jobs that should be returned. If not specified of <= 0, all jobs are returned
	 * @param start The earliest date (inclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS. If not specified, no lower bound on date.
	 * @param end The latest date (exclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS, If not specified, no upper bound on the submit time.
	 * @return
	 * @throws VOURPException
	 */
	public List<RDBJobModel> queryUserRDBJobs(UserProfile user, boolean open, int top, String start, String end) throws VOURPException{
		TransientObjectManager tom = vourpContext.newTOM();
		StringBuilder where = new StringBuilder();
		if(open)
			where.append(" AND j.status <= :finished ");
		Date dstart = toDate(start);
		if(dstart != null)
			where.append(" AND j.submitTime >= :start ");
		Date dend = toDate(end);
		if(dend != null)
			where.append(" AND j.submitTime < :end ");

		Query q = tom.createQuery(
				"select j from RDBJob j where j.submitter.id=:id "
				+ where.toString()
				+ " order by j.submitTime desc")
				.setParameter("id", user.getId());

		q.setHint(QueryHints.LEFT_FETCH, "j.message");
		q.setHint(QueryHints.LEFT_FETCH, "j.userVolume.userVolume");
		q.setHint(QueryHints.LEFT_FETCH, "j.computeDomain.resourceContext");
		q.setHint(QueryHints.LEFT_FETCH, "j.submitter");
		q.setHint(QueryHints.LEFT_FETCH, "j.runBy");
		q.setHint(QueryHints.LEFT_FETCH, "j.databaseContext");
		q.setHint(QueryHints.LEFT_FETCH, "j.target");

		if(open)
			q.setParameter("finished", JobStatus.STATUS_FINISHED);
		if(dstart != null)
			q.setParameter("start", dstart);
		if(dend != null)
			q.setParameter("end", dend);

		List<RDBJob> l = tom.queryJPA(q, RDBJob.class);
		if (top > 0 && top < l.size())
			l = l.subList(0, top);

		List<RDBJobModel> jms = new ArrayList<>();
		if(l != null){
			for(RDBJob job : l){
				RDBJobModel jm = (RDBJobModel) jobmModelFactory.newCOMPMJobModel(job, true);
				jms.add(jm);
			}
		}
		return jms;
	}

	/**
	 * Return jobs queues for all batch domains, consisting of information about running and pending jobs.<br/>
	 * Special entry indicates where a job submitted now by the user would end up in the queue.
	 * Only the username of the user submitting this request is shown, checksums for others. 
	 * @param up
	 * @return
	 */
	public NativeQueryResult queryJobsQueues(UserProfile up) {
	    TransientObjectManager tom = up.getTom();
	    String columns = "domainName,domainClass,queueId,ranking,username,status,submitTime,startedTime";
	    Query q = tom.createNativeQuery("select "+columns+" from racm.allJobsQueues(?) order by domainName,queueId,ranking");
	    q.setParameter(1, up.getUser().getId());
	    List<?> rows = tom.executeNativeQuery(q);
	    NativeQueryResult r = new NativeQueryResult();
        r.setColumns(columns);
        r.setRows(rows);
        return r;
	}
	
	/**
	 * Query open jobs for a user.<br/>
	 * @param userId
	 * @return
	 */
	public List<COMPMJobModel> queryOpenCOMPMJobs(long compmId){
		TransientObjectManager tom = vourpContext.newTOM();
		Query q = tom.createQuery("select j from Job j where j.runBy.id=:compmid and j.status <= :finishedStatus");
		q.setParameter("compmid", compmId).setParameter("finishedStatus", JobStatus.STATUS_FINISHED);
		List<MetadataObject> l = tom.queryJPA(q);
		List<COMPMJobModel> jms = new ArrayList<>();
		for(MetadataObject mo:l){
			Job job = (Job)mo;
			COMPMJobModel jm = jobmModelFactory.newCOMPMJobModel(job, true);
			if(jm != null)
				jms.add(jm);
		}
		return jms;
	}
	/**
	 * Query canceled, unfinished jobs for a COMPM.<br/>
	 * @param compm
	 * @return
	 */
	public Long[] queryCanceledCOMPMJobs(String compmUUID){
		TransientObjectManager tom = vourpContext.newTOM();
		Query q = tom.createNativeQuery("select jobId from racm.CanceledCOMPMJobs(?)");
		q.setParameter(1, compmUUID);
		List<?> l = tom.executeNativeQuery(q);
		Long[] ids = new Long[l.size()];
		for(int i = 0; i < l.size(); i++){
			Long id = (Long)l.get(i);
			ids[i]=id;
		}
		return ids;
	}
	/**
	 * Return next N pending Jobs available for the specified COMPM.<br/>
	 * @param uuid
	 * @return
	 * @throws VOURPException
	 */
	public List<Job> nextJobs(String compmUUID, long defaultJobTimeout,
			long defaultJobsPerUser, short maxResults) throws VOURPException {
		TransientObjectManager tom = vourpContext.newTOM();
		if(maxResults < 1) maxResults=1;
		Query jobIdQuery = tom.createNativeQuery(String.format(
				"SELECT jobId FROM racm.nextJobs('%s',%d,%d,%d,%d) order by ranking",
				compmUUID, defaultJobTimeout,
				defaultJobTimeout, maxResults,
				defaultJobsPerUser));
		List<String> jobids = tom.executeNativeQuery(jobIdQuery).stream()
				.map(Object::toString)
				.collect(toList());

		if (jobids.isEmpty())
			return Collections.emptyList();

		Query jobQuery = tom.createQuery("SELECT j FROM Job j WHERE j.id IN :jobids")
				.setParameter("jobids", jobids);

		return tom.queryJPA(jobQuery, Job.class).stream()
				.sorted(comparing(j -> jobids.indexOf(j.getId().toString())))
				.collect(toList());
	}

	/**
	 * Find all docker jobs for a user, with specified constraints.
	 * @param user The user for whom the jobs are queried.
	 * @param open If true, only open jobs, i.e. status <= FINISHED (FINISHED means not yet confirmed that job is properly destroyed).
	 * @param top If specified as a positive integer the maximum number of jobs that should be returned. If not specified of <= 0, all jobs are returned
	 * @param start The earliest date (inclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS. If not specified, no lower bound on date.
	 * @param end The latest date (exclusive) to search for jobs, in format yyyy-MM-dd hh:mm:ss.SSS, If not specified, no upper bound on the submit time.
	 * @return
	 * @throws VOURPException
	 */
	public NativeQueryResult queryUserDockerJobsNative(UserProfile user, boolean open, int top, String start, String end, String labelReg) throws VOURPException{
		TransientObjectManager tom = vourpContext.newTOM();
		
		NativeQueryResult r = new NativeQueryResult();
		String columns="id,status,publisherDID,submitterDID,submitTime,startedTime,finishedTime,duration,resultsFolderURI,command,scriptURI,image,computeDomain,dockerComputeResourceContextUUID";
		r.setColumns(columns);

		String stop="";
		if(top > 0) 
			stop=String.format(" top %d ",top);

		String sql=String.format("select "+stop+
				" j.id,j.status,j.publisherDID,j.submitterDID,j.submitTime,j.startedTime,j.finishedTime,j.duration,j.resultsFolderURI,j.command,j.scriptURI"
				+ " ,i.name as image,d.name as computeDomain,rc.uuid as dockerComputeResourceContextUUID "+
				" from DockerJob j "+
				"    join dockercomputedomain d " + 
				"	 on d.id=j.computeDomainId " + 
				"    join ResourceContext rc " +
				"    on rc.id=d.resourceContextId" +
				"	 join DockerImage i " + 
				"	 on i.id=j.imageId "+
				" where j.submitterId=%d ",user.getId());

		
		StringBuilder where = new StringBuilder();
		ArrayList<Object> pars = new ArrayList<Object>();
		if(labelReg != null && labelReg.trim().length() > 0) {
			labelReg="%"+labelReg+"%";
			where.append(" and j.submitterDID like ? ");  // UNCLEAR whether this works, or how parameter should be set. seems unsafe for sql injection to 
			pars.add(labelReg);
		}
		if(open) {
			where.append(String.format(" AND j.status <= %d ",JobStatus.STATUS_FINISHED));
		}
		Date dstart = toDate(start);
		if(dstart != null) {
			where.append(" AND j.submitTime >= ? ");
			pars.add(dstart);
		}
		Date dend = toDate(end);
		if(dend != null) {
			where.append(" AND j.submitTime < ? ");
			pars.add(dend);
		}
	
		Query q = tom.createNativeQuery(sql
				+ where.toString() 
				+ " order by j.submitTime desc");
		for(int i = 0; i < pars.size(); i++)
			q.setParameter(i+1, pars.get(i));
	
		r.setRows(tom.executeNativeQuery(q));
		return r;
	}
}
