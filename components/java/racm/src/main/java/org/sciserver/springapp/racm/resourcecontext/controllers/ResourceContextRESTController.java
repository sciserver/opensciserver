package org.sciserver.springapp.racm.resourcecontext.controllers;

import static org.sciserver.springapp.racm.auth.SciServerHeaderAuthenticationFilter.SERVICE_TOKEN_HEADER;

import java.util.Collection;
import java.util.Set;

import org.ivoa.dm.VOURPException;
import org.sciserver.racm.resourcecontext.model.AssociatedResourceModel;
import org.sciserver.racm.resourcecontext.model.AssociatedSciserverEntityModel;
import org.sciserver.racm.resourcecontext.model.NewResourceModel;
import org.sciserver.racm.resourcecontext.model.RegisteredResourceModel;
import org.sciserver.racm.resourcecontext.model.ResourceFromUserPerspectiveModel;
import org.sciserver.racm.resourcecontext.model.ServiceResourceFromUserPerspectiveModel;
import org.sciserver.springapp.racm.resourcecontext.applicaton.ResourceContextMapper;
import org.sciserver.springapp.racm.resourcecontext.domain.Resource;
import org.sciserver.springapp.racm.resourcecontext.vourp.ResourceContextAuthentication;
import org.sciserver.springapp.racm.resourcecontext.vourp.ResourceRepository;
import org.sciserver.springapp.racm.resources.application.ResourceManager;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("rc/{resourceContextUUID}")
@Tag(name = "Resource contexts",
     description = "Register and manage the resources belonging to one resource context.")
public class ResourceContextRESTController {
	private final ResourceContextMapper mapper;
	private final ResourceRepository repo;
	private final ResourceContextAuthentication resourceContextAuthentication;
	private final ResourceManager resourceManager;
	
	ResourceContextRESTController(
			ResourceContextMapper resourceContextMapper, ResourceRepository repo,
			ResourceContextAuthentication resourceContextAuthentication,
			ResourceManager resourceManager) {
		this.mapper = resourceContextMapper;
		this.repo = repo;
		this.resourceContextAuthentication = resourceContextAuthentication;
		this.resourceManager = resourceManager;
	}

