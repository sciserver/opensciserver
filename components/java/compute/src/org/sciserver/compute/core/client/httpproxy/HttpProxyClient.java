/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.client.httpproxy;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;

public interface HttpProxyClient {

	void setProxy(String route, String target) throws Exception;

	void setProxy(String route, String target, String userId) throws Exception;

	void setProxy(String route, String target, String userId, long containerId) throws Exception;

	void deleteProxy(String route) throws Exception;

	JsonNode getRoutes(Date inactiveSince, int timeout) throws Exception;

}
