/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.dask;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1ContainerPortBuilder;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;
import io.kubernetes.client.openapi.models.V1EnvVarBuilder;
import io.kubernetes.client.openapi.models.V1EnvVarSourceBuilder;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressBuilder;
import io.kubernetes.client.openapi.models.V1IngressRule;
import io.kubernetes.client.openapi.models.V1IngressRuleBuilder;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServicePortBuilder;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.compute.SecurityUtils;
import org.sciserver.compute.core.client.k8s.K8sClient;
import org.sciserver.compute.core.registry.DaskCluster;
import org.sciserver.compute.core.registry.GenericVolume;
import org.sciserver.compute.core.registry.K8sCluster;
import org.sciserver.compute.core.volume.GenericVolumeManager;
import org.sciserver.compute.model.DaskClusterInfo;
import org.sciserver.compute.model.DaskConnectionInfo;
import org.sciserver.compute.model.UserInfo;
import org.springframework.stereotype.Service;

@Service
public class DaskK8sHelper {
    private K8sClient k8sClient;
    private K8sCluster k8sCluster;
    private static final Logger logger = LogManager.getLogger(DaskK8sHelper.class);
    private static final Encoder base64 = Base64.getEncoder();
    
    private enum ServiceType {
        CLUSTER_IP,
        NODE_PORT
    }
    
    public DaskK8sHelper(K8sCluster k8sCluster) throws Exception {
        this.k8sCluster = k8sCluster;
        this.k8sClient = new K8sClient(k8sCluster.getApiUrl().toString(), k8sCluster.getApiToken());
    }
    
    private void createNamespace(String name, Map<String, String> labels) throws Exception {
        V1Namespace namespace = new V1NamespaceBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withLabels(labels)
                    .endMetadata()
                .build();
        
        k8sClient.getCoreApi().createNamespace(namespace, null, null, null);
    }
    
    private void createSecret(String namespace, String name, Map<String, byte[]> data) throws Exception {
        V1Secret secret = new V1SecretBuilder()
                .withNewMetadata()
                    .withName(name)
                    .endMetadata()
                .withType("Opaque")
                .withData(data)
                .build();
        
        k8sClient.getCoreApi().createNamespacedSecret(namespace, secret, null, null, null);
    }
    
    private void createService(String namespace, String name, Map<Integer, Integer> tcpPorts,
            Map<String, String> selector, ServiceType type) throws Exception {
        String serviceType;
        switch (type) {
            case NODE_PORT: 
                serviceType = "NodePort";
                break;
            case CLUSTER_IP: serviceType = "ClusterIP";
                break;
            default: 
                throw new Exception("Unknown service type");
        }
        
        List<V1ServicePort> servicePorts = new ArrayList<V1ServicePort>();
        
        for (int port : tcpPorts.keySet()) {
            V1ServicePort servicePort = new V1ServicePortBuilder()
                    .withProtocol("TCP")
                    .withPort(port)
                    .withTargetPort(new IntOrString(tcpPorts.get(port)))
                    .build();
            
            servicePorts.add(servicePort);
        }
        
        V1Service service = new V1ServiceBuilder()
                .withNewMetadata()
                    .withName(name)
                    .endMetadata()
                .withNewSpec()
                    .withSelector(selector)
                    .withPorts(servicePorts)
                    .withType(serviceType)
                    .endSpec()
                .build();
        
        k8sClient.getCoreApi().createNamespacedService(namespace, service, null, null, null);
    }
    
    private void createIngress(String namespace, String name, String serviceName, int port,
            String path, Map<String, String> annotations) throws Exception {
        
        V1IngressRule httpRule = new V1IngressRuleBuilder()
                .withNewHttp()
                    .addNewPath()
                        .withNewBackend()
                            .withNewService()
                                .withName(serviceName)
                                .withNewPort()
                                    .withNumber(port)
                                    .endPort()
                                .endService()
                            .endBackend()
                        .withPath(path)
                        .withPathType("ImplementationSpecific")
                        .endPath()
                    .endHttp()
                .build();
        
        V1Ingress ingress = new V1IngressBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withAnnotations(annotations)
                    .endMetadata()
                .withNewSpec()
                    .withRules(httpRule)
                    .endSpec()
                .build();
        
        k8sClient.getNetApi().createNamespacedIngress(namespace, ingress, null, null, null);
    }
    
