// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue';
import Vuex from 'vuex';
import VueResource from 'vue-resource';
import * as uiv from 'uiv';
import Vue2Filters from 'vue2-filters';
import axios from 'axios';
import get from 'lodash/get';
import App from './App';
import store from './store';
import router from './router';
import handleError from './error-handler';
// eslint-disable-next-line import/prefer-default-export
export const EventBus = new Vue();

require('./allStyles.css');

Vue.use(VueResource);
Vue.use(uiv);
Vue.use(Vue2Filters);
Vue.use(Vuex);

axios.interceptors.response.use(undefined, (err) => {
  // Ignore axios errors if requesting user profile
  // We do this to avoid giving a visible error when testing
  // tokens on the first load
  if (err.request.responseURL === `${RACM_URL}/ugm/rest/user`) {
    return Promise.reject(err);
  }
  handleError(get(err, 'response.status'), get(err, 'request.responseURL'),
    err.data, err, get(err, 'response.statusText'));
  return Promise.reject(err);
});

Vue.http.interceptors.push(() =>
  (response) => {
    if (response.status < 299) return;
    let jsonResponse = null;
    try {
      jsonResponse = JSON.parse(response.body);
    } catch (ignored) {
      // ignore if the body is not valid json
    }
    handleError(response.status, response.url, jsonResponse, undefined, response.statusText);
  },
);

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  store,
  template: '<App/>',
  components: { App },
});
