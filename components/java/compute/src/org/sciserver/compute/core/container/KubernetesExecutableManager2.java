/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.container;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentBuilder;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressBuilder;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1ScaleBuilder;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;
import io.kubernetes.client.util.Config;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.core.registry.Container;
import org.sciserver.compute.core.registry.ContainerStatus;
import org.sciserver.compute.core.registry.Domain;
import org.sciserver.compute.core.registry.ExecutableContainer;
import org.sciserver.compute.core.registry.GenericVolume;
import org.sciserver.compute.core.registry.Image;
import org.sciserver.compute.core.registry.K8sCluster;
import org.sciserver.compute.core.registry.RACMVolumeImage;
import org.sciserver.compute.core.registry.Registry;
import org.sciserver.compute.core.registry.RegistryLock;
import org.sciserver.compute.core.registry.VolumeImage;
import org.sciserver.compute.core.volume.GenericVolumeManager;
import org.sciserver.racm.jobm.model.VolumeContainerModel;


public class KubernetesExecutableManager2 extends ContainerManager implements ExecutableManager {

    static private int servicePort = 8888;
    private String namespace;
    private String ingressBase;
    private String ingressHost;
    private float cpuOvercommit;
    private float memOvercommit;
    private ApiClient k8sClient;
    private AppsV1Api appsApi;
    private CoreV1Api coreApi;
    private BatchV1Api batchApi;
    private NetworkingV1Api netApi;

    public KubernetesExecutableManager2(Image image) {
        super(image);
    }

    private K8sCluster getK8sCluster(Container container) throws Exception {
        Domain domain = container.getImage().getDomain();
        return getRegistry().getK8sClusters(domain).iterator().next();
    }

    private void setupClientsForContainer(Container container) throws Exception {
        K8sCluster k8s = getK8sCluster(container);
        namespace = k8s.getNamespace();
        memOvercommit = k8s.getMemOvercommit();
        cpuOvercommit = k8s.getCpuOvercommit();
        ingressBase = k8s.getPublicUrl().getPath();
        ingressHost = k8s.getPublicUrl().getHost();

        k8sClient = Config.fromToken(k8s.getApiUrl().toString(), k8s.getApiToken(), false);
        appsApi = new AppsV1Api(k8sClient);
        coreApi = new CoreV1Api(k8sClient);
        netApi = new NetworkingV1Api(k8sClient);
        batchApi = new BatchV1Api(k8sClient);
    }

    @Override
    public ExecutableContainer createContainer(
        String name, String description, AuthenticatedUser user, Iterable<VolumeContainerModel> publicVolumes,
        Iterable<VolumeImage> userVolumeImages) throws Exception
    {
        return createContainer(name, description, user, publicVolumes, userVolumeImages,
                               new String[] { "/opt/startup.sh" }, false);
    }

    @Override
    public ExecutableContainer createContainer(
        String name, String description, AuthenticatedUser user, Iterable<VolumeContainerModel> publicVolumes,
        Iterable<VolumeImage> userVolumeImages,  String[] commands) throws Exception
    {
        return createContainer(name, description, user, publicVolumes, userVolumeImages,
                               commands, false);
    }

    private void addVolumes(
        String source, String dest, Boolean ro,
        List<V1Volume> vols, List<V1VolumeMount> mounts)
    {
        String srv = source.split("/")[2];
        String path = "/" + source.split("/", 4)[3];
        String name = "vol-" + vols.size();
        vols.add(new V1VolumeBuilder().withName(name).withNewNfs().withServer(srv).withPath(path).
                 withReadOnly(ro).endNfs().build());
        mounts.add(new V1VolumeMountBuilder().withName(name).withMountPath(dest).
                   withReadOnly(ro).build());
    }

