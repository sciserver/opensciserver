package org.sciserver.springapp.racm.jobm.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.jobm.model.COMPMDockerJobModel;
import org.sciserver.racm.jobm.model.COMPMJobModel;
import org.sciserver.racm.jobm.model.COMPMModel;
import org.sciserver.racm.jobm.model.ComputeDomainUserVolumeModel;
import org.sciserver.racm.jobm.model.ComputeResourceModel;
import org.sciserver.racm.jobm.model.DBCOMPMModel;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.DockerComputeDomainModel;
import org.sciserver.racm.jobm.model.DockerImageModel;
import org.sciserver.racm.jobm.model.JobMessageModel;
import org.sciserver.racm.jobm.model.JobStatus;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;
import org.sciserver.racm.jobm.model.RDBJobModel;
import org.sciserver.racm.jobm.model.RDBTargetModel;
import org.sciserver.racm.jobm.model.RequiredUserVolumeModel;
import org.sciserver.racm.jobm.model.RootVolumeOnComputeDomainModel;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.jobm.model.VolumeContainerModel;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.storem.application.STOREMConstants;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.CollectionMerger;
import org.sciserver.springapp.racm.utils.RACMException;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import edu.jhu.file.RootVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.job.COMPM;
import edu.jhu.job.ComputeDomain;
import edu.jhu.job.ComputeResource;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.DockerComputeDomain;
import edu.jhu.job.DockerImage;
import edu.jhu.job.DockerJob;
import edu.jhu.job.Job;
import edu.jhu.job.JobMessage;
import edu.jhu.job.MessageType;
import edu.jhu.job.RDBComputeDomain;
import edu.jhu.job.RDBJob;
import edu.jhu.job.RDBJobTarget;
import edu.jhu.job.RDBTargetType;
import edu.jhu.job.RequiredUserVolume;
import edu.jhu.job.RequiredVolume;
import edu.jhu.job.RootVolumeOnComputeDomain;
import edu.jhu.job.VolumeContainer;
import edu.jhu.user.User;

@Component
public class JOBMModelFactory {
	private static final Logger logger = LogManager.getLogger();

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private final JOBMAccessControl jobmAccessControl;
	private DockerComputeDomainManager dockerComputeDomainManager;
	private final VOURPContext vourpContext;

	@Autowired
	public JOBMModelFactory(JOBMAccessControl jobmAccessControl, VOURPContext vourpContext) {
		this.jobmAccessControl = jobmAccessControl;
		this.vourpContext = vourpContext;
	}

	/* Set later to avoid circular dependency */
	@Autowired
	public void setDockerComputeDomainManager(DockerComputeDomainManager dockerComputeDomainManager) {
		this.dockerComputeDomainManager = dockerComputeDomainManager;
	}
	public COMPMJobModel newCOMPMJobModel(Job job, boolean fillDetails) {
		COMPMJobModel jm;
		if (job instanceof DockerJob)
			jm = newDockerJobModel((DockerJob) job, fillDetails);
		else if (job instanceof RDBJob)
			jm = newRDBJobModel((RDBJob) job, fillDetails);
		else
			jm = new COMPMJobModel(job.getId());

		jm.setSubmitterTrustId(job.getSubmitter().getTrustId());
		// only full jobtype model needed if also details of job need to be filled in

		jm.setStatus(job.getStatus() != null ? job.getStatus() : 0);
		jm.setExecutorDID(job.getExcutorDID());
		jm.setSubmitterDID(job.getSubmitterDID());
		jm.setSubmissionTime(job.getSubmitTime());
		jm.setStartTime(job.getStartedTime());
		jm.setEndTime(job.getFinishedTime());
		jm.setResultsFolderURI(job.getResultsFolderURI());
		jm.setTimeout(job.getTimeout());

		if (job.getDuration() != null)
			jm.setDuration(job.getDuration());
		jm.setRunByUUID(job.getRunBy() != null ? job.getRunBy().getUuid() : null);

		if (fillDetails) {
			List<JobMessage> msgs = job.getMessage();
			if (msgs != null && !msgs.isEmpty()) {
				List<JobMessageModel> jmsgs = new ArrayList<>();
				for (JobMessage m : msgs)
					jmsgs.add(newJobMessageModel(m));
				jm.setMessages(jmsgs);
			}
		}
		return jm;
	}

