package org.sciserver.springapp.racm.ugm.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import edu.jhu.user.GroupRole;
import edu.jhu.user.Member;
import edu.jhu.user.MemberStatus;
import edu.jhu.user.User;
import edu.jhu.user.UserGroup;

public class UserProfile implements UserDetails {
	private static final long serialVersionUID = 1L;
	private User user;
	private String token;
	private String userid;
	private String trustId;
	/**
	 * indicates that the user is a member of the 'admin' group.
	 */
	private Boolean isAdmin = null;
	public String getTrustId() {
		return trustId;
	}
	private Long id;

	public TransientObjectManager getTom(){
		return user != null? user.getTom():null;
	}
	public UserProfile(User user){
		if(user == null || user.getTom() == null){
			throw new IllegalArgumentException("UserProfile MUST be instantiated with a not-null User with a not-null TOM");
		}
		this.user = user;
		this.id = user.getId();
		this.userid=user.getUserId();
		this.trustId=user.getTrustId();
	}

	@Override
	public String getUsername(){
		return this.user.getUsername();
	}
	public String getEmail(){
		return this.user.getContactEmail();
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	public Long getId() {
		return id;
	}
	/*
	 * The User object is a JPA entity, but it is usually retrieved
	 * in its own transaction as part of authentication. We try to return
	 * an attached entity when possible since the rest of racm doesn't track
	 * what detached/attached entities are used.
	 *
	 * For testing, we skip this step if the TransientObjectManager appears mocked.
	 */
	public User getUser(){
		if (user.getTom().getEntityManager() == null) return user;

		EntityManager em = user.getTom().getEntityManager();
		if (!em.contains(user))
			return em.merge(user);
		else
			return user;
	}

	public String getUserid() {
		return userid;
	}
	public boolean isAdmin() {
		if(isAdmin == null){
			try{
  			isAdmin = isUserAdmin();
			}catch(Exception e){
				isAdmin = false;
			}
		}
		return isAdmin;
	}


	/**
	 * Return true if the specified user is the owner or an ADIN of the groupe, false otherwise.<br/>
	 * @param ug
	 * @param up
	 * @return
	 */
	public  boolean isGroupEditor(UserGroup ug){
		if(ug.getOwner().equals(user))
			return true;
		for(Member m: ug.getMember())
			if(m.getScisEntity().equals(user) && m.getMemberRole() == GroupRole.ADMIN && m.getStatus() == MemberStatus.ACCEPTED)
				return true;
		return false;
	}

	/**
	 * TODO SQL Server specific formatting: [user]
	 * @param userId
	 * @param tom
	 * @return
	 * @throws VOURPException
	 */
	private boolean isUserAdmin() {
			String sql = "select m.id as memberId from usergroup ug inner join member m on ug.id=m.containerId inner join [user] u on u.id=m.scisentityid and u.userid=? where ug.name=?";
			Query q = getTom().createNativeQuery(sql);
			q.setParameter(1, userid);
			q.setParameter(2, RACMNames.USERGROUP_ADMIN);
			List<?> rows = getTom().executeNativeQuery(q);
			return rows != null && !rows.isEmpty();
		}

	/* The next few getters are so that UserProfile implements
	 * Spring Security's UserDetails.
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

		if (isAdmin())
			authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

		return authorities;
	}

	@Override
	public String getPassword() {
		return null;
	}
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	@Override
	public boolean isEnabled() {
		return true;
	}

}
