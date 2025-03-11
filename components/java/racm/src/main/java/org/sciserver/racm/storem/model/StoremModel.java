package org.sciserver.racm.storem.model;

import java.util.List;
import java.util.Objects;

public final class StoremModel {
	private final List<String> fileservices;

	public StoremModel(List<String> fileservices) {
		super();
		this.fileservices = fileservices;
	}

	public List<String> getFileservices() {
		return fileservices;
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof StoremModel)) {
			return false;
		}
		StoremModel castOther = (StoremModel) other;
		return Objects.equals(fileservices, castOther.fileservices);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileservices);
	}

	@Override
	public String toString() {
		return "StoremModel [fileservices=" + fileservices + "]";
	}
}
