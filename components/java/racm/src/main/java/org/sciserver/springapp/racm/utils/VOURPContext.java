package org.sciserver.springapp.racm.utils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.ivoa.dm.model.TransientObjectManager;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class VOURPContext {
	@PersistenceContext
	private EntityManager em;

	private TransientObjectManager currentTom;

	public TransientObjectManager newTOM(){
		if (currentTom == null)
			currentTom = new TransientObjectManager(em);
		return currentTom;
	}
}
