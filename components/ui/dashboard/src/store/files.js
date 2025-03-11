import Vue from 'vue';
import axios from 'axios';
import { normalize, schema } from 'normalizr';
import throttle from 'lodash/throttle';
import keyBy from 'lodash/keyBy';
import forEach from 'lodash/forEach';
import find from 'lodash/find';
import flatMap from 'lodash/flatMap';
import map from 'lodash/map';
import deepExtend from 'deep-extend';
import filesUtils from '../files-utils';

const _ = require('lodash');

/* All API calls that are throttled will
wait at least this many milliseconds between calls */
const API_THROTTLE_TIME = 2000;

export default {
  state: {
    fileservices: {},
    myFileserviceIdentifiers: [],
    rootVolumes: {},
    dataVolumes: {},
    userVolumes: {},
    serviceOwnedResources: [],
    /* structure:
       {
         <uservolumeId>: {
           '/some/path': { <jsonTree> },
           '/some/other/path': { <jsonTree> },
         },
         etc..
       } */
    jsonTreeCache: {},
    quotasPerFileservice: {},
    fileLinkStack: '',
    loading_status: false,
    hasFileService: true,
  },
  mutations: {
    setServiceOwnedResources(state, serviceOwnedResources) {
      state.serviceOwnedResources = serviceOwnedResources;
    },
    setHasFileService(state, hasFileServiceStatus) {
      state.hasFileService = hasFileServiceStatus;
    },
    setMyFileservices(state, listOfFileServiceIdentifiers) {
      state.myFileserviceIdentifiers = listOfFileServiceIdentifiers;
    },
    updateFileServices(state, newFileServices) {
      state.fileservices = deepExtend({}, state.fileservices, newFileServices);
    },
    updateRootVolumes(state, newRootVolumes) {
      state.rootVolumes = deepExtend({}, state.rootVolumes, newRootVolumes);
    },
    updateDataVolumes(state, newDataVolumes) {
      state.dataVolumes = deepExtend({}, state.dataVolumes, newDataVolumes);
    },
    updateUserVolumes(state, newUserVolumes) {
      state.userVolumes = deepExtend({}, state.userVolumes, newUserVolumes);
    },
    setJsonTreeCache(state, { userVolumeId, path, jsonTree }) {
      if (!state.jsonTreeCache[userVolumeId]) {
        Vue.set(state.jsonTreeCache, userVolumeId, {});
      }
      Vue.set(state.jsonTreeCache[userVolumeId], path, jsonTree);
    },
    setQuotas(state, { fileServiceIdentifier, listOfQuotas }) {
      Vue.set(state.quotasPerFileservice, fileServiceIdentifier, listOfQuotas);
    },
    updateFSLinkStack(state, lastBrowsed) {
      state.fileLinkStack = lastBrowsed;
    },
    change(state, loadingstat) {
      state.loading_status = loadingstat;
    },
  },
  getters: {
    getUserVolumeById: state => id => find(state.userVolumes, { id }),
    getMyUserVolumeById: (state, getters) => id => find(getters.myUserVolumeObjects, { id }),
    getMyDVUserVolumeById: (state, getters) => id => find(getters.myDataVolumeObjects, { id }),
    getMyRootVolumeById: (state, getters) => id => find(getters.myRootVolumeObjects, { id }),
    getRootVolumeById: state => id => find(state.rootVolumes, { id }),
    getDataVolumeById: state => id => find(state.dataVolumes, { id }),
    // getloadingstatus: {
    //   loading_status: state => state.loading_status,
    // },
    getFileserviceByIdentifier: state => identifier => find(state.fileservices, { identifier }),
    /* These next three getters return a list of the original objects from the normalized JSON,
      with references to the containing object(s) */
    myFileSystemObjects(state, getters) {
      return state.myFileserviceIdentifiers
        .map(getters.getFileserviceByIdentifier);
    },
    myRootVolumeObjects(state, getters) {
      return flatMap(getters.myFileSystemObjects, fs =>
        map(fs.rootVolumes, getters.getRootVolumeById)
        .map(rv => ({
          fileserviceObj: fs,
          ...rv,
        })));
    },
    myDataVolumeObjects(state, getters) {
      return flatMap(getters.myFileSystemObjects, fs =>
        map(fs.dataVolumes, getters.getDataVolumeById)
        .map(dv => ({
          apiEndpoint: fs.apiEndpoint,
          fileserviceObj: fs,
          type: 'datavolumes',
          ...dv,
        })));
    },
    myUserVolumeObjects(state, getters) {
      return flatMap(getters.myRootVolumeObjects, rv =>
        map(rv.userVolumes, getters.getUserVolumeById)
        .map(uv => ({
          rootVolumeObj: rv,
          fileserviceObj: rv.fileserviceObj,
          apiEndpoint: rv.fileserviceObj.apiEndpoint,
          rootVolumeName: rv.name,
          Rtype: 'USERVOLUME',
          type: 'uservolumes',
          urlSegment: rv.containsSharedVolumes ?
            `${rv.name}/${uv.owner}/${uv.name}/`
            : `${rv.name}/${uv.owner}/`,
          ...uv,
        })));
    },
    myPersistentVol(state, getters, rootState) {
        return _.find(getters.myUserVolumeObjects,
                      { type: 'uservolumes', name: 'persistent', owner: rootState.userProfile.username });
    },
    myScratchVol(state, getters, rootState) {
        return _.find(getters.myUserVolumeObjects,
                      { type: 'uservolumes', name: 'scratch', owner: rootState.userProfile.username });
    },
    lastRoute(state) {
      return state.fileLinkStack;
    },

  },
  actions: {
    loadFileServices: throttle(({ commit, dispatch }) =>
      Vue.http.get(`${RACM_URL}/storem/fileservices`)
        .then((response) => {
          dispatch('loadExcludedResources');
          const allFileServices = keyBy(response.data, 'identifier');
          commit('updateFileServices', allFileServices);
          commit('setMyFileservices', Object.keys(allFileServices));
          commit('setHasFileService', Object.getOwnPropertyNames(allFileServices).length !== 0);
        }), API_THROTTLE_TIME, { trailing: false }),

    loadUserVolumesFromFileService: ({ commit }, fileServiceEndpoint) =>
        Vue.http.get(`${fileServiceEndpoint}api/volumes/`)
          .then((response) => {
            const userVolume = new schema.Entity('userVolumes');
            const rootVolume = new schema.Entity('rootVolumes', {
              userVolumes: [userVolume],
            });
            const dataVolume = new schema.Entity('dataVolumes', {
              userVolumes: [userVolume],
            });
            const fileService = new schema.Entity('fileservices', {
              rootVolumes: [rootVolume],
              dataVolumes: [dataVolume],
            }, { idAttribute: 'identifier' });

            const normalizedData = normalize(response.body, fileService);
            commit('updateUserVolumes', normalizedData.entities.userVolumes);
            commit('updateRootVolumes', normalizedData.entities.rootVolumes);
            commit('updateDataVolumes', normalizedData.entities.dataVolumes);
            commit('updateFileServices', normalizedData.entities.fileservices);
          }),
    loadQuotaFromFileService({ getters, commit }, fileServiceIdentifier) {
      const fileservice = getters.getFileserviceByIdentifier(fileServiceIdentifier);
      return Vue.http.get(`${fileservice.apiEndpoint}api/usage`)
        .then(response => commit('setQuotas', {
          fileServiceIdentifier: fileservice.identifier,
          listOfQuotas: response.body,
        }));
    },
    loadAllQuotas: throttle(({ state, dispatch }) =>
      Promise.all(
        state.myFileserviceIdentifiers
          .map(id => dispatch('loadQuotaFromFileService', id)),
      ), API_THROTTLE_TIME, { trailing: false }),
      loadExcludedResources({ commit }) {
        Vue.http.get(`${RACM_URL}/rest/myserviceownedresources`)
        .then((response) => {
          commit('setServiceOwnedResources', map(response.data.rows, x => x[3]));
        });
      },
    loadAllUserVolumes: throttle(({ state, dispatch }) =>
      dispatch('loadFileServices')
        .then(() => forEach(state.fileservices, f => dispatch('loadUserVolumesFromFileService', f.apiEndpoint))), API_THROTTLE_TIME, { trailing: false }),
    loadJsonTree({ commit }, { userVolumeObject, path }) {
      const url = filesUtils.joinURLWithPath(userVolumeObject, 'api/jsontree/', path);
      const urlLevelTwo = `${url}?level=2`;
      Vue.http.get(urlLevelTwo)
        .then((response) => {
          commit('setJsonTreeCache', {
            userVolumeId: userVolumeObject.id,
            path,
            jsonTree: response.body,
          });
        });
    },
    createUV({ dispatch }, { description, path, config }) {
      axios
        .put(
          path,
          { description }, config)
        .then(() => {
          dispatch('loadAllUserVolumes');
        }, (error) => {
          alert(`Error Message: ${error.response.data.error}`);
        });
    },
    createFolder({ dispatch }, { path, config }) {
      axios
        .put(path, '', config)
        .then(() => {
          dispatch('loadAllUserVolumes');
        }, (error) => {
          alert(`Error Message: ${error.response.data.error}`);
        });
    },
    deleteFile({ dispatch }, { path, config }) {
      axios.delete(path, config)
      .then(() => {
        dispatch('loadAllUserVolumes');
      }, (error) => {
        alert(`Error Message: ${error}`);
      });
    },
    loadLastPath({ commit }, path) {
      commit('updateFSLinkStack', path);
    },
    loadJsonTreeById: ({ dispatch, getters }, { userVolumeId, path, type }) =>
      dispatch('loadJsonTree', type === 'uservolumes' ? { userVolumeObject: getters.getMyUserVolumeById(userVolumeId), path, type } : { userVolumeObject: getters.getMyDVUserVolumeById(userVolumeId), path, type }),
  },
};
