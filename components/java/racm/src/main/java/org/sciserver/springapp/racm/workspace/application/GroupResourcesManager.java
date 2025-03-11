package org.sciserver.springapp.racm.workspace.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.workspace.model.WorkspaceGroupModel;
import org.sciserver.racm.workspace.model.WorkspaceGroupsModel;
import org.sciserver.racm.workspace.model.WorkspaceResourceModel;
import org.sciserver.racm.workspace.model.WorkspaceUserModel;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class GroupResourcesManager {

	/**
	 * Return informaiton about the gorups the specified user is a direct member of.<br/>
	 * Includes all other members and resources the groups has been given access to.
	 * @param up
	 * @return
	 */
	public WorkspaceGroupsModel getWorkspaceGroups(UserProfile up){
		TransientObjectManager tom = up.getTom();
		WorkspaceGroupsModel wgsm = new WorkspaceGroupsModel();
		wgsm.setUsername(up.getUsername());
		wgsm.setUserid(up.getUserid());
		String columns="id,name,description";
		String sql= String.format("select %s from racm.userGroups(?) order by name",columns);
		Query q = tom.createNativeQuery(sql).setParameter(1, up.getUsername());
		List<?> r = tom.executeNativeQuery(q);
		Map<Long,WorkspaceGroupModel> dict = new HashMap<>();
		for(Object o:r){
			Object[] row = (Object[])o;
			int i = 0;
			Long id = (Long)row[i++];
			WorkspaceGroupModel	wgm = new WorkspaceGroupModel(id);
			dict.put(id, wgm); // for later use with members
			wgm.setGroupName((String)row[i++]);
			wgm.setDescription((String)row[i++]);
			wgsm.addGroup(wgm);
		}
		addWorkspaceGroupResources(up,dict);
		addWorkspaceGroupUsers(up, dict);
		return wgsm;
	}
	private static Map<Long,WorkspaceGroupModel> addWorkspaceGroupResources(UserProfile up, Map<Long,WorkspaceGroupModel> dict){
		TransientObjectManager tom = up.getTom();
		//
		String columns="groupid,resourceId,action,contextClass,resourceContextAPIEndpoint,resourceType,resourceName,resourcePubDID";
		String sql= String.format("select %s from racm.groupResourcesForUser(?) order by groupid, resourceid, contextClass, resourceType,action",columns);
		Query q = tom.createNativeQuery(sql).setParameter(1, up.getUsername());
		List<?> r = tom.executeNativeQuery(q);
		Long resourceId = null;
		WorkspaceResourceModel rm = null;
		for(Object o:r){
			Object[] row = (Object[])o;
			int i=0;
			Long groupId = (Long)row[i++];
			WorkspaceGroupModel wgm = dict.get(groupId);
			if(wgm == null)
				continue; // TODO log error and throw exception,
			Long rid=(Long)row[i++];
			String action = (String)row[i++];
			if(resourceId == null || !resourceId.equals(rid)){
				resourceId = rid;
				rm = new WorkspaceResourceModel(rid);
				rm.setContextClass((String)row[i++]);
				rm.setResourceContextAPIEndpoint((String)row[i++]);
				rm.setResourceType((String)row[i++]);
				rm.setName((String)row[i++]);
				rm.setPubDID((String)row[i++]);
				wgm.addResource(rm);
			}
			rm.addAction(action);
		}
		return dict;
	}
	private static void addWorkspaceGroupUsers(UserProfile up, Map<Long,WorkspaceGroupModel> wgsm){
		TransientObjectManager tom = up.getTom();
		//
		String columns="groupId,memberuserid,memberRole, memberName, memberEmail,fullName,affiliation";
		String sql= String.format("select %s from racm.groupFriends(?) order by groupid,memberName",columns);
		Query q = tom.createNativeQuery(sql).setParameter(1, up.getUsername());
		List<?> r = tom.executeNativeQuery(q);
		for(Object o:r){
			Object[] row = (Object[])o;
			int i = 0;
			Long gid=(Long)row[i++];
			WorkspaceGroupModel wgm = wgsm.get(gid);
			if(wgm == null)
				continue; // TODO LOG and throw error
			WorkspaceUserModel m = new WorkspaceUserModel();
			wgm.addMember(m);
			m.setUserid((Long)row[i++]);
			m.setMemberrole((String)row[i++]);
			m.setUsername((String)row[i++]);
			m.setEmail((String)row[i++]);
			m.setFullName((String)row[i++]);
			m.setAffiliation((String)row[i++]);
		}
	}
}
