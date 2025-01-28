package org.sciserver.springapp.racm.collaboration.application;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.collaboration.model.Collaboration;
import org.sciserver.racm.collaboration.model.CollaborationMember;
import org.sciserver.racm.collaboration.model.ResourceFacade;
import org.sciserver.racm.collaboration.model.ResourceOwnedGroup;
import org.sciserver.racm.collaboration.model.ResourceWithAnyType;
import org.sciserver.racm.collaboration.model.User;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.ugm.controller.UserManagementRESTController;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.ModelAttribute;

import edu.jhu.file.DataVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.job.DatabaseContext;
import edu.jhu.job.DockerImage;
import edu.jhu.job.VolumeContainer;
import edu.jhu.rac.AccessControl;
import edu.jhu.rac.OwnershipCategory;
import edu.jhu.rac.Privilege;
import edu.jhu.rac.RoleAssignment;
import edu.jhu.user.GroupAccessibility;
import edu.jhu.user.GroupRole;
import edu.jhu.user.Member;
import edu.jhu.user.MemberStatus;
import edu.jhu.user.SciserverEntity;
import edu.jhu.user.UserGroup;

@Repository
public class CollaborationManager {

	public RepresentationModel<? extends Object> getCollaborations(@ModelAttribute UserProfile up) {
		Collection<UserGroup> groups = getGroups(up);
		
		Collection<SciserverEntity> entities = loadGroupUsers(up);
  	groups.forEach(ug -> ug.getMember().forEach(Member::getScisEntity));

		Map<Long, User> users = new HashMap<>();

		Map<UserGroup, List<ResourceFacade>> allResources = getAllResources(up, groups);

		Collection<Collaboration> collaborationResourceList =
				groups
				.stream()
				.map(ug -> {
					Collaboration output;
					List<ResourceFacade> resources =
							allResources.getOrDefault(ug, Collections.emptyList());
					boolean isGroupAdmin = isAdmin(up.getId(), ug);
					boolean isOwner = isOwner(up.getId(), ug);
					boolean isInvited = isInvited(up.getId(), ug);
					boolean canShareResources;

					List<CollaborationMember> members =
							new ArrayList<>(ug.getMember())
							.stream()
							// Always show self and owner.
							// Show everyone else if an actual member
							.filter(m ->
								m.getScisEntity().getId().equals(up.getId()) ||
								m.getStatus().equals(MemberStatus.OWNER) ||
								!isInvited
							)
							.map(m -> {
								users.computeIfAbsent(m.getScisEntity().getId(),
										id -> new User(m.getScisEntity()));
								return new CollaborationMember(
										m.getScisEntity().getId(),
										m.getMemberRole(),
										m.getStatus());
							})
							.collect(Collectors.toList());

					output = new Collaboration(
							ug.getName(),
							ug.getDescription(),
							"GROUP",
							resources,
							members,
							ug.getId());
					canShareResources = true;
					output.add(linkTo(
							methodOn(UserManagementRESTController.class)
							.queryMyGroup(ug.getId(), null,null))
							.withSelfRel());
					if (isGroupAdmin) {
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.updateGroup(null, ug.getId(), null, null))
								.withRel("editName"));
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.updateGroup(null, ug.getId(), null, null))
								.withRel("editDescription"));
					}
					if (isOwner) {
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.deleteMyGroup(ug.getId(), null, null))
								.withRel("delete"));
					}

					if (isGroupAdmin) {
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.queryMyGroup(ug.getId(), null,null))
								.withRel("editMemberList"));
					}
					if (isGroupAdmin && up.isAdmin()) {
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.queryMyGroup(ug.getId(), null, null))
								.withRel("forceAddMember"));
					}
					if (!isInvited && canShareResources) {
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.shareResource(null, ug.getId(),
										null, null, null, null))
								.withRel("shareResource"));
					}

					if (isInvited) {
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.acceptInvitation(ug.getId(), null, null))
								.withRel("acceptInvitation"));
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.declineInvitation(ug.getId(), null, null))
								.withRel("declineInvitation"));
					}

					if (!isInvited && !isOwner) {
						output.add(linkTo(
								methodOn(UserManagementRESTController.class)
								.leaveGroup(ug.getId(), null, null))
								.withRel("leave"));
					}

					return output;
				})
				.collect(Collectors.toList());
		
		Collection<? extends Object> allObjects =
				CollectionUtils.union(users.values(), collaborationResourceList);

