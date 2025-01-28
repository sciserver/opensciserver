/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.authentication.client.UnauthenticatedException;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.PingContainerAction;
import org.sciserver.compute.Utilities;
import org.sciserver.compute.core.registry.DaskCluster;
import org.sciserver.compute.core.registry.DaskClusterStatus;
import org.sciserver.compute.core.registry.DomainType;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.ExecutableImage;
import org.sciserver.compute.core.registry.GenericVolume;
import org.sciserver.compute.core.registry.K8sCluster;
import org.sciserver.compute.core.registry.Node;
import org.sciserver.compute.core.registry.PublicVolume;
import org.sciserver.compute.core.registry.RACMVolumeImage;
import org.sciserver.compute.core.registry.Registry;
import org.sciserver.compute.core.registry.VolumeContainer;
import org.sciserver.compute.core.registry.VolumeImage;
import org.sciserver.compute.dask.DaskK8sHelper;
import org.sciserver.compute.model.ContainerInfo;
import org.sciserver.compute.model.DomainInfo;
import org.sciserver.compute.model.ErrorContent;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.jobm.model.VolumeContainerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriUtils;
import sciserver.logging.Message;


@Controller
public class UiController {
    private static final Logger logger = LogManager.getLogger(UiController.class);

    @Autowired
    AppConfig appConfig;

    @RequestMapping(value = { "/health" }, method = RequestMethod.GET)
    public String health(
        Model model,
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        model.addAttribute("build", appConfig.getVersion());
        model.addAttribute("maintenanceMode", appConfig.getAppSettings().isMaintenanceMode());
        model.addAttribute("cleanupEnabled", appConfig.getAppSettings().isCleanupEnabled());
        model.addAttribute("cleanupInterval", appConfig.getAppSettings().getCleanupIntervalHours() + " h");
        model.addAttribute("cleanupInactive", appConfig.getAppSettings().getCleanupInactiveHours() + " h");
        model.addAttribute("maxContainers", appConfig.getAppSettings().getMaxContainersPerUser());
        model.addAttribute("defaultDomain", appConfig.getAppSettings().getDefaultDomainId());
        model.addAttribute("lastCleanup", appConfig.getLastCleanup());
        return "health";
    }

    @RequestMapping(value = { "/create" }, method={RequestMethod.POST})
    public String create(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam(value = "userVolume", required = false) List<Long> userVolumeIds,
        @RequestParam(value = "publicVolume", required = false) List<Long> publicVolumeIds,
        @RequestParam(value = "containerName") String containerName,
        @RequestParam(value = "imageId") long imageId,
        @RequestParam(value = "daskCluster", required = false) String createDaskCluster,
        @RequestParam(value = "dc_clusterName", required = false) String daskClusterName,
        @RequestParam(value = "dc_imageId", required = false) Long daskImageId,
        @RequestParam(value = "dc_workers", required = false) Integer daskWorkers,
        @RequestParam(value = "dc_memory", required = false) String daskMemory,
        @RequestParam(value = "dc_threads", required = false) Integer daskThreads,
        @RequestParam(value = "dc_publicVolume", required = false) List<Long> daskPublicVolumeIds)
        throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        List<ExecutableContainer> containers =
            StreamSupport.stream(appConfig.getRegistry().getContainers(user.getUserId()).spliterator(), false)
            .filter(c -> !c.getDescription().equals("API container"))
            .filter(c -> !c.getDescription().equals("NOTEBOOK"))
            .collect(Collectors.toList());

        int max = appConfig.getAppSettings().getMaxContainersPerUser();
        if (!appConfig.getUserGroupCache().get(token).stream().anyMatch(
                appConfig.getAppSettings().getPrivilegedGroups()::contains
            ) && Iterables.size(containers) >= max) {
            throw new Exception("No more than " + max + " containers per user allowed");
        }

        ExecutableImage image = appConfig.getRegistry().getExecutableImage(imageId);
        List<UserDockerComputeDomainModel> domains = appConfig.getInteractiveUserDomainsCache().get(token);

        UserDockerComputeDomainModel domain = domains.stream()
            .filter(d -> Long.parseLong(d.getPublisherDID()) == image.getDomainId()).findAny().get();

        ArrayList<VolumeContainerModel> publicVolumes = new ArrayList<VolumeContainerModel>();
        ArrayList<VolumeImage> userVolumes = new ArrayList<VolumeImage>();

        List<VolumeContainerModel> jobmModelVolumes = domain.getVolumes();