    private void createDaskWorkers(String namespace, int replicas, String image,
            String memory, int threads, Iterable<GenericVolume> volumes) throws Exception {
        
        List<V1VolumeMount> k8sVolumeMounts = new ArrayList<V1VolumeMount>();
        List<V1Volume> k8sVolumes = new ArrayList<V1Volume>();
        k8sVolumeMounts.add(
                new V1VolumeMountBuilder()
                    .withName("dask-certs")
                    .withMountPath("/etc/dask-certs")
                    .withReadOnly(true)
                    .build());
        k8sVolumes.add(
                new V1VolumeBuilder()
                    .withName("dask-certs")
                    .withNewSecret()
                        .withSecretName("dask-certs")
                        .endSecret()
                    .build());
        int i = 0;
        for (GenericVolume v : volumes) {
            String name = "volume-" + (++i);
            GenericVolumeManager volumeManager = v.createVolumeManager();
            List<V1VolumeMount> generatedVolumeMounts = volumeManager.getK8sVolumeMounts(name);
            List<V1Volume> generatedVolumes = volumeManager.getK8sVolumes(name);
            if (generatedVolumeMounts.size() != generatedVolumes.size()) {
                throw new Exception("The number of volumes does not match the number of volume mounts");
            }
            k8sVolumeMounts.addAll(generatedVolumeMounts);
            k8sVolumes.addAll(generatedVolumes);
        }
        V1Container container = new V1ContainerBuilder()
                .withName("dask-worker")
                .withCommand("dask-worker")
                .withArgs(Arrays.asList(
                        "--worker-port", 
                        "8888",
                        "--local-directory",
                        "/home/idies",
                        "--name",
                        "$(DASK_WORKER_NAME)",
                        "--memory-limit",
                        memory,
                        "--nthreads",
                        Integer.toString(threads),
                        "--resources",
                        "MEMORY=" + memory,
                        "tls://dask-scheduler:8786",
                        "--tls-ca-file",
                        "/etc/dask-certs/ca.pem",
                        "--tls-cert",
                        "/etc/dask-certs/server-cert.pem",
                        "--tls-key",
                        "/etc/dask-certs/server-key.pem"))
                .withEnv(Arrays.asList(
                        new V1EnvVarBuilder()
                            .withName("DASK_NODE_NAME")
                            .withValueFrom(
                                    new V1EnvVarSourceBuilder()
                                        .withNewFieldRef()
                                            .withApiVersion("v1")
                                            .withFieldPath("spec.nodeName")
                                            .endFieldRef()
                                        .build())
                            .build(),
                        new V1EnvVarBuilder()
                            .withName("DASK_WORKER_NAME")
                            .withValueFrom(
                                    new V1EnvVarSourceBuilder()
                                        .withNewFieldRef()
                                            .withApiVersion("v1")
                                            .withFieldPath("metadata.name")
                                            .endFieldRef()
                                        .build())
                            .build()))
                .withImage(image)
                .withImagePullPolicy("Never")
                .withPorts(
                        new V1ContainerPortBuilder()
                            .withContainerPort(8888)
                            .withProtocol("TCP")
                            .build())
                .withVolumeMounts(k8sVolumeMounts)
                .build();
        
        Map<String, String> labels = new ImmutableMap.Builder<String, String>()
                .put("app.kubernetes.io/component", "dask-worker")
                .build();
        
        V1Deployment deployment = new V1DeploymentBuilder()
                .withNewMetadata()
                    .withName("dask-worker")
                    .withLabels(labels)
                    .endMetadata()
                .withNewSpec()
                    .withReplicas(replicas)
                    .withNewSelector()
                        .withMatchLabels(labels)
                        .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(labels)
                            .endMetadata()
                        .withNewSpec()
                            .withContainers(container)
                            .withVolumes(k8sVolumes)
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .build();
        
        k8sClient.getAppsApi().createNamespacedDeployment(namespace, deployment, null, null, null);
    }
    