    @Override
    public ExecutableContainer createContainer(
        String name, String description, AuthenticatedUser user, Iterable<VolumeContainerModel> publicVolumes,
        Iterable<VolumeImage> userVolumeImages, String[] commands, boolean isJob)
        throws Exception {

        Registry registry = getRegistry();
        try (RegistryLock lock = new RegistryLock(registry)) {
            Domain domain = getImage().getDomain();

            ExecutableContainer container = new ExecutableContainer(registry);
            container.setName(name);
            container.setDescription(description);
            container.setExecutableImageId(getImage().getId());
            container.setUserId(user.getUserId());

            registry.registerExecutableContainer(container, domain);

            setupClientsForContainer(container);

            String cref = container.getExternalRef();
            container.setDockerRef(cref);
            String base_url = ingressBase + cref;
            List<String> args = new ArrayList<String>();
            for (String arg: commands)
                args.add(arg);
            args.add(base_url);

            List<V1Volume> volumes = new LinkedList<V1Volume>();
            List<V1VolumeMount> volumeMounts = new LinkedList<V1VolumeMount>();
            List<GenericVolume> registryPubVolumes = new ArrayList<GenericVolume>();

            for (VolumeContainerModel vcm : publicVolumes) {
                GenericVolume pv = registry.getGenericVolume(Long.parseLong(vcm.getPublisherDID()));
                pv.setWritable(vcm.isWritable());
                registryPubVolumes.add(pv);
            }

            for (VolumeImage vol: userVolumeImages) {
                Boolean ro = !((RACMVolumeImage)vol).isWritable();
                addVolumes(vol.getLocalPathTemplate(), vol.getContainerPath(), ro, volumes, volumeMounts);
            }
            int volumeNumber = volumes.size();
            for (GenericVolume pvol: registryPubVolumes) {
                String volumePrefix = "vol-" + (volumeNumber++);
                GenericVolumeManager volumeManager = pvol.createVolumeManager();
                List<V1VolumeMount> generatedVolumeMounts = volumeManager.getK8sVolumeMounts(volumePrefix);
                List<V1Volume> generatedVolumes = volumeManager.getK8sVolumes(volumePrefix);
                if (generatedVolumeMounts.size() != generatedVolumes.size()) {
                    //throw new Exception("The number of volumes does not match the number of volume mounts");
                }

                for (V1Volume v : generatedVolumes) {
                    if (!volumes.stream().anyMatch(x -> v.getName().equals(x.getName()))) {
                        volumes.add(v);
                    }
                }
                volumeMounts.addAll(generatedVolumeMounts);
            }

            volumes.add(new V1VolumeBuilder().withName("config").withNewConfigMap().
                        withName("etc-sciserver-config-files").endConfigMap().build());
            volumeMounts.add(new V1VolumeMountBuilder().withName("config").
                              withMountPath("/etc/sciserver").withReadOnly(true).build());

            if (domain.getShmBytes() > 0) {
                volumes.add(new V1VolumeBuilder().withName("shm").withNewEmptyDir().withMedium("Memory")
                            .withSizeLimit(new Quantity(String.valueOf(domain.getShmBytes()))).endEmptyDir().build());
                volumeMounts.add(new V1VolumeMountBuilder().withName("shm").withMountPath("/dev/shm").build());
            }

            // Resource limits and requests. Requests instruct K8S how to schedule resources onto nodes, while limits
            // are enforced. Since many containers will probably not use max resources, we can overcommit them. The overCommit
            // settings per k8s cluster determine how much to overcommit in cpu and mem.
            Map<String, Quantity> resource_requests = new HashMap<String, Quantity>();
            Map<String, Quantity> resource_limits = new HashMap<String, Quantity>();
            if (domain.getMaxMemory() > 0) {
                resource_limits.put("memory", new Quantity(String.valueOf(domain.getMaxMemory())));
                resource_requests.put("memory", new Quantity(String.valueOf((int)(domain.getMaxMemory()/memOvercommit))));
            }
            if (domain.getNanoCpus() > 0) {
                resource_limits.put("cpu", new Quantity(String.format("%dm", (int)(domain.getNanoCpus()/1000000))));
                resource_requests.put("cpu", new Quantity(String.format("%dm", (int)(domain.getNanoCpus()/1000000/cpuOvercommit))));
            }

            // for interactive
            V1Deployment deployment = new V1DeploymentBuilder().
                withNewMetadata().withName(cref).endMetadata().
                withNewSpec().withNewSelector().addToMatchLabels("externalref", cref).endSelector().
                withNewTemplate().withNewMetadata().addToLabels("externalref", cref).endMetadata().
                withNewSpec().addNewContainer().withImage(getImage().getDockerRef()).withName("compute").
                withVolumeMounts(volumeMounts).withArgs(args).
                withNewResources().withLimits(resource_limits).withRequests(resource_requests).endResources().
                addNewEnv().withName("SCISERVER_USER_ID").withValue(user.getUserId()).endEnv().
                addNewEnv().withName("SCISERVER_USER_NAME").withValue(user.getUserName()).endEnv().
                endContainer().withVolumes(volumes).
                endSpec().endTemplate().endSpec().
                build();

            // for jobs
            V1Job job = new V1JobBuilder().
                withNewMetadata().withName(cref).endMetadata().
                withNewSpec().withNewTemplate().withNewMetadata().addToLabels("externalref", cref).endMetadata().
                withNewSpec().withRestartPolicy("Never").addNewContainer().withImage(getImage().getDockerRef()).withName("compute").
                withVolumeMounts(volumeMounts).withArgs(args).
                withNewResources().withLimits(resource_limits).withRequests(resource_requests).endResources().
                addNewEnv().withName("SCISERVER_USER_ID").withValue(user.getUserId()).endEnv().
                addNewEnv().withName("SCISERVER_USER_NAME").withValue(user.getUserName()).endEnv().
                endContainer().withVolumes(volumes).
                endSpec().endTemplate().endSpec().
                build();

            V1Service service = new V1ServiceBuilder().
                withNewMetadata().withName("svc-"+cref).endMetadata().
                withNewSpec().addToSelector("externalref", cref).
                addNewPort().withPort(servicePort).withProtocol("TCP").endPort().
                endSpec().
                build();


            Map<String, String> ingressAnnotations = new HashMap<String, String>();
            ingressAnnotations.put(
                "nginx.ingress.kubernetes.io/auth-url",
                AppConfig.getInstance().getAppSettings().getLoginPortalUrl() + "api/check-cookie-token/" + user.getUserId()
            );
            ingressAnnotations.put("nginx.ingress.kubernetes.io/auth-method", "HEAD");
            ingressAnnotations.put("nginx.ingress.kubernetes.io/proxy-body-size", "0");
            ingressAnnotations.put("nginx.ingress.kubernetes.io/proxy-request-buffering", "off");
            ingressAnnotations.put("nginx.ingress.kubernetes.io/proxy-buffering", "off");

            V1Ingress ingress = new V1IngressBuilder()
                    .withNewMetadata()
                        .withName(cref)
                        .withAnnotations(ingressAnnotations)
                        .endMetadata()
                    .withNewSpec()
                        .addNewRule()
                        .withHost(ingressHost)
                            .withNewHttp()
                                .addNewPath()
                                    .withPath(base_url)
                                    .withNewBackend()
                                        .withNewService()
                                            .withName("svc-"+cref)
                                            .withNewPort()
                                                .withNumber(servicePort)
                                                .endPort()
                                            .endService()
                                        .endBackend()
                                    .withPathType("ImplementationSpecific")
                                    .endPath()
                                .endHttp()
                            .endRule()
                        .endSpec()
                    .build();

            if (isJob) {
                batchApi.createNamespacedJob(namespace, job, null, null, null);
            }
            else {
                appsApi.createNamespacedDeployment(namespace, deployment, null, null, null);
                coreApi.createNamespacedService(namespace, service, null, null, null);
                netApi.createNamespacedIngress(namespace, ingress, null, null, null);
            }

            container.setStatus(ContainerStatus.CREATED);
            container.update();

            return container;
        }
    }

