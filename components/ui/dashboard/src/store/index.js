import Vue from 'vue';
import Vuex from 'vuex';

import { normalize, schema } from 'normalizr';
/* node-deep-extend is used to recurisvely
 merge object properties, but override arrays.
 For example, when merging group information, we
 want to keep the latest member list, not a union
 of old and new members. */
import deepExtend from 'deep-extend';
import find from 'lodash/find';
import filter from 'lodash/filter';
import findIndex from 'lodash/findIndex';
import throttle from 'lodash/throttle';
import some from 'lodash/some';
import keyBy from 'lodash/keyBy';
import map from 'lodash/map';
import get from 'lodash/get';
import Notifications from 'vue-notification';

import FilesModule from './files';
import JobsModule from './jobs';
import CoursesModule from './courses';
import ComputeModule from './compute';
import ScienceModule from './scienceDomains';

Vue.use(Vuex);
Vue.use(Notifications);

/* All API calls that are throttled will
wait at least this many milliseconds between calls */
const API_THROTTLE_TIME = 2000;

export const modules = {
  files: FilesModule,
  jobs: JobsModule,
  compute: ComputeModule,
  course: CoursesModule,
  science: ScienceModule,
};

export const initialState = {
  userProfile: {},
  token: undefined,
  myCollaborations: [],
  publicGroups: [],
  publicUsers: [],
  collaborations: {},
  collaborationLinks: [],
  configLinks: [],
  userActivities: [],
  groups: {},
  users: {},
  resources: [],
  /* either NOTLOADED, LOADING, LOADED, or RELOADING */
  userActivitiesStatus: 'NOTLOADED',
  collaborationLoadingStatus: 'NOTLOADED',
  configLoadingStatus: 'NOTLOADED',
  resourceLoadingStatus: 'NOTLOADED',
  groupLinkStack: '',
  linkedAccounts: [],
  actionName: '', // Name of the action
  actionFileName: '', // Name of the file for the action
  actionStatus: false, // Status for the action on the file
  actionArray: [],
  responseArray: [], // actions that require response like failed upload
  numOfNotifications: 0,
  loadingAction: '',
  loadingStatusIcon: false,
  sortingStatus: '+name',
  loadScienceDomains: false,
  totalScienceDomains: 0,
  joinedScienceDomains: 0,
  collaborationResponse: {},
};

