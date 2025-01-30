/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.UnauthorizedException;
import org.sciserver.compute.Utilities;
import org.sciserver.compute.core.registry.DaskCluster;
import org.sciserver.compute.core.registry.NotFoundException;
import org.sciserver.compute.dask.DaskK8sHelper;
import org.sciserver.compute.model.DaskClusterInfo;
import org.sciserver.compute.model.ErrorContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@CrossOrigin(origins = "*")
public class DaskApiController {
    private static final Logger logger = LogManager.getLogger(DaskApiController.class);

    @Autowired
    AppConfig appConfig;

    @RequestMapping(value = "/api/dask/clusters/{externalRef}", method = RequestMethod.GET)
    public DaskClusterInfo getDaskClusterInfo(@PathVariable String externalRef,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean connectionInfo = Boolean.parseBoolean(request.getParameter("connectionInfo"));
        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        DaskCluster cluster = appConfig.getRegistry().getDaskCluster(externalRef);
        if (!user.getUserId().equals(cluster.getUserId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        DaskK8sHelper helper = new DaskK8sHelper(cluster.getK8sCluster());
        return helper.getDaskClusterInfo(externalRef, connectionInfo);
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
}
