package org.sciserver.springapp.racm.resourcecontext.vourp;

import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.springapp.racm.resourcecontext.controllers.InvalidServiceToken;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.stereotype.Service;

import edu.jhu.rac.ResourceContext;

@Service
public class ResourceContextAuthentication {
	private final VOURPContext vourpContext;

	ResourceContextAuthentication(VOURPContext vourpContext) {
		this.vourpContext = vourpContext;
	}

	public void verifyCorrectToken(String resourceContextUUID, String token) {
		TransientObjectManager tom = vourpContext.newTOM();
		ResourceContext rc = tom.queryOne(
				tom.createQuery("SELECT rc FROM ResourceContext rc WHERE rc.uuid = :uuid")
				.setParameter("uuid", resourceContextUUID)
				, ResourceContext.class);
		if (rc.getAccount() == null || !rc.getAccount().getServiceToken().equals(token)) {
			throw new InvalidServiceToken();
		}
	}
}
