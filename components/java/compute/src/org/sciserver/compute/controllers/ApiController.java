/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.controllers;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.UnauthorizedException;
import org.sciserver.compute.Utilities;
import org.sciserver.compute.core.registry.Container;
import org.sciserver.compute.core.registry.Domain;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.ExecutableImage;
import org.sciserver.compute.core.registry.Node;
import org.sciserver.compute.core.registry.NotFoundException;
import org.sciserver.compute.core.registry.RACMVolumeImage;
import org.sciserver.compute.core.registry.VolumeImage;
import org.sciserver.compute.model.ContainerInfo;
import org.sciserver.compute.model.DomainInfo;
import org.sciserver.compute.model.ErrorContent;
import org.sciserver.compute.model.Logs;
import org.sciserver.compute.model.NodeInfo;
import org.sciserver.racm.jobm.model.COMPMDockerJobModel;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.jobm.model.VolumeContainerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sciserver.logging.Message;


@RestController
@CrossOrigin(origins = "*")
public class ApiController {
    private static final Logger logger = LogManager.getLogger(ApiController.class);

    @Autowired
    AppConfig appConfig;

    @RequestMapping(value = "/api/proxy", method = RequestMethod.POST)
    public void updateProxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = Utilities.getToken(request, response);

        if (!appConfig.getUserGroupCache().get(token).contains("admin")) {
            throw new UnauthorizedException("User must have admin role");
        }

