package org.sciserver.springapp.racm.jobm.controller;

import java.util.ArrayList;
import java.util.List;

import org.ivoa.dm.VOURPException;
import org.sciserver.racm.jobm.model.COMPMModel;
import org.sciserver.springapp.racm.admin.controller.ForbiddenException;
import org.sciserver.springapp.racm.jobm.application.COMPMManager;
import org.sciserver.springapp.racm.jobm.application.JOBMModelFactory;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACM;
import org.sciserver.springapp.racm.utils.RACMNames;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import edu.jhu.job.COMPM;

@Controller
@RequestMapping("compm/mvc")
public class COMPMMvcController {

	private static final String LOGGING_FIELD_COMPM = "compm";
	private static final String LOGGING_PREDICATE_COMPM = "COMPM '%s'";
	private final COMPMManager compmManager;
	private final RACM racm;
	private final JOBMModelFactory jobmModelFactory;

	@Autowired
	public COMPMMvcController(COMPMManager compmManager, RACM racm, JOBMModelFactory jobmModelFactory) {
		this.compmManager = compmManager;
		this.racm = racm;
		this.jobmModelFactory = jobmModelFactory;
	}

	@GetMapping("/list")
	public ModelAndView viewCompms(@AuthenticationPrincipal UserProfile user) {
		List<COMPM> compms = compmManager.queryCOMPMs(user);
		List<COMPMModel> cms = new ArrayList<>();
		for (COMPM c : compms)
			cms.add(jobmModelFactory.newCOMPMModel(c, user));
		LogUtils.buildLog()
			.forJOBM()
			.user(user)
			.sentence()
				.subject(user.getUsername())
				.verb("listed")
				.predicate("all compms")
			.log();
		ModelAndView mav = new ModelAndView();
		mav.setViewName("ViewCOMPMs");
		mav.addObject("compms", cms);
		mav.addObject("user", user.getUsername());
		return mav;
	}

	@GetMapping("/new")
	public ModelAndView viewCompm(@AuthenticationPrincipal UserProfile user) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("ViewCOMPM");
		COMPMModel cm = jobmModelFactory.newCOMPMModel(null, user);
		mav.addObject("compmModel", cm);

		LogUtils.buildLog()
			.forJOBM()
			.user(user)
			.sentence()
				.subject(user.getUsername())
				.verb("openned ")
				.predicate("new compm form")
			.log();
		return mav;
	}

	/**
	 *
	 * @param compmModel
	 * @param nextPage
	 *          possible values: "this" "list"
	 * @param request
	 * @param response
	 * @return
	 * @throws VOURPException
	 */
	@PostMapping("/new")
	public ModelAndView createCompm(COMPMModel compmModel, @RequestParam(required = false) String nextPage,
			@AuthenticationPrincipal UserProfile user) throws VOURPException {

		if (!racm.canUserDoRootAction(user.getUsername(), RACMNames.A_REGISTER_COMPMM))
			throw new ForbiddenException();

		ModelAndView mav = new ModelAndView();
		compmManager.createCOMPM(compmModel, user);

		LogUtils.buildLog()
			.forJOBM()
			.showInUserHistory()
			.user(user)
			.sentence()
				.subject(user.getUsername())
				.verb("registered")
				.predicate(LOGGING_PREDICATE_COMPM, compmModel.getDescription())
			.extraField(LOGGING_FIELD_COMPM, compmModel.getId())
			.log();

		mav.setViewName("redirect:/compm/mvc/list");
		return mav;
	}

	/**
	 *
	 * @param compmModel
	 * @param nextPage
	 *          possible values: "this" "list"
	 * @param request
	 * @param response
	 * @return
	 * @throws VOURPException
	 */
	@PostMapping("/view/{uuid}")
	public ModelAndView updateCompm(COMPMModel compmModel, @AuthenticationPrincipal UserProfile user)
			throws VOURPException {
		ModelAndView mav = new ModelAndView();
		compmManager.updateCOMPM(compmModel, user);

		LogUtils.buildLog()
			.forJOBM()
			.showInUserHistory()
			.user(user)
			.sentence()
				.subject(user.getUsername())
				.verb("updated")
				.predicate(LOGGING_PREDICATE_COMPM, compmModel.getDescription())
			.extraField(LOGGING_FIELD_COMPM, compmModel.getId())
			.log();
		mav.setViewName("redirect:/compm/mvc/list");
		return mav;
	}

	@GetMapping("/view/{compmUuid}")
	public ModelAndView viewCompm(@PathVariable String compmUuid,
			@AuthenticationPrincipal UserProfile user) {
		// / next will throw exception if not allowed to do this action.
		COMPM compm = compmManager.find(compmUuid);
		if (!user.getUserid().equals(compm.getCreatorUserid())) {
			throw new InsufficientPermissionsException("view compm " + compm.getDescription());
		}

		COMPMModel compmModel = jobmModelFactory.newCOMPMModel(compm, user);
		LogUtils.buildLog()
			.forJOBM()
			.user(user)
			.sentence()
				.subject(user.getUsername())
				.verb("viewed")
				.predicate(LOGGING_PREDICATE_COMPM, compmModel.getDescription())
			.extraField(LOGGING_FIELD_COMPM, compmModel.getId())
			.log();
		ModelAndView mav = new ModelAndView();

		mav.setViewName("ViewCOMPM");
		mav.addObject("compmModel", compmModel);
		return mav;
	}
}
