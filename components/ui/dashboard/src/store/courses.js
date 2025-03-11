import Vue from 'vue';
import Vuex from 'vuex';
import throttle from 'lodash/throttle';

Vue.use(Vuex);

/* All API calls that are throttled will
wait at least this many milliseconds between calls */
const API_THROTTLE_TIME = 2000;

export default {
  state: {
    userProfile: {},
    courses: {},
    selectedCourse: [],
    coursesLoadingStatus: 'NOTLOADED',
  },
  getters: {
  },
  mutations: {
    setCourses(state, courses) {
      state.courses = courses;
    },
    setCourseByID(state, course) {
      state.selectedCourse = course;
    },
    startLoadingCourses(state) {
      switch (state.coursesLoadingStatus) {
        case 'NOTLOADING':
          state.coursesLoadingStatus = 'LOADING';
          break;
        case 'LOADED':
          state.coursesLoadingStatus = 'RELOADING';
          break;
        default:
          break;
      }
    },
    finishLoadingCourses(state) {
      state.coursesLoadingStatus = 'LOADED';
    },
  },
  actions: {
    // works
    loadCourses: throttle(({ commit }) => {
      commit('startLoadingCourses');
      return Vue.http.get(`${COURSEWARE_URL}/courses`, { headers: { Accept: '*' } })
          .then((response) => {
            commit('setCourses', response.body);
            commit('finishLoadingCourses');
          });
    }, API_THROTTLE_TIME, { trailing: false }),
    // works
    loadCourseByID: ({ commit }, courseID) =>
      Vue.http.get(`${COURSEWARE_URL}/course/${courseID}`, { headers: { Accept: '*' } }).then((response) => {
        commit('setCourseByID', response.body);
      }),
    editCourse({ dispatch }, { courseID, newGroupInfo }) {
      Vue.http.patch(`${COURSEWARE_URL}/course/${courseID}`, newGroupInfo).then(() => {
        dispatch('loadCourses');
        dispatch('loadCourseByID');
      });
    },
    editMember({ dispatch }, { courseID, newMembersList }) {
      Vue.http.post(`${COURSEWARE_URL}/courses/${courseID}`, newMembersList).then(() => {
        dispatch('loadCourses');
      });
    },
    shareResources({ dispatch }, { courseID, resourceType, permissions, entityId }) {
      Vue.http.put(`${COURSEWARE_URL}/courses/${courseID}/sharedResources?resourceType=${resourceType}&actions=${permissions}&entityId=${entityId}`).then(() => {
        dispatch('loadCourses');
      });
    },
    // works
    createCourse({ dispatch }, { course }) {
      Vue.http.post(`${COURSEWARE_URL}/course`, course).then(() => {
        dispatch('loadCourses');
      });
    },
  },
};
