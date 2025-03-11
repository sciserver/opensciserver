package org.sciserver.springapp.racm.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.Query;

import org.eclipse.persistence.config.QueryHints;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.ivoa.dm.model.TransientObjectManager.ChangeSet;
import org.sciserver.racm.rctree.model.ResourceGrants;
import org.sciserver.racm.resources.model.UserResourceModel;
import org.sciserver.racm.resources.v2.model.ResourceModel;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.resources.application.RACMModelFactory;
import org.sciserver.springapp.racm.resources.application.ResourceModelFactory;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.file.DataVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.DockerImage;
import edu.jhu.job.VolumeContainer;
import edu.jhu.rac.Action;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceType;
import edu.jhu.user.User;

@Service
public class RACMAccessControl {
	private final RACM racm;
	private final RACMModelFactory racmModelFactory;
	private final ResourceModelFactory resourceModelFactory;

	@Autowired
	public RACMAccessControl(RACM racm, RACMModelFactory racmModelFactory, ResourceModelFactory resourceModelFactory) {
		this.racm = racm;
		this.racmModelFactory = racmModelFactory;
		this.resourceModelFactory = resourceModelFactory;
	}
	public Resource findResource(String resourceuuid, TransientObjectManager tom) {
		Query q = tom.createQuery("select r from Resource r where r.uuid=:uuid")
				.setParameter("uuid", resourceuuid);
		return tom.queryOne(q, Resource.class);
	}

	public Resource saveResource(ResourceGrants rg, UserProfile up) throws VOURPException{
		TransientObjectManager tom = up.getTom();
		if (!racm.canUserGrantOnResource(up.getUsername(), rg.getResourceUUID())) {
			throw new InsufficientPermissionsException("change resource grants");
		}
		Resource r = findResource(rg.getResourceUUID(),tom);

		ChangeSet changeSet = racmModelFactory.updateResource(r, rg, up);

		tom.persistChangeSet(changeSet,  true);
		return r;
	}

	/**
	 * Return a table with information on all the Resources the specified user has access to, 
	 * but which are OWNED by a resource on another resourcecontext.<br/>
	 * @param up
	 * @return
	 */
	public NativeQueryResult queryServiceOwnedResources(UserProfile up) {
		TransientObjectManager tom = up.getTom();

		String columns = "contextClass,resourceContextUUID,resourceType,resourceUUID" + 
		        ",resourceId,resourcePubDID" + 
		        ",usage,owningResourceName,owningResourceContextEndpoint" + 
		        ",owningContextClassName,owningResourceContextUUID,owningResourceType,owningResourceUUID";
		String sql=String.format("select distinct %s from racm.serviceOwnedUserResources(?) "
		        + "order by 1,2,3",columns);
		Query q = tom.createNativeQuery(sql);
		q.setParameter(1,up.getUsername());

		return new NativeQueryResult(columns,tom.executeNativeQuery(q));
	}

    /**
     * Return a table with information on all the Resources the specified user has access to, including 
     * information on the action the user is allowed to perform.<br/>
     * @param up
     * @return
     */
    public NativeQueryResult queryUserResources(UserProfile up) {
        TransientObjectManager tom = up.getTom();

        String columns = "contextClass,resourceContextUUID,resourceContextAPIEndpoint,resourceContextLabel,resourceType,resourceName,resourcePubDID,resourceuuid,action,actionCategory";
        String sql=String.format("select distinct %s from racm.userActions(?) order by 1,2,3",columns);
        Query q = tom.createNativeQuery(sql);
        q.setParameter(1,up.getUsername());

        NativeQueryResult r = new NativeQueryResult();
        r.setColumns(columns);
        r.setRows(tom.executeNativeQuery(q));
        return r;
    }

