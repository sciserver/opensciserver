import axios from 'axios';
import { normalize, schema } from 'normalizr';
import keyBy from 'lodash/keyBy';
import find from 'lodash/find';

export default {
  state: {
    racmComputeDomains: {},
    computeComputeDomains: {},
    racmImages: {},
    computeImages: {},
    racmVolumeContainers: {},
    computeVolumeContainers: {},
    computeNodes: {},
    racmRootVolumesOnCD: {},
      userComputeDomains: {},
      userComputeDomainsLoaded: false,
  },
  mutations: {
    setFromRACMComputeDomains(state, domains) {
      const image = new schema.Entity('images');
      const volumeContainer = new schema.Entity('volumeContainers');
      const rootVolumeOnCD = new schema.Entity('rootvolumesOnCD');
      const domain = new schema.Entity('domains', {
        images: [image],
        volumes: [volumeContainer],
        rootVolumes: [rootVolumeOnCD],
      });
      const normalizedData = normalize(domains, [domain]);

      state.racmComputeDomains = normalizedData.entities.domains;
      state.racmImages = normalizedData.entities.images;
      state.racmVolumeContainers = normalizedData.entities.volumeContainers;
      state.racmRootVolumesOnCD = normalizedData.entities.rootvolumesOnCD;
    },
    setFromComputeDomains(state, domains) {
      state.computeComputeDomains = keyBy(domains, 'id');
    },
    setComputeImages(state, images) {
      state.computeImages = keyBy(images, 'id');
    },
    setComputeVolumeContainers(state, volumes) {
      state.computeVolumeContainers = keyBy(volumes, 'id');
    },
    setComputeNodes(state, nodes) {
      state.computeNodes = keyBy(nodes, 'id');
    },
    setFromUserComputeDomains(state, domains) {
        state.userComputeDomains = domains;
        state.userComputeDomainsLoaded = true;
    },
  },
  actions: {
    loadUserComputeDomains({ rootState, commit }) {
      return axios.get(`${RACM_URL}/jobm/rest/computedomains`, {
        headers: {
          'X-Auth-Token': rootState.token,
        },
      }).then((response) => {
        commit('setFromUserComputeDomains', response.data);
      });
    },
    loadCompute({ rootState, commit }) {
      return axios.get(`${RACM_URL}/jobm/rest/dockercomputedomains`, {
        headers: {
          'X-Auth-Token': rootState.token,
        },
      }).then((response) => {
        commit('setFromRACMComputeDomains', response.data);
      });
    },
    loadDomainsFromCompute({ rootState, commit }) {
      return axios.get(`${COMPUTE_URL}/admin/domain`, {
        headers: {
          'X-Auth-Token': rootState.token,
        },
      }).then((response) => {
        commit('setFromComputeDomains', response.data);
      });
    },
    loadImagesFromCompute({ rootState, commit }) {
      return axios.get(`${COMPUTE_URL}/admin/image`, {
        headers: {
          'X-Auth-Token': rootState.token,
        },
      }).then((response) => {
        commit('setComputeImages', response.data);
      });
    },
    loadVolumesFromCompute({ rootState, commit }) {
      return axios.get(`${COMPUTE_URL}/admin/volume`, {
        headers: {
          'X-Auth-Token': rootState.token,
        },
      }).then((response) => {
        commit('setComputeVolumeContainers', response.data);
      });
    },
    loadNodesFromCompute({ rootState, commit }) {
      return axios.get(`${COMPUTE_URL}/admin/node`, {
        headers: {
          'X-Auth-Token': rootState.token,
        },
      }).then((response) => {
        commit('setComputeNodes', response.data);
      });
    },
  },
  getters: {
    getUserComputeDomainById: state => id =>
      find(state.userComputeDomains, { publisherDID: id.toString() }),
  },
};
