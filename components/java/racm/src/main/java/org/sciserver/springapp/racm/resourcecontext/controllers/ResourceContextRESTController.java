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

@RestController
@RequestMapping("rc/{resourceContextUUID}")
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
    @GetMapping("pubdid")
    public Collection<String> getResourceUUIDsForPubDID(
            @AuthenticationPrincipal UserProfile up,
            @RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
            @PathVariable("resourceContextUUID") String resourceContextUUID,
            @RequestParam(name="pubdid", required=true) String pubdid) {
        return mapper.getResourceUUIDsForPubDID(serviceToken, resourceContextUUID, pubdid);
    }

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

	@DeleteMapping("resource/{resourceUUID}")
	public void deleteResource(
			@AuthenticationPrincipal UserProfile up,
			@RequestHeader(SERVICE_TOKEN_HEADER) String serviceToken,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID) {
		Resource resource = repo.get(resourceUUID);
		repo.delete(resource, serviceToken);
	}

	@GetMapping("resource/{resourceUUID}")
	public ResourceFromUserPerspectiveModel getResource(
			@AuthenticationPrincipal UserProfile up,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID) {
		return mapper.getResourceWithActions(up.getUsername(), resourceUUID);
	}
	
	@GetMapping("serviceresource/{resourceUUID}")
	public ServiceResourceFromUserPerspectiveModel getServiceResource(
			@AuthenticationPrincipal UserProfile up,
			@PathVariable("resourceContextUUID") String resourceContextUUID,
			@PathVariable("resourceUUID") String resourceUUID) {
		return mapper.getServiceResourceWithActions(up.getUsername(), resourceUUID);
	}
}
