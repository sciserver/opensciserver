package org.sciserver.springapp.racm.resourcecontext.vourp;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.Query;

import org.ivoa.dm.model.TransientObjectManager;
import org.junit.Test;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;

import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.user.ServiceAccount;

public class ResourceContextAuthenticationTests {
	private static final String RESOURCE_CONTEXT_UUID = "context-a";
	private static final String RESOURCE_UUID = "resource-b";
	private static final String SERVICE_TOKEN = "context-a-token";

	@Test
	public void rejectsResourceOutsideContext() {
		VOURPContext vourpContext = mock(VOURPContext.class);
		TransientObjectManager tom = mock(TransientObjectManager.class);
		Query query = mock(Query.class);
		Query contextQuery = mock(Query.class);
		ResourceContext context = mock(ResourceContext.class);
		ServiceAccount account = mock(ServiceAccount.class);
		when(vourpContext.newTOM()).thenReturn(tom);
		when(tom.createQuery("SELECT rc FROM ResourceContext rc WHERE rc.uuid = :uuid"))
				.thenReturn(contextQuery);
		when(contextQuery.setParameter("uuid", RESOURCE_CONTEXT_UUID)).thenReturn(contextQuery);
		when(tom.queryOne(contextQuery, ResourceContext.class)).thenReturn(context);
		when(context.getAccount()).thenReturn(account);
		when(account.getServiceToken()).thenReturn(SERVICE_TOKEN);
		when(tom.createQuery("SELECT r FROM Resource r WHERE r.uuid = :resourceUUID "
				+ "AND r.container.uuid = :resourceContextUUID")).thenReturn(query);
		when(query.setParameter("resourceUUID", RESOURCE_UUID)).thenReturn(query);
		when(query.setParameter("resourceContextUUID", RESOURCE_CONTEXT_UUID)).thenReturn(query);
		when(tom.queryOne(query, Resource.class)).thenReturn(null);

		ResourceContextAuthentication authentication = new ResourceContextAuthentication(vourpContext);
		try {
			authentication.verifyCorrectTokenForResource(RESOURCE_CONTEXT_UUID, RESOURCE_UUID, SERVICE_TOKEN);
			fail("Expected a resource/context mismatch to be rejected");
		} catch (ResourceNotFoundException expected) {
			verify(query).setParameter("resourceUUID", RESOURCE_UUID);
			verify(query).setParameter("resourceContextUUID", RESOURCE_CONTEXT_UUID);
		}
	}
}