	public List<ResourceModel> queryUserResourcesV2(UserProfile up) {
		TransientObjectManager tom = up.getTom();
		List<ResourceModel> output = new ArrayList<>();

		Query q = tom.createNativeQuery(
				"SELECT distinct resourceuuid, actionId FROM racm.userActions(?)")
				.setParameter(1, up.getUsername());
		@SuppressWarnings("unchecked")
		List<Object[]> actions = (List<Object[]>) tom.executeNativeQuery(q);

		Map<String, Set<Long>> actionsPerResource = new HashMap<>();
		for (Object[] row : actions) {
			String resourceUUID = row[0].toString();
			long actionId = Long.parseLong(row[1].toString());

			actionsPerResource.computeIfAbsent(resourceUUID, ignored -> new HashSet<>());
			actionsPerResource.get(resourceUUID).add(actionId);
		}
        // TODO: If user has more than 2100 resources, this will cause the following SQL Server JDBC driver error:
        // com.microsoft.sqlserver.jdbc.SQLServerException: The incoming request has too many parameters. The server
        // supports a maximum of 2100 parameters.
		Query queryResources = tom.createQuery("SELECT r FROM Resource r WHERE r.uuid IN :uuids")
				.setParameter("uuids", actionsPerResource.keySet())
				.setHint(QueryHints.FETCH, "r.resourceType.action");
		Query queryActions = tom.createQuery("SELECT a FROM Action a WHERE a.id IN :ids")
				.setParameter("ids", actionsPerResource.values()
						.stream()
						.flatMap(Collection::stream)
						.collect(Collectors.toSet()));
		Query queryUserVolumes = tom.createQuery("SELECT uv FROM UserVolume uv WHERE uv.resource.uuid IN :uuids")
				.setParameter("uuids", actionsPerResource.keySet());
		Query queryDockerImages = tom.createQuery("SELECT di FROM DockerImage di WHERE di.resource.uuid IN :uuids")
				.setParameter("uuids", actionsPerResource.keySet());
		Query queryVolumeContainers = tom.createQuery("SELECT vc FROM VolumeContainer vc WHERE vc.resource.uuid IN :uuids")
				.setParameter("uuids", actionsPerResource.keySet());
		Query queryDataVolumes = tom.createQuery("SELECT dv FROM DataVolume dv WHERE dv.resource.uuid IN :uuids")
				.setParameter("uuids", actionsPerResource.keySet());
		Query queryDatabaseContexts = tom.createQuery("SELECT dc FROM DatabaseContext dc WHERE dc.resource.uuid IN :uuids")
				.setParameter("uuids", actionsPerResource.keySet());

		Map<String, Resource> allResources;
		Map<Long, Action> allActions;
		Map<String, UserVolume> allUserVolumes;
		Map<String, DockerImage> allDockerImages;
		Map<String, VolumeContainer> allVolumeContainers;
		Map<String, DataVolume> allDataVolumes;
		Map<String, DatabaseContext> allDatabaseContexts;

		try {
			allResources = tom.queryJPA(queryResources, Resource.class)
					.stream()
					.collect(Collectors.toMap(Resource::getUuid, Function.identity()));
			allActions = tom.queryJPA(queryActions, Action.class)
					.stream()
					.collect(Collectors.toMap(Action::getId, Function.identity()));
			allUserVolumes = tom.queryJPA(queryUserVolumes, UserVolume.class)
					.stream()
					.collect(Collectors.toMap(uv -> uv.getResource().getUuid(), Function.identity()));
			allDockerImages = tom.queryJPA(queryDockerImages, DockerImage.class)
					.stream()
					.collect(Collectors.toMap(uv -> uv.getResource().getUuid(), Function.identity()));
			allVolumeContainers = tom.queryJPA(queryVolumeContainers, VolumeContainer.class)
					.stream()
					.collect(Collectors.toMap(uv -> uv.getResource().getUuid(), Function.identity()));
			allDataVolumes = tom.queryJPA(queryDataVolumes, DataVolume.class)
					.stream()
					.collect(Collectors.toMap(dv -> dv.getResource().getUuid(), Function.identity()));
			allDatabaseContexts = tom.queryJPA(queryDatabaseContexts, DatabaseContext.class)
					.stream()
					.collect(Collectors.toMap(uv -> uv.getResource().getUuid(), Function.identity()));
		} catch (VOURPException e) {
			throw new IllegalStateException("A VO-URP query appears to have failed", e);
		}

		for (Map.Entry<String, Set<Long>> resourceWithActions : actionsPerResource.entrySet()) {
			Resource resource = allResources.get(resourceWithActions.getKey());
			List<Action> allowedActions =
					resourceWithActions.getValue().stream().map(allActions::get).collect(Collectors.toList());
			if (allUserVolumes.containsKey(resource.getUuid())) {
				output.add(resourceModelFactory.convertToResourceModel(
						allUserVolumes.get(resource.getUuid()),
						allowedActions));
			} else if (allDockerImages.containsKey(resource.getUuid())) {
				output.add(resourceModelFactory.convertToResourceModel(
						allDockerImages.get(resource.getUuid()),
						allowedActions));
			} else if (allVolumeContainers.containsKey(resource.getUuid())) {
				output.add(resourceModelFactory.convertToResourceModel(
						allVolumeContainers.get(resource.getUuid()),
						allowedActions));
			} else if (allDataVolumes.containsKey(resource.getUuid())) {
				output.add(resourceModelFactory.convertToResourceModel(
						allDataVolumes.get(resource.getUuid()),
						allowedActions));
			} else if (allDatabaseContexts.containsKey(resource.getUuid())) {
				output.add(resourceModelFactory.convertToResourceModel(
						allDatabaseContexts.get(resource.getUuid()),
						allowedActions));
			} else {
				output.add(resourceModelFactory.convertToResourceModel(resource, allowedActions));
			}
		}
		return output;
	}

