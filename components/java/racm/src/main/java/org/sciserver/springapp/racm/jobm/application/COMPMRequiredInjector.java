package org.sciserver.springapp.racm.jobm.application;

import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import org.ivoa.dm.model.TransientObjectManager;
import org.sciserver.springapp.racm.login.NotAuthorizedException;
import org.sciserver.springapp.racm.utils.VOURPContext;
import org.sciserver.springapp.racm.utils.controller.JsonAPIHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.JsonNode;

import edu.jhu.job.COMPM;

@ControllerAdvice(annotations= {COMPMRequired.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class COMPMRequiredInjector {
	private final VOURPContext vourpContext;
	private final JsonAPIHelper jsonAPIHelper;
	@Autowired
	COMPMRequiredInjector(JsonAPIHelper jsonAPIHelper, VOURPContext vourpContext) {
		this.jsonAPIHelper = jsonAPIHelper;
		this.vourpContext = vourpContext;
	}

	@ModelAttribute
	COMPMInfo verifyCOMPM(HttpServletRequest request) {
		String compmId = request.getHeader("X-Service-Auth-ID");
		TransientObjectManager tom = vourpContext.newTOM();
		Query q = tom.createQuery("select c from COMPM c where c.uuid=:uuid").setParameter("uuid", compmId);
		COMPM compm = tom.queryOne(q, COMPM.class);
		if (compm == null)
			throw new NotAuthorizedException();

		return new COMPMInfo(compm.getUuid(), compm.getId(),
				compm.getDefaultJobTimeout(), compm.getDefaultJobsPerUser());
	}

	@ExceptionHandler(NotAuthorizedException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	ResponseEntity<JsonNode> compmNotAuthorizedMessage() {
		return jsonAPIHelper.notAuthorizedServiceEntity();
	}

	public class COMPMInfo {
		private final String uuid;
		private final long id;
		private final long defaultJobTimeout;
		private final long defaultJobsPerUser;
		private COMPMInfo(String uuid, long id, long defaultJobTimeout, long defaultJobsPerUser) {
			this.uuid = uuid;
			this.id = id;
			this.defaultJobTimeout = defaultJobTimeout;
			this.defaultJobsPerUser = defaultJobsPerUser;
		}
		public String uuid() {
			return uuid;
		}
		public long id() {
			return id;
		}
		public long defaultJobTimeout() {
			return defaultJobTimeout;
		}
		public long defaultJobsPerUser() {
			return defaultJobsPerUser;
		}
	}
}
