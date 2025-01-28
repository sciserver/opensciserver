/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.springframework.http.HttpStatus;

public class PingContainerAction {
	private static final int CONNECTION_TIMEOUT_MS = 3000;
	public static void execute(ExecutableContainer container) throws Exception {
		String containerUrl = container.getNode().getProxyBaseUrl() + container.getExternalRef().toLowerCase();
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		try {
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
					.setConnectTimeout(CONNECTION_TIMEOUT_MS)
					.setSocketTimeout(CONNECTION_TIMEOUT_MS)
					.build();
			
			HttpHead httpHead = new HttpHead(containerUrl + "/");
			httpHead.setConfig(requestConfig);

			CloseableHttpResponse res = httpClient.execute(httpHead);

			try {
				int statusCode = res.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.NOT_FOUND.value()) {
					container.setProxy();
				}
				if (statusCode != HttpStatus.METHOD_NOT_ALLOWED.value() && statusCode != HttpStatus.UNAUTHORIZED.value()) {
					Utilities.ensureSuccessStatusCode(res);
				}
			}
			finally {
				res.close();
			}

		} finally {
			httpClient.close();
		}
	}
}