	private JobMessageModel newJobMessageModel(JobMessage jm) {
		JobMessageModel jmm = new JobMessageModel(jm.getId());

		jmm.setContent(jm.getContent());
		jmm.setLabel(jm.getLabel().name());
		return jmm;
	}

	private void initializeJob(Job job, COMPMJobModel jm) {
		job.setSubmitTime(new Date());
		job.setCreationDate(job.getSubmitTime());
		job.setStatus(JobStatus.STATUS_PENDING);
		job.setSubmitterDID(jm.getSubmitterDID());

		if (!StringUtils.isEmpty(jm.getResultsFolderURI())) {
			job.setResultsFolderURI(jm.getResultsFolderURI());
		}
	}

	DockerJob newDockerJob(COMPMDockerJobModel jobModel, UserProfile up) throws VOURPException, RACMException {
		TransientObjectManager tom = up.getTom();
		DockerJob job = new DockerJob(tom);
		initializeJob(job, jobModel);

		DockerComputeDomain cd = findDockerComputeDomainForAPIEndpoint(jobModel.getDockerComputeEndpoint(), tom);
		if (cd != null)
			job.setComputeDomain(cd);
		else {
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					"A docker job must reference a dockercomputedomain");
		}

		// one and only one of the following two should be not null for a DockerJob
		job.setCommand(jobModel.getCommand());
		job.setScriptURI(jobModel.getScriptURI());

