/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.UnauthorizedException;
import org.sciserver.compute.Utilities;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.model.ContainerInfo;
import org.sciserver.compute.model.ErrorContent;
import org.sciserver.compute.model.admin.DomainInfo;
import org.sciserver.compute.model.admin.ImageInfo;
import org.sciserver.compute.model.admin.K8sClusterInfo;
import org.sciserver.compute.model.admin.NodeInfo;
import org.sciserver.compute.model.admin.PublicVolumeInfo;
import org.sciserver.compute.model.admin.SlotsInfo;
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

@RestController
@CrossOrigin(origins = "*")
public class AdminController {
    private static final Logger logger = LogManager.getLogger(AdminController.class);

    @Autowired
    AppConfig appConfig;

    @RequestMapping(value = "/admin/domain", method = RequestMethod.GET)
    public List<DomainInfo> getDomains(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminGetDomains();
    }

    @RequestMapping(value = "/admin/domain", method = RequestMethod.POST)
    public long createDomain(@RequestBody DomainInfo domainInfo,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminCreateDomain(domainInfo);
    }

    @RequestMapping(value = "/admin/node", method = RequestMethod.GET)
    public List<NodeInfo> getNodes(HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminGetNodes();
    }

    @RequestMapping(value = "/admin/node", method = RequestMethod.POST)
    public long createNode(@RequestBody NodeInfo nodeInfo,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminCreateNode(nodeInfo);
    }

    @RequestMapping(value = "/admin/slots", method = RequestMethod.GET)
    public List<SlotsInfo> getSlots(HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminGetSlots();
    }

    @RequestMapping(value = "/admin/slots", method = RequestMethod.POST)
    public void createSlots(@RequestBody SlotsInfo slotsInfo,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        appConfig.getRegistry().adminCreateSlots(slotsInfo);
    }

    @RequestMapping(value = "/admin/image", method = RequestMethod.GET)
    public List<ImageInfo> getImages(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminGetImages();
    }

    @RequestMapping(value = "/admin/image", method = RequestMethod.POST)
    public long createImage(@RequestBody ImageInfo imageInfo,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminCreateImage(imageInfo);
    }

    @RequestMapping(value = "/admin/volume", method = RequestMethod.GET)
    public List<PublicVolumeInfo> getVolumes(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminGetPublicVolumes();
    }

    @RequestMapping(value = "/admin/volume", method = RequestMethod.POST)
    public long createPublicVolume(@RequestBody PublicVolumeInfo publicVolumeInfo,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminCreatePublicVolume(publicVolumeInfo);
    }

    @RequestMapping(value = "/admin/k8s_cluster", method = RequestMethod.GET)
    public List<K8sClusterInfo> createK8sCluster(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminGetK8sCluster();
    }

    @RequestMapping(value = "/admin/k8s_cluster", method = RequestMethod.POST)
    public long createK8sCluster(@RequestBody K8sClusterInfo k8sCluster,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        return appConfig.getRegistry().adminCreateK8sCluster(k8sCluster);
    }

    @RequestMapping(value = "/admin/containers/userid/{userid}", method = RequestMethod.GET)
    public List<ContainerInfo> listContainersByUserId(@PathVariable String userid, HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        List<ContainerInfo> containers = new ArrayList<ContainerInfo>();
        for (ExecutableContainer c: appConfig.getRegistry().getContainers(userid)) {
            containers.add(ContainerInfo.fromContainer(c));
        }
        return containers;
    }

    @RequestMapping(value = "/admin/containers/userid/{userid}", method = RequestMethod.DELETE)
    public void deleteContainersByUserId(@PathVariable String userid,
                                         @RequestParam(required=false, defaultValue="false") Boolean stop,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        for (ExecutableContainer c: appConfig.getRegistry().getContainers(userid)) {
            if (stop) {
                c.stop();
            } else {
                c.delete();
            }
        }
    }

    @RequestMapping(value = "/admin/containers/{containerId}", method = RequestMethod.DELETE)
    public void deleteContainersByContainerId(@PathVariable String containerId,
                                         @RequestParam(required=false, defaultValue="false") Boolean stop,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
        checkAdminToken(request, response);
        ExecutableContainer c = appConfig.getRegistry().getContainer(Long.parseLong(containerId));
        if (stop) {
            c.stop();
        } else {
            c.delete();
        }
    }

    private void checkAdminToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = Utilities.getToken(request, response);
        if (!appConfig.getUserGroupCache().get(token).contains("admin")) {
            throw new UnauthorizedException("User must have admin role");
        }
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
}
