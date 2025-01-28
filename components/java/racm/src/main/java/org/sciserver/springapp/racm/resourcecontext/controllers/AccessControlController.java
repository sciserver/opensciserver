package org.sciserver.springapp.racm.resourcecontext.controllers;

import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.SERVICE_TOKEN_HEADER;

import java.util.Arrays;
import java.util.List;

import org.ivoa.dm.model.InvalidTOMException;
import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.racm.resourcecontext.model.PrivilegeModel;
import org.sciserver.springapp.racm.resourcecontext.vourp.ResourceContextAuthentication;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.jhu.rac.Privilege;
import edu.jhu.rac.Resource;
import edu.jhu.user.SciserverEntity;
@RestController
@RequestMapping("rc/{resourceContextUUID}/resource/{resourceUUID}")
public class AccessControlController {
	private final VOURPContext vourpContext;
	private final RACMUtil racmUtil;
	private final ResourceContextAuthentication resourceContextAuthentication;

	AccessControlController(VOURPContext vourpContext, RACMUtil racmUtil,
			ResourceContextAuthentication resourceContextAuthentication) {
		this.vourpContext = vourpContext;
		this.racmUtil = racmUtil;
		this.resourceContextAuthentication = resourceContextAuthentication;
	}

	@PostMapping("privileges")
	public void addNewPrivilege(@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID,
			@RequestBody List<PrivilegeModel> newPrivileges) throws InvalidTOMException {
		resourceContextAuthentication.verifyCorrectToken(resourceContextUUID, serviceToken);

		Resource resource = getResource(resourceUUID);
		newPrivileges.forEach(newPrivilege -> {
			Privilege newDatabasePrivilege = new Privilege(resource);
			newDatabasePrivilege.setAction(racmUtil.findAction(resource, newPrivilege.getActionName()));
			newDatabasePrivilege.setScisEntity(getEntity(newPrivilege));
		});
		vourpContext.newTOM().persist();
	}

	private SciserverEntity getEntity(PrivilegeModel privilegeModel) {
		if (privilegeModel.getSciserverEntityId() != null) {
			return vourpContext.newTOM().find(SciserverEntity.class, privilegeModel.getSciserverEntityId());
		}
		if (StringUtils.isEmpty(privilegeModel.getSciserverEntityName()) ||
				StringUtils.isEmpty(privilegeModel.getSciserverEntityType()) ||
				!Arrays.asList("USER", "GROUP").contains(privilegeModel.getSciserverEntityType())) {
			throw new RegistrationInvalidException(
					"Need either entity id, or entity name/type where the type is USER or GROUP");
		}
		if (privilegeModel.getSciserverEntityType().equals("USER")) {
			return getUser(privilegeModel.getSciserverEntityName());
		} else {
			return getGroup(privilegeModel.getSciserverEntityName());
		}
	}

	private SciserverEntity getUser(String username) {
		TransientObjectManager tom = vourpContext.newTOM();
		return tom.queryOne(
				tom.createQuery("SELECT u FROM User u WHERE u.username = :user").setParameter("user", username)
				, SciserverEntity.class);
	}
	private SciserverEntity getGroup(String name) {
		TransientObjectManager tom = vourpContext.newTOM();
		return tom.queryOne(
				tom.createQuery("SELECT ug FROM UserGroup ug WHERE ug.name = :name").setParameter("name", name)
				, SciserverEntity.class);
	}

	private Resource getResource(String uuid) {
		TransientObjectManager tom = vourpContext.newTOM();
		return tom.queryOne(
				tom.createQuery("SELECT r FROM Resource r WHERE r.uuid = :uuid").setParameter("uuid", uuid)
				, Resource.class);
	}
}
