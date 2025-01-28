package org.sciserver.springapp.racm.resources.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.cctree.model.ContextClassModel;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.jhu.rac.Action;
import edu.jhu.rac.ActionCategory;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.ResourceType;
import edu.jhu.rac.Role;

@Service
public class ContextClassManager {
	private final VOURPContext vourpContext;

	@Autowired
	public ContextClassManager(VOURPContext vourpContext) {
		this.vourpContext = vourpContext;
	}
	/**
	 * If contextClassModel passed by the input parameter does not have its ID,
	 * this model instance is for creating new context class. otherwise the model instance is for
	 * updating existing context class.
	 * @param contextClassModel
	 * @return
	 * @throws Exception
	 */
	public ContextClassModel editContextClass(ContextClassModel contextClassModel, UserProfile user) {
		if (contextClassModel.getId() == null)
			return createContextClass(contextClassModel, user);
		else
			return updateContextClass(contextClassModel, user);
	}

	public static ResourceType getResourceType(String name, ContextClass cc) {
		if(name == null)
			return null;
    for(ResourceType rt: cc.getResourceType())
    	if(name.equals(rt.getName()))
    		return rt;
    return null;
	}
	public static Role getRole(String name, ResourceType rt) {
		if(name == null)
			return null;
    for(Role r: rt.getRole())
    	if(name.equals(r.getName()))
    		return r;
    return null;
	}

	private ContextClassModel createContextClass(ContextClassModel contextClassModel, UserProfile user) {
		TransientObjectManager tom = user.getTom();
		try {
			validate(contextClassModel, null, tom);
			ContextClass cc = new ContextClass(tom);
			cc.setCreator(user.getUser());
			fill(cc, contextClassModel);

			ResourceType root = RACMUtil.addRootContextRT(cc);
			Action a = new Action(root);
			a.setName(RACMNames.A_GRANT);
			a.setCategory(ActionCategory.G);
			 a = new Action(root);
			a.setName(RACMNames.A_CREATE_ANY_RESOURCE);
			a.setCategory(ActionCategory.C);

			tom.persist();

			//Once new ContextClass cc is persisted in the database,  the Id of cc is set.
			//replace ContextClassModel instance with the persisted the context class instance
			//in newContextClassModel factory method.
			contextClassModel = RACMModelFactory.newContextClassModel(cc);
			contextClassModel.setValid(true);
		} catch (Exception e) {
			//If fails set Exception to the ContextClassModel object passed by input argument
			contextClassModel.setException(e);
			contextClassModel.setValid(true);
		}
		return contextClassModel;
	}

	private ContextClassModel updateContextClass(ContextClassModel ccm, UserProfile user) {
		return updateContextClass(ccm, null, user);
	}

	/*
	 * This method is called directly from 'postContextClass' method in ResourceManagementController
	 * if there is a ResourceType is marked for deletion(i.e., nextaction=deleteresourcetype)
	 */
	public ContextClassModel updateContextClass(ContextClassModel ccm, Long resourceTypeToBeDeleted, UserProfile user) {
		TransientObjectManager tom = user.getTom();
		ContextClass cc = queryContextClass(ccm.getId(), tom);
		if (cc == null)
			throw new IllegalArgumentException(String.format("Cannot update non-existent ContextClass with ID = '%s'", ccm.getId()));
		if(cc.getCreator() != user.getUser())
			throw new InsufficientPermissionsException(String.format("update ContextClass with ID = '%s'", ccm.getId()));
		try {
			validate(ccm, cc, tom);
			fill(cc, ccm);

			//If this method is called in the context of 'Delete ResourceType',
			//first save any change in the container of the ResourceType marked deleted
			//and delete the marked ResourceType instance from the container's resourceType collection.
			if(resourceTypeToBeDeleted != null && resourceTypeToBeDeleted > 0){
				for(ResourceType rt: cc.getResourceType())
					if(rt.getId().equals(resourceTypeToBeDeleted))
					{
						cc.getResourceType().remove(rt);
						break;
					}
			}
			tom.persist();

			//Once all the changes are made in the database, reset the ContextClassModel instance.
			ccm = RACMModelFactory.newContextClassModel(cc);
			ccm.setValid(true);
		} catch (Exception e) {
			ccm = RACMModelFactory.newContextClassModel(cc);
			ccm.setException(e);
			ccm.setValid(false);
		}
		return ccm;
	}

	/**
	 *
	 * @param contextClassModel
	 * @param tom
	 *          MUST NOT be null (precondition)
	 * @return
	 */
	private boolean existContextClass(String name, TransientObjectManager tom) {
		Query query = tom.createNamedQuery("ContextClass.findByName").setParameter("name", name);
		List<MetadataObject> os = tom.queryJPA(query, false);
		return !os.isEmpty();
	}

	/**
	 * Validate ContextClassModel from JSP.
	 */
	private void validate(ContextClassModel ccm, ContextClass cc, TransientObjectManager tom) {
		String name = ccm.getName();
		if (null == name || "".equals(name.trim())) {
			throw new IllegalArgumentException("A ContextClass MUST have a name");
		}
		if ((cc == null || !cc.getName().equals(name.trim())) && existContextClass(name, tom))
			throw new IllegalStateException(String.format("A ContextClass with name '%s' already exists", name));
	}

	/**
	 * If ContextClassModel passed from JSP is valid, set ContextClass instance with ContextClassModel.
	 * The ContextClass instance is either new instance if called from CreateContextClass method or
	 * existing instance in the database if called from UpdateContextClass method.
	 */
	private void fill(ContextClass cc, ContextClassModel ccm) {
		cc.setName(ccm.getName().trim());
		cc.setDescription(ccm.getDescription());
		cc.setRelease(ccm.getRelease());
	}

	/**
	 * Since ContextClass is a root entity, use EntityManager's transaction remove method directly
	 * instead of using Transient Object Manager's remove method.
	 * @param id
	 * @throws VOURPException
	 */
	public void deleteContextClass(long id) {
		TransientObjectManager tom = vourpContext.newTOM();

		EntityManager em = tom.getEntityManager();
		ContextClass cc = em.find(ContextClass.class, id);
		em.remove(cc);
		em.flush();
	}

	/**
	 * Return  ContextClassModel instance of given Id.
	 * @param id
	 * @return
	 * @throws Exception
	 */

	public ContextClassModel getModel(long id) {
		ContextClassModel contextClassModel;
		TransientObjectManager tom = vourpContext.newTOM();
		ContextClass cc = queryContextClass(id, tom);
		contextClassModel = RACMModelFactory.newContextClassModel(cc);
		return contextClassModel;
	}

	static ContextClass queryContextClass(long id, TransientObjectManager tom) {
		if (tom != null) {
			return tom.find(ContextClass.class, id);
		}
		return null;
	}

	public static List<ContextClass> queryContextClasses(UserProfile up) {
		return queryContextClasses(up.getTom());
	}


	static List<ContextClass> queryContextClasses(TransientObjectManager tom) {
		Objects.requireNonNull(tom);
		List<ContextClass> cc = new ArrayList<>();
		String query = "select o from ContextClass o order by o.name";

		List<MetadataObject> os = tom.queryJPA(query, false);
		for (MetadataObject o : os)
			cc.add((ContextClass) o);
		return cc;
	}

}
