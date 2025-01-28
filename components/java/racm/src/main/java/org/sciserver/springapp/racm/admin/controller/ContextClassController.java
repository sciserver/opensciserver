package org.sciserver.springapp.racm.admin.controller;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ivoa.dm.VOURPException;
import org.sciserver.racm.cctree.model.ActionModel;
import org.sciserver.racm.cctree.model.ContextClassModel;
import org.sciserver.racm.cctree.model.ResourceTypeModel;
import org.sciserver.racm.cctree.model.RoleModel;
import org.sciserver.springapp.racm.resources.application.ActionManager;
import org.sciserver.springapp.racm.resources.application.ContextClassManager;
import org.sciserver.springapp.racm.resources.application.RACMModelFactory;
import org.sciserver.springapp.racm.resources.application.ResourceTypeManager;
import org.sciserver.springapp.racm.resources.application.RoleManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMAccessControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import edu.jhu.rac.Action;
import edu.jhu.rac.ContextClass;
import edu.jhu.rac.ResourceType;

@Controller
@RequestMapping("cctree")
public class ContextClassController {
	private static final String REDIRECT_RESOURCE_TYPE_PREFIX = "redirect:/cctree/resourceType/";
	private static final String MODEL_CONTEXT_CLASS = "contextClassModel";
	private static final String MODEL_RESOURCE_TYPE = "resourceTypeModel";
	private static final String MODEL_ROLE = "roleModel";
	private static final String MODEL_ACTION = "actionModel";
	private static final String VIEW_CONTEXT_CLASS = "ViewContextClass";
	private static final String VIEW_RESOURCE_TYPE = "ViewResourceType";
	private static final String VIEW_ROLE = "ViewRole";
	private static final String VIEW_ACTION = "ViewAction";

	private final ContextClassManager ccm;
	private final ResourceTypeManager resourceTypeManager;
	private final RoleManager rm;
	private final ActionManager am;
	private final RACMAccessControl rac;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public ContextClassController(RACMAccessControl rac, ContextClassManager ccm,
			RoleManager rm, ResourceTypeManager rtm, ActionManager am) {
		this.rac = rac;
		this.ccm = ccm;
		this.resourceTypeManager = rtm;
		this.rm = rm;
		this.am = am;
	}


