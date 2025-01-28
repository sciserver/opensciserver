/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.controllers;


import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.core.registry.Domain;
import org.sciserver.compute.core.registry.DomainType;
import org.sciserver.compute.core.registry.PublicVolume;
import org.sciserver.compute.model.DomainContents;
import org.sciserver.compute.model.DomainInfo;
import org.sciserver.compute.model.ErrorContent;
import org.sciserver.compute.model.SelectionInfo;
import org.sciserver.compute.Utilities;
import org.sciserver.racm.jobm.model.ComputeDomainUserVolumeModel;
import org.sciserver.racm.jobm.model.DockerImageModel;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.jobm.model.VolumeContainerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class UiHelperController {
    private static final Logger logger = LogManager.getLogger(UiHelperController.class);

    @Autowired
    AppConfig appConfig;

    @RequestMapping(value = "/ui/daskDomains", method = RequestMethod.GET)
    public Iterable<DomainInfo> getDaskDomains(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        ArrayList<DomainInfo> result = new ArrayList<DomainInfo>();
        for (UserDockerComputeDomainModel domain : Utilities.getDaskDomains(token)) {
            if (domain.getImages().isEmpty()) {
                continue;
            }
            DomainInfo item = new DomainInfo();
            item.setId(Long.parseLong(domain.getPublisherDID()));
            item.setName(domain.getName());
            item.setDescription(domain.getDescription());
            result.add(item);
        }

        return result;
    }
    @RequestMapping(value = "/ui/domains/{id}", method = RequestMethod.GET)
    public DomainContents getDomainContents(
            @PathVariable long id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        List<UserDockerComputeDomainModel> domains = appConfig.getInteractiveUserDomainsCache().get(token);
        UserDockerComputeDomainModel domainModel = domains.stream()
                .filter(d -> Long.parseLong(d.getPublisherDID()) == id).findFirst().get();
        Domain domain = appConfig.getRegistry().getDomain(Long.parseLong(domainModel.getPublisherDID()));

        List<DockerImageModel> images = domainModel.getImages();
        if (images == null) {
            throw new Exception("Images == null");
        }
        images.sort(Comparator.comparing(DockerImageModel::getName, (x, y) -> x.compareTo(y)));

        long[] defaultImages = appConfig.getAppSettings().getDefaultImages();

        ArrayList<SelectionInfo> imageList = new ArrayList<>();
        for (DockerImageModel image : images) {
            SelectionInfo item = new SelectionInfo();
            item.setName(image.getName());
            item.setDescription(image.getDescription());
            item.setId(Long.parseLong(image.getPublisherDID()));
            item.setSelected(Arrays.binarySearch(defaultImages, item.getId()) >= 0);
            imageList.add(item);
        }

        List<VolumeContainerModel> publicVolumes = domainModel.getVolumes();
        if (publicVolumes == null) {
            throw new Exception("Public volumes == null");
        }
        publicVolumes.sort(Comparator.comparing(VolumeContainerModel::getName, (x, y) -> x.compareTo(y)));

        ArrayList<SelectionInfo> publicVolumeList = new ArrayList<>();
        for (VolumeContainerModel volume : publicVolumes) {
            long volumeId = Long.parseLong(volume.getPublisherDID());
            SelectionInfo item = new SelectionInfo();
            item.setName(volume.getName() + (volume.isWritable() ? " [W]" : ""));
            item.setDescription(volume.getDescription());
            item.setId(volumeId);

            if (!DomainType.DASK.equals(domain.getType())) {
                try {
                    PublicVolume pv = appConfig.getRegistry().getPublicVolume(volumeId);
                    item.setSelected(pv.isSelectedByDefault());
                } catch (Exception e) {
                    // Do nothing if public volume not found
                }
            }

            publicVolumeList.add(item);
        }

        List<ComputeDomainUserVolumeModel> userVolumes = domainModel.getUserVolumes();
        if (userVolumes == null) {
            throw new Exception("User volumes == null");
        }
        userVolumes.sort(Comparator.comparing(ComputeDomainUserVolumeModel::getName, (x, y) -> x.compareTo(y)));

        ArrayList<SelectionInfo> userVolumeList = new ArrayList<>();
        for (ComputeDomainUserVolumeModel image : userVolumes) {
            if (image.getAllowedActions().contains("read")) {
                SelectionInfo item = new SelectionInfo();
                item.setName(String.format("%s, %s Volume created by %s",
                        image.getName(), image.getRootVolumeName(), image.getOwner()));
                item.setDescription(image.getDescription());
                item.setId(image.getId());
                item.setSelected(user.getUserId().equals(image.getOwnerId()));
                userVolumeList.add(item);
            }
        }

        DomainContents result = new DomainContents();
        result.setImages(imageList);
        result.setPublicVolumes(publicVolumeList);
        result.setUserVolumes(userVolumeList);

        return result;
    }

    @RequestMapping(value = "/ui/domains/{id}/images", method = RequestMethod.GET)
    public Iterable<SelectionInfo> getImages(
            @PathVariable long id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);

        List<UserDockerComputeDomainModel> domains = appConfig.getInteractiveUserDomainsCache().get(token);
        UserDockerComputeDomainModel domain = domains.stream()
                .filter(d -> Long.parseLong(d.getPublisherDID()) == id).findFirst().get();
        List<DockerImageModel> images = domain.getImages();

        ArrayList<SelectionInfo> imageList = new ArrayList<>();
        for (DockerImageModel image : images) {
            SelectionInfo item = new SelectionInfo();
            item.setName(image.getName());
            item.setDescription(image.getDescription());
            item.setId(Long.parseLong(image.getPublisherDID()));
            imageList.add(item);
        }

        imageList.sort(Comparator.comparing(SelectionInfo::getName, (x, y) -> x.compareTo(y)));
        return imageList;
    }

    @RequestMapping(value = "/ui/domains/{id}/publicVolumes", method = RequestMethod.GET)
    public Iterable<SelectionInfo> getPublicVolumes(
            @PathVariable long id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);

        List<UserDockerComputeDomainModel> domains = appConfig.getInteractiveUserDomainsCache().get(token);
        UserDockerComputeDomainModel domainModel = domains.stream()
                .filter(d -> Long.parseLong(d.getPublisherDID()) == id).findFirst().get();
        Domain domain = appConfig.getRegistry().getDomain(Long.parseLong(domainModel.getPublisherDID()));

        List<VolumeContainerModel> publicVolumes = domainModel.getVolumes();

        ArrayList<SelectionInfo> publicVolumeList = new ArrayList<>();
        for (VolumeContainerModel volume : publicVolumes) {
            long volumeId = Long.parseLong(volume.getPublisherDID());
            SelectionInfo item = new SelectionInfo();
            item.setName(volume.getName() + (volume.isWritable() ? " [W]" : ""));
            item.setDescription(volume.getDescription());
            item.setId(volumeId);

            if (!DomainType.DASK.equals(domain.getType())) {
                try {
                    PublicVolume pv = appConfig.getRegistry().getPublicVolume(volumeId);
                    item.setSelected(pv.isSelectedByDefault());
                } catch (Exception e) {
                    // Do nothing if public volume not found
                }
            }

            publicVolumeList.add(item);
        }

        publicVolumeList.sort(Comparator.comparing(SelectionInfo::getName, (x, y) -> x.compareTo(y)));
        return publicVolumeList;
    }

    @RequestMapping(value = "/ui/domains/{id}/userVolumes", method = RequestMethod.GET)
    public Iterable<SelectionInfo> getUserVolumes(
            @PathVariable long id,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);

        List<UserDockerComputeDomainModel> domains = appConfig.getInteractiveUserDomainsCache().get(token);
        UserDockerComputeDomainModel domain = domains.stream()
                .filter(d -> Long.parseLong(d.getPublisherDID()) == id).findFirst().get();

        List<ComputeDomainUserVolumeModel> userVolumes = domain.getUserVolumes();

        ArrayList<SelectionInfo> userVolumeList = new ArrayList<>();
        for (ComputeDomainUserVolumeModel image : userVolumes) {
            if (image.getAllowedActions().contains("read")) {
                SelectionInfo item = new SelectionInfo();
                item.setName(image.getName());
                item.setDescription(image.getDescription());
                item.setId(image.getId());
                if (!image.getIsShareable()) {
                    item.setSelected(true);
                }
                userVolumeList.add(item);
            }
        }

        userVolumeList.sort(Comparator.comparing(SelectionInfo::getName, (x, y) -> x.compareTo(y)));
        return userVolumeList;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Object handleGenericException(Exception ex) {
        logger.error("Generic error: ", ex);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory.objectNode().putPOJO("error", new ErrorContent(ex));
    }
}