export default new Vuex.Store({
  modules,
  state: initialState,
  getters: {
    lastGroupRoute(state) {
      return state.groupLinkStack;
    },
    isAdmin: (state, getters) =>
      getters.getGroupIds.find(g => get(getters.getCollaborationById(g), 'name') === 'admin') !== undefined,
    userIdForUserName: state => name => find(state.users, { username: name }).id,
    getGroupById: state => id => state.groups[id],
    getUserById: state => id => state.users[id],
    getCollaborationById: state => id => state.collaborations[id],
    getGroupIds: (state, getters) =>
      state.myCollaborations.filter(id => get(getters.getCollaborationById(id), 'type') === 'GROUP'),
    getMembershipIn: (state, getters) => id =>
      find(get(getters.getCollaborationById(id), 'members'), { id: state.userProfile.id }),
    getRoleIn: (state, getters) => collaborationId =>
      getters.getMembershipIn(collaborationId).role,
    getStatusIn: (state, getters) => collaborationId =>
      getters.getMembershipIn(collaborationId).status,
    getGrantableResources: state => resourceType =>
      filter(state.resources,
        r => r.type === resourceType &&
          findIndex(r.allowedActions, { category: 'G' }) >= 0),
    getResourceDetails: state => (type, entityId) =>
      find(state.resources, { type, entityId }),
    getNumOfNotifications:
      state => state.numOfNotifications,
    getActionArray:
      state => state.actionArray,
    getResponseArray:
      state => state.responseArray,
    getScienceDomainLoadStatus:
      state => state.loadScienceDomains,
    getTotalScienceDomains:
      state => state.totalScienceDomains,
    getJoinedScienceDomains:
      state => state.joinedScienceDomains,
  },
  mutations: {
    setTotalScienceDomains(state, newState) {
      Vue.set(state, 'totalScienceDomains', newState);
    },
    setJoinedScienceDomains(state, newState) {
      Vue.set(state, 'joinedScienceDomains', newState);
    },
    loadScienceDomainStatus(state, newState) {
      Vue.set(state, 'loadScienceDomains', newState);
    },
    setSortingStatus(state, newState) {
      Vue.set(state, 'sortingStatus', newState);
    },
    loadingStatus(state, [message, stateOfMessage]) {
      Vue.set(state, 'loadingAction', message);
      Vue.set(state, 'loadingStatusIcon', stateOfMessage);
    },
    updateMessage(state, actionObject) {
      // actionStatus : To know if to display that object
      // actionRequired: To determine if that object requires action
      if (actionObject.actionRequired === 'True') {
        state.responseArray.push(actionObject);
      }
      if (actionObject.actionStatus === 'False') {
        for (let j = 0; j < state.actionArray.length; j += 1) {
          if (state.actionArray[j].actionFileName === actionObject.actionFileName) {
            state.actionArray.splice(j, 1);
          }
        }
      } else {
        state.actionArray.push(actionObject);
      }
      if (state.responseArray) {
        state.numOfNotifications = state.actionArray.length + state.responseArray.length;
      } else {
        state.numOfNotifications = state.actionArray.length;
      }
    },
    updateResponseArray(state, actionName) {
      for (let j = 0; j < state.responseArray.length; j += 1) {
        if (state.responseArray[j].actionFileName === actionName) {
          state.responseArray.splice(j, 1);
          state.numOfNotifications = state.actionArray.length + state.responseArray.length;
        }
      }
    },
    removeAllNotifications(state) {
      state.responseArray = [];
      if (state.actionArray) {
        state.numOfNotifications = state.actionArray.length;
      }
    },
    callAlert() {
      console.log('Alert');
    },
    setUserProfile(state, newProfile) {
      Vue.set(state, 'userProfile', newProfile);
    },
    setToken(state, newToken) {
      Vue.http.headers.common['X-Auth-Token'] = newToken;
      Vue.set(state, 'token', newToken);
    },
    setPublicGroups(state, groups) {
      state.publicGroups = groups;
    },
    setPublicUsers(state, users) {
      state.publicUsers = users;
    },
    updateGroups(state, groups) {
      state.groups = deepExtend({}, state.groups, groups);
    },
    setCollaborations(state, collaborations) {
      state.collaborations = collaborations;
    },
    setCollaborationIds(state, collaborationIds) {
      state.myCollaborations = collaborationIds;
    },
    setCollaborationLinks(state, collaborationLinks) {
      state.collaborationLinks = collaborationLinks;
    },
    setConfigUrls(state, configUrls) {
      state.configLinks = configUrls;
    },
    setUserActivities(state, userActivities) {
      state.userActivities = userActivities;
    },
    setResources(state, resources) {
      state.resources = resources;
    },
    startUpdatingUserInCollaboration(state, { collaborationId, user, newStatus, newRole }) {
      if (newStatus !== 'WITHDRAWN' && newStatus !== 'DECLINED') {
        if (!some(state.collaborations[collaborationId].members, { id: user.id })) {
          state.collaborations[collaborationId].members.push({
            id: user.id,
            role: newRole,
            status: newStatus,
          });
        } else {
          find(state.collaborations[collaborationId].members, { id: user.id }).role = newRole;
          find(state.collaborations[collaborationId].members, { id: user.id }).status = newStatus;
        }
      }

      Vue.set(find(state.collaborations[collaborationId].members, { id: user.id }), 'updating', true);
    },
    updateUsers(state, users) {
      state.users = deepExtend({}, state.users, users);
    },
    updateResources(state, resources) {
      state.resources = deepExtend({}, state.resources, resources);
    },
    startLoadingConfig(state) {
      switch (state.configLoadingStatus) {
        case 'NOTLOADING':
          state.configLoadingStatus = 'LOADING';
          break;
        case 'LOADED':
          state.configLoadingStatus = 'RELOADING';
          break;
        default:
          break;
      }
    },
    finishLoadingConfig(state) {
      state.configLoadingStatus = 'LOADED';
    },
    startLoadingUserActivities(state) {
      switch (state.userActivitiesStatus) {
        case 'NOTLOADING':
          state.userActivitiesStatus = 'LOADING';
          break;
        case 'LOADED':
          state.userActivitiesStatus = 'RELOADING';
          break;
        default:
          break;
      }
    },
    finishLoadingUserActivities(state) {
      state.userActivitiesStatus = 'LOADED';
    },
    startLoadingResources(state) {
      switch (state.resourceLoadingStatus) {
        case 'NOTLOADING':
          state.resourceLoadingStatus = 'LOADING';
          break;
        case 'LOADED':
          state.resourceLoadingStatus = 'RELOADING';
          break;
        default:
          break;
      }
    },
    finishLoadingResources(state) {
      state.resourceLoadingStatus = 'LOADED';
    },
    startLoadingCollaborations(state) {
      switch (state.collaborationLoadingStatus) {
        case 'NOTLOADING':
          state.collaborationLoadingStatus = 'LOADING';
          break;
        case 'LOADED':
          state.collaborationLoadingStatus = 'RELOADING';
          break;
        default:
          break;
      }
    },
    updateCollaborationLinks(state, response) {
      const racmUrlFormat = new URL(`${RACM_URL}`);
      const creategroupUrlFormat = new URL(response._links.createGroup.href);
      // Check if create group URL is different
      if (racmUrlFormat.host !== creategroupUrlFormat.host) {
        const collaborationList = response._embedded.collaborationList;
        // eslint-disable-next-line guard-for-in,no-restricted-syntax
        for (const eachCollab in collaborationList) {
          const tempLinks = collaborationList[eachCollab]._links;
          // eslint-disable-next-line guard-for-in,no-restricted-syntax
          for (const linkDescription in tempLinks) {
            const tempURLformat = new URL(tempLinks[linkDescription].href);
            const tempURL = tempLinks[linkDescription].href.replace(tempURLformat.host,
              racmUrlFormat.host);
            tempLinks[linkDescription].href = tempURL;
          }
        }
        const _links = response._links;
        // eslint-disable-next-line no-restricted-syntax,guard-for-in
        for (const eachLink in _links) {
            const tempURLformat = new URL(_links[eachLink].href);
            const tempURL = _links[eachLink].href.replace(tempURLformat.host,
              racmUrlFormat.host);
            _links[eachLink].href = tempURL;
        }
      }
      state.collaborationResponse = response;
    },
    finishLoadingCollaborations(state) {
      state.collaborationLoadingStatus = 'LOADED';
    },
    updateGroupLinkStack(state, lastBrowsed) {
      state.groupLinkStack = lastBrowsed;
    },
    setLinkedAccounts(state, linkedAccounts) {
      state.linkedAccounts = linkedAccounts;
    },
  },
  actions: {
    loadConfig: throttle(({ commit }) => {
      commit('startLoadingConfig');
      return Vue.http.get(`${RACM_URL}/config`)
        .then((response) => {
          commit('setConfigUrls', response.body);
          commit('finishLoadingConfig');
        });
    }, API_THROTTLE_TIME, { trailing: false }),
    loadResources: throttle(({ commit }) => {
      commit('startLoadingResources');
      return Vue.http.get(`${RACM_URL}/rest/resources/v2`)
        .then((response) => {
          commit('setResources', response.body);
          commit('finishLoadingResources');
        });
    }, API_THROTTLE_TIME, { trailing: false }),
    loadUserActivities: throttle(({ commit }) => {
      commit('startLoadingUserActivities');
      Vue.http.get(`${LOGGING_URL}/api/messages/applications?type=authentication&doShowInUserHistory=true&top=10`)
        .then((response) => {
          commit('setUserActivities', response.body);
          commit('finishLoadingUserActivities');
        });
    }, API_THROTTLE_TIME, { trailing: false }),
    loadCollaborations: throttle(({ commit }) => {
      commit('startLoadingCollaborations');
      return Vue.http.get(`${RACM_URL}/collaborations`, { headers: { Accept: '*' } })
        .then((response) => {
          commit('updateCollaborationLinks', response.body);
          response.body = initialState.collaborationResponse;
          commit('updateUsers', keyBy(get(response.body, '_embedded.userList'), 'id'));
          commit('setCollaborations', keyBy(get(response.body, '_embedded.collaborationList'), c => `${c._links.self.href}`));
          commit('setCollaborationIds', map(get(response.body, '_embedded.collaborationList'), '_links.self.href'));
          commit('setCollaborationLinks', response.body._links);
          commit('finishLoadingCollaborations');
        });
    }, API_THROTTLE_TIME, { trailing: false }),
    loadPublicGroupsAndUsers: throttle(({ commit }) =>
      Vue.http.get(`${RACM_URL}/ugm/rest/users/public`)
        .then((response) => {
          const user = new schema.Entity('users');
          const group = new schema.Entity('groups', {
            owner: user,
          });
          const normalizedData = normalize(response.body, {
            groups: [group],
            users: [user],
          });
          /* API sets these properties to blank for all objects: */
          Object.values(normalizedData.entities.groups).forEach((g) => {
            delete g.memberGroups;
            delete g.memberUsers;
          });
          commit('updateUsers', normalizedData.entities.users);
          commit('updateGroups', normalizedData.entities.groups);
          commit('setPublicGroups', normalizedData.result.groups);
          commit('setPublicUsers', normalizedData.result.users);
        }), API_THROTTLE_TIME, { trailing: false }),

    changeCollaborationMembership(
      { commit, dispatch }, { collaborationId, editMemberListEndpoint, user, newRole, newStatus }) {
      for (let i = 0; i < user.length; i += 1) {
        commit('startUpdatingUserInCollaboration', { collaborationId, user: user[i], newStatus, newRole });
      }
      return Vue.http.get(editMemberListEndpoint).then((response) => {
        const groupInfo = response.body;
        for (let i = 0; i < user.length; i += 1) {
          if (some(groupInfo.memberUsers, { userid: user[i].id })) {
            const membership = find(groupInfo.memberUsers, {
              userid: user[i].id,
            });
            if (newRole !== undefined) {
              membership.role = newRole;
            }
            if (newStatus !== undefined) {
              membership.status = newStatus;
            }
          } else {
            groupInfo.memberUsers.push({
              user: {
                username: user[i].username,
              },
              status: newStatus,
              role: newRole,
            });
          }
        }
        return Vue.http.post(`${RACM_URL}/ugm/rest/groups`, groupInfo);
      }).then(() => dispatch('loadCollaborations'));
    },
    loadLastGroupPath({ commit }, path) {
      commit('updateGroupLinkStack', path);
    },
    loadLinkedAccounts: throttle(({ commit }) =>
      Vue.http.get(`${LOGIN_PORTAL_URL}api/accounts`)
        .then((response) => {
          commit('setLinkedAccounts', response.data);
        }), API_THROTTLE_TIME, { trailing: false }),
  },
});
