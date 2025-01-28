package org.sciserver.springapp.racm.jobm.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.jobm.model.COMPMModel;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.job.COMPM;
import edu.jhu.job.DockerComputeDomain;

@Service
public class COMPMManager {
	private final JOBMAccessControl jobmAccessControl;
	private final JOBMModelFactory jobmModelFactory;
	private final DockerComputeDomainManager dockerComputeDomainManager;

	@Autowired
	public COMPMManager(JOBMAccessControl jobmAccessControl, JOBMModelFactory jobmModelFactory,
			DockerComputeDomainManager dockerComputeDomainManager) {
		this.jobmAccessControl = jobmAccessControl;
		this.jobmModelFactory = jobmModelFactory;
		this.dockerComputeDomainManager = dockerComputeDomainManager;
	}

	public COMPMModel createCOMPM(COMPMModel compmModel, UserProfile up) throws VOURPException {
		if (!jobmAccessControl.canRegisterCOMPM(up))
			throw new InsufficientPermissionsException("register a COMPM");

		TransientObjectManager tom = up.getTom();
		COMPM compm = new COMPM(tom);
		compm.setCreatorUserid(up.getUser().getUserId());
		compm.setLabel(compmModel.getLabel());
		compm.setUuid(UUID.randomUUID().toString());
		compm.setDescription(compmModel.getDescription());
		compm.setDefaultJobTimeout(compmModel.getDefaultJobTimeout());
		compm.setDefaultJobsPerUser(compmModel.getDefaultJobsPerUser());

		long domainId = -1;

		if (compmModel.getAllComputeDomains() != null && !compmModel.getAllComputeDomains().isEmpty()) {
			UserDockerComputeDomainModel dcdm = compmModel.getAllComputeDomains().get(0);
			domainId = dcdm.getId();
		} else if (compmModel.getComputeDomainId() != null) {
			domainId = compmModel.getComputeDomainId();
		}
		if (domainId > 0) {
			DockerComputeDomain dcd = dockerComputeDomainManager.queryDockerComputeDomainForId(domainId, tom);
			if (dcd == null) {
				throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT, String.format(
						"Compute domain with id = %d does not exist or user has no access rights on it", domainId));
			}
			compm.setComputeDomain(dcd);
		}
		tom.persist();
		return jobmModelFactory.newCOMPMModel(compm, up);
	}

	/**
	 * Update the COMPM represented by the specified COMPMModel with the contents of
	 * that model object.<br/>
	 *
	 * Restrictions: ComputeDomain cannot be updated!
	 *
	 * @param compmModel
	 * @param up
	 * @return
	 * @throws VOURPException
	 */
	public COMPMModel updateCOMPM(COMPMModel compmModel, UserProfile up) throws VOURPException {
		TransientObjectManager tom = up.getTom();
		COMPM compm = find(compmModel.getUuid(), tom);
		if (compm == null)
			throw new VOURPException("Unable to find COMPM with uuid = " + compmModel.getUuid());

		if (!up.getUser().getUserId().equals(compm.getCreatorUserid()))
			throw new VOURPException(
					String.format("Only creator is allowed to update COMPM with uuid = '%s'", compmModel.getUuid()));

		compm.setLabel(compmModel.getLabel());
		compm.setDescription(compmModel.getDescription());
		compm.setDefaultJobTimeout(compmModel.getDefaultJobTimeout());
		compm.setDefaultJobsPerUser(compmModel.getDefaultJobsPerUser());
		tom.persist();
		return jobmModelFactory.newCOMPMModel(compm, up);
	}

	/**
	 * Precondition: user != null && user.getTom() != null
	 *
	 * @param user
	 * @return
	 */
	public List<COMPM> queryCOMPMs(UserProfile user) {
		TransientObjectManager tom = user.getTom();
		Query q = tom.createQuery("select c from COMPM c where c.creatorUserid=:userid").setParameter("userid",
				user.getUserid());
		List<MetadataObject> l = user.getTom().queryJPA(q, false);
		List<COMPM> compms = new ArrayList<>();
		for (MetadataObject o : l)
			compms.add((COMPM) o);
		return compms;
	}

	private COMPM find(String uuid, TransientObjectManager tom) {
		Query q = tom.createQuery("select c from COMPM c where c.uuid=:uuid").setParameter("uuid", uuid);
		return tom.queryOne(q, COMPM.class);
	}

	@Autowired
	private VOURPContext vourpContext;

	public COMPM find(String uuid) {
		return find(uuid, vourpContext.newTOM());
	}
}
