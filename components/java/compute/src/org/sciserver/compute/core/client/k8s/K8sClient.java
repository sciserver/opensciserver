/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.client.k8s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.Config;


public class K8sClient {
    private ApiClient k8sClient;
    private AppsV1Api appsApi;
    private CoreV1Api coreApi;
    private NetworkingV1Api netApi;
    
    public K8sClient(String apiUrl, String apiToken) {
        k8sClient = Config.fromToken(apiUrl, apiToken, false);
        appsApi = new AppsV1Api(k8sClient);
        coreApi = new CoreV1Api(k8sClient);
        netApi = new NetworkingV1Api(k8sClient);
    }

    public AppsV1Api getAppsApi() {
        return appsApi;
    }

    public CoreV1Api getCoreApi() {
        return coreApi;
    }

    public NetworkingV1Api getNetApi() {
        return netApi;
    }
}
