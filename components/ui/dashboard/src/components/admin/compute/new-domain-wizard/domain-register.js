import axios from 'axios';
import find from 'lodash/find';
import range from 'lodash/range';

export async function registerDomainInCompute(token, model) {
  const requestBody = {
    name: model.name,
    description: model.description,
    max_memory: parseInt(model.maxMemory, 10),
  };
  const response = await axios.post(
    `${COMPUTE_URL}/admin/domain`,
    requestBody,
    {
      headers: {
        'X-Auth-Token': token,
      },
    });

  return response.data;
}

export async function registerNodeInCompute(token, model, node) {
  const requestBody = {
    name: node.name,
    description: node.name,
    docker_api_url: node.dockerApiUrl,
    docker_api_client_cert: node.clientCert,
    docker_api_client_key: node.clientKey,
    proxy_api_url: node.proxyApiUrl,
    proxy_api_client_cert: node.clientCert,
    proxy_api_client_key: node.clientKey,
    proxy_base_url: node.proxyBaseUrl,
    domain_id: parseInt(model.domain_id, 10),
  };

  const response = await axios.post(
    `${COMPUTE_URL}/admin/node`,
    requestBody,
    {
      headers: {
        'X-Auth-Token': token,
      },
    });

  return response.data;
}

export async function registerSlotsForNode(token, nodeId) {
  const requestBody = {
    node_id: nodeId,
    // Skipping port 10010 since it is used for CRI
    port_numbers: range(10000, 10010).concat(range(10011, 10101)),
  };

  const response = await axios.post(
    `${COMPUTE_URL}/admin/slots`,
    requestBody,
    {
      headers: {
        'X-Auth-Token': token,
      },
    });

  return response.data;
}

export async function registerImageInCompute(token, model, image) {
  const requestBody = {
    name: image.name,
    description: image.description,
    docker_ref: image.dockerRef,
    container_manager_class: image.containerManagerClass,
    domain_id: parseInt(model.domain_id, 10),
  };

  const response = await axios.post(
    `${COMPUTE_URL}/admin/image`,
    requestBody,
    {
      headers: {
        'X-Auth-Token': token,
      },
    });

  return response.data;
}

export async function registerImageInRACM(token, domainId, image) {
  const existingDomainsResponse = await axios.get(
    `${RACM_URL}/jobm/rest/dockercomputedomains`,
    {
      headers: {
        'X-Auth-Token': token,
      },
    });
  const modifiedDomain = find(existingDomainsResponse.data, { id:
    parseInt(domainId, 10) });
  modifiedDomain.images.push({
    name: image.name,
    description: image.description,
    publisherDID: image.id.toString(),
  });
  const response = await axios.post(
    `${RACM_URL}/jobm/rest/computedomains/docker`,
    modifiedDomain,
    {
      headers: {
        'X-Auth-Token': token,
      },
      params: {
        admins: 'admin',
      },
    });
  return response.data;
}

export async function registerVolumeInCompute(token, model, volume) {
  const requestBody = {
    name: volume.name,
    description: volume.description,
    docker_ref: volume.dockerRef,
    domain_id: parseInt(model.domain_id, 10),
  };

  const response = await axios.post(
    `${COMPUTE_URL}/admin/volume`,
    requestBody,
    {
      headers: {
        'X-Auth-Token': token,
      },
    });

  return response.data;
}

export async function registerDomainInRACM(token, model) {
  const requestBody = {
    name: model.name,
    description: model.description,
    publisherDID: model.domain_id.toString(),
    apiEndpoint: `${COMPUTE_URL}/api/domains/${model.domain_id}`,
    images: model.images.map(image => ({
      name: image.name,
      description: image.description,
      publisherDID: image.id.toString(),
    })),
    volumes: model.volumes.map(volume => ({
      name: volume.name,
      description: volume.description,
      publisherDID: volume.id.toString(),
    })),
    rootVolumes: model.rootVolumesOnCD.map(rootVolume => ({
      displayName: rootVolume.name,
      pathOnCD: model.rootVolumePathOnCDs[rootVolume.id],
      rootVolumeId: rootVolume.id,
    })),
  };

  const response = await axios.post(
    `${RACM_URL}/jobm/rest/computedomains/docker`,
    requestBody,
    {
      headers: {
        'X-Auth-Token': token,
      },
      params: {
        admins: 'admin',
      },
    });

  return response.data;
}

export async function saveEntityToAdminGroup(adminGroup, token, resourceType, actions, entityId) {
  const url = adminGroup._links.shareResource.href.split('?');
  const path = url[0];
  await axios.put(
    path,
    {},
    {
      headers: {
        'X-Auth-Token': token,
      },
      params: {
        resourceType,
        actions,
        entityId,
      },
    });
}

export async function saveAllToAdminGroup(adminGroup, token, racmObject) {
  // This is sequential instead of using Promise.all, since RACM
  // Can't handle parrallel changes to the same ResourceContext
  await racmObject.images.map(async (image) => {
    await saveEntityToAdminGroup(adminGroup, token, 'DOCKERIMAGE', 'grant', image.id);
  });
  await racmObject.volumes.map(async (volume) => {
    await saveEntityToAdminGroup(adminGroup, token, 'VOLUMECONTAINER', 'grant', volume.id);
  });
}