	@GetMapping("/contextClassList")
	// value= context relative path since it starts with '/' relative to webapp
	// root
	public ModelAndView showContextClasses(@AuthenticationPrincipal UserProfile up) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("ListContextClasses");
		List<ContextClass> contextClasses = ContextClassManager.queryContextClasses(up);
		mav.addObject("contextClasses", contextClasses);
		return mav;
	}

	@PostMapping("/contextClass/{id}/delete")
	public ModelAndView deleteContextClasSubmit(@PathVariable("id") long id,
			@AuthenticationPrincipal UserProfile up) {
		if(!rac.canUserEditContextClass(up.getUser(), id))
			throw new ForbiddenException();


		ccm.deleteContextClass(id);
		ModelAndView mav = new ModelAndView();
		mav.setViewName("redirect:/cctree/contextClassList/");

		return mav;
	}



	@GetMapping("/contextClass")
	public ModelAndView contextClassCreateForm(@AuthenticationPrincipal UserProfile up) {
		ContextClassModel contextClassModel = new ContextClassModel();
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_CONTEXT_CLASS, contextClassModel);
		mav.setViewName(VIEW_CONTEXT_CLASS);
		return mav;
	}

	@PostMapping("/contextClass")
	public ModelAndView contextClassCreateSubmit(ContextClassModel contextClassModel,
			@RequestParam(required = true) String nextaction, @RequestParam(required = false) long selectedRTId, @AuthenticationPrincipal UserProfile up) {
		if(!rac.canUserCreateContextClass(up.getUser()))
			throw new ForbiddenException();

		return postContextClass(contextClassModel, nextaction, selectedRTId, up);
	}

	@GetMapping("/contextClass/{contextClassId}")
	public ModelAndView contextClassEditForm(@PathVariable("contextClassId") long contextClassId,
			@AuthenticationPrincipal UserProfile up) {
		ContextClassModel contextClassModel = ccm.getModel(contextClassId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_CONTEXT_CLASS, contextClassModel);
		mav.setViewName(VIEW_CONTEXT_CLASS);
		return mav;
	}

	@PostMapping("/contextClass/{contextClassId}")
	public ModelAndView contextClassEditSubmit(Model model, ContextClassModel contextClassModel,
			@RequestParam(required = false) String nextaction, @RequestParam(required = false) long selectedRTId, @PathVariable("contextClassId") long contextClassId,
			@AuthenticationPrincipal UserProfile up) {

		if(!rac.canUserEditContextClass(up.getUser(), contextClassId))
			throw new ForbiddenException();

		return postContextClass(contextClassModel, nextaction, selectedRTId, up);
	}

	private ModelAndView postContextClass(ContextClassModel contextClassModel, String nextaction, long selectedRTId, UserProfile user) {
		ModelAndView mav = new ModelAndView();
		if("deleteresourcetype".equals(nextaction))
			contextClassModel = ccm.updateContextClass(contextClassModel, selectedRTId, user);
		else
			contextClassModel = ccm.editContextClass(contextClassModel, user);


		mav.addObject(MODEL_CONTEXT_CLASS,contextClassModel);
		mav.setViewName(VIEW_CONTEXT_CLASS);

		if (!contextClassModel.isValid())
			return mav;

		if ("back".equals(nextaction)) {
			mav.setViewName("redirect:/cctree/contextClassList");
		} else if ("createresourcetype".equals(nextaction)) {
			mav.setViewName("redirect:/cctree/resourceType?containerId=" + contextClassModel.getId());
		} else if ("editresourcetype".equals(nextaction)) {
			mav.setViewName(REDIRECT_RESOURCE_TYPE_PREFIX + selectedRTId);
		}
		return mav;
	}

	private ModelAndView postResourceType(ResourceTypeModel resourceTypeModel, String nextaction,
			Long selectedRoleId, long selectedActionId) {
		ModelAndView mav = new ModelAndView();
		if("deleterole".equals(nextaction) || "deleteaction".equals(nextaction))
			resourceTypeModel = resourceTypeManager.updateResourceType(resourceTypeModel, selectedRoleId, selectedActionId);
		else
			resourceTypeModel = resourceTypeManager.editResourceType(resourceTypeModel);

		mav.setViewName(VIEW_RESOURCE_TYPE);
		mav.addObject(MODEL_RESOURCE_TYPE, resourceTypeModel);
		if (!resourceTypeModel.isValid()) {
			return mav;
		}

		if ("createrole".equals(nextaction)) {
			mav.setViewName("redirect:/cctree/role?containerId=" + resourceTypeModel.getId());
		} else if ("createaction".equals(nextaction)) {
			mav.setViewName("redirect:/cctree/action?containerId=" + resourceTypeModel.getId());
		} else if("back".equals(nextaction)) {
			mav.setViewName("redirect:/cctree/contextClass/" + resourceTypeModel.getContextClassId());
		} else if ("editrole".equals(nextaction)) {
			mav.setViewName("redirect:/cctree/role/" + selectedRoleId);
		} else if ("editaction".equals(nextaction)) {
			mav.setViewName("redirect:/cctree/action/" + selectedActionId);
		}

		return mav;
	}

	@PostMapping("/resourceType/{id}/delete")
	public ModelAndView deleteResourceTypeSubmit(@RequestParam(required = true) long containerId, @PathVariable("id") long id,
			@AuthenticationPrincipal UserProfile up) throws VOURPException {

		if(!rac.canUserEditContextClass(up.getUser(), containerId))
			throw new ForbiddenException();

		resourceTypeManager.deleteResourceType(id);
		ModelAndView mav = new ModelAndView();
		mav.setViewName("redirect:/cctree/contextClass/" + containerId);

		return mav;
	}


	@GetMapping("/resourceType")
	public ModelAndView resourceTypeCreateForm(@RequestParam(required = true) long containerId,
			@AuthenticationPrincipal UserProfile up) {

		ResourceTypeModel rtm = new ResourceTypeModel();
		rtm.setContextClassId(containerId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_RESOURCE_TYPE, rtm);
		mav.setViewName(VIEW_RESOURCE_TYPE);
		return mav;
	}

	@GetMapping("/resourceType/{resourceTypeId}")
	public ModelAndView resourceTypeEditForm(@PathVariable long resourceTypeId,
			@AuthenticationPrincipal UserProfile up) {

		ResourceTypeModel resourceTypeModel = resourceTypeManager.getModel(resourceTypeId);
		if (resourceTypeModel == null)
			throw new IllegalArgumentException("No ResourceType exists with id = " + resourceTypeId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_RESOURCE_TYPE, resourceTypeModel);
		mav.setViewName(VIEW_RESOURCE_TYPE);
		return mav;
	}

	/**
	 * This method is often called when a user wants to drill down in a ResourceType.<br/>
	 * Currently this will cause an error. Better would be if the UI would not try to save if the user is not allowed to do so.
	 *
	 * @param resourceTypeModel
	 * @param nextaction
	 * @param selectedRoleId
	 * @param selectedActionId
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/resourceType")
	public ModelAndView resourceTypeCreateSubmit(ResourceTypeModel resourceTypeModel,
			@RequestParam(required = false) String nextaction, @RequestParam(required = false) Long selectedRoleId,
			@RequestParam(required = false) long selectedActionId, @AuthenticationPrincipal UserProfile up) {

		if(!rac.canUserEditContextClass(up.getUser(), resourceTypeModel.getContextClassId()))
			throw new ForbiddenException();

		return postResourceType(resourceTypeModel, nextaction, selectedRoleId, selectedActionId);
	}

	@PostMapping("/resourceType/{resourceTypeId}")
	public ModelAndView resourceTypeEditSubmit(ResourceTypeModel resourceTypeModel, @PathVariable long resourceTypeId,
			@RequestParam(required = false) String nextaction, @RequestParam(required = false) long selectedRoleId,
			@RequestParam(required = false) long selectedActionId, @AuthenticationPrincipal UserProfile up) {

		if(!rac.canUserEditResourceType(up.getUser(), resourceTypeId))
			throw new ForbiddenException();

		return postResourceType(resourceTypeModel, nextaction, selectedRoleId, selectedActionId);
	}

	@PostMapping("/role/{id}/delete")
	public ModelAndView deleteRoleSubmit(@RequestParam(required = true) long containerId, @PathVariable("id") long id,
			@AuthenticationPrincipal UserProfile up) throws VOURPException {

		if(!rac.canUserEditResourceType(up.getUser(), containerId))
			throw new ForbiddenException();

		rm.deleteRole(id);
		ModelAndView mav = new ModelAndView();
		mav.setViewName(REDIRECT_RESOURCE_TYPE_PREFIX + containerId);

		return mav;
	}

	@GetMapping("/role")
	public ModelAndView roleCreateForm(@RequestParam(required = true) long containerId,
			@AuthenticationPrincipal UserProfile up) {

		ResourceType rt = resourceTypeManager.queryResourceType(containerId, up.getTom());
		if(rt == null)
			throw new IllegalArgumentException("Cannot find ResourceType for Role::containerId = "+containerId);

		RoleModel roleModel = RACMModelFactory.newRoleModel(rt);
		roleModel.setResourceTypeId(containerId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_ROLE, roleModel);
		mav.setViewName(VIEW_ROLE);
		return mav;
	}

	@GetMapping("/role/{roleId}")
	public ModelAndView roleEditForm(@PathVariable long roleId) {

		RoleModel roleModel = rm.getModel(roleId);
		if (roleModel == null)
			throw new IllegalArgumentException("No Role exists with id = " + roleId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_ROLE, roleModel);
		mav.setViewName(VIEW_ROLE);
		return mav;
	}

	private ModelAndView postRole(RoleModel roleModel) {
		ModelAndView mav = new ModelAndView();
		roleModel = rm.editRole(roleModel);
		if (!roleModel.isValid()) {
			mav.setViewName(VIEW_ROLE);
			mav.addObject(MODEL_ROLE, roleModel);
		}
		else
			mav.setViewName(REDIRECT_RESOURCE_TYPE_PREFIX + roleModel.getResourceTypeId());
		return mav;
	}

	@PostMapping({ "/role", "/role/{roleId}" })
	public ModelAndView roleCreateOrEditSubmit(RoleModel roleModel,
			@PathVariable(required=false) Long roleId,
			@AuthenticationPrincipal UserProfile up) {

		if(!rac.canUserEditResourceType(up.getUser(), roleModel.getResourceTypeId()))
			throw new ForbiddenException();

		return postRole(roleModel);
	}

	@PostMapping("/action/{id}/delete")
	public ModelAndView deleteActionSubmit(@RequestParam(required = true) long containerId, @PathVariable("id") long id,
			@AuthenticationPrincipal UserProfile up) throws VOURPException {

		am.deleteAction(id);
		ModelAndView mav = new ModelAndView();
		mav.setViewName(REDIRECT_RESOURCE_TYPE_PREFIX + containerId);

		return mav;
	}

	@GetMapping("/action")
	public ModelAndView actionCreateForm(@RequestParam(required = true) long containerId,
			@AuthenticationPrincipal UserProfile up) {

		ActionModel actionModel = new ActionModel();
		actionModel.setResourceTypeId(containerId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_ACTION, actionModel);
		mav.setViewName(VIEW_ACTION);
		return mav;
	}

	@GetMapping("/action/{actionId}")
	public ModelAndView actionEditForm(@PathVariable long actionId,
			@AuthenticationPrincipal UserProfile up) {
		Action action = am.queryAction(actionId, up.getTom());
		if (action == null)
			throw new IllegalArgumentException("No Action exists with id = " + actionId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_ACTION, RACMModelFactory.newActionModel(action));
		mav.setViewName(VIEW_ACTION);
		return mav;
	}

	private ModelAndView postAction(ActionModel actionModel) {
		ModelAndView mav = new ModelAndView();
		actionModel = am.editAction(actionModel);
		if (!actionModel.isValid()) {
			mav.setViewName(VIEW_ACTION);
			mav.addObject(MODEL_ACTION, actionModel);
		}
		else
			mav.setViewName(REDIRECT_RESOURCE_TYPE_PREFIX + actionModel.getResourceTypeId());
		return mav;
	}

	@PostMapping("/action")
	public ModelAndView actionCreateSubmit(ActionModel actionModel) {

		return postAction(actionModel);
	}

	@PostMapping("/action/{actionId}")
	public ModelAndView actionEditSubmit(ActionModel actionModel, @PathVariable long actionId,
			HttpServletRequest request, HttpServletResponse response) {

		return postAction(actionModel);
	}
}
