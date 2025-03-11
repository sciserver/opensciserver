package org.sciserver.springapp.racm.resources.application;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.rctree.model.ResourceContextMVCModel;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import edu.jhu.rac.ContextClass;
import edu.jhu.rac.Resource;
import edu.jhu.rac.ResourceContext;
import edu.jhu.user.ServiceAccount;
import edu.jhu.user.User;

/**
 * Main class that should control whether a user is allowed to perform certain action on some resource.<br/>
 * @author Gerard
 *
 */
@Service
public class ResourceContextManager {
	private final VOURPContext vourpContext;

	@Autowired
	public ResourceContextManager(VOURPContext vourpContext) {
		this.vourpContext = vourpContext;
	}

	public ResourceContextMVCModel editResourceContext(UserProfile up, ResourceContextMVCModel resourceContextModel) {
		if (resourceContextModel.getId() == null)
			return createResourceContext(up, resourceContextModel);
		else
			return updateResourceContext(up, resourceContextModel);
	}

	private ResourceContextMVCModel createResourceContext(UserProfile up, ResourceContextMVCModel resourceContextModel) {
		TransientObjectManager tom = up.getTom();
		ResourceContext rc = null;
		List<ContextClass> ccs = null;
		try {

			ccs = ContextClassManager.queryContextClasses(tom);
			validate(resourceContextModel, null);
			rc = new ResourceContext(tom);

			fill(rc, resourceContextModel, ccs);

			Resource rcroot=RACMUtil.addRootContext(rc);
			RACMUtil.assignAllPrivileges(rcroot, up.getUser());

			tom.persist();

			resourceContextModel = RACMModelFactory.newResourceContextModel(rc, ccs, tom);
			resourceContextModel.setValid(true);
		} catch (Exception e) {
			//If fails set Exception to the ResourceContextModel object passed by input argument
			resourceContextModel.setException(e);
			resourceContextModel.setValid(false);

		}
		return resourceContextModel;
	}

	private ResourceContextMVCModel updateResourceContext(UserProfile up, ResourceContextMVCModel ccm) {
		return updateResourceContext(up, ccm, null);
	}

	/*
	 * This method is called directly from 'postResourceContext' method in ResourceManagementController
	 * if there is a ResourceType is marked for deletion(i.e., nextaction=deleteresource)
	 */
	public ResourceContextMVCModel updateResourceContext(UserProfile up, ResourceContextMVCModel rcm, Long resourceToBeDeleted) {
		TransientObjectManager tom = up.getTom();
		ResourceContext rc = queryResourceContext(rcm.getId(), tom);
		List<ContextClass> ccs = null;
		if (rc == null)
			throw new IllegalArgumentException(String.format("Cannot update non-existent ResourceContext with ID = '%s'", rcm.getId()));

		try {
			ccs = ContextClassManager.queryContextClasses(tom);
			validate(rcm, rc);
			fill(rc, rcm, ccs);

			//If this method is called in the context of 'Delete Resource',
			//first save any change in the container of the Resource marked deleted
			//and delete the marked Resource instance from the container's resource collection.
			if(resourceToBeDeleted != null && resourceToBeDeleted > 0){
				for(Resource r: rc.getResource())
					if(r.getId().equals(resourceToBeDeleted))
					{
						rc.getResource().remove(r);
						break;
					}
			}
			tom.persist();


			rcm = RACMModelFactory.newResourceContextModel(rc, ccs, tom);
			rcm.setValid(true);
		} catch (Exception e) {
			rcm = RACMModelFactory.newResourceContextModel(rc, ccs, tom);
			rcm.setException(e);
			rcm.setValid(false);
		}
		return rcm;
	}

	/**
	 * Validate ResourceContextModeResourceContext
	 */
	private void validate(ResourceContextMVCModel rcm, ResourceContext rc) {
		String trimeduuid = (null==rcm.getUuid()) ? null:rcm.getUuid().trim();
		String uuid = (null==trimeduuid || "".equals(trimeduuid))? null:trimeduuid;
		if (null != rc){
			if (rc.isPurelyTransient() && uuid !=null)
				throw new IllegalArgumentException("ResourceContext's uuid must be set by the system.");

			if (!rc.isPurelyTransient() && uuid ==null)
				throw new IllegalArgumentException("ResourceContext must have uuid.");
		}
		if (rcm.getContextClassModel() == null)
			throw new IllegalArgumentException("A ResourceContext MUST have a context class");
	}

