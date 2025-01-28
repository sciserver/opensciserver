package org.sciserver.springapp.racm.resourcecontext.domain;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.sciserver.springapp.racm.resourcecontext.domain.AssociatedSciserverEntity;
import org.sciserver.springapp.racm.resourcecontext.domain.Resource;

public class ResourceTests {
	private static final String SHARED_RESOURCE_CONTEXT_UUID = "xxxx-xxxx-xxxx-xxxx";
	private static final String SHARED_ASSOCIATED_USAGE = "a description";
	private static final String SHARED_RESOURCE_TYPE_NAME = "MyResourceTypeName";

	@Test
	public void addAssociatedGroup() {
		Resource resource = Resource.createNew(SHARED_RESOURCE_CONTEXT_UUID,
				"", "", "", SHARED_RESOURCE_TYPE_NAME);
		resource.addAssociationWithSciserverEntity(
				new AssociatedSciserverEntity(2, "GROUP", SHARED_ASSOCIATED_USAGE, true));
		resource.addAssociationWithSciserverEntity(
				new AssociatedSciserverEntity(4, "USER", SHARED_ASSOCIATED_USAGE, false));

		assertThat(resource.associatedSciserverEntities(),
				containsInAnyOrder(
						new AssociatedSciserverEntity(2, "GROUP", SHARED_ASSOCIATED_USAGE, true),
						new AssociatedSciserverEntity(4, "USER", SHARED_ASSOCIATED_USAGE, false)));
	}
}
