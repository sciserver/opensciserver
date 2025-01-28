package org.sciserver.springapp.racm.jobm.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.jobm.model.RootVolumeOnComputeDomainModel;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.job.ComputeDomain;
import edu.jhu.job.RootVolumeOnComputeDomain;
import edu.jhu.rac.Action;
import edu.jhu.rac.ActionCategory;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.ResourceType;
import edu.jhu.rac.Role;
import edu.jhu.user.User;

@Service
public class ComputeDomainManager {
	private final JOBMModelFactory jobmModelFactory;
	@Autowired
	public ComputeDomainManager(JOBMModelFactory jobmModelFactory) {
		this.jobmModelFactory = jobmModelFactory;
	}

	public static ContextClass initRDBComputeDomainCC(User sysUser) throws VOURPException {
		TransientObjectManager tom = sysUser.getTom();
		String name = RACMNames.RDB_COMPUTE_DOMAIN_CC_NAME;
		ContextClass cc = RACMUtil.queryContextClass(name, tom);
		if(cc != null)
			return null;
		cc = new ContextClass(tom);
		cc.setName(name);
		cc.setDescription("Represents SciServer RDB Compute Domains.");
		cc.setCreator(sysUser);

		// ResourceType 'Context'
		ResourceType rt = RACMUtil.addRootContextRT(cc);

		Role admin = new Role(rt);
		admin.setName(RACMNames.R_COMPUTE_DOMAIN_ROOT_ADMIN);

		Action a = new Action(rt);
		a.setName(RACMNames.A_REGISTER_DATABASE_CONTEXT);
		a.setDescription("Action of creating a database context.");
		a.setCategory(ActionCategory.C);

		RACMUtil.addAction(admin, a);


		// ResourceType 'DatabaseContext'
		rt = new ResourceType(cc);
		rt.setName(RACMNames.RT_DATABASE_CONTEXT);
		rt.setDescription("Represents database contexts that queries can be sent to.");

		Action databaseContextRead = new Action(rt);
		databaseContextRead.setName(RACMNames.A_DATABASE_CONTEXT_QUERY);
		databaseContextRead.setDescription("Action of connecting to and sending a query to a database context.");
		databaseContextRead.setCategory(ActionCategory.R);

		Action databaseContextUpdate = new Action(rt);
		databaseContextUpdate.setName(RACMNames.A_DATABASE_CONTEXT_UPDATE);
		databaseContextUpdate.setDescription("Action of updating a database context, creating/deleting tables/views/indexes/functions; inserting, updating, deleting from tables; etc.");
		databaseContextUpdate.setCategory(ActionCategory.U);

		Action databaseContextGrant = new Action(rt);
		databaseContextGrant.setName(RACMNames.A_DATABASE_CONTEXT_GRANT);
		databaseContextGrant.setDescription("Action of granting access to a database context.");
		databaseContextGrant.setCategory(ActionCategory.G);

		Role adminRoleOnDatabaseContext = new Role(rt);
		adminRoleOnDatabaseContext.setName(RACMNames.R_DATABASE_CONTEXT_ADMIN);
		adminRoleOnDatabaseContext.setDescription("Allow reading/writing of this database context");
		RACMUtil.addAction(adminRoleOnDatabaseContext, databaseContextUpdate);
		RACMUtil.addAction(adminRoleOnDatabaseContext, databaseContextRead);
		RACMUtil.addAction(adminRoleOnDatabaseContext, databaseContextGrant);

		Role databaseContextReader = new Role(rt);
		databaseContextReader.setName(RACMNames.R_DATABASE_CONTEXT_READER);
		databaseContextReader.setDescription("Allow querying of this database context");
		RACMUtil.addAction(databaseContextReader, databaseContextRead);

		Role databaseContextWriter = new Role(rt);
		databaseContextWriter.setName(RACMNames.R_DATABASE_CONTEXT_WRITER);
		databaseContextWriter.setDescription("Allow updating of this database context");
		RACMUtil.addAction(databaseContextWriter, databaseContextUpdate);
		RACMUtil.addAction(databaseContextWriter, databaseContextRead);

		return cc;
	}