        Iterable<ExecutableContainer> containers = appConfig.getRegistry().getContainers();
        for (ExecutableContainer c : containers) {
            c.setProxy();
        }
    }

    @RequestMapping(value = "/api/inactiveContainers", method = RequestMethod.GET)
    public JsonNode getInactiveContainers(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = Utilities.getToken(request, response);

        if (!appConfig.getUserGroupCache().get(token).contains("admin")) {
            throw new UnauthorizedException("User must have admin role");
        }

        int hours = 0;
        int minutes = 0;
        try {
            hours = Integer.parseInt(request.getParameter("hours"));
        } catch (NumberFormatException ex) {
            // Do nothing
        }
        try {
            minutes = Integer.parseInt(request.getParameter("minutes"));
        } catch (NumberFormatException ex) {
            // Do nothing
        }

        boolean unknown = Boolean.parseBoolean(request.getParameter("unknown"));

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        cal.add(Calendar.MINUTE, -minutes);

        Iterable<ExecutableContainer> containers = appConfig.getRegistry()
                .getInactiveContainers(cal.getTime(), unknown);

        JsonNodeFactory factory = JsonNodeFactory.instance;
        ArrayNode result = factory.arrayNode();

        for (ExecutableContainer container : containers) {
            ContainerInfo item = ContainerInfo.fromContainer(container);
            result.add(
                factory.objectNode()
                    .put("last_activity", container.getAccessedAt() == null
                            ? null : Long.valueOf(container.getAccessedAt().getTime()))
                    .putPOJO("container_info", item));
        }

        return result;
    }

    @RequestMapping(value = "/api/domains/{domainId}", method = RequestMethod.GET)
    public DomainInfo getDomainInfo(
            @PathVariable long domainId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String serviceAuthId = request.getHeader("X-Service-Auth-ID");
        try {
            appConfig.getRACMClient().checkServiceToken(serviceAuthId);
        } catch (Exception ex) {
            throw new UnauthorizedException("Could not validate service ID", ex);
        }

        DomainInfo info = new DomainInfo();
        Domain domain = appConfig.getRegistry().getDomain(domainId);
        info.setId(domain.getId());
        info.setName(domain.getName());
        info.setNodes(new ArrayList<NodeInfo>());

        for (Node node : appConfig.getRegistry().getNodes(domain)) {
            NodeInfo nodeInfo = new NodeInfo();
            nodeInfo.setId(node.getId());
            nodeInfo.setName(node.getName());
            nodeInfo.setUsedSlots(node.getUsedSlots());
            nodeInfo.setTotalSlots(node.getTotalSlots());
            ((ArrayList<NodeInfo>) info.getNodes()).add(nodeInfo);
        }

        return info;
    }

    @RequestMapping(value = "/api/domains/{domainId}/containers/{id}/json", method = RequestMethod.GET)
    public JsonNode getContainerJson(
            @PathVariable long domainId,
            @PathVariable long id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String serviceAuthId = request.getHeader("X-Service-Auth-ID");
        try {
            appConfig.getRACMClient().checkServiceToken(serviceAuthId);
        } catch (Exception ex) {
            throw new UnauthorizedException("Could not validate service ID", ex);
        }

        Container container = appConfig.getRegistry().getContainer(id);
        return container.getInfo();
    }

    @RequestMapping(value = "/api/domains/{domainId}/containers/{id}/logs", method = RequestMethod.GET)
    public Logs getContainerLogs(
            @PathVariable long domainId,
            @PathVariable long id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String serviceAuthId = request.getHeader("X-Service-Auth-ID");
        try {
            appConfig.getRACMClient().checkServiceToken(serviceAuthId);
        } catch (Exception ex) {
            throw new UnauthorizedException("Could not validate service ID", ex);
        }

        Container container = appConfig.getRegistry().getContainer(id);
        Logs logs = new Logs();
        logs.setStdOut(container.getStdOut());
        logs.setStdErr(container.getStdErr());
        return logs;
    }

    @RequestMapping(value = "/api/domains/{domainId}/containers/{id}", method = RequestMethod.DELETE)
    public void deleteContainer(
            @PathVariable long domainId,
            @PathVariable long id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String serviceAuthId = request.getHeader("X-Service-Auth-ID");

        try {
            appConfig.getRACMClient().checkServiceToken(serviceAuthId);
        } catch (Exception ex) {
            throw new UnauthorizedException("Could not validate service ID", ex);
        }

        ExecutableContainer container = appConfig.getRegistry().getContainer(id);

        ((ExecutableImage) container.getImage()).createContainerManager().deleteContainer(container);

        sciserver.logging.Logger log = appConfig.getLogger();
        Message msg =  log.createComputeMessage(Utilities.structuredMessage(null, "deleted", container), false);
        Utilities.fillLoggingMessage(msg, null, request, "ComputeAPI.DeleteContainer");
        log.SendMessage(msg);
    }

    @RequestMapping(value = "/api/domains/{domainId}/containers", method = RequestMethod.POST)
    public long createContainer(
            @PathVariable long domainId,
            @RequestBody COMPMDockerJobModel jobmModel,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String description = Optional.ofNullable(request.getHeader("X-Description")).orElse("API container");
        boolean ephemeral = "EPHEMERAL".equals(description) || "NOTEBOOK".equals(description);
        //logger.info(new ObjectMapper().writeValueAsString(jobmModel));

        String serviceAuthId = request.getHeader("X-Service-Auth-ID");
        try {
            if (!ephemeral) {
                appConfig.getRACMClient().checkServiceToken(serviceAuthId);
            }
        } catch (Exception ex) {
            throw new UnauthorizedException("Could not validate service ID", ex);
        }

        String token = Utilities.getToken(request, response);
        Domain domain = appConfig.getRegistry().getDomain(domainId);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        UserDockerComputeDomainModel racmDomain = (ephemeral
            ? appConfig.getInteractiveUserDomainsCache().get(token)
            : appConfig.getJobsUserDomainsCache().get(token))
                .stream()
                .filter(d -> Long.parseLong(d.getPublisherDID()) == domainId).findAny().get();

        List<VolumeContainerModel> requestedJobmModelVolumes = jobmModel.getVolumeContainers();
        List<VolumeContainerModel> accessibleVolumes = racmDomain.getVolumes();

        List<VolumeContainerModel> requestedPublicVolumes = new ArrayList<VolumeContainerModel>();
        for (VolumeContainerModel requestedVol : requestedJobmModelVolumes) {
            if (accessibleVolumes.stream()
                .anyMatch(accessibleVol ->
                          accessibleVol.getPublisherDID().equals(requestedVol.getPublisherDID())
                          && (!requestedVol.isWritable() || accessibleVol.isWritable()))) {

                requestedPublicVolumes.add(requestedVol);

            } else {
                throw new Exception("Insufficient permissions on public volume " + requestedVol.getPublisherDID());
            }
        }

        List<VolumeImage> userVolumeImages = racmDomain.getUserVolumes().stream()
            .filter(existingVol -> jobmModel.getUserVolumes().stream()
                    .anyMatch(requestedVol -> requestedVol.getUserVolumeId().equals(existingVol.getId())))
            .map(model -> RACMVolumeImage.fromModel(model, domainId, appConfig.getRegistry()))
            .collect(Collectors.toList());

        String imageName = jobmModel.getDockerImageName();
        if (!racmDomain.getImages().stream().anyMatch(image -> image.getName().equals(imageName))) {
            throw new Exception("Insufficient permissions on image '" + imageName + "'");
        }

        ExecutableImage image = appConfig.getRegistry().getExecutableImage(domain, imageName);

        ExecutableContainer container = image.createContainerManager().createContainer(
            ephemeral ? "ephemeral" : ("job_" + jobmModel.getId()),
            description,
            user,
            requestedPublicVolumes,
            userVolumeImages,
            ephemeral ? new String[] { "/opt/startup.sh" } : jobmModel.getFullDockerCommand(),
            ephemeral ? false : true);

        jobmModel.setStatus(0); // needed to avoid NPE, but usure why NON_NULL filter doesn't exclude this field.
        appConfig.getRegistry().setExecutableContainerJson(
            container.getId(),
            new ObjectMapper().setSerializationInclusion(Include.NON_NULL).writer()
            .writeValueAsString(jobmModel)
        );

        sciserver.logging.Logger log = appConfig.getLogger();
        Message msg =  log.createComputeMessage(
            Utilities.structuredMessage(
                user, "created", container, image, requestedPublicVolumes, userVolumeImages), false);
        Utilities.fillLoggingMessage(msg, user, request, "ComputeAPI.CreateContainer");
        log.SendMessage(msg);

        return container.getId();
    }

    @RequestMapping(value = "/api/container/{containerId}/ping", method = RequestMethod.POST)
    public void pingContainer(@PathVariable long containerId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        List<ExecutableContainer> containers =
            Lists.newArrayList(appConfig.getRegistry().getContainers(user.getUserId()))
            .stream().filter(c -> c.getId() == containerId).collect(Collectors.toList());
        ExecutableContainer container = null;
        try {
            container = containers.get(0);
        } catch (Exception ex) {
            throw new NotFoundException("container not found");
        }
        appConfig.getRegistry().updateAccessTime(container);
    }

    @RequestMapping(value = "/api/containers", method = RequestMethod.GET)
    public List<ContainerInfo> getContainers(
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        List<ContainerInfo> containers = new ArrayList<ContainerInfo>();
        for (ExecutableContainer c : appConfig.getRegistry().getContainers(user.getUserId())) {
            containers.add(ContainerInfo.fromContainer(c));
        }
        return containers;
    }

    @RequestMapping(value = "/api/domains/{domainId}/containers", method = RequestMethod.GET)
    public Iterable<Long> getContainerIds(
            @PathVariable long domainId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);
        String name = request.getParameter("name");
        String serviceAuthId = request.getHeader("X-Service-Auth-ID");
        try {
            appConfig.getRACMClient().checkServiceToken(serviceAuthId);
        } catch (Exception ex) {
            throw new UnauthorizedException("Could not validate service ID", ex);
        }

        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        List<ExecutableContainer> containers =
                Lists.newArrayList(appConfig.getRegistry().getContainers(user.getUserId()));
        if (name != null && !name.isEmpty()) {
            containers = containers.stream()
                    .filter(c -> c.getName().equals(name)).collect(Collectors.toList());
        }

        String description = request.getParameter("description");
        if (description != null && !description.isEmpty()) {
            containers = containers.stream()
                    .filter(c -> c.getDescription().equals(description)).collect(Collectors.toList());
        }

        String isRunning = request.getParameter("isRunning");
        if (isRunning != null && !isRunning.isEmpty()) {
            containers = containers.stream().filter(c -> {
                try {
                    return c.isRunning() == Boolean.parseBoolean(isRunning);
                } catch (Exception e) {
                    return false;
                }
            }).collect(Collectors.toList());
        }

        return containers.stream().filter(c -> {
            try {
                return c.getNode().getDomainId() == domainId;
            } catch (Exception e) {
                // TBD: must somehow throw an exception here...
                return false;
            }
        }).map(c -> Long.valueOf(c.getId())).collect(Collectors.toList());
    }

    @RequestMapping(value = "/api/inventory/images", method = RequestMethod.GET)
    public List<String> getImageRefs(@RequestParam(name = "domain", required = false) Integer domain) throws Exception {
        return appConfig.getRegistry().adminGetImages().stream().filter(
            img -> domain == null ? true : img.getDomainId() == domain
        ).map(
            img -> img.getDockerRef()
        ).collect(Collectors.toList());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ResponseBody
    public Object handleNotFoundException(Exception ex) {
        logger.error(ex.getMessage(), ex);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory.objectNode().putPOJO("error", new ErrorContent(ex));
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Object handleUnauthorizedException(Exception ex) {
        logger.error(ex.getMessage(), ex);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory.objectNode().putPOJO("error", new ErrorContent(ex));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Object handleGenericException(Exception ex) {
        logger.error(ex.getMessage(), ex);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory.objectNode().putPOJO("error", new ErrorContent(ex));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Object handleUnauthenticatedException(Exception ex) {
        logger.error(ex.getMessage(), ex);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory.objectNode().putPOJO("error", new ErrorContent(ex));
    }
}