    private void createDaskScheduler(String namespace, String image) throws Exception {
        Map<String, String> labels = new ImmutableMap.Builder<String, String>()
                .put("app.kubernetes.io/component", "dask-scheduler")
                .build();
        
        V1Container container = new V1ContainerBuilder()
                .withName("dask-scheduler")
                .withCommand("dask-scheduler")
                .withArgs(Arrays.asList(
                        "--dashboard-prefix",
                        "/" + namespace + "/dashboard",
                        "--tls-ca-file",
                        "/etc/dask-certs/ca.pem",
                        "--tls-cert",
                        "/etc/dask-certs/server-cert.pem",
                        "--tls-key",
                        "/etc/dask-certs/server-key.pem"))
                .withEnv(Arrays.asList(
                        new V1EnvVarBuilder()
                            .withName("DASK_NODE_NAME")
                            .withValueFrom(
                                    new V1EnvVarSourceBuilder()
                                        .withNewFieldRef()
                                            .withApiVersion("v1")
                                            .withFieldPath("spec.nodeName")
                                            .endFieldRef()
                                        .build())
                            .build(),
                        new V1EnvVarBuilder()
                            .withName("DASK_WORKER_NAME")
                            .withValueFrom(
                                    new V1EnvVarSourceBuilder()
                                        .withNewFieldRef()
                                            .withApiVersion("v1")
                                            .withFieldPath("metadata.name")
                                            .endFieldRef()
                                        .build())
                            .build()))
                .withImage(image)
                .withImagePullPolicy("Never")
                .withPorts(Arrays.asList(
                        new V1ContainerPortBuilder()
                            .withContainerPort(8786)
                            .withProtocol("TCP")
                            .build(),
                        new V1ContainerPortBuilder()
                            .withContainerPort(8787)
                            .withProtocol("TCP")
                            .build()))
                .withVolumeMounts(
                        new V1VolumeMountBuilder()
                            .withName("dask-certs")
                            .withMountPath("/etc/dask-certs")
                            .withReadOnly(true)
                            .build())
                .build();
        
        V1Deployment deployment = new V1DeploymentBuilder()
                .withNewMetadata()
                    .withName("dask-scheduler")
                    .withLabels(labels)
                    .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                        .withMatchLabels(labels)
                        .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(labels)
                            .endMetadata()
                        .withNewSpec()
                            .withContainers(container)
                            .withVolumes(
                                    new V1VolumeBuilder()
                                        .withName("dask-certs")
                                        .withNewSecret()
                                            .withSecretName("dask-certs")
                                            .endSecret()
                                        .build())
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .build();
        
        k8sClient.getAppsApi().createNamespacedDeployment(namespace, deployment, null, null, null);
        
    }
    
    public void createDaskCluster(DaskCluster cluster, int workers,
            String memory, int threads, Iterable<GenericVolume> volumes) throws Exception {
        
        String externalRef = cluster.getExternalRef();
        
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("sciserver.org/user-id", cluster.getUserId());
        
        // Generate CA
        KeyPair caKeyPair = SecurityUtils.createKeyPair();
        X509Certificate ca = SecurityUtils.createRootCA(caKeyPair, "CN=Default CA");
        
        // Generate server certificate
        KeyPair serverKeyPair = SecurityUtils.createKeyPair();
        X509Certificate server = SecurityUtils.createSingedCert(
                serverKeyPair, ca, caKeyPair.getPrivate(), "CN=Server");
        
        // Generate client certificate
        KeyPair clientKeyPair = SecurityUtils.createKeyPair();
        X509Certificate client = SecurityUtils.createSingedCert(
                clientKeyPair, ca, caKeyPair.getPrivate(), "CN=Client");
        
        createNamespace(externalRef, labels);
        
        createSecret(
                externalRef, 
                "dask-certs", 
                new ImmutableMap.Builder<String, byte[]>()
                    .put("ca.pem", SecurityUtils.getCertPem(ca))
                    .put("server-cert.pem", SecurityUtils.getCertPem(server))
                    .put("client-cert.pem", SecurityUtils.getCertPem(client))
                    .put("server-key.pem", SecurityUtils.getPrivateKeyPem(serverKeyPair.getPrivate()))
                    .put("client-key.pem", SecurityUtils.getPrivateKeyPem(clientKeyPair.getPrivate()))
                    .build());
        
        createService(
                externalRef, 
                "dask-scheduler", 
                new ImmutableMap.Builder<Integer, Integer>()
                    .put(8786, 8786)
                    .build(),
                new ImmutableMap.Builder<String, String>()
                    .put("app.kubernetes.io/component", "dask-scheduler")
                    .build(),
                ServiceType.NODE_PORT);
        
        createService(
                externalRef, 
                "dask-dashboard", 
                new ImmutableMap.Builder<Integer, Integer>()
                    .put(8787, 8787)
                    .build(),
                new ImmutableMap.Builder<String, String>()
                    .put("app.kubernetes.io/component", "dask-scheduler")
                    .build(),
                ServiceType.CLUSTER_IP);
        
        createIngress(
                externalRef,
                "dask-dashboard",
                "dask-dashboard",
                8787,
                "/" + externalRef + "/dashboard",
                new ImmutableMap.Builder<String, String>()
                    .put("nginx.ingress.kubernetes.io/ssl-redirect", "false")
                    .build());
        
        String image = cluster.getImage().getDockerRef();
        
        createDaskScheduler(
                externalRef,
                image);
        
        createDaskWorkers(
                externalRef,
                workers,
                image,
                memory,
                threads, 
                volumes);
    }
    
