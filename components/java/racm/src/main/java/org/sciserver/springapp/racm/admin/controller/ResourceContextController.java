package org.sciserver.springapp.racm.admin.controller;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.ivoa.dm.VOURPException;
import org.sciserver.racm.rctree.model.ResourceContextMVCModel;
import org.sciserver.racm.rctree.model.ResourceMVCModel;
import org.sciserver.springapp.racm.resources.application.RACMModelFactory;
import org.sciserver.springapp.racm.resources.application.ResourceContextManager;
import org.sciserver.springapp.racm.resources.application.ResourceManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMAccessControl;
import org.sciserver.springapp.racm.utils.controller.ResourceNotFoundException;
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

import edu.jhu.rac.ResourceContext;

@Controller
@RequestMapping("rctree")
public class ResourceContextController {
	private static final String MODEL_RESOURCE = "resourceModel";
	private static final String MODEL_RESOURCE_CONTEXT = "resourceContextModel";
	private static final String VIEW_RESOURCE_CONTEXT = "ViewResourceContext";
	private static final String VIEW_RESOURCE = "ViewResource";
	private final ResourceContextManager rcm;
	private final ResourceManager rm;
	private final RACMAccessControl rac;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public ResourceContextController(ResourceContextManager rcm, ResourceManager rm, RACMAccessControl rac) {
		this.rcm = rcm;
		this.rm = rm;
		this.rac = rac;
	}

	@GetMapping("/resourceContextList")
	// value= context relative path since it starts with '/' relative to webapp
	// root
	public ModelAndView showResourceContexts(@AuthenticationPrincipal UserProfile up) throws VOURPException {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("ListResourceContexts");
		List<ResourceContext> resourceContexts = rcm.queryResourceContexts();
		mav.addObject("resourceContexts", resourceContexts);
		return mav;
	}

	@GetMapping("/resourceContext")
	public ModelAndView resourceContextCreateForm(@AuthenticationPrincipal UserProfile up) {
		ResourceContextMVCModel resourceContextModel = RACMModelFactory.newResourceContextModel(up);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_RESOURCE_CONTEXT, resourceContextModel);
		mav.setViewName(VIEW_RESOURCE_CONTEXT);
		return mav;
	}

	@PostMapping("/resourceContext")
	public ModelAndView resourceContextCreateSubmit(ResourceContextMVCModel resourceContextModel,
			@RequestParam(required = true) String nextaction,
			@RequestParam(required = false) Long selectedResourceId,
			@AuthenticationPrincipal UserProfile up) {
		return postResourceContext(up, resourceContextModel, nextaction, selectedResourceId);
	}

	@GetMapping("/resourceContext/{resourceContextId}")
	public ModelAndView resourceContextEditForm(@PathVariable("resourceContextId") long resourceContextId,
			@AuthenticationPrincipal UserProfile up) {
		ResourceContextMVCModel resourceContextModel = rcm.getModel(up.getTom(), resourceContextId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_RESOURCE_CONTEXT, resourceContextModel);
		mav.setViewName(VIEW_RESOURCE_CONTEXT);
		return mav;
	}

	@PostMapping("/resourceContext/{resourceContextId}")
	public ModelAndView resourceContextEditSubmit(Model model, ResourceContextMVCModel resourceContextModel,
			@RequestParam(required = false) String nextaction, @RequestParam(required = false) long selectedResourceId, @PathVariable("resourceContextId") long resourceContextId,
			@AuthenticationPrincipal UserProfile up) {
		return postResourceContext(up, resourceContextModel, nextaction, selectedResourceId);
	}

	private ModelAndView postResourceContext(UserProfile up, ResourceContextMVCModel resourceContextModel,
			String nextaction, long selectedResourceId) {
		ModelAndView mav = new ModelAndView();
		if("deleteresource".equals(nextaction))
			resourceContextModel = rcm.updateResourceContext(up, resourceContextModel, selectedResourceId);
		else
			resourceContextModel = rcm.editResourceContext(up,resourceContextModel);


		mav.setViewName(VIEW_RESOURCE_CONTEXT);
		mav.addObject(MODEL_RESOURCE_CONTEXT, resourceContextModel);

		if (!resourceContextModel.isValid()) {
			return mav;
		}
		if ("back".equals(nextaction)) {
			mav.setViewName("redirect:/rctree/resourceContextList");
		} else if ("createresource".equals(nextaction)) {
			mav.setViewName("redirect:/rctree/resource?containerId=" + resourceContextModel.getId());
		} else if ("editresource".equals(nextaction)) {
			mav.setViewName("redirect:/rctree/resource/" + selectedResourceId);
		}

		return mav;
	}


	@PostMapping("/resourceContext/{id}/delete")
	public ModelAndView deleteResourceContextSubmit(@PathVariable("id") long id,
			@AuthenticationPrincipal UserProfile up) {
		rcm.deleteResourceContext(id);
		ModelAndView mav = new ModelAndView();
		mav.setViewName("redirect:/rctree/resourceContextList/");

		return mav;
	}


	private ModelAndView postResource(UserProfile up, ResourceMVCModel resourceModel, String nextaction) {
		ModelAndView mav = new ModelAndView();
		resourceModel = rm.editResource(up, resourceModel);

		mav.setViewName(VIEW_RESOURCE);
		mav.addObject(MODEL_RESOURCE, resourceModel);

		if (!resourceModel.isValid()) {
			return mav;
		} else if("back".equals(nextaction)) {
			mav.setViewName("redirect:/rctree/resourceContext/" + resourceModel.getContainerId());
		}

		return mav;
	}

	@PostMapping("/resource/{id}/delete")
	public ModelAndView deleteResourceSubmit(@RequestParam(required = true) long containerId, @PathVariable("id") long id,
			@AuthenticationPrincipal UserProfile up) throws VOURPException {
		rm.deleteResource(id);
		ModelAndView mav = new ModelAndView();
		mav.setViewName("redirect:/rctree/resourceContext/" + containerId);

		return mav;
	}

	@GetMapping("/resource")
	public ModelAndView resourceCreateForm(@RequestParam(required = true) long containerId,
			@AuthenticationPrincipal UserProfile up) {
		ResourceMVCModel rmm = RACMModelFactory.newResourceModel(up.getTom(), containerId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_RESOURCE, rmm);
		mav.setViewName(VIEW_RESOURCE);
		return mav;
	}

	@GetMapping("/resource/{resourceId}")
	public ModelAndView resourceEditForm(@PathVariable long resourceId,
			@AuthenticationPrincipal UserProfile up) {
		ResourceMVCModel resourceModel = rm.getModel(up.getTom(),resourceId);
		if (resourceModel == null)
			throw new ResourceNotFoundException("No Resource exists with id = " + resourceId);
		ModelAndView mav = new ModelAndView();
		mav.addObject(MODEL_RESOURCE, resourceModel);
		mav.setViewName(VIEW_RESOURCE);
		return mav;
	}

	@PostMapping("/resource")
	public ModelAndView resourceCreateSubmit(ResourceMVCModel resourceModel,
			@RequestParam(required = false) String nextaction,
			@AuthenticationPrincipal UserProfile up) {
		if(!rac.canUserCreateResource(up.getUser(), resourceModel.getContextUUID()))
			throw new ForbiddenException();
		return postResource(up, resourceModel, nextaction);
	}

	@PostMapping("/resource/{resourceId}")
	public ModelAndView resourceEditSubmit(ResourceMVCModel resourceModel, @PathVariable long resourceId,
			@RequestParam(required = false) String nextaction, @AuthenticationPrincipal UserProfile up) {

		return postResource(up, resourceModel, nextaction);
	}

}
