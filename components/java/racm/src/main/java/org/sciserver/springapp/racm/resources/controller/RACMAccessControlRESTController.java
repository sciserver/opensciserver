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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This REST controller wraps access control requests in REST API calls.<br/>
 *
 * @author gerard
 *
 */
@RestController
@CrossOrigin
@RequestMapping("rest")
@Tag(name = "Access control",
     description = "Query the resources a user may act on and the actions permitted on them.")
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
	@Operation(
	    summary = "List every resource the caller may act on.",
	    description = "Returns all resources the calling user has any rights over, across every "
	                  + "resource context, with the resource type, context class and context of "
	                  + "each, and the actions permitted on it. Returned as a NativeQueryResult "
	                  + "of columns and rows.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/resources")
	public ResponseEntity<JsonNode> queryResources(@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(rac.queryUserResources(up));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "List every resource the caller may act on, as model objects.",
	    description = "Same selection as /resources, returned as ResourceModel objects rather "
	                  + "than a column-and-row result. Prefer this when consuming the result as "
	                  + "typed JSON.")
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/resources/v2")
	public ResponseEntity<JsonNode> queryResourcesV2(@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(rac.queryUserResourcesV2(up));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "Grant or revoke actions on resources.",
	    description = "Applies a set of grants, each naming a resource, the entity receiving the "
	                  + "rights and the actions involved. The caller must be allowed to grant on "
	                  + "every resource named.",
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "ResourceGrants as JSON, listing the grants to apply."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller may not grant on one of the resources named."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
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
	@Operation(
	    summary = "List the privileges held on one resource.",
	    description = "Returns who may do what on the identified resource: the users and groups "
	                  + "holding rights, and the actions each of them may perform.",
	    parameters = {
	        @Parameter(name = "resourceuuid", in = ParameterIn.QUERY,
	                   description = "Identifier of the resource whose privileges are returned.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The caller may not view the privileges on this resource."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
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
	@Operation(
	    summary = "List the resources the caller may act on within one resource context.",
	    description = "Narrows the resource listing to a single resource context, returning each "
	                  + "resource with its type and the actions the caller is permitted to "
	                  + "perform on it.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context to look within.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("/rc/{resourceContextUUID}/resources")
	public ResponseEntity<JsonNode> queryResources(@PathVariable String resourceContextUUID,
			@AuthenticationPrincipal UserProfile up) {
		try {
			return jsonAPIHelper.success(rac.queryUserResources(up, resourceContextUUID));
		} catch (Exception e) {
			return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
		}
	}

	@Operation(
	    summary = "Check whether the caller may perform an action on a resource context as a "
	              + "whole.",
	    description = "Tests a single action against the root resource of the resource context, "
	                  + "which is where context-wide rights such as registering a new resource "
	                  + "are held. Returns a boolean.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context to test against."),
	        @Parameter(name = "action", in = ParameterIn.PATH,
	                   description = "Name of the action to test, as registered on the context "
	                                 + "class.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
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
	@Operation(
	    summary = "Check whether the caller may perform an action on one resource.",
	    description = "Tests a single action against a single resource and returns a boolean. "
	                  + "Intended for services deciding whether to offer an operation, rather "
	                  + "than for enforcing it.",
	    parameters = {
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource to test against."),
	        @Parameter(name = "action", in = ParameterIn.PATH,
	                   description = "Name of the action to test, as registered on the resource's "
	                                 + "type.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200"),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
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
    @Operation(
        summary = "List resources the caller may use that are owned by another service.",
        description = "Returns resources the calling user has access to but which belong to a "
                      + "different service, together with information about the owning resource, "
                      + "so the caller can follow the ownership chain.")
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping("/myserviceownedresources")
    public ResponseEntity<JsonNode> queryServiceOwnedResources(@AuthenticationPrincipal UserProfile up) {
        try {
            return jsonAPIHelper.success(rac.queryServiceOwnedResources(up));
        } catch (Exception e) {
            return jsonAPIHelper.logAndReturnJsonExceptionEntity(QUERY_RESOURCE_ERROR_MESSAGE, Optional.of(up), e);
        }
    }

	
}