// next is old code, no longer legal
//		CollectionModel<? extends Object> collaborations = 
//			CollectionModel<>(allObjects); 
// TODO test next replacement
		CollectionModel<? extends Object> collaborations = CollectionModel.of(allObjects);

		collaborations.add(linkTo(
				methodOn(UserManagementRESTController.class)
				.manageGroup(null, null, null))
				.withRel("createGroup"));

		return collaborations;
	}

	private boolean isInvited(long userId, UserGroup ug) {
		return new ArrayList<>(ug.getMember())
				.stream()
				.anyMatch(m ->
				m.getScisEntity().getId().equals(userId) &&
				m.getStatus().equals(MemberStatus.INVITED));
	}

	private boolean isAdmin(long userId, UserGroup ug) {
		return new ArrayList<>(ug.getMember())
				.stream()
				.anyMatch(m ->
				m.getScisEntity().getId().equals(userId) &&
				!m.getStatus().equals(MemberStatus.INVITED) &&
				EnumSet.of(GroupRole.ADMIN, GroupRole.OWNER).contains(m.getMemberRole()));
	}

	private boolean isOwner(long userId, UserGroup ug) {
		return new ArrayList<>(ug.getMember())
				.stream()
				.anyMatch(m ->
				m.getScisEntity().getId().equals(userId) &&
				!m.getStatus().equals(MemberStatus.INVITED) &&
				m.getMemberRole().equals(GroupRole.OWNER));
	}

	private Collection<UserGroup> getGroups(UserProfile up) {
		Query q = up.getTom().createQuery("SELECT distinct ug FROM UserGroup ug "
				+ "JOIN ug.member m  JOIN FETCH ug.member m2 "
				+ "WHERE ug.accessibility != :public and m.scisEntity = :user AND "
				+ "m.status not in :excludedStatuses AND "
				+ "ug.name <> :publicGroup AND "
				+ "not exists (SELECT r FROM Resource r join r.associatedGroup ae where ae.sciEntity = ug and ae.ownership=:owned)") ;
		q.setParameter("user", up.getUser());
		q.setParameter("public", GroupAccessibility.PUBLIC);
		q.setParameter("excludedStatuses",
				Arrays.asList(MemberStatus.DECLINED, MemberStatus.WITHDRAWN));
		q.setParameter("publicGroup", RACMNames.USERGROUP_PUBLIC);
		q.setParameter("owned", OwnershipCategory.OWNED);
		try {
			TransientObjectManager tom = up.getTom();
			List<UserGroup> ugs = tom.queryJPA(q, UserGroup.class);
			return ugs;
		} catch (VOURPException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return informaiton about those groups that the specified User is a member of, that are owned by a Resource.<br/>
	 * @param up
	 * @return
	 */
	private Collection<ResourceOwnedGroup> getGroupsWithOwningResource(UserProfile up) {
		String sql="SELECT distinct ug.id as groupId,ug.name as groupName, r.uuid as resourceUUID, rt.name as resourceType\r\n" + 
				"  FROM UserGroup ug " + 
				"	inner JOIN Member m on m.containerId=ug.id" + 
				"	  and m.status not in ('DECLINED','WITHDRAWN') " + 
				"	  and m.scisEntityId=? " + 
				"	inner join AssociatedSciEntity rg on rg.sciEntityId=ug.id " + 
				"	inner join Resource r on r.id=rg.containerId " + 
				"	inner join ResourceType rt on rt.ID=r.resourceTypeId " + 
				"  WHERE  ug.accessibility != ?" + 
				"	and ug.name != ?";
		Query q = up.getTom().createNativeQuery(sql);
		q.setParameter(1, up.getId());
		q.setParameter(2, GroupAccessibility.PUBLIC.name());
		q.setParameter(3,  RACMNames.USERGROUP_PUBLIC);
		
		List<?> qr=up.getTom().executeNativeQuery(q);
		ArrayList<ResourceOwnedGroup> r = new ArrayList<ResourceOwnedGroup>();
		for(Object o: qr) {
			Object[] row = (Object[])o;
			ResourceOwnedGroup rog = new ResourceOwnedGroup();
			int i = 0;
			rog.setGroupId((Long)row[i++]);
			rog.setGroupName((String)row[i++]);
			rog.setResourceUUID((String)row[i++]);
			rog.setResourceType((String)row[i++]);
			r.add(rog);
		}
		return r;
	}
	
	
	private Collection<SciserverEntity> loadGroupUsers(UserProfile up) {
		// NB: write next query like this to avoid returning usergroups too often.
		String sq = "SELECT distinct u " + 
				" FROM User u  " + 
				" WHERE exists (select m2 from UserGroup ug join ug.member m1 join ug.member m2 " +
				" where m1.scisEntity=:user and m2.scisEntity.id = u.id" +
				"   and ug.name <> :publicGroup and ug.accessibility != :public AND m1.status not in :excludedStatuses)";
		
		
		Query q = up.getTom().createQuery(sq);
		q.setParameter("user", up.getUser());
		q.setParameter("public", GroupAccessibility.PUBLIC);
		q.setParameter("excludedStatuses",
				Arrays.asList(MemberStatus.DECLINED, MemberStatus.WITHDRAWN));
		q.setParameter("publicGroup", RACMNames.USERGROUP_PUBLIC);
		try {
			return up.getTom().queryJPA(q, SciserverEntity.class);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private Map<UserGroup, List<ResourceFacade>> getAllResources(
			UserProfile up, Collection<UserGroup> userGroups) {
		if (userGroups.isEmpty())
			return new HashMap<>();

		return Stream.of(
				getAnyResourcesSharedWith(up, userGroups).entrySet().stream(),
				getUserVolumesSharedWith(up, userGroups).entrySet().stream(),
				getVolumeContainersSharedWith(up, userGroups).entrySet().stream(),
				getDataVolumesSharedWith(up, userGroups).entrySet().stream(),
				getDatabaseContextsSharedWith(up, userGroups).entrySet().stream(),
				getDockerImagesSharedWith(up, userGroups).entrySet().stream()
				)
				.reduce(Stream::concat)
				.orElseGet(Stream::empty)
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						ListUtils::union));
	}

	private Map<UserGroup, List<ResourceFacade>> getUserVolumesSharedWith(
			UserProfile up, Collection<UserGroup> userGroups) {
		Query q = up.getTom().createQuery("SELECT uv, ac FROM UserVolume uv "
				+ "JOIN uv.resource.accesControl ac "
				+ "WHERE ac.scisEntity in :groupList");
		q.setParameter("groupList", userGroups);
		return this.<UserVolume>getResources(
				q,
				e -> ResourceFacade.createResourceFacadeFromEntity(e.getKey().getName(), e.getKey().getDescription(), ResourceFacade.TYPE.USERVOLUME, e.getKey().getResource().getId(), e.getKey().getId(),
						e.getValue()));
	}

	private Map<UserGroup, List<ResourceFacade>> getAnyResourcesSharedWith(
			UserProfile up, Collection<UserGroup> userGroups) {
		Query q = up.getTom().createQuery("SELECT resource, ac FROM Resource resource "
				+ "JOIN resource.accesControl ac "
				+ "WHERE ac.scisEntity in :groupList");
		q.setParameter("groupList", userGroups);
		return this.<edu.jhu.rac.Resource>getResources(
				q,
				e -> ResourceFacade.createResourceFacadeFromResource(
						e.getKey().getName(),
						e.getKey().getDescription(),
						e.getValue(),
						e.getKey().getResourceType().getName(),
						e.getKey().getId(),
						new ResourceWithAnyType.ResourceContextInfo(
								e.getKey().getContainer().getLabel(),
								e.getKey().getContainer().getDescription(),
								e.getKey().getContainer().getContextClass().getName())));
	}

	private Map<UserGroup, List<ResourceFacade>> getVolumeContainersSharedWith(
			UserProfile up, Collection<UserGroup> userGroups) {
		Query q = up.getTom().createQuery("SELECT vc, ac FROM VolumeContainer vc "
				+ "JOIN vc.resource.accesControl ac "
				+ "WHERE ac.scisEntity in :groupList");
		q.setParameter("groupList", userGroups);
		return this.<VolumeContainer>getResources(
				q,
				e -> ResourceFacade.createResourceFacadeFromEntity(e.getKey().getName(), e.getKey().getDescription(), ResourceFacade.TYPE.VOLUMECONTAINER, e.getKey().getResource().getId(), e.getKey().getId(),
						e.getValue()));
	}

	private Map<UserGroup, List<ResourceFacade>> getDataVolumesSharedWith(
			UserProfile up, Collection<UserGroup> userGroups) {
		Query q = up.getTom().createQuery("SELECT dv, ac FROM DataVolume dv "
				+ "JOIN dv.resource.accesControl ac "
				+ "WHERE ac.scisEntity in :groupList");
		q.setParameter("groupList", userGroups);
		return this.<DataVolume>getResources(
				q,
				e -> ResourceFacade.createResourceFacadeFromEntity(e.getKey().getName(), e.getKey().getDescription(), ResourceFacade.TYPE.DATAVOLUME, e.getKey().getResource().getId(), e.getKey().getId(),
						e.getValue()));
	}

	private Map<UserGroup, List<ResourceFacade>> getDatabaseContextsSharedWith(
			UserProfile up, Collection<UserGroup> userGroups) {
		Query q = up.getTom().createQuery("SELECT db, ac FROM DatabaseContext db "
				+ "JOIN db.resource.accesControl ac "
				+ "WHERE ac.scisEntity in :groupList");
		q.setParameter("groupList", userGroups);
		return this.<DatabaseContext>getResources(
				q,
				e -> ResourceFacade.createResourceFacadeFromEntity(e.getKey().getName(), e.getKey().getDescription(), ResourceFacade.TYPE.DATABASE, e.getKey().getResource().getId(), e.getKey().getId(),
						e.getValue()));
	}

	private Map<UserGroup, List<ResourceFacade>> getDockerImagesSharedWith(
			UserProfile up, Collection<UserGroup> userGroups) {
		Query q = up.getTom().createQuery("SELECT im, ac FROM DockerImage im "
				+ "JOIN im.resource.accesControl ac "
				+ "WHERE ac.scisEntity in :groupList");
		q.setParameter("groupList", userGroups);
		return this.<DockerImage>getResources(
				q,
				e -> ResourceFacade.createResourceFacadeFromEntity(e.getKey().getName(), e.getKey().getDescription(), ResourceFacade.TYPE.DOCKERIMAGE, e.getKey().getResource().getId(), e.getKey().getId(),
						e.getValue()));
	}

	@SuppressWarnings("unchecked")
	private <M extends MetadataObject> Map<UserGroup, List<ResourceFacade>> getResources(
			Query q, Function<Map.Entry<M, List<String>>, ResourceFacade> converter) {
		Map<UserGroup, Map<M, List<String>>> output = new HashMap<>();
		new ArrayList<Object[]>(q.getResultList())
			.stream()
			.forEach(row -> {
				M vourpEntity = (M) row[0];
				AccessControl ac = (AccessControl) row[1];
				UserGroup ug = (UserGroup) ac.getScisEntity();
				output.putIfAbsent(ug, new HashMap<>());
				output.get(ug).putIfAbsent(vourpEntity, new ArrayList<>());
				if (ac instanceof Privilege) {
					output.get(ug).get(vourpEntity).add(((Privilege) ac).getAction().getName());
				} else if (ac instanceof RoleAssignment) {
					((RoleAssignment)ac).getRole().getAction().forEach(ra ->
						output.get(ug).get(vourpEntity).add(ra.getAction().getName())
					);
				} else {
					throw new IllegalStateException(
							"Found AccessControl that wasn't Privilege or RoleAssignment");
				}
			});
		return output.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e ->
				e.getValue().entrySet()
					.stream()
					.map(converter)
					.collect(Collectors.toList())
			));
	}
}
