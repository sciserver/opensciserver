package org.sciserver.compute.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sciserver.authentication.client.AuthenticatedUser;
import org.sciserver.compute.AppConfig;
import org.sciserver.compute.UnauthorizedException;
import org.sciserver.compute.Utilities;
import org.sciserver.compute.core.registry.DaskCluster;
import org.sciserver.compute.core.registry.DaskClusterStatus;
import org.sciserver.compute.core.registry.Image;
import org.sciserver.compute.core.registry.K8sCluster;
import org.sciserver.compute.core.registry.GenericVolume;
import org.sciserver.compute.core.registry.Registry;
import org.sciserver.compute.dask.DaskK8sHelper;
import org.sciserver.compute.model.DaskClusterInfo;
import org.sciserver.compute.model.DomainInfo;
import org.sciserver.racm.jobm.model.UserDockerComputeDomainModel;
import org.sciserver.racm.jobm.model.VolumeContainerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DaskUiController {

    @Autowired
    AppConfig appConfig;

    @RequestMapping(value = { "/dask" }, method = RequestMethod.GET)
    public String showClusters(
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        model.addAttribute("authenticated", true);
        model.addAttribute("username", user.getUserName());

        Registry reg = appConfig.getRegistry();

        List<UserDockerComputeDomainModel> domains = Utilities.getDaskDomains(token);

        if (domains.isEmpty()) {
            return "index";
        }
        else {
            model.addAttribute("isDaskAvailable", true);

            ArrayList<DomainInfo> domainList = new ArrayList<>();
            for (UserDockerComputeDomainModel domain : domains) {
                if (domain.getImages().isEmpty()) continue;
                DomainInfo item = new DomainInfo();
                item.setId(Long.parseLong(domain.getPublisherDID()));
                item.setName(domain.getName());
                item.setDescription(domain.getDescription());
                domainList.add(item);
            }
            model.addAttribute("domains", domainList);

            List<DaskClusterInfo> clustersInfo = new ArrayList<DaskClusterInfo>();
            Iterable<DaskCluster> list = appConfig.getRegistry().getDaskClusters(user.getUserId());
            for (DaskCluster c : list) {
                DaskClusterInfo item = new DaskClusterInfo();
                item.setDashboardUrl(c.getK8sCluster().getPublicUrl() + c.getExternalRef() + "/dashboard/status");
                item.setExternalRef(c.getExternalRef());
                item.setUserId(user.getUserId());
                item.setId(c.getId());
                clustersInfo.add(item);
            }

            model.addAttribute("clusters", clustersInfo);
            model.addAttribute("defaultWorkers", appConfig.getAppSettings().getDaskWorkers());
            model.addAttribute("defaultMemory", appConfig.getAppSettings().getDaskMemory());
            model.addAttribute("defaultThreads", appConfig.getAppSettings().getDaskThreads());

            return "dask";
        }
    }

    @RequestMapping(value = { "/dask/delete" }, method = RequestMethod.GET)
    public String deleteCluster(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("id") long id) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        DaskCluster daskCluster = appConfig.getRegistry().getDaskCluster(id);
        if (!user.getUserId().equals(daskCluster.getUserId())) throw new UnauthorizedException("Unauthorized");
        DaskK8sHelper helper = new DaskK8sHelper(daskCluster.getK8sCluster());

        helper.deleteDaskCluster(daskCluster.getExternalRef());
        daskCluster.setStatus(DaskClusterStatus.DELETED);
        appConfig.getRegistry().updateDaskCluster(daskCluster);

        return "redirect:/dask";
    }

    @RequestMapping(value = { "/dask/create" }, method = RequestMethod.POST)
    public String createCluster(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("clusterName") String clusterName,
            @RequestParam("imageId") long imageId,
            @RequestParam("workers") int workers,
            @RequestParam("memory") String memory,
            @RequestParam("threads") int threads,
            @RequestParam(value="publicVolume", required=false) List<Long> publicVolumeIds) throws Exception {

        String token = Utilities.getToken(request, response);
        AuthenticatedUser user = appConfig.getAuthClient().getAuthenticatedUser(token);

        Registry reg = appConfig.getRegistry();
        Image image = reg.getExecutableImage(imageId);
        K8sCluster k8s;
        try {
            k8s = reg.getK8sClusters(image.getDomain()).iterator().next();
        }
        catch (NoSuchElementException ex) {
            throw new Exception("No available K8s clusters");
        }

        List<UserDockerComputeDomainModel> domains = appConfig.getInteractiveUserDomainsCache().get(token);
        UserDockerComputeDomainModel domain = domains.stream().filter(
            d -> Long.parseLong(d.getPublisherDID()) == image.getDomainId()).findAny().get();
        ArrayList<GenericVolume> publicVolumes = new ArrayList<>();
        List<VolumeContainerModel> jobmModelVolumes = domains.stream().filter(
            d -> Long.parseLong(d.getPublisherDID()) == image.getDomainId()).findFirst().get().getVolumes();

        if (publicVolumeIds != null) {
            for (Long id : publicVolumeIds) {
                publicVolumes.add(appConfig.getRegistry().getGenericVolume(id));
            }
        }

        for (GenericVolume v : publicVolumes) {
            VolumeContainerModel vcm = jobmModelVolumes.stream().filter(volume -> v.getId() == Long.parseLong(volume.getPublisherDID())).findFirst().get();
            v.setWritable(vcm.isWritable());
        }

        DaskCluster cluster = new DaskCluster(reg);
        cluster.setName(clusterName);
        cluster.setDescription("");
        cluster.setK8sClusterId(k8s.getId());
        cluster.setImageId(image.getId());
        cluster.setUserId(user.getUserId());
        reg.registerDaskCluster(cluster);

        DaskK8sHelper helper = new DaskK8sHelper(k8s);
        helper.createDaskCluster(
                cluster,
                workers,
                memory,
                threads,
                publicVolumes);

        cluster.setStatus(DaskClusterStatus.CREATED);
        reg.updateDaskCluster(cluster);

        return "redirect:/dask";
    }
}
