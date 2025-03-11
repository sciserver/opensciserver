package org.sciserver.racm.collaboration.model;

import java.util.Objects;

import edu.jhu.user.GroupRole;
import edu.jhu.user.MemberStatus;

public class CollaborationMember {
	private final long id;
	private final GroupRole role;
	private final MemberStatus status;
	public CollaborationMember(long id, GroupRole role, MemberStatus status) {
		super();
		this.id = id;
		this.role = role;
		this.status = status;
	}
	public long getId() {
		return id;
	}
	public GroupRole getRole() {
		return role;
	}
	public MemberStatus getStatus() {
		return status;
	}
	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof CollaborationMember)) {
			return false;
		}
		CollaborationMember castOther = (CollaborationMember) other;
		return Objects.equals(id, castOther.id) && Objects.equals(role, castOther.role)
				&& Objects.equals(status, castOther.status);
	}
	@Override
	public int hashCode() {
		return Objects.hash(id, role, status);
	}
}