    @Override
    public void deleteContainer(ExecutableContainer container) throws Exception {
        setupClientsForContainer(container);
        String extRef = container.getExternalRef();

        container.unregister();

        try {
            appsApi.deleteNamespacedDeployment(extRef, namespace, null, null, 0, null, null, null);
            netApi.deleteNamespacedIngress(extRef, namespace, null, null, 0, null, null, null);
            coreApi.deleteNamespacedService("svc-"+extRef, namespace, null, null, 0, null, null, null);
        } catch (Exception e) {};
        try {
            batchApi.deleteNamespacedJob(extRef, namespace, null, null, 0, null, null, null);
        } catch (Exception e) {};
        try {
            String pod = getPodFromRef(extRef).getMetadata().getName();
            coreApi.deleteNamespacedPod(pod, namespace, null, null, 0, null, null, null);
        } catch (Exception e) {};
    }

    @Override
    public void injectToken(ExecutableContainer container, String token) throws Exception {
        setupClientsForContainer(container);
        String extRef = container.getExternalRef();
        String[] cmd = {"bash", "-c", "echo " + token + " > /home/idies/keystone.token"};
        new Exec(k8sClient).exec(namespace, getPodFromRef(extRef).getMetadata().getName(), cmd, false);

    }

    private V1Pod getPodFromRef(String extref) throws Exception {
        return coreApi.listNamespacedPod(namespace, null, null, null, null, "externalref="+extref, 1, null, null, 5, false).
            getItems().get(0);
    }

