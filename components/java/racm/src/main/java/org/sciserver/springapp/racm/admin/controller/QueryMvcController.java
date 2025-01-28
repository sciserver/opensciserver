package org.sciserver.springapp.racm.admin.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.ivoa.dm.VOURPException;
import org.ivoa.dm.model.MetadataObject;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.admin.model.JPQLQueryModel;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACMAccessControl;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value="/query")
public class QueryMvcController {

	private final RACMAccessControl rac;
	private final VOURPContext vourpContext;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	public QueryMvcController(RACMAccessControl rac, VOURPContext vourpContext) {
		this.rac = rac;
		this.vourpContext = vourpContext;
	}

	@GetMapping("/jpql")
	public ModelAndView jpqlQuery(@AuthenticationPrincipal UserProfile up) throws VOURPException {

		if(!rac.canUserQueryJOQL(up.getUser()))
			throw new VOURPException(VOURPException.UNAUTHORIZED, "User not allowed to submit JOQL queries");

		ModelAndView mav = new ModelAndView();
		mav.addObject("jpqlQueryModel",new JPQLQueryModel());
		mav.setViewName("JPQLQuery");
		return mav;
	}

	@PostMapping("/jpql")
	public ModelAndView executeJpqlQuery(JPQLQueryModel query, @AuthenticationPrincipal UserProfile up)
			throws VOURPException {
		if(!rac.canUserQueryJOQL(up.getUser()))
			throw new VOURPException(VOURPException.UNAUTHORIZED, "User not allowed to submit JOQL queries");

		TransientObjectManager tom = vourpContext.newTOM();

		try {
			Query q = tom.createQuery(query.getJpql());
			List<?> l = tom.customJPQL(q, false);
			List<String> rows = new ArrayList<>();
			for(Object o:l){
				if(o instanceof MetadataObject)
					rows.add(((MetadataObject)o).deepToString());
				else if (o instanceof Object[])
					rows.add(Arrays.deepToString((Object[])o));
				else 
					rows.add(o != null?o.toString():null);
			}
			query.setRows(rows);
		} catch(Exception e) {
			query.setError(e.getMessage());
		}
		ModelAndView mav = new ModelAndView();
		mav.addObject("jpqlQueryModel", query);
		mav.setViewName("JPQLQuery");
		return mav;
	}

}