	/**
	 * Return a table with information on all the Resources managed by the ResourceContext identified by the specified UUID that
	 * the specified user has access to, including information on the action the user is allowed to perform.<br/>
	 * @param up
	 * @return
	 */
	public List<UserResourceModel> queryUserResources(UserProfile up, String resourceContextUUID) {
		TransientObjectManager tom = up.getTom();

		// TODO could be better encapsulated than part here, part in UserResource
		String sql = UserResourceModel.SQL;
		Query q = tom.createNativeQuery(sql);
		q.setParameter(1,resourceContextUUID);
		q.setParameter(2,up.getUsername());

		@SuppressWarnings("unchecked")
		List<Object[]> r = (List<Object[]>)tom.executeNativeQuery(q);
		List<UserResourceModel> rs=new ArrayList<>();
		UserResourceModel currentUR = null;
		for(Object[] os:r){
			String uuid=(String)os[0];
			if(currentUR == null || !uuid.equals(currentUR.getResourceuuid())){
				currentUR = new UserResourceModel(os);
				rs.add(currentUR);
			} else {
				currentUR.addAction((String)os[1]);
			}
		}

		return rs;
	}

	public boolean canUserCreateContextClass(User user) {
		return racm.canUserDoRootAction(user.getUsername(), RACMNames.A_CREATE_CONTEXT_CLASS);
	}

	/**
	 * Only creator of a ContextClass can edit it.<br/>
	 */
	public boolean canUserEditContextClass(User user, Long ccid) {
		TransientObjectManager tom = user.getTom();
		Query q = tom.createQuery("select cc from ContextClass cc where cc.id=:ccid and cc.creator.id=:id").setParameter("ccid",ccid).setParameter("id", user.getId());
		ContextClass cc = tom.queryOne(q, ContextClass.class);
		return cc != null;
	}

	public boolean canUserEditResourceType(User user, Long rtid) {
		TransientObjectManager tom = user.getTom();
		Query q = tom.createQuery("select rt from ResourceType rt where rt.id=:rtid and rt.container.creator.id=:id").setParameter("rtid",rtid).setParameter("id", user.getId());
		ResourceType rt = tom.queryOne(q, ResourceType.class);
		return rt != null;
	}

	public boolean canUserCreateResource(User user, String rcUUID){
		// TODO find UUIS of __rootcontext__ on resourcecontext
		// TODO implement
		return true; //RACM.canUserDoActionOnResource(user.getUsername(), rcUUID, rcUUID)
	}

	public boolean canUserQueryJOQL(User user) {
		return racm.canUserDoRootAction(user.getUsername(), RACMNames.A_QUERY_JOQL);
	}
}
