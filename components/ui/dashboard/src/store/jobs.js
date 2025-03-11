import Vue from 'vue';
import throttle from 'lodash/throttle';

/* All API calls that are throttled will
wait at least this many milliseconds between calls */
const API_THROTTLE_TIME = 2000;

export default {
  state: {
    jobs: [],
  },
  mutations: {
    setJobs(state, jobsList) {
      state.jobs = jobsList;
    },
  },
  actions: {
    loadJobs: throttle(({ commit }) =>
      Vue.http.get(`${RACM_URL}/jobm/rest/jobsstats?since=24`)
        .then((response) => {
          commit('setJobs', response.data);
        }), API_THROTTLE_TIME, { trailing: false }),
  },
};