	/**
	 * If ResourceContextModel passed from JSP is valid, set ResourceContext instance with ResourceContextModel.
	 * The ResourceContext instance is either new instance if called from CreateResourceContext method or
	 * existing instance in the database if called from UpdateResourceContext method.
	 */
	private void fill(ResourceContext rc, ResourceContextMVCModel rcm, List<ContextClass> ccs) {
		rc.setLabel(rcm.getLabel().trim());
		rc.setDescription(rcm.getDescription());
		rc.setRacmEndpoint(rcm.getEndpoint());
        if (rc.isPurelyTransient())
            rc.setUuid(UUID.randomUUID().toString());
		if (!StringUtils.isEmpty(rcm.getSecretToken())) {
			if (rc.getAccount() == null) {
				ServiceAccount account = new ServiceAccount(rc.getTom());
				account.setServiceToken(rcm.getSecretToken());
				account.setPublisherDID(rc.getUuid());
                rc.setAccount(account);
			} else {
				rc.getAccount().setServiceToken(rcm.getSecretToken());
			}
		}

		String ccName = rcm.getContextClassModel().getName();
		for (ContextClass cc: ccs) {
			if (ccName.equals(cc.getName())){
				rc.setContextClass(cc);
				break;
			}
		}

		if (null == rc.getContextClass())
			throw new IllegalArgumentException("Referred context class " + ccName + " is not found in the database.");
	}

	/**
	 * Since ContextClass is a root entity, use EntityManager's transaction remove method directly
	 * instead of using Transient Object Manager's remove method.
	 * @param id
	 * @throws VOURPException
	 */
	public void deleteResourceContext(long id) {
		TransientObjectManager tom = vourpContext.newTOM();

		EntityManager em = tom.getEntityManager();
		ResourceContext rc = em.find(ResourceContext.class, id);
		em.remove(rc);
		em.flush();

	}


	public ResourceContextMVCModel getModel(TransientObjectManager tom, long id) {
		ResourceContextMVCModel resourceContextModel;
		ResourceContext rc = queryResourceContext(id, tom);

		List<ContextClass> ccs = ContextClassManager.queryContextClasses(tom);
		resourceContextModel = RACMModelFactory.newResourceContextModel(rc, ccs, tom);
		return resourceContextModel;
	}

	public static ResourceContext queryResourceContext(long id, TransientObjectManager tom) {
		if (tom != null) {
			return tom.find(ResourceContext.class, id);
		}
		return null;
	}


	public List<ResourceContext> queryResourceContexts() throws VOURPException {
		TransientObjectManager tom = vourpContext.newTOM();
		String query = "select o from ResourceContext o order by o.label";
		return tom.queryJPA(tom.createQuery(query), ResourceContext.class);
	}

	/**
	 * Create a new ServiceAccount for the specified ResourceContext if it does not yet have one.<br/>
	 * In the latter case check if a serviceToken exists
	 * @param rc
	 */
	public static void newServiceAccount(ResourceContext rc){
	    ServiceAccount account = rc.getAccount();
	    if(account == null) {
	        account = new ServiceAccount(rc.getTom());
            account.setServiceToken(ResourceContextManager.generateRandomServiceToken());
            account.setPublisherDID(UUID.randomUUID().toString());
            rc.setAccount(account);
        }
	    
	}
	
	/**
	 * Generate a random alphanumeric string of length 48 to serve as the secret serviceToken for a ServiceAccount.<br/>
	 * This code is written to be equivalent to the buttonNewToken_clickHandler in ViewResourceContext.jsp.
	 * @return String the random token
	 * @TODO may be optimized?
	 */
	public static String generateRandomServiceToken() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 48;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
          .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
          .limit(targetStringLength)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();

        return generatedString;   
    }
}