		job.setFullDockerCommand(dockerCommandFromArray(jobModel.getFullDockerCommand()));
		for (ComputeResource cs : cd.getComputeResource()) {
			if (cs instanceof DockerImage && cs.getName().equals(jobModel.getDockerImageName())) {
				jobmAccessControl.tryUseDockerImage(up, (DockerImage) cs);
				job.setImage((DockerImage) cs);
				break;
			}
		}
		if (job.getImage() == null)
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "No valid Docker image on submitted job model");

		// TODO optimize next from nested loop?
		if (jobModel.getVolumeContainers() != null) {
			for (VolumeContainerModel vcm : jobModel.getVolumeContainers()) {
				for (ComputeResource cs : cd.getComputeResource()) {
					if (cs instanceof VolumeContainer && cs.getName().equals(vcm.getName())) {
						jobmAccessControl.tryUseVolumeContainer(up, (VolumeContainer) cs);
                        if(vcm.isWritable()) // must check whether user is allowed to write, if not throw exception
                            jobmAccessControl.tryWriteVolumeContainer(up, (VolumeContainer) cs);
						RequiredVolume rv = new RequiredVolume(job);
						rv.setVolume((VolumeContainer) cs);
						rv.setNeedsWriteAccess(vcm.isWritable());
					}
				}
			}
		}
		// if userjob requires user volumes to be mounted, first check whether user has
		// access to those volumes, and if so creates an appropriate entry
		if (jobModel.getUserVolumes() != null && !jobModel.getUserVolumes().isEmpty()) {
			// retrieve all uservolumes on this computedomain that the user has some access
			// to
			Map<Long, ComputeDomainUserVolumeModel> cduvms = dockerComputeDomainManager.queryMountableUserVolumes(up,
					cd.getId());
			for (RequiredUserVolumeModel vcm : jobModel.getUserVolumes()) {
				ComputeDomainUserVolumeModel cduvm = cduvms.get(vcm.getUserVolumeId());
				if (cduvm == null)
					continue; // this is expected if mulitple requests for the same volume, see next comment
				else
					cduvms.remove(vcm.getUserVolumeId()); // this ensures that a volume will be registered to be mounted
															// only once, even if request has mistake
				boolean canRead = cduvm.getAllowedActions().contains(STOREMConstants.A_FILESERVICE_USERVOLUME_READ);
				boolean canWrite = cduvm.getAllowedActions().contains(STOREMConstants.A_FILESERVICE_USERVOLUME_WRITE);
				if (canRead || canWrite) {
					RequiredUserVolume rv = new RequiredUserVolume(job);
					Query q = tom.createQuery("select uv from UserVolume uv where uv.id=:id")
							.setParameter("id", vcm.getUserVolumeId());
					UserVolume uv = tom.queryOne(q, UserVolume.class);
					rv.setUserVolume(uv);
					rv.setPathOnComputeDomain(cduvm.getFullPath());
					rv.setNeedsWriteAccess(vcm.isNeedsWriteAccess() && canWrite);
				}
			}
		}
		return job;
	}

	RDBJob newRDBJob(RDBJobModel jobModel, UserProfile up) throws VOURPException {
		TransientObjectManager tom = up.getTom();
		RDBJob job = new RDBJob(tom);
		initializeJob(job, jobModel);

		RDBComputeDomain cd = findRDBComputeDomain(jobModel.getRdbDomainId(), tom);
		if (cd != null)
			job.setComputeDomain(cd);
		else {
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "An RDB job must reference an rdbcomputedomain");
		}

		// one and only one of the following two should be not null for a DockerJob
		job.setInputSQL(jobModel.getInputSql());
		job.setSql(jobModel.getSql());

		if (jobModel.getDatabaseContextName() != null) {
			DatabaseContext dbContext = queryDatabaseContextForName(jobModel.getRdbDomainId(), jobModel.getDatabaseContextName(), tom);
			job.setDatabaseContext(dbContext);
			if (job.getDatabaseContext() == null)
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
						String.format("'%s' is not an existing database context on RDB Compute Domain '%s'",
								jobModel.getDatabaseContextName(), jobModel.getRdbDomainName()));
		} else {
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, "RDB Job MUST be given a database context name");
		}

		for (RDBTargetModel tm : jobModel.getTargets()) {
			RDBJobTarget t = new RDBJobTarget(job);
			t.setLocation(tm.getLocation());
			t.setTargetType(RDBTargetType.fromValue(tm.getType()));
			t.setResultNumber(tm.getResultNumber());
		}
		return job;
	}

	private void updateJobContent(COMPMJobModel jobModel, Job job) throws VOURPException {

		if (JobStatus.isCompleted(job.getStatus()))
			throw new VOURPException(VOURPException.ILLEGAL_STATE,
					"Illegal attempt made to update job whose status is already COMPLETED");
		// if change from not-yet-started to started, set startedTime
		if (JobStatus.isStarted(jobModel.getStatus()) && job.getStatus() < JobStatus.STATUS_STARTED) {
			job.setStartedTime(new Date());
			// next is only point in workflow where the resulsFolderURI can be updated, by a
			// COMPM!!
			job.setResultsFolderURI(jobModel.getResultsFolderURI());
		}
		// if change from not-finished to finished, set finishedTime
		if (JobStatus.hasFinished(jobModel.getStatus()) && !JobStatus.hasFinished(job.getStatus())) {
			// job.finishedTime should already be populated by the CompM, only update here if missing
			if (job.getFinishedTime() == null) {
				job.setFinishedTime(new Date());
			}
			job.setDuration(jobModel.getDuration());
		}

		job.setStatus(jobModel.getStatus());

		if (job.getExcutorDID() != null && !job.getExcutorDID().equals(jobModel.getExecutorDID()))
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					"Illegal attempt made to update executorDID of job");
		job.setExcutorDID(jobModel.getExecutorDID());

		// merge Results
		User submitter = job.getSubmitter();
		submitter.setTom(job.getTom());

		if (jobModel.getUserVolumes() != null) {
			Map<Long, ComputeDomainUserVolumeModel> cduvms = dockerComputeDomainManager.queryMountableUserVolumes(
					new UserProfile(submitter),
					job.getComputeDomain().getId());

			jobModel.getUserVolumes().stream()
				.filter(newRUV -> job.getUserVolume().stream()
						.noneMatch(existingRUV -> existingRUV.getUserVolume().getId().equals(newRUV.getUserVolumeId())))
				.forEach(newRUV -> {
					ComputeDomainUserVolumeModel cduvm = cduvms.get(newRUV.getUserVolumeId());
					if (cduvm == null)
						throw new RegistrationInvalidException("User does not have access to the user volume specified");

					RequiredUserVolume rv = new RequiredUserVolume(job);
					UserVolume uv = job.getTom().find(UserVolume.class, newRUV.getUserVolumeId());
					rv.setUserVolume(uv);
					rv.setPathOnComputeDomain(cduvm.getFullPath());
					rv.setNeedsWriteAccess(newRUV.isNeedsWriteAccess());
					job.getUserVolume().add(rv);
				});
		}

		// First check that there can be at most 1 message each for the labels TIMEOUT
		// ERROR
		if (jobModel.getMessages() != null) {
			List<JobMessageModel> cleanedMessages = new ArrayList<>();
			JobMessageModel timeoutMessage = null;
			for (JobMessageModel jmm : jobModel.getMessages()) {
				if (JobMessageModel.TIMEOUT.equals(jmm.getLabel())) {
					if (timeoutMessage == null || jmm.getId() != null)
						timeoutMessage = jmm;
				} else {
					cleanedMessages.add(jmm);
				}
			}
			if (timeoutMessage != null)
				cleanedMessages.add(timeoutMessage);
			jobModel.setMessages(cleanedMessages);
		}

		// merge Messages. Note, messages can not be removed, hence addOnly argument set
		// to true.
		CollectionMerger<JobMessage, JobMessageModel> messages = new CollectionMerger<>(job.getMessage(),
				jobModel.getMessages(), true);
		for (JobMessage r : messages.getRemove())
			job.getMessage().remove(r);
		for (JobMessageModel jrm : messages.getInsert()) {
			JobMessage r = new JobMessage(job);
			updateMessage(r, jrm);
		}
		int i = 0;
		for (JobMessage r : messages.getUpdateD())
			updateMessage(r, messages.getUpdateM().get(i++));

	}

	/**
	 *
	 * @param jobModel
	 * @param compm
	 * @throws VOURPException
	 */
	public COMPMJobModel updateJob(COMPMJobModel jobModel, long compmId) throws VOURPException {
		TransientObjectManager tom = vourpContext.newTOM();
		Job job = getJob(tom, Job.class, compmId, jobModel.getId());
		updateJobContent(jobModel, job);

		tom.persist();
		return newCOMPMJobModel(job, true);
	}

	/**
	 *
	 * @param jobModel
	 * @param compm
	 * @throws VOURPException
	 */
	public COMPMDockerJobModel updateDockerJob(COMPMDockerJobModel jobModel, long compmId) throws VOURPException {
		TransientObjectManager tom = vourpContext.newTOM();
		DockerJob job = getJob(tom, DockerJob.class, compmId, jobModel.getId());
		updateJobContent(jobModel, job);

		if (JobStatus.hasStarted(jobModel.getStatus()) && !JobStatus.hasStarted(job.getStatus())) {
			job.setFullDockerCommand(dockerCommandFromArray(jobModel.getFullDockerCommand()));
		}

		tom.persist();
		return (COMPMDockerJobModel) newCOMPMJobModel(job, true);
	}

	/**
	 *
	 * @param jobModel
	 * @param compm
	 * @throws VOURPException
	 */
	public RDBJobModel updateRDBJob(RDBJobModel jobModel, long compmId) throws VOURPException {
		TransientObjectManager tom = vourpContext.newTOM();
		RDBJob job = getJob(tom, RDBJob.class, compmId, jobModel.getId());
		updateJobContent(jobModel, job);

		updateRDBJobTargets(jobModel, job);

		tom.persist();
		return (RDBJobModel) newCOMPMJobModel(job, true);
	}

	private <T extends Job> T getJob(TransientObjectManager tom, Class<T> jobType, long compmId, long jobId) {
		Query q = tom.createQuery("select j from Job j where j.id=:id and j.runBy.id=:compmid")
				.setParameter("id", jobId)
				.setParameter("compmid", compmId);
		T job = tom.queryOne(q, jobType);
		if (job == null)
			throw new IllegalStateException(
					String.format("No job found with id '%d' for compm with id '%s'", jobId, compmId));
		return job;
	}

	/**
	 * Updates to the RDBJob state made by the executor.<br/>
	 *
	 * @param jm
	 * @param j
	 */
	private void updateRDBJobTargets(RDBJobModel jm, RDBJob j) throws VOURPException {
		j.setSql(jm.getSql());

		Map<Long, RDBJobTarget> jts = new HashMap<>();
		for (RDBJobTarget t : j.getTarget())
			jts.put(t.getId(), t);

		for (RDBTargetModel tm : jm.getTargets()) {
			Long id = tm.getId();
			if (id != null) {
				RDBJobTarget t = jts.get(id);
				if (t == null)
					throw new VOURPException(VOURPException.ILLEGAL_STATE,
							"RDBJobModel expects RDBJobTarget that does not exist on RDBJob");
				// ONLY location may be updated
				t.setLocation(tm.getLocation());
			} else {
				RDBJobTarget t = new RDBJobTarget(j);
				t.setLocation(tm.getLocation());
				if (tm.getType() != null)
					t.setTargetType(RDBTargetType.fromValue(tm.getType()));
				t.setResultNumber(tm.getResultNumber());
			}
		}
	}

	private void updateMessage(JobMessage r, JobMessageModel jrm) {
		r.setContent(jrm.getContent());
		if (jrm.getLabel() != null)
			r.setLabel(MessageType.valueOf(jrm.getLabel()));
	}

	private DockerComputeDomain findDockerComputeDomainForAPIEndpoint(String endpoint, TransientObjectManager tom) {
		Query q = tom.createQuery("select dcd from DockerComputeDomain dcd where dcd.apiEndpoint=:api")
				.setParameter("api", endpoint);
		return tom.queryOne(q, DockerComputeDomain.class);
	}

	private RDBComputeDomain findRDBComputeDomain(Long id, TransientObjectManager tom) {
		Query q = tom.createQuery("select rcd from RDBComputeDomain rcd where rcd.id=:id").setParameter("id", id);
		return tom.queryOne(q, RDBComputeDomain.class);
	}

	private DatabaseContext queryDatabaseContextForName(Long domainId, String name, TransientObjectManager tom) {
		Query q = tom.createQuery("SELECT dbc FROM DatabaseContext dbc WHERE dbc.container.id=:domainId And Lower(dbc.name)=Lower(:name)")
                .setParameter("domainId", domainId).setParameter("name", name);
		return tom.queryOne(q, DatabaseContext.class);
	}

	public COMPMModel newCOMPMModel(COMPM compm, UserProfile user) {
		COMPMModel cm = new COMPMModel();

		if (compm != null) {
			cm.setId(compm.getId());
			cm.setUuid(compm.getUuid());
			cm.setLabel(compm.getLabel());
			cm.setDefaultJobTimeout(compm.getDefaultJobTimeout());
			cm.setDefaultJobsPerUser(compm.getDefaultJobsPerUser());
			cm.setDescription(compm.getDescription());
			cm.setCreatorUserid(compm.getCreatorUserid());
		}
		List<UserDockerComputeDomainModel> cds = new ArrayList<>();
		cm.setAllComputeDomains(cds);
		// use (includeBatch=false, includeInteractive=true) in next call, as 'batch'
		// are those that have already been assigned to a COMPM !
		Collection<UserDockerComputeDomainModel> l = dockerComputeDomainManager.queryUserDockerComputeDomains(user,
				true, true);
		for (UserDockerComputeDomainModel o : l) {
			cds.add(o);
		}

		return cm;
	}

	public DBCOMPMModel newDBCOMPMModel(COMPM compm) {
		DBCOMPMModel cm = new DBCOMPMModel();

		if (compm != null) {
			cm.setId(compm.getId());
			cm.setUuid(compm.getUuid());
			cm.setLabel(compm.getLabel());
			cm.setDescription(compm.getDescription());
			cm.setCreatorUserid(compm.getCreatorUserid());
			cm.setComputeDomain(compm.getComputeDomain().getId());
			cm.setDefaultJobsPerUser(compm.getDefaultJobsPerUser());
			cm.setDefaultJobTimeout(compm.getDefaultJobTimeout());
		}

		return cm;
	}

	private COMPMDockerJobModel newDockerJobModel(DockerJob job, boolean fillDetails) {
		COMPMDockerJobModel jm = new COMPMDockerJobModel(job.getId());

		jm.setUsername(job.getSubmitter().getUsername());
		jm.setCommand(job.getCommand());
		jm.setFullDockerCommand(dockerCommand2Array(job.getFullDockerCommand()));
		jm.setScriptURI(job.getScriptURI());
		jm.setDockerComputeEndpoint(job.getComputeDomain().getApiEndpoint());
		jm.setDockerComputeResourceContextUUID(job.getComputeDomain().getResourceContext().getUuid());

		if (job.getImage() != null) {
			jm.setDockerImageName(job.getImage().getName());
		} else {
			jm.setDockerImageName("UNKNOWN");
		}

		if (fillDetails) {
			List<RequiredVolume> rs = job.getRequiredVolume();
			List<VolumeContainerModel> jrms = new ArrayList<>();
			if (rs != null && !rs.isEmpty()) {
				for (RequiredVolume r : rs) {
					VolumeContainerModel vcm = newVolumeContainerModel(r.getVolume());
					vcm.setWritable(r.getNeedsWriteAccess());
					jrms.add(vcm);
				}
			}
			jm.setVolumeContainers(jrms);

			List<RequiredUserVolume> uvs = job.getUserVolume();
			List<RequiredUserVolumeModel> uvms = new ArrayList<>();
			if (uvs != null && !uvs.isEmpty()) {
				for (RequiredUserVolume uv : uvs) {
					RequiredUserVolumeModel uvm = new RequiredUserVolumeModel(uv.getId());
					uvm.setUserVolumeId(uv.getUserVolume().getId());
					uvm.setFullPath(uv.getPathOnComputeDomain());
					uvm.setNeedsWriteAccess(uv.getNeedsWriteAccess());
					uvms.add(uvm);
				}
			}
			jm.setUserVolumes(uvms);

		}

		return jm;
	}

	private RDBJobModel newRDBJobModel(RDBJob job, boolean fillDetails) {
		RDBJobModel jm = new RDBJobModel(job.getId());
		jm.setUsername(job.getSubmitter().getUsername());
		jm.setInputSql(job.getInputSQL());
		jm.setDatabaseContextName(job.getDatabaseContext().getName());
		jm.setRdbDomainId(job.getComputeDomain().getId());
		jm.setRdbDomainName(job.getComputeDomain().getName());
		if (job.getComputeDomain().getResourceContext() != null)
			jm.setRdbResourceContextUUID(job.getComputeDomain().getResourceContext().getUuid());

		if (fillDetails) {
			List<RDBJobTarget> targets = job.getTarget();
			List<RDBTargetModel> tms = new ArrayList<>();
			if (targets != null && !targets.isEmpty()) {
				for (RDBJobTarget t : targets) {
					RDBTargetModel tm = new RDBTargetModel(t.getId());
					tm.setLocation(t.getLocation());
					tm.setType(t.getTargetType().toString());
					tm.setResultNumber(t.getResultNumber());
					tms.add(tm);
				}
			}
			jm.setTargets(tms);
		}

		return jm;
	}

	/*
	 * Invalid docker command strings are logged in case they indicate a problem in the database
	 * (not throwing exceptions since these were previously allowed, and Postel's law)
	 */
	private String[] dockerCommand2Array(String c) {
		if (StringUtils.isEmpty(c))
			return EMPTY_STRING_ARRAY;

		ObjectMapper om = RACMUtil.newObjectMapper();
		JsonNode json;
		try {
			json = om.readTree(c);
		} catch (IOException e) {
			logger.error("Expected dockerCommand to be parsable as JSON, instead it is: " + c,
					e);
			return EMPTY_STRING_ARRAY;
		}

		if (json instanceof ArrayNode) {
			List<String> strings = new ArrayList<>();
			Iterator<JsonNode> nodes = ((ArrayNode) json).elements();
			while (nodes.hasNext())
				strings.add(nodes.next().asText());
			return strings.toArray(new String[] {});
		} else {
			logger.error("Expected dockerCommand to parse as an array, instead it is: " + c);
			return EMPTY_STRING_ARRAY;
		}
	}

	private String dockerCommandFromArray(String[] c) {
		if (c == null)
			return null;
		ObjectMapper om = RACMUtil.newObjectMapper();

		JsonNode json = om.valueToTree(c);
		return json.toString();
	}

	VolumeContainerModel newVolumeContainerModel(VolumeContainer vc) {
		VolumeContainerModel vcm = new VolumeContainerModel(vc.getId());
		fillComputeResourceModel(vc, vcm);
		return vcm;
	}

	VolumeContainer newVolumeContainer(VolumeContainerModel vcm, DockerComputeDomain dcd) {
		VolumeContainer vc = new VolumeContainer(dcd);
		fillComputeResource(vc, vcm);
		return vc;
	}

	private RootVolume queryRootVolume(TransientObjectManager tom, Long id) {
		Query q = tom.createQuery("SELECT rv from RootVolume rv where rv.id = :id").setParameter("id", id);
		return tom.queryOne(q, RootVolume.class);
	}

	RootVolumeOnComputeDomain newRootVolumeOnComputeDomain(RootVolumeOnComputeDomainModel rvm, ComputeDomain cd)
			throws VOURPException {
		RootVolume rv = queryRootVolume(cd.getTom(), rvm.getRootVolumeId());
		if (rv == null)
			throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,
					String.format("%d does not identify an existing RootVolume", rvm.getRootVolumeId()));
		RootVolumeOnComputeDomain rvcd = new RootVolumeOnComputeDomain(cd);
		rvcd.setDisplayName(rvm.getDisplayName());
		rvcd.setPath(rvm.getPathOnCD());
		rvcd.setRootVolume(rv);
		return rvcd;
	}

	private RootVolumeOnComputeDomainModel newRootVolumeOnComputeDomainModel(RootVolumeOnComputeDomain rv) {
		RootVolumeOnComputeDomainModel rvm = new RootVolumeOnComputeDomainModel();
		rvm.setDisplayName(rv.getDisplayName());
		rvm.setPathOnCD(rv.getPath());
		rvm.setRootVolumeId(rv.getRootVolume().getId());
		rvm.setId(rv.getId());
		return rvm;
	}

	public DockerComputeDomainModel newDockerComputeDomainModel(DockerComputeDomain o, boolean doRecurse) {
		DockerComputeDomainModel m = new DockerComputeDomainModel(o.getId());
		m.setApiEndpoint(o.getApiEndpoint());
		m.setRacmUUID(o.getResourceContext().getUuid());
		m.setName(o.getName());
		m.setDescription(o.getDescription());
		m.setPublisherDID(o.getPublisherDID());
		List<DockerImageModel> images = new ArrayList<>();
		List<VolumeContainerModel> volumes = new ArrayList<>();
		List<RootVolumeOnComputeDomainModel> rootvolumes = new ArrayList<>();
		m.setImages(images);
		m.setVolumes(volumes);
		m.setRootVolumes(rootvolumes);
		if (doRecurse) {
			for (ComputeResource r : o.getComputeResource()) {
				if (r instanceof DockerImage)
					images.add(newDockerImageModel((DockerImage) r));
				else if (r instanceof VolumeContainer)
					volumes.add(newVolumeContainerModel((VolumeContainer) r));
			}
			for (RootVolumeOnComputeDomain rv : o.getRootVolume())
				rootvolumes.add(newRootVolumeOnComputeDomainModel(rv));
		}
		return m;
	}

	/**
	 *
	 * @param o
	 * @param doRecurse
	 * @return
	 */
	UserDockerComputeDomainModel newUserDockerComputeDomainModel(DockerComputeDomain o, boolean doRecurse) {
		UserDockerComputeDomainModel m = new UserDockerComputeDomainModel(o.getId());
		m.setApiEndpoint(o.getApiEndpoint());
		m.setRacmUUID(o.getResourceContext().getUuid());
		m.setName(o.getName());
		m.setDescription(o.getDescription());
		m.setPublisherDID(o.getPublisherDID());
		List<DockerImageModel> images = new ArrayList<>();
		List<VolumeContainerModel> volumes = new ArrayList<>();
		List<ComputeDomainUserVolumeModel> userVolumes = new ArrayList<>();
		m.setImages(images);
		m.setVolumes(volumes);
		m.setUserVolumes(userVolumes);
		if (doRecurse) {
			for (ComputeResource r : o.getComputeResource()) {
				if (r instanceof DockerImage)
					images.add(newDockerImageModel((DockerImage) r));
				else if (r instanceof VolumeContainer)
					volumes.add(newVolumeContainerModel((VolumeContainer) r));
			}
		}
		return m;
	}

	DockerImageModel newDockerImageModel(DockerImage di) {
		DockerImageModel dim = new DockerImageModel(di.getId());
		fillComputeResourceModel(di, dim);
		return dim;
	}

	private void fillComputeResourceModel(ComputeResource cr, ComputeResourceModel crm) {
		crm.setDescription(cr.getDescription());
		crm.setName(cr.getName());
		crm.setPublisherDID(cr.getPublisherDID());
		crm.setRacmUUID(cr.getResource().getUuid());
	}

	DockerImage newDockerImage(DockerImageModel dim, DockerComputeDomain dcd) {
		DockerImage di = new DockerImage(dcd);
		fillComputeResource(di, dim);
		return di;
	}

	private void fillComputeResource(ComputeResource cr, ComputeResourceModel crm) {
		cr.setDescription(crm.getDescription());
		cr.setName(crm.getName());
		cr.setPublisherDID(crm.getPublisherDID());
	}

	DatabaseContext newDatabaseContext(DatabaseContextModel dcm, RDBComputeDomain rcd) {
		DatabaseContext dc = new DatabaseContext(rcd);
		dc.setDescription(dcm.getDescription());
		dc.setName(dcm.getName());
		dc.setPublisherDID(dcm.getPublisherDID());
		return dc;
	}

	public RDBComputeDomainModel newRDBComputeDomainModel(RDBComputeDomain o, boolean doRecurse) {
		RDBComputeDomainModel m = new RDBComputeDomainModel(o.getId());
		m.setApiEndpoint(o.getApiEndpoint());
		m.setRacmUUID(o.getResourceContext().getUuid());
		m.setName(o.getName());
		m.setDescription(o.getDescription());
		m.setPublisherDID(o.getPublisherDID());
		List<DatabaseContextModel> databaseContexts = new ArrayList<>();
		m.setDatabaseContexts(databaseContexts);
		if (doRecurse) {
			for (DatabaseContext r : o.getDatabaseContext()) {
				databaseContexts.add(newDatabaseContextModel(r));
			}
		}
		return m;
	}

	public DatabaseContextModel newDatabaseContextModel(DatabaseContext dbc) {
		DatabaseContextModel dbcm = new DatabaseContextModel(dbc.getId());
		dbcm.setDescription(dbc.getDescription());
		dbcm.setName(dbc.getName());
		dbcm.setPublisherDID(dbc.getPublisherDID());
		dbcm.setRacmUUID(dbc.getResource().getUuid());
		return dbcm;
	}
}
