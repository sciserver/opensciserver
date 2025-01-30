/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.volume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.compute.core.registry.GenericVolume;


public class PvcVolumeManager extends GenericVolumeManager {
    private static final Logger logger = LogManager.getLogger(PvcVolumeManager.class);

    public PvcVolumeManager(GenericVolume volume) {
        super(volume);
    }

    @Override
    public List<V1Volume> getK8sVolumes(String name) throws Exception {
        List<V1Volume> result = new ArrayList<>();

        try {
            Map<String, List<MountInfo>> map = getMountInfo();
            int i = 0;
            for (String claimName : map.keySet()) {
                result.add(new V1VolumeBuilder().withName(claimName).withNewPersistentVolumeClaim()
                        .withClaimName(claimName).endPersistentVolumeClaim().build());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        }

        return result;
    }

    @Override
    public List<V1VolumeMount> getK8sVolumeMounts(String name) throws Exception {
        List<V1VolumeMount> result = new ArrayList<>();
        GenericVolume volume = getVolume();

        try {
            Map<String, List<MountInfo>> map = getMountInfo();
            int i = 0;
            for (String claimName : map.keySet()) {
                for (MountInfo mountInfo : map.get(claimName)) {
                    V1VolumeMountBuilder vb = new V1VolumeMountBuilder().withName(claimName)
                            .withMountPath(mountInfo.mountPath).withReadOnly(!volume.isWritable());
                    if (mountInfo.subPath != null) {
                        vb = vb.withSubPath(mountInfo.subPath);
                    }
                    result.add(vb.build());
                }

                i++;
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        }

        return result;
    }

    Map<String, List<MountInfo>> getMountInfo() throws Exception {
        Map<String, List<MountInfo>> result = new HashMap<String, List<MountInfo>>();
        GenericVolume volume = getVolume();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode mountPaths = mapper.readTree(volume.getMountPath());
            JsonNode sources = mapper.readTree(volume.getSource());
            if (!mountPaths.isArray() || !sources.isArray()) {
                throw new Exception("Mount path and source must be JSON arrays");
            }
            Iterator<JsonNode> i = sources.iterator();
            Iterator<JsonNode> j = mountPaths.iterator();

            while (i.hasNext() && j.hasNext()) {
                String source = i.next().asText();
                String mountPath = j.next().asText();
                int n = source.indexOf(':');
                String claimName = (n < 0) ? source : source.substring(0, n);
                String subPath = (n < 0) ? null : source.substring(n + 1);
                if (!result.containsKey(claimName)) {
                    result.put(claimName, new ArrayList<MountInfo>());
                }
                result.get(claimName).add(new MountInfo(mountPath, subPath));
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
        return result;
    }

    class MountInfo {
        public String mountPath;
        public String subPath;

        public MountInfo(String mountPath, String subPath) {
            this.mountPath = mountPath;
            this.subPath = subPath;
        }
    }

}
