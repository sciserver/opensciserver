package org.sciserver.springapp.racm.jobm.application;

import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACM;
import org.sciserver.springapp.racm.utils.RACMException;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.job.ComputeDomain;
import edu.jhu.job.DockerImage;
import edu.jhu.job.VolumeContainer;
import edu.jhu.user.User;
/**
 * This class defines utility methods to allow JOBM functions to check whether a user is allowed to do particular things.<br/>
 * The methods define these explicit requests into calls to RACM with appropriate parameters.
 * These methods will throw an exception if the user is not allowed to perform the requested action.
 * It is done like this iso just returning a boolean so that the appropriate, standardized exception can be thrown.
 *
 * @author gerard
 *
 */
@Service
public class JOBMAccessControl {
	private final RACM racm;
	@Autowired
	public JOBMAccessControl(RACM racm) {
		this.racm = racm;
	}

	void tryUseDockerImage(UserProfile u, DockerImage di ) throws RACMException{
		racm.tryDoActionOnResource(u, di.getResource().getUuid(), RACMNames.A_DOCKER_IMAGE_CREATE_CONTAINER);
	}
	void tryUseVolumeContainer(UserProfile u, VolumeContainer vc ) throws RACMException{
		racm.tryDoActionOnResource(u, vc.getResource().getUuid(), RACMNames.A_VOLUME_CONTAINER_READ);
	}
	void tryWriteVolumeContainer(UserProfile u, VolumeContainer vc ) throws RACMException{
        racm.tryDoActionOnResource(u, vc.getResource().getUuid(), RACMNames.A_VOLUME_CONTAINER_WRITE);
    }

	void tryUserSubmitJob(UserProfile up) throws RACMException{
		// no-op for now
	}
	boolean canRegisterCOMPM(UserProfile u) {
		return racm.canUserDoRootAction(u.getUsername(), RACMNames.A_REGISTER_COMPMM);
	}
	boolean canRegisterComputeDomain(User u) {
		return racm.canUserDoRootAction(u.getUsername(),  RACMNames.A_REGISTER_COMPUTE_DOMAIN);
	}
	boolean canEditComputeDomain(User u, ComputeDomain cd) {
		return racm.doesUserHaveRoleOnResource(u.getUsername(), cd.getResourceContext().getUuid(), RACMNames.CONTEXT_ROOTRESOURCE_PUBDID, RACMNames.R_COMPUTE_DOMAIN_ROOT_ADMIN);
	}
}
