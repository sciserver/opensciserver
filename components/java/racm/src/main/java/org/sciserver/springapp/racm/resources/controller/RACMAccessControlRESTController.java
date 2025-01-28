package org.sciserver.springapp.racm.resources.controller;

import java.util.Optional;

import org.sciserver.racm.rctree.model.ResourceGrants;
import org.sciserver.racm.utils.model.NativeQueryResult;
import org.sciserver.springapp.racm.login.InsufficientPermissionsException;
import org.sciserver.springapp.racm.resources.application.RACMModelFactory;
import org.sciserver.springapp.racm.storem.application.RegistrationInvalidException;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.sciserver.springapp.racm.utils.RACM;
import org.sciserver.springapp.racm.utils.RACMAccessControl;
import org.sciserver.springapp.racm.utils.RACMUtil;
import org.sciserver.springapp.racm.utils.controller.RACMController;
import org.sciserver.springapp.racm.utils.logging.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.jhu.rac.Resource;

/**
 * This REST controller wraps access control requests in REST API calls.<br/>
 *
 * @author gerard
 *
 */
@RestController
@CrossOrigin
@RequestMapping("rest")
public class RACMAccessControlRESTController extends RACMController {
	private static final String QUERY_RESOURCE_ERROR_MESSAGE = "Error querying resources";
	private RACMAccessControl rac;
	private final RACM racm;

	@Autowired
	public RACMAccessControlRESTController(RACM racm, RACMAccessControl rac) {
		this.racm = racm;
		this.rac = rac;
	}

	/**
	 * Query all resources a user has rights to as known by RACM.<br/>
	 * This includes resources owned by the user etc. Returns all resources with
	 * info on resource type, context class and context as well as the actual
	 * actions the user is allowed to do.
	 *
	 * @return
	 */
	@GetMapping("/resources")
	public ResponseEntity<JsonNode> queryResources(@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(rac.queryUserResources(up));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
		}
	}

	@GetMapping("/resources/v2")
	public ResponseEntity<JsonNode> queryResourcesV2(@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(rac.queryUserResourcesV2(up));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
		}
	}

	@PostMapping("/resources")
	public ResponseEntity<JsonNode> postResourceGrants(@RequestBody String body, @AuthenticationPrincipal UserProfile up) {
		try {
			ObjectMapper mapper = RACMUtil.newObjectMapper();
			ResourceGrants node = mapper.readValue(body, ResourceGrants.class);

			Resource r = rac.saveResource(node, up);
			node = RACMModelFactory.newResourceGrants(r, up);
			LogUtils.buildLog()
				.showInUserHistory()
				.user(up)
				.sentence()
					.subject(up.getUsername())
					.verb("updated")
					.predicate("resource '%s'", r.getName())
				.extraField("resource", r.getId())
				.log();

			ObjectMapper om = RACMUtil.newObjectMapper();
			JsonNode json = om.valueToTree(node);
			return new ResponseEntity<>(json, HttpStatus.OK);
		} catch (InsufficientPermissionsException | RegistrationInvalidException e) {
			throw e;
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error updating resources", Optional.of(up), e);
		}
	}

	/**
	 * Query for all privileges of the resource specified by the two parameters
	 * resources a user has rights to as known by RACM.<br/>
	 * This includes resources owned by the user etc. Returns all resources with
	 * info on resource type, context class and context as well as the actual
	 * actions the user is allowed to do.
	 *
	 * NOTE ServiceAccounts are not included, filtered out in newResourceGrants!
	 *
	 * @return
	 */
	@GetMapping("/privileges")
	public ResponseEntity<JsonNode> queryResource(@RequestParam String resourceuuid, @AuthenticationPrincipal UserProfile up) {
		try {
			Resource resource = rac.findResource(resourceuuid, up.getTom());
			ResourceGrants grants = RACMModelFactory.newResourceGrants(resource, up);
			return jsonAPIHelper.success(grants);
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error querying privileges", Optional.of(up), e);
		}
	}

	/**
	 * Query all resources on a specified resourcecontext a user has rights to as
	 * known by RACM.<br/>
	 * * Returns all resources with info on resource type, resource, as well as the
	 * actual actions the user is allowed to do.
	 *
	 * @return
	 */
	@GetMapping("/rc/{resourceContextUUID}/resources")
	public ResponseEntity<JsonNode> queryResources(@PathVariable String resourceContextUUID,
			@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(rac.queryUserResources(up, resourceContextUUID));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
		}
	}

	@GetMapping("/rc/{resourceContextUUID}/root/{action}")
	public ResponseEntity<JsonNode> canUserDoActionOnRootContext(
			@PathVariable String resourceContextUUID,
			@PathVariable String action,
			@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(racm.canUserDoActionOnRootContext(
					up.getUsername(), resourceContextUUID, action));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error checking"
					, Optional.of(up), e);
		}
	}
	@GetMapping("/rc/resource/{resourceUUID}/action/{action}")
	public ResponseEntity<JsonNode> canUserDoActionOnResource(
			@PathVariable String resourceUUID,
			@PathVariable String action,
			@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(racm.canUserDoActionOnResource(up.getUsername(), resourceUUID, action));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity("Error checking"
					, Optional.of(up), e);
		}
	}
	
	/**
	 * Return resources on the resource context identified in the path that the specified user has access to,
	 * but that are owned by another service.<br/>
	 * Provide some info on the owneing resource and resource context.
	 * @param resourceContextUUID
	 * @param up
	 * @return
	 */
    @GetMapping("/myserviceownedresources")
    public ResponseEntity<JsonNode> queryServiceOwnedResources(@AuthenticationPrincipal UserProfile up) {
        try {
            return jsonAPIHelper.success(rac.queryServiceOwnedResources(up));
        } catch (Exception e) {
            return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
        }
    }

	
}
