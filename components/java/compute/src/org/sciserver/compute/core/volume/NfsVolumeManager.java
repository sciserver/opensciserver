package org.sciserver.compute.core.volume;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.compute.core.registry.GenericVolume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;

public class NfsVolumeManager extends GenericVolumeManager {
	private static final Logger logger = LogManager.getLogger(NfsVolumeManager.class);
	
	public NfsVolumeManager(GenericVolume volume) {
		super(volume);
	}

	@Override
	public List<V1Volume> getK8sVolumes(String name) throws Exception {
		List<V1Volume> result = new ArrayList<>();
		GenericVolume volume = getVolume();
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode sources = mapper.readTree(volume.getSource());
			if (sources.isArray()) {
				int i=0;
				for (JsonNode source: sources) {
					String[] nfs = source.asText().split(":");
					result.add(new V1VolumeBuilder()
							.withName(name + "-" + (++i))
							.withNewNfs()
								.withServer(nfs[0])
								.withPath(nfs[1])
								.endNfs()
							.build());
				}
			}
			else throw new Exception("Source must be JSON array");
		}
		catch (Exception ex) {
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
			ObjectMapper mapper = new ObjectMapper();
			JsonNode mountPaths = mapper.readTree(volume.getMountPath());
			if (mountPaths.isArray()) {
				int i=0;
				for (JsonNode mountPath: mountPaths) {
					result.add(new V1VolumeMountBuilder()
							.withName(name + "-" + (++i))
							.withMountPath(mountPath.asText())
							.withReadOnly(!volume.isWritable())
							.build());
				}
			}
			else throw new Exception("Mount path must be JSON array");
		}
		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
		
		return result;
	}

}