	@Operation(
	    summary = "Register a resource in a resource context.",
	    description = "Creates a resource of the given type within the context, so that rights "
	                  + "can then be granted on it. Called by the service that owns the context, "
	                  + "identified by its service token.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context the resource belongs "
	                                 + "to."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
	                   description = "Service token of the resource context. It must correspond "
	                   + "to the resource context in the path; if it does not, the response is "
	                   + "not reliably typed and may be empty.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "NewResourceModel as JSON, naming the resource and its "
	                                 + "resource type."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The registered resource, including the UUID RACM assigned it. The service stores "
	                               + "that UUID and uses it in later calls."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not grant access to this resource "
	                              + "context."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("resource")
	@ResponseStatus(HttpStatus.CREATED)
	public RegisteredResourceModel newResource(@AuthenticationPrincipal UserProfile up,
			@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@RequestBody NewResourceModel resourceModel) {
		resourceContextAuthentication.verifyCorrectToken(resourceContextUUID, serviceToken);
		Resource resource =
				mapper.toDomainModel(resourceContextUUID, resourceModel);
		Resource newResource = repo.add(resource, serviceToken);
		return mapper.toDTO(newResource);
	}

	@Operation(
	    summary = "Change a resource's name or description.",
	    description = "Updates the descriptive fields of a resource. Only the parameters supplied "
	                  + "are changed; rights and associations are left untouched.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context the resource belongs "
	                                 + "to."),
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource to update."),
	        @Parameter(name = "name", in = ParameterIn.QUERY,
	                   description = "New name for the resource. Omit to leave it unchanged."),
	        @Parameter(name = "description", in = ParameterIn.QUERY,
	                   description = "New description for the resource. Omit to leave it "
	                                 + "unchanged."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
	                   description = "Service token of the resource context. It must correspond "
	                   + "to the resource context in the path.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "true if the resource was changed. A change RACM rejected returns false with this "
	                               + "same 200, not an error status."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not grant access to this resource "
	                              + "context."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("resource/{resourceUUID}/metadata")
	@ResponseStatus(HttpStatus.OK)
	public boolean updateResourceMetadata(@AuthenticationPrincipal UserProfile up,
			@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID,
			@RequestParam(name = "name", required=false) String name,
			@RequestParam(name = "description", required=false) String description) {
		resourceContextAuthentication.verifyCorrectToken(resourceContextUUID, serviceToken);

		try {
			resourceManager.editResourceMetadata(resourceUUID, name, description, up.getTom());
			return true; 
		} catch(VOURPException e) {
			return false;
		}
	}

	/**
	 * 
	 * PRECONDITION: servicetoken and resourceContextUUID must be compatible.
	 *     if violated caller should not expect properly typed response, may be null.
	 * @param up
	 * @param serviceToken
	 * @param resourceContextUUID
	 * @return
	 */
	@Operation(
	    summary = "List the resources in a resource context.",
	    description = "Returns every resource registered in the context. The service token and "
	                  + "the resource context in the path must correspond; if they do not, the "
	                  + "response is not reliably typed and may be empty.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context to list."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
	                   description = "Service token of the resource context. It must correspond "
	                   + "to the resource context in the path.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The resources in the context, each with its associations and the actions the "
	                               + "named user may perform on it."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not grant access to this resource "
	                              + "context."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("resources")
	public Set<ResourceFromUserPerspectiveModel> getResources(
			@AuthenticationPrincipal UserProfile up,
			@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID) {
		return mapper.getResourcesWithActions(up.getUsername(), resourceContextUUID);
	}

    /**
     * 
     * PRECONDITION: servicetoken and resourceContextUUID must be compatible.
     *     if violated caller should not expect properly typed response, may be null.
     * @param up
     * @param serviceToken
     * @param resourceContextUUID
     * @return
     */
    @Operation(
        summary = "Find the resources carrying a given publisher identifier.",
        description = "Looks resources up by the identifier their publisher assigned them, rather "
                      + "than by the one RACM assigned. Every match is returned, since a "
                      + "publisher identifier is not required to be unique.",
        parameters = {
            @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
                       description = "Identifier of the resource context to search within."),
            @Parameter(name = "pubdid", in = ParameterIn.QUERY,
                       description = "Publisher-assigned identifier to match."),
            @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
                       description = "Service token of the resource context. It must correspond "
                       + "to the resource context in the path.")
        })
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The UUIDs of the resources in this context carrying that publisher identifier. "
                                   + "An empty list if none do."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The service token does not grant access to this resource "
                                  + "context."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @GetMapping("pubdid")
    public Collection<String> getResourceUUIDsForPubDID(
            @AuthenticationPrincipal UserProfile up,
            @RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
            @PathVariable("resourceContextUUID") String resourceContextUUID,
            @RequestParam(name="pubdid", required=true) String pubdid) {
        return mapper.getResourceUUIDsForPubDID(serviceToken, resourceContextUUID, pubdid);
    }

    @Operation(
        summary = "Record that a resource is associated with another resource.",
        description = "Links the resource to a second resource, so that rights and lifecycle can "
                      + "follow the association. Used when one service's resource is derived from "
                      + "or contained in another's.",
        parameters = {
            @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
                       description = "Identifier of the resource context the resource belongs "
                                     + "to."),
            @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
                       description = "Identifier of the resource gaining the association."),
            @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
                       description = "Service token of the resource context. It must correspond "
                       + "to the resource context in the path.")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                       description = "AssociatedResourceModel as JSON, naming the resource to "
                       + "associate and the nature of the association."))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "The association was recorded. The response has no body."),
        @ApiResponse(responseCode = "400",
                     description = "The request is not valid; the response body carries the "
                                  + "reason."),
        @ApiResponse(responseCode = "401",
                     description = "No user token was supplied, or it is not valid."),
        @ApiResponse(responseCode = "403",
                     description = "The service token does not grant access to this resource "
                                  + "context."),
        @ApiResponse(responseCode = "500",
                     description = "Unexpected error. The response body carries a message.")
    })
    @PostMapping("resource/{resourceUUID}/associatedResource")
	public void associateWithResource(
			@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID,
			@RequestBody AssociatedResourceModel associatedResourceModel) {
		Resource resource = repo.get(resourceUUID);

		resource.addAssociationWithResource(
				mapper.toDomainModel(associatedResourceModel));
		repo.add(resource, serviceToken);
	}

	@Operation(
	    summary = "Record that a user or group is associated with a resource.",
	    description = "Links the resource to a SciServer entity, such as the user or group that "
	                  + "owns it. This records ownership; it does not by itself grant rights, "
	                  + "which are added through the privileges endpoint.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context the resource belongs "
	                                 + "to."),
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource gaining the association."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
	                   description = "Service token of the resource context. It must correspond "
	                   + "to the resource context in the path.")
	    },
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	                   description = "AssociatedSciserverEntityModel as JSON, naming the entity "
	                   + "and the nature of the association."))
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The association was recorded. The response has no body."),
	    @ApiResponse(responseCode = "400",
	                 description = "The request is not valid; the response body carries the "
	                               + "reason."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not grant access to this resource "
	                              + "context."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@PostMapping("resource/{resourceUUID}/associatedSciserverEntity")
	public void associateWithSciserverEntity(
			@AuthenticationPrincipal UserProfile up,
			@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID,
			@RequestBody AssociatedSciserverEntityModel associatedSciserverEntityModel) {
		Resource resource = repo.get(resourceUUID);

		resource.addAssociationWithSciserverEntity(
				mapper.toDomainModel(associatedSciserverEntityModel));
		repo.add(resource, serviceToken);
	}

	@Operation(
	    summary = "Delete a resource from a resource context.",
	    description = "Removes the resource together with the privileges granted on it. Called by "
	                  + "the service that owns the context when the thing the resource stands for "
	                  + "has gone.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context the resource belongs "
	                                 + "to."),
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource to delete."),
	        @Parameter(name = "X-Service-Auth-ID", in = ParameterIn.HEADER,
	                   description = "Service token of the resource context. It must correspond "
	                   + "to the resource context in the path.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The resource was deleted, together with the grants held on it. The response has "
	                               + "no body."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "403",
	                 description = "The service token does not grant access to this resource "
	                              + "context."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@DeleteMapping("resource/{resourceUUID}")
	public void deleteResource(
			@AuthenticationPrincipal UserProfile up,
			@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID) {
		Resource resource = repo.get(resourceUUID);
		repo.delete(resource, serviceToken);
	}

	@Operation(
	    summary = "Get one resource.",
	    description = "Returns the resource with its type, name, description and publisher "
	                  + "identifier.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context the resource belongs "
	                                 + "to."),
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource to return.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The resource, with its associations and the actions the caller may perform on "
	                               + "it."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("resource/{resourceUUID}")
	public ResourceFromUserPerspectiveModel getResource(
			@AuthenticationPrincipal UserProfile up,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID) {
		return mapper.getResourceWithActions(up.getUsername(), resourceUUID);
	}
	
	@Operation(
	    summary = "Get one resource as the owning service sees it.",
	    description = "Returns the resource together with the service-level detail that the "
	                  + "ordinary resource view leaves out.",
	    parameters = {
	        @Parameter(name = "resourceContextUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource context the resource belongs "
	                                 + "to."),
	        @Parameter(name = "resourceUUID", in = ParameterIn.PATH,
	                   description = "Identifier of the resource to return.")
	    })
	@ApiResponses({
	    @ApiResponse(responseCode = "200",
	                 description = "The resource, with its associations and the actions the named user may perform "
	                               + "on it. The body has the same shape as the user-facing form of this operation."),
	    @ApiResponse(responseCode = "401",
	                 description = "No user token was supplied, or it is not valid."),
	    @ApiResponse(responseCode = "500",
	                 description = "Unexpected error. The response body carries a message.")
	})
	@GetMapping("serviceresource/{resourceUUID}")
	public ServiceResourceFromUserPerspectiveModel getServiceResource(
			@AuthenticationPrincipal UserProfile up,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID) {
		return mapper.getServiceResourceWithActions(up.getUsername(), resourceUUID);
	}
}
