package org.sciserver.springapp.racm.utils;

import java.util.List;

import javax.persistence.Query;

import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RACM {
	private final VOURPContext vourpContext;
	@Autowired
	public RACM(VOURPContext vourpContext) {
		this.vourpContext = vourpContext;
	}

	public TransientObjectManager newTom() {
		return vourpContext.newTOM();
	}
	
	/**
	 * Fundamental method that requests whether a certain user can perform the specified action on the specified resource identified by its resourceUUID.<br/>
	 * @param user  name of User
	 * @param contextUUID
	 * @param resourceContextId
	 * @param action name of Action
	 * @return
	 */
	public boolean canUserDoActionOnResource(String username, String resourceUUID, String action){
		TransientObjectManager tom = vourpContext.newTOM();
		String sql="select * from racm.canUserDoAction(?,?,?)";
		Query q = tom.createNativeQuery(sql);
		q.setParameter(1,resourceUUID);
		q.setParameter(2,action);
		q.setParameter(3,username);
		List<?> rows = tom.executeNativeQuery(q);
		return rows != null && !rows.isEmpty();
	}
	
    /**
     * Fundamental method that requests whether a certain service can perform the specified action on the specified resource identified by its resourceUUID.<br/>
     * @param user  name of User
     * @param contextUUID
     * @param resourceContextId
     * @param action name of Action
     * @return
     */
    public boolean canServiceDoActionOnResource(String resourceUUID, String action, String serviceToken){
        TransientObjectManager tom = vourpContext.newTOM();
        String sql="select * from racm.canServiceDoAction(?,?,?)";
        Query q = tom.createNativeQuery(sql);
        q.setParameter(1,resourceUUID);
        q.setParameter(2,action);
        q.setParameter(3,serviceToken);
        List<?> rows = tom.executeNativeQuery(q);
        return rows != null && !rows.isEmpty();
    }
	
	public boolean canUserGrantOnResource(String username, String resourceUUID) {
		TransientObjectManager tom = vourpContext.newTOM();
		Query q = tom
				.createNativeQuery("SELECT * FROM racm.userActions(?) WHERE actionCategory = 'G' AND resourceUUID = ?")
				.setParameter(1, username)
				.setParameter(2, resourceUUID);
		return !tom.executeNativeQuery(q).isEmpty();
	}
	public boolean canUserDoRootAction(String username, String action){
		TransientObjectManager tom = vourpContext.newTOM();
		String sql="select * from racm.canUserDoRootAction(?,?)";
		Query q = tom.createNativeQuery(sql);
		q.setParameter(1,username);
		q.setParameter(2,action);
		List<?> rows = tom.executeNativeQuery(q);
		return rows != null && !rows.isEmpty();

	}
	public boolean doesUserHaveRoleOnResource(String username, String contextUUID, String resourceContextId, String role){
		TransientObjectManager tom = vourpContext.newTOM();
		String sql="select * from racm.doesUserHaveRole(?,?,?,?)";
		Query q = tom.createNativeQuery(sql);
		q.setParameter(1,contextUUID);
		q.setParameter(2,resourceContextId);
		q.setParameter(3,role);
		q.setParameter(4,username);
		List<?> rows = tom.executeNativeQuery(q);
		return rows != null && !rows.isEmpty();
	}

	public boolean canUserDoActionOnRootContext(String username, String resourceContextUUID, String action) {
		return canUserDoActionOnResource(username,
				RACMUtil.getRootResource(vourpContext.newTOM(), resourceContextUUID).getUuid(),
				action);
	}

	public void tryDoActionOnResource(UserProfile user, String resourceUUID, String action) throws RACMException{
		if(!canUserDoActionOnResource(user.getUsername(), resourceUUID, action))
			 throw new RACMException(user.getUsername(), resourceUUID, action);
	}
	
	/**
	 * Return true if the resourcecontext identified by the servicetoken has a resource that owns (through associatedresource relation) the specified resource.<br/>
	 * Return false otherwise.
	 * @param serviceToken
	 * @param resourceUUID
	 * @return
	 */
	public boolean isResourceOwnedByThisService(String serviceToken, String resourceUUID) {
		TransientObjectManager tom = vourpContext.newTOM();
		String sql="select * from racm.isResourceOwnedByThisService(?,?)";
		Query q = tom.createNativeQuery(sql);
		q.setParameter(1,serviceToken);
		q.setParameter(2,resourceUUID);
		List<?> rows = tom.executeNativeQuery(q);
		return rows != null && !rows.isEmpty();
	}
	/**
	 * Return true if the specified resource is 'owned' by another resource using an associatedresource relation.<br/>
	 * @param resourceUUID
	 * @return
	 */
    public boolean isResourceOwnedByAnotherResource(String resourceUUID) {
        TransientObjectManager tom = vourpContext.newTOM();
        String sql="select * from racm.isResourceOwnedByAnotherResource(?)";
        Query q = tom.createNativeQuery(sql);
        q.setParameter(1,resourceUUID);
        List<?> rows = tom.executeNativeQuery(q);
        return rows != null && !rows.isEmpty();
    }
}