        if (userVolumeIds != null) {
            for (Long id : userVolumeIds) {
                userVolumes.add(RACMVolumeImage.fromDomain(domain, id, appConfig.getRegistry()));
            }
        }
        if (publicVolumeIds != null) {
            for (Long id : publicVolumeIds) {
                try {
                    publicVolumes.add(
                        jobmModelVolumes.stream()
                            .filter(volume -> id == Long.parseLong(volume.getPublisherDID())).findFirst().get());
                } catch(NoSuchElementException ex) {
                    throw new Exception("Insufficient permissions on public volume, or publisherDID does not exist: " + id);
                }
            }
        }

        Registry reg = appConfig.getRegistry();
        DaskCluster daskCluster = null;
        if (createDaskCluster != null) {
            K8sCluster k8s;
            ExecutableImage daskImage = reg.getExecutableImage(daskImageId);
            try {
                k8s = reg.getK8sClusters(daskImage.getDomain()).iterator().next();
            } catch (Exception ex) {
                throw new Exception("No available K8s clusters");
            }
            List<UserDockerComputeDomainModel> daskDomains = appConfig.getInteractiveUserDomainsCache().get(token);
            ArrayList<GenericVolume> daskPublicVolumes = new ArrayList<GenericVolume>();
            List<VolumeContainerModel> daskJobmModelVolumes = daskDomains.stream()
                .filter(d -> Long.parseLong(d.getPublisherDID()) == daskImage.getDomainId())
                .findFirst().get().getVolumes();
            if (daskPublicVolumeIds != null) {
                for (Long id : daskPublicVolumeIds) {
                    daskPublicVolumes.add(appConfig.getRegistry().getGenericVolume(id));
                }
            }
            for (GenericVolume v : daskPublicVolumes) {
                VolumeContainerModel vcm = daskJobmModelVolumes.stream()
                    .filter(volume -> v.getId() == Long.parseLong(volume.getPublisherDID())).findFirst().get();
                v.setWritable(vcm.isWritable());
            }
            daskCluster = new DaskCluster(reg);
            daskCluster.setName(daskClusterName);
            daskCluster.setDescription("");
            daskCluster.setK8sClusterId(k8s.getId());
            daskCluster.setImageId(daskImage.getId());
            daskCluster.setUserId(user.getUserId());
            reg.registerDaskCluster(daskCluster);
            DaskK8sHelper helper = new DaskK8sHelper(k8s);
            helper.createDaskCluster(daskCluster, daskWorkers, daskMemory, daskThreads, daskPublicVolumes);
            daskCluster.setStatus("CREATED");
            reg.updateDaskCluster(daskCluster);
        }
        ExecutableContainer container = image.createContainerManager()
            .createContainer(containerName, "", user, publicVolumes, userVolumes);
        if (daskCluster != null) {
            reg.linkDaskCluster(container, daskCluster);
        }
        sciserver.logging.Logger log = appConfig.getLogger();
        Message msg = log.createComputeMessage(Utilities.structuredMessage(user, "created", container,
                                                                           image, publicVolumes, userVolumes), true);
        Utilities.fillLoggingMessage(msg, user, request, "ComputeUI.CreateContainer");
        log.SendMessage(msg);
        return "redirect:/";
    }

    @RequestMapping(value = { "/delete" }, method = RequestMethod.GET)
    public String delete(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam("id") long id) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        ExecutableContainer container = appConfig.getRegistry().getContainer(id);
        if (container.getUserId().equals(user.getUserId())) {
            DaskCluster daskCluster = container.getDaskCluster();
            if (daskCluster != null && !DaskClusterStatus.DELETED.equals(daskCluster.getStatus())) {
                DaskK8sHelper helper = new DaskK8sHelper(daskCluster.getK8sCluster());
                helper.deleteDaskCluster(daskCluster.getExternalRef());
                daskCluster.setStatus(DaskClusterStatus.DELETED);
                appConfig.getRegistry().updateDaskCluster(daskCluster);
            }
            ((ExecutableImage) container.getImage()).createContainerManager().deleteContainer(container);
            sciserver.logging.Logger log = appConfig.getLogger();
            Message msg = log.createComputeMessage(Utilities.structuredMessage(user, "deleted", container), true);
            Utilities.fillLoggingMessage(msg, user, request, "ComputeUI.DeleteContainer");
            log.SendMessage(msg);
        }
        return "redirect:/";
    }

    @RequestMapping(value = { "/start" }, method = RequestMethod.GET)
    public String start(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam("id") long id,
        @RequestParam(value = "target", required = false) String target) throws Exception {

        if (target == null) {
            target = "info?id=" + id;
        }
        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        ExecutableContainer container = appConfig.getRegistry().getContainer(id);
        if (container.getUserId().equals(user.getUserId())) {
            container.start();
        }
        return "redirect:/" + target;
    }

    @RequestMapping(value = { "/stop" }, method = RequestMethod.GET)
    public String stop(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam("id") long id,
        @RequestParam(value = "target", required = false) String target) throws Exception {

        if (target == null) {
            target = "info?id=" + id;
        }
        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        ExecutableContainer container = appConfig.getRegistry().getContainer(id);
        if (container.getUserId().equals(user.getUserId())) {
            container.stop();
        }
        return "redirect:/" + target;
    }

    @RequestMapping(value = { "/go" }, method = RequestMethod.GET)
    public String go(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam("id") long id,
        @RequestParam(name = "path", required = false) String path) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        ExecutableContainer container = appConfig.getRegistry().getContainer(id);
        if (container.getUserId().equals(user.getUserId())) {
            try {
                container.start();
            } catch (Exception e) {
                // Do nothing if already started,
                // but maybe we should check for
                // other errors here
            }
            Utilities.retryContainerAction(5, 5000, container, PingContainerAction::execute);
            Utilities.injectToken(container, token);

            appConfig.getRegistry().updateAccessTime(container);

            String containerUrl = container.getProxyUrl() + container.getExternalRef().toLowerCase();
            return "redirect:" + containerUrl + Optional.ofNullable(path).orElse("/") + "?computeToken=" + token;
        } else {
            throw new Exception("Container does not belong to user");
        }
    }

    @RequestMapping(value = { "/info" }, method = RequestMethod.GET)
    public String info(
        Model model,
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam("id") long id) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        ExecutableContainer container = appConfig.getRegistry().getContainer(id);
        if (container.getUserId().equals(user.getUserId())) {
            ContainerInfo item = ContainerInfo.fromContainer(container);

            model.addAttribute("container", item);
            model.addAttribute("authenticated", true);
            model.addAttribute("username", user.getUserName());

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(container.getInfo());
            model.addAttribute("json", json);
            return "info";
        } else {
            throw new Exception("Container does not belong to user");
        }
    }

    @RequestMapping(value = { "/", "/dashboard" }, method = RequestMethod.POST)
    public String indexPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Utilities.getToken(request, response);
        return "redirect:/";
    }

    @RequestMapping(value = { "/", "/dashboard" }, method = RequestMethod.GET)
    public String index(
        Model model,
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        model.addAttribute("username", user.getUserName());
        model.addAttribute("token", token);

        Registry reg = this.appConfig.getRegistry();
        List<UserDockerComputeDomainModel> domains = this.appConfig.getInteractiveUserDomainsCache()
            .get(token).stream().filter(d -> {
                    try {
                        return !DomainType.DASK.equals(reg.getDomain(Long.parseLong(d.getPublisherDID()))
                                                       .getType());
                    } catch (Exception e) {
                        return false;
                    }
                }).collect(Collectors.toList());
        ArrayList<DomainInfo> domainList = new ArrayList<DomainInfo>();

        for (UserDockerComputeDomainModel domain : domains) {
            if (domain.getImages().isEmpty()) {
                continue;
            }
            DomainInfo item = new DomainInfo();
            item.setId(Long.parseLong(domain.getPublisherDID()));
            item.setName(domain.getName());
            item.setDescription(domain.getDescription());
            domainList.add(item);
        }
        model.addAttribute("domains", domainList);
        Iterable<ExecutableContainer> containers = this.appConfig.getRegistry().getContainers(user.getUserId());
        ArrayList<ContainerInfo> containerList = new ArrayList<ContainerInfo>();
        for (ExecutableContainer container : containers) {
            if ("API container".equals(container.getDescription())
                || "NOTEBOOK".equals(container.getDescription())) {
                continue; // Skip async job containers
            }

            ContainerInfo item = new ContainerInfo();
            StringBuilder attachedVolumes = new StringBuilder();
            for (VolumeContainer volumeContainer : container.getUserVolumes()) {
                if (attachedVolumes.length() > 0) {
                    attachedVolumes.append("; ");
                }
                attachedVolumes.append(volumeContainer.getImage().getName());
            }
            for (PublicVolume publicVolume : container.getPublicVolumes()) {
                if (attachedVolumes.length() > 0) {
                    attachedVolumes.append("; ");
                }
                attachedVolumes.append(publicVolume.getName());
            }
            item.setId(container.getId());
            item.setDescription(container.getDescription());
            try {
                item.setStatus(container.isRunning() ? "running" : "stopped");
            } catch (Exception ex) {
                item.setStatus("error");
            }
            Node node = container.getNode();
            item.setName(container.getName());
            item.setCreatedAt(container.getCreatedAt());
            item.setDockerRef(container.getDockerRef());
            item.setNodeName(node.getName());
            item.setExternalRef(container.getExternalRef());
            item.setImageName(container.getImage().getName());
            item.setDomainName(node.getDomain().getName());
            item.setAttachedVolumes(attachedVolumes.toString());
            containerList.add(item);
        }
        model.addAttribute("containers", containerList);
        model.addAttribute("alert", appConfig.getAppSettings().getAlertMessage());
        model.addAttribute("authenticated", true);

        ArrayList<DomainInfo> daskDomains = new ArrayList<DomainInfo>();
        for (UserDockerComputeDomainModel daskDomain : Utilities.getDaskDomains(token)) {
            if (daskDomain.getImages().isEmpty()) {
                continue;
            }
            DomainInfo item = new DomainInfo();
            item.setId(Long.parseLong(daskDomain.getPublisherDID()));
            item.setName(daskDomain.getName());
            item.setDescription(daskDomain.getDescription());
            daskDomains.add(item);
        }

        model.addAttribute("isDaskAvailable", !daskDomains.isEmpty());
        model.addAttribute("dc_domains", daskDomains);
        model.addAttribute("dc_defaultWorkers", appConfig.getAppSettings().getDaskWorkers());
        model.addAttribute("dc_defaultMemory", appConfig.getAppSettings().getDaskMemory());
        model.addAttribute("dc_defaultThreads", appConfig.getAppSettings().getDaskThreads());

        return "dashboard";
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public String jobs(
        Model model,
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);
        model.addAttribute("username", user.getUserName());
        model.addAttribute("token", token);
        model.addAttribute("racmUrl", appConfig.getAppSettings().getRacmUrl());
        model.addAttribute("authenticated", true);

        boolean isDaskAvailable = Utilities.getDaskDomains(token).stream().anyMatch(domain -> !domain.getImages().isEmpty());

        model.addAttribute("isDaskAvailable", isDaskAvailable);
        model.addAttribute("daskDefaultWorkers", appConfig.getAppSettings().getDaskWorkers());
        model.addAttribute("daskDefaultMemory", appConfig.getAppSettings().getDaskMemory());
        model.addAttribute("daskDefaultThreads", appConfig.getAppSettings().getDaskThreads());

        return "jobs";
    }

    @RequestMapping(value = { "/login" }, method = RequestMethod.GET)
    public String login(HttpServletRequest request) throws Exception {
        String callbackUrl = appConfig.getAppSettings().getLoginCallback();
        if (request.getParameter("callbackUrl") != null) {
            callbackUrl = request.getParameter("callbackUrl");
        }
        return "redirect:" + appConfig.getAppSettings().getLoginPortalUrl() + "Account/Login?callbackUrl="
            + UriUtils.encode(callbackUrl, "UTF-8");
    }

    @RequestMapping(value = { "/logout" }, method = RequestMethod.GET)
    public String logout(
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        Utilities.deleteCookie(response);
        return "redirect:" + appConfig.getAppSettings().getLoginPortalUrl() + "Account/Logout?callbackUrl="
            + UriUtils.encode(appConfig.getAppSettings().getLogoutCallback(), "UTF-8");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String exceptionHandler(
        Model model,
        Exception ex,
        HttpServletResponse response) throws Exception {

        logger.error("Generic error: ", ex);
        model.addAttribute("error", new ErrorContent(ex));
        return "error";
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseStatus(HttpStatus.OK) // return OK since this is a UI redirect to login
    public String unauthenticatedLoginPageHandler(
        Model model,
        Exception ex,
        HttpServletResponse response) throws Exception {
        return "index";
    }

    @ModelAttribute
    public void addConfiguredUrls(Model model) {
        model.addAttribute("dashboardUrl", appConfig.getAppSettings().getDashboardUrl());
        model.addAttribute("casJobsUrl", appConfig.getAppSettings().getCasJobsUrl());
        model.addAttribute("sciDriveUrl", appConfig.getAppSettings().getSciDriveUrl());
        model.addAttribute("skyServerUrl", appConfig.getAppSettings().getSkyServerUrl());
        model.addAttribute("skyQueryUrl", appConfig.getAppSettings().getSkyQueryUrl());
        model.addAttribute("racmUrl", appConfig.getAppSettings().getRacmUrl());
        model.addAttribute("helpUrl", appConfig.getAppSettings().getHelpUrl());
    }
}