    @Override
    public JsonNode getInfo(Container container) throws Exception {
        setupClientsForContainer(container);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode oj = mapper.createObjectNode();
        ObjectNode state = mapper.createObjectNode();


        // if we are scaled to 0 replicas, we do not expect any pod to be present so can short-circuit here
        try {
            V1Scale scale = appsApi.readNamespacedDeploymentScale(container.getExternalRef(), namespace, null);
            if (scale.getStatus().getReplicas() == 0) {
                state.put("Status", "Stopped");
                state.put("Running", false);
                oj.set("State", state);
                return oj;
            }
        } catch (Exception e) {
            // in case of job, we can ignore this as the pod info should cover that
        }

        try {
            V1Pod podInfo = getPodFromRef(container.getDockerRef());
            List<V1ContainerStatus> containerStatuses = podInfo.getStatus().getContainerStatuses();
            List<V1PodCondition> containerConditions = podInfo.getStatus().getConditions();
            String phase = podInfo.getStatus().getPhase();
            String startedAt = "";
            try {
                startedAt = podInfo.getStatus().getStartTime().toString();
                state.put("StartedAt", startedAt);
            } catch (Exception e) {
                // pending might not be a runnable state
            }
            if (phase.equals("Pending") && startedAt.equals("")) {
                state.put("Status", "Pending");
                state.put("ExitCode", 0);
                try {
                    state.put("Error", containerConditions.get(0).getMessage());
                } catch (Exception e) {
                    state.put("Error", "");
                }
                state.put("Running", false);
            }
            else if (phase.equals("Running") || phase.equals("Pending")) {
                state.put("Status", "Running");
                state.put("ExitCode", 0);
                state.put("Error", "");
                state.put("Running", true);
            }
            else {
                state.put("Status", "exited");
                state.put("ExitCode", containerStatuses.get(0).getState().getTerminated().getExitCode());
                state.put("Error", containerStatuses.get(0).getState().getTerminated().getMessage());
                state.put("Running", false);
                state.put("FinishedAt", containerStatuses.get(0).getState().getTerminated().getFinishedAt().toString());
            }
        } catch (Exception e) {
            state.put("Status", "error");
            state.put("Error", "Unkown error");
        }
        oj.set("State", state);
        return oj;
    }

    public void startContainer(ExecutableContainer container) throws Exception {
        setupClientsForContainer(container);
        V1Scale scale = new V1ScaleBuilder()
            .withNewMetadata().withNamespace(namespace).withName(container.getExternalRef()).endMetadata()
            .withNewSpec().withReplicas(1).endSpec().build();
        appsApi.replaceNamespacedDeploymentScale(container.getExternalRef(), namespace, scale, null, null, null);
    }

    public void stopContainer(ExecutableContainer container) throws Exception {
        setupClientsForContainer(container);
        V1Scale scale = new V1ScaleBuilder()
            .withNewMetadata().withNamespace(namespace).withName(container.getExternalRef()).endMetadata()
            .withNewSpec().withReplicas(0).endSpec().build();
        appsApi.replaceNamespacedDeploymentScale(container.getExternalRef(), namespace, scale, null, null, null);
    }

    public void setProxy(ExecutableContainer container) throws Exception {
    }

    public void deleteProxy(ExecutableContainer container) throws Exception {
    }

    public String getStdOut(Container container) throws Exception {
        return "";
    }
    public String getStdErr(Container container) throws Exception {
        return "";
    }

    public URL getProxyUrl(ExecutableContainer container) throws Exception {
        return getK8sCluster(container).getPublicUrl();
    }

}