	public static ContextClass initDockerComputeDomainCC(User sysUser) throws VOURPException {
		TransientObjectManager tom = sysUser.getTom();
		String name = RACMNames.DOCKER_COMPUTE_DOMAIN_CC_NAME;
		ContextClass cc = RACMUtil.queryContextClass(name, tom);
		if(cc != null)
			return null;
		cc = new ContextClass(tom);
		cc.setName(name);
		cc.setDescription("Represents SciServer Compute Domains.");
		cc.setCreator(sysUser);

		// ResourceType 'Context'
		ResourceType rt = RACMUtil.addRootContextRT(cc);

		Role admin = new Role(rt);
		admin.setName(RACMNames.R_COMPUTE_DOMAIN_ROOT_ADMIN);

		Action a = new Action(rt);
		a.setName(RACMNames.A_REGISTER_VOLUME_CONTAINER);
		a.setDescription("Action of creating a public volume container.");
		a.setCategory(ActionCategory.C);

		RACMUtil.addAction(admin, a);

		a = new Action(rt);
		a.setName(RACMNames.A_REGISTER_DOCKER_IMAGE);
		a.setDescription("Action of creating a docker image.");
		a.setCategory(ActionCategory.C);

		RACMUtil.addAction(admin, a);


		// ResourceType 'VolumeContainer'
		rt = new ResourceType(cc);
		rt.setName(RACMNames.RT_VOLUME_CONTAINER);
		rt.setDescription("Represents public, shared Docker volume containers.");

		Action volumeContainerRead = new Action(rt);
		volumeContainerRead.setName(RACMNames.A_VOLUME_CONTAINER_READ);
		volumeContainerRead.setDescription("Action of attaching a shared volume container when creating a docker container");
		volumeContainerRead.setCategory(ActionCategory.R);


		Action volumeContainerWrite = new Action(rt);
		volumeContainerWrite.setName(RACMNames.A_VOLUME_CONTAINER_WRITE);
		volumeContainerWrite.setDescription("Action of updating a shared volume container.");
		volumeContainerWrite.setCategory(ActionCategory.U);

		Action volumeContainerGrant = new Action(rt);
		volumeContainerGrant.setName(RACMNames.A_VOLUME_CONTAINER_GRANT);
		volumeContainerGrant.setDescription("Action of granting access to a shared volume container.");
		volumeContainerGrant.setCategory(ActionCategory.G);

		Role adminRoleOnvolumeContainer = new Role(rt);
		adminRoleOnvolumeContainer.setName(RACMNames.R_VOLUME_CONTAINER_ADMIN);
		adminRoleOnvolumeContainer.setDescription("Allow reading/writing of this volume container");
		RACMUtil.addAction(adminRoleOnvolumeContainer, volumeContainerRead);
		RACMUtil.addAction(adminRoleOnvolumeContainer, volumeContainerGrant);

		Role userRoleOnvolumeContainer = new Role(rt);
		userRoleOnvolumeContainer.setName(RACMNames.R_VOLUME_CONTAINER_USER);
		userRoleOnvolumeContainer.setDescription("Allow usage of this volume container");
		RACMUtil.addAction(userRoleOnvolumeContainer, volumeContainerRead);


		// ResourceType 'DockerImage'
		rt = new ResourceType(cc);
		rt.setName(RACMNames.RT_DOCKER_IMAGE);
		rt.setDescription("Represents a Docker Image with which one can create a docker container.");

		Action dockerImageCreateContainer = new Action(rt);
		dockerImageCreateContainer.setName(RACMNames.A_DOCKER_IMAGE_CREATE_CONTAINER);
		dockerImageCreateContainer.setDescription("Action of creating a container for this image.");
		dockerImageCreateContainer.setCategory(ActionCategory.X);

		Action dockerImageGrant = new Action(rt);
		dockerImageGrant.setName(RACMNames.A_DOCKER_IMAGE_GRANT);
		dockerImageGrant.setDescription("Action of granting the right to create a container for this image.");
		dockerImageGrant.setCategory(ActionCategory.G);

		Action dockerImageUnregisterImage = new Action(rt);
		dockerImageUnregisterImage.setName(RACMNames.A_DOCKER_IMAGE_UNREGISTER);
		dockerImageUnregisterImage.setDescription("Action of unregistering the docker image.");
		dockerImageUnregisterImage.setCategory(ActionCategory.D);

		Role adminRoleOnDockerImage = new Role(rt);
		adminRoleOnDockerImage.setName(RACMNames.R_VOLUME_CONTAINER_ADMIN);
		adminRoleOnDockerImage.setDescription("Allow reading/writing of this volume container");
		RACMUtil.addAction(adminRoleOnDockerImage, dockerImageCreateContainer);
		RACMUtil.addAction(adminRoleOnDockerImage, dockerImageGrant);

		Role userRoleOnDockerImage = new Role(rt);
		userRoleOnDockerImage.setName(RACMNames.R_DOCKER_IMAGE_USER);
		userRoleOnDockerImage.setDescription("Allow usage of this docker image");
		RACMUtil.addAction(userRoleOnDockerImage, dockerImageCreateContainer);

		// ResourceType 'DockerContainer'
		rt = new ResourceType(cc);
		rt.setName(RACMNames.RT_DOCKER_CONTAINER);
		rt.setDescription("Represents a Docker Container.");

		Role owner = new Role(rt);
		owner.setName(RACMNames.R_DOCKER_CONTAINER_OWNER);

		Role reader = new Role(rt);
		reader.setName(RACMNames.R_DOCKER_CONTAINER_READER);

		Role writer = new Role(rt);
		writer.setName(RACMNames.R_DOCKER_CONTAINER_WRITER);


		a = new Action(rt);
		a.setName(RACMNames.A_DOCKER_CONTAINER_STOP);
		a.setDescription("Action of stopping the container.");
		a.setCategory(ActionCategory.X);
		RACMUtil.addAction(owner,a);

		a = new Action(rt);
		a.setName(RACMNames.A_DOCKER_CONTAINER_START);
		a.setDescription("Action of stopping the container.");
		a.setCategory(ActionCategory.X);
		RACMUtil.addAction(owner,a);

		a = new Action(rt);
		a.setName(RACMNames.A_DOCKER_CONTAINER_GRANT);
		a.setDescription("Action of granting access to a container.");
		a.setCategory(ActionCategory.G);
		RACMUtil.addAction(owner,a);

		a = new Action(rt);
		a.setName(RACMNames.A_DOCKER_CONTAINER_DELETE);
		a.setDescription("Action of deleting the container.");
		a.setCategory(ActionCategory.D);
		RACMUtil.addAction(owner,a);

		a = new Action(rt);
		a.setName(RACMNames.A_DOCKER_CONTAINER_READ);
		a.setDescription("Action of accessing the contents of the container in read-only mode.");
		a.setCategory(ActionCategory.R);
		RACMUtil.addAction(owner,a);
		RACMUtil.addAction(reader,a);
		RACMUtil.addAction(writer,a);

		a = new Action(rt);
		a.setName(RACMNames.A_DOCKER_CONTAINER_WRITE);
		a.setDescription("Action of accessing the contents of the container in read-write mode.");
		a.setCategory(ActionCategory.U);
		RACMUtil.addAction(owner,a);
		RACMUtil.addAction(writer,a);

		return cc;
	}

	void synchronizeRootVolumes(ComputeDomain cd, List<RootVolumeOnComputeDomainModel> rvms) throws VOURPException{
		Map<Long,RootVolumeOnComputeDomain> rvs=new HashMap<>();
		for(RootVolumeOnComputeDomain rv: cd.getRootVolume())
			rvs.put(rv.getId(), rv);

		for(RootVolumeOnComputeDomainModel rvm: rvms){
			if(rvm.getId() == null)
				jobmModelFactory.newRootVolumeOnComputeDomain(rvm, cd);
			else {
				RootVolumeOnComputeDomain rv = rvs.get(rvm.getId());
				if(rv == null)
					throw new VOURPException(VOURPException.ILLEGAL_ARGUMENT,String.format("RootVolumeOnComputeDomain '%s' does not exist on compute domain repository",rvm.getId()));
				rvs.remove(rv.getId());
			}
		}
		// delete remaining RootVolumeOnComputeDomain, those that were not represented in the DockerComputeDomainModel
		for(RootVolumeOnComputeDomain rv: rvs.values()){
		  cd.getRootVolume().remove(rv);
		}

	}

}
