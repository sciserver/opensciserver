package org.sciserver.springapp.racm.storem.application;

import java.util.Objects;

import edu.jhu.file.DataVolume;
import edu.jhu.file.UserVolume;
import edu.jhu.rac.Resource;
import edu.jhu.user.SciserverEntity;

public interface VOURPEntityWithResource {
	Resource getResource();
	boolean showPrivilegesForUser(SciserverEntity user);

	static VOURPEntityWithResource wrap(DataVolume dataVolume) {
		return new WrappedDataVolume(dataVolume);
	}

	static VOURPEntityWithResource wrap(UserVolume userVolume) {
		return new WrappedUserVolume(userVolume);
	}

	class WrappedDataVolume implements VOURPEntityWithResource {
		private final DataVolume dataVolume;

		private WrappedDataVolume(DataVolume dataVolume) {
			this.dataVolume = dataVolume;
		}

		@Override
		public Resource getResource() {
			return dataVolume.getResource();
		}

		@Override
		public boolean showPrivilegesForUser(SciserverEntity user) {
			return true;
		}

		@Override
		public int hashCode() {
			return Objects.hash(dataVolume);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WrappedDataVolume other = (WrappedDataVolume) obj;
			return Objects.equals(dataVolume, other.dataVolume);
		}
	}

	class WrappedUserVolume implements VOURPEntityWithResource {
		private UserVolume userVolume;

		private WrappedUserVolume(UserVolume userVolume) {
			this.userVolume = userVolume;
		}

		@Override
		public Resource getResource() {
			return userVolume.getResource();
		}

		@Override
		public boolean showPrivilegesForUser(SciserverEntity user) {
			return !userVolume.getOwner().equals(user);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userVolume);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WrappedUserVolume other = (WrappedUserVolume) obj;
			return Objects.equals(userVolume, other.userVolume);
		}
	}
}
