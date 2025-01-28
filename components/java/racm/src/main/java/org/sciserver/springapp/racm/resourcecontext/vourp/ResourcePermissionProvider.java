package org.sciserver.springapp.racm.resourcecontext.vourp;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.sciserver.springapp.racm.resourcecontext.domain.Action;
import org.springframework.stereotype.Service;

@Service
public class ResourcePermissionProvider {
	@PersistenceContext
	private EntityManager em;

	public Map<String, Set<Action>> getActionsAllowedInResourceContext(String username,
			String resourceContextUUID) {
		Query query = em.createNativeQuery(
				"SELECT DISTINCT resourceUUID, action, actionCategory FROM racm.userActions(?) "
				+ "WHERE resourceContextUUID = ?")
				.setParameter(1, username)
				.setParameter(2, resourceContextUUID);
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		return results
				.stream()
				.collect(groupingBy(
						row -> row[0].toString(),
						mapping(row -> new Action(row[1].toString(), row[2].toString()), toSet())
						));
	}


	public Set<Action> getActionsForResource(String username, String resourceUUID) {
		Query query = em.createNativeQuery(
				"SELECT DISTINCT action, actionCategory FROM racm.userActions(?) "
				+ "WHERE resourceUUID = ?")
				.setParameter(1, username)
				.setParameter(2, resourceUUID);

		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();

		return results.stream()
				.map(row -> new Action(row[0].toString(), row[1].toString()))
				.collect(toSet());
	}
}