    private DaskClusterInfo getDaskClusterInfo(V1Namespace namespace, boolean connectionInfo) throws Exception {
        DaskClusterInfo info = new DaskClusterInfo();
        info.setExternalRef(namespace.getMetadata().getName());
        info.setUserId(namespace.getMetadata().getLabels().get("sciserver.org/user-id"));
        info.setDashboardUrl(k8sCluster.getPublicUrl() + info.getExternalRef() + "/dashboard/status");
        
        if (connectionInfo) {
            info.setConnection(getDaskConnectionInfo(namespace));
        }
        
        return info;
    }
    
    private DaskConnectionInfo getDaskConnectionInfo(V1Namespace namespace) throws Exception {
        DaskConnectionInfo connection = new DaskConnectionInfo();
        
        V1ServiceList services = k8sClient.getCoreApi().listNamespacedService(
                namespace.getMetadata().getName(), 
                null, false, null, "metadata.name=dask-scheduler", null, 1, null, null, 10, false);
        
        if (services.getItems().size() == 0) {
            throw new Exception("Service not found");
        }
        
        V1Service daskScheduler = services.getItems().get(0);
        int nodePort = daskScheduler.getSpec().getPorts().get(0).getNodePort();
        connection.setSchedulerUrl("tls://" + k8sCluster.getApiUrl().getHost() + ":" + nodePort);
        
        V1SecretList secrets = k8sClient.getCoreApi().listNamespacedSecret(
                namespace.getMetadata().getName(), 
                null, false, null, "metadata.name=dask-certs", null, 1, null, null, 10, false);
        
        if (secrets.getItems().size() == 0) {
            throw new Exception("Secret not found");
        }
        
        V1Secret daskCerts = secrets.getItems().get(0);
        connection.setCa(new String(base64.encode(daskCerts.getData().get("ca.pem"))));
        connection.setClientCert(new String(base64.encode(daskCerts.getData().get("client-cert.pem"))));
        connection.setClientKey(new String(base64.encode(daskCerts.getData().get("client-key.pem"))));

        return connection;
    }
    
    public DaskClusterInfo getDaskClusterInfo(String refId, boolean connectionInfo) throws Exception {
        V1NamespaceList namespaces = k8sClient.getCoreApi().listNamespace(
                null, false, null, "status.phase!=Terminating,metadata.name=" + refId, null, 1, null, null, 10, false);
        if (namespaces.getItems().size() == 0) {
            return null;
        } else {
            return getDaskClusterInfo(namespaces.getItems().get(0), connectionInfo);
        }
    }
    
    public DaskConnectionInfo getDaskConnectionInfo(String refId) throws Exception {
        V1NamespaceList namespaces = k8sClient.getCoreApi().listNamespace(
                null, false, null, "status.phase!=Terminating,metadata.name=" + refId, null, 1, null, null, 10, false);
        if (namespaces.getItems().size() == 0) {
            return null;
        } else {
            return getDaskConnectionInfo(namespaces.getItems().get(0));
        }
    }
    
    public List<DaskClusterInfo> getDaskClusterInfo(UserInfo user) throws Exception {
        V1NamespaceList namespaces = k8sClient.getCoreApi().listNamespace(
                null, false, null, "status.phase!=Terminating", "sciserver.org/user-id=" + user.getUserId(),
                -1, null, null, 10, false);
        List<DaskClusterInfo> result = new ArrayList<DaskClusterInfo>();
        for (V1Namespace ns : namespaces.getItems()) {
            result.add(getDaskClusterInfo(ns, false));
        }
        
        return result;
    }
    
    public void deleteDaskCluster(String refId) throws Exception {
        try {
            k8sClient.getCoreApi().deleteNamespace(refId, null, null, 0, null, null, null);
        } catch (RuntimeException ex) {
            // See https://github.com/kubernetes-client/java/issues/86
        }
    }
}
