package org.sciserver.springapp.racm.resourcecontext.controllers;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;

import org.ivoa.dm.model.InvalidTOMException;
import org.junit.Test;
import org.sciserver.springapp.racm.resourcecontext.applicaton.ResourceContextMapper;
import org.sciserver.springapp.racm.resourcecontext.vourp.ResourceContextAuthentication;
import org.sciserver.springapp.racm.resourcecontext.vourp.ResourceRepository;
import org.sciserver.springapp.racm.resources.application.ResourceManager;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;

public class ResourceContextMutationTests {
	private static final String RESOURCE_CONTEXT_UUID = "context-a";
	private static final String RESOURCE_UUID = "resource-b";
	private static final String SERVICE_TOKEN = "context-a-token";

	@Test
	public void contextMismatchStopsMetadataMutation() {
		ResourceContextAuthentication authentication = rejectingAuthentication();
		ResourceManager resourceManager = mock(ResourceManager.class);
		ResourceContextRESTController controller = new ResourceContextRESTController(
				mock(ResourceContextMapper.class), mock(ResourceRepository.class), authentication, resourceManager);

		try {
			controller.updateResourceMetadata(null, SERVICE_TOKEN, RESOURCE_CONTEXT_UUID,
					RESOURCE_UUID, "new name", "new description");
			fail("Expected a resource/context mismatch to be rejected");
		} catch (ResourceNotFoundException expected) {
			verifyNoInteractions(resourceManager);
		}
	}

	@Test
	public void contextMismatchStopsAssociatedResourceMutation() {
		ResourceContextAuthentication authentication = rejectingAuthentication();
		ResourceContextMapper mapper = mock(ResourceContextMapper.class);
		ResourceRepository repository = mock(ResourceRepository.class);
		ResourceContextRESTController controller = new ResourceContextRESTController(
				mapper, repository, authentication, mock(ResourceManager.class));

		try {
			controller.associateWithResource(SERVICE_TOKEN, RESOURCE_CONTEXT_UUID, RESOURCE_UUID, null);
			fail("Expected a resource/context mismatch to be rejected");
		} catch (ResourceNotFoundException expected) {
			verifyNoInteractions(mapper, repository);
		}
	}

	@Test
	public void contextMismatchStopsAssociatedEntityMutation() {
		ResourceContextAuthentication authentication = rejectingAuthentication();
		ResourceContextMapper mapper = mock(ResourceContextMapper.class);
		ResourceRepository repository = mock(ResourceRepository.class);
		ResourceContextRESTController controller = new ResourceContextRESTController(
				mapper, repository, authentication, mock(ResourceManager.class));

		try {
			controller.associateWithSciserverEntity(null, SERVICE_TOKEN,
					RESOURCE_CONTEXT_UUID, RESOURCE_UUID, null);
			fail("Expected a resource/context mismatch to be rejected");
		} catch (ResourceNotFoundException expected) {
			verifyNoInteractions(mapper, repository);
		}
	}

	@Test
	public void contextMismatchStopsResourceDeletion() {
		ResourceContextAuthentication authentication = rejectingAuthentication();
		ResourceRepository repository = mock(ResourceRepository.class);
		ResourceContextRESTController controller = new ResourceContextRESTController(
				mock(ResourceContextMapper.class), repository, authentication, mock(ResourceManager.class));

		try {
			controller.deleteResource(null, SERVICE_TOKEN, RESOURCE_CONTEXT_UUID, RESOURCE_UUID);
			fail("Expected a resource/context mismatch to be rejected");
		} catch (ResourceNotFoundException expected) {
			verifyNoInteractions(repository);
		}
	}

	@Test
	public void contextMismatchStopsPrivilegeMutation() throws InvalidTOMException {
		ResourceContextAuthentication authentication = rejectingAuthentication();
		VOURPContext vourpContext = mock(VOURPContext.class);
		RACMUtil racmUtil = mock(RACMUtil.class);
		AccessControlController controller = new AccessControlController(vourpContext, racmUtil, authentication);

		try {
			controller.addNewPrivilege(SERVICE_TOKEN, RESOURCE_CONTEXT_UUID,
					RESOURCE_UUID, Collections.emptyList());
			fail("Expected a resource/context mismatch to be rejected");
		} catch (ResourceNotFoundException expected) {
			verifyNoInteractions(vourpContext, racmUtil);
		}
	}

	private ResourceContextAuthentication rejectingAuthentication() {
		ResourceContextAuthentication authentication = mock(ResourceContextAuthentication.class);
		doThrow(new ResourceNotFoundException()).when(authentication)
				.verifyCorrectTokenForResource(RESOURCE_CONTEXT_UUID, RESOURCE_UUID, SERVICE_TOKEN);
		return authentication;
	}
}
