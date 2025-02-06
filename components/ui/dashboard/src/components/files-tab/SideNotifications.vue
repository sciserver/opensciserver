<template>
  <div style="width: 400px;">
    <h5 class="card-title" style="text-align:left; padding: 10px;">Notifications</h5>
    <div class="dropdown-divider"></div>
    <ul>
      <li v-if="numOfNotifications === 0">
        <div class="alert alert-dismissable" role="alert">
          <span class = "fa fa-check-circle-o"></span> No new notifications
        </div>
      </li>
      <li
        v-for="item in getActionArray" >
        <div class="alert alert-success" role="alert">
          <span class = "fa fa-cloud-upload"></span> {{ item.actionName }} {{ item.actionFileName }} to {{ item.actionLocation }} {{ item.actionurl }}
        </div>
      </li>
      <li class="list-group-item list-group-item-action list-group-item-danger"
          v-for="item in actionRequiredItems">
        <button type="button"  v-on:click="removeAction(item.actionFileName)" class="close" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
        Failed {{ item.actionName }}
        <router-link :to="`/files/${item.actionurl}`"> {{ item.actionFileName }}
        </router-link>
      </li>
    </ul>
    <h5 style="text-align: center; padding: 10px;"><a v-on:click="removeAction(item.actionFileName)">Clear All</a></h5>
  </div>
</template>

<script>
  // import Vue from 'vue';
  const $ = require('jquery');

  // eslint-disable-next-line import/first
  import { mapState, mapGetters } from 'vuex';

  export default {
    name: 'SideNotifications',
    computed: {
      ...mapState(['loadingUI', 'statusUpdate', 'numOfNotifications']),
      ...mapGetters(['getActionArray', 'getResponseArray']),
      actionRequiredItems() {
        return this.getResponseArray;
      },
    },
    mounted() {
      $().on('click', 'dropdown', (e) => {
        console.log('lol');
        e.stopPropagation();
      });
    },
    methods: {
      removeAction(actionName) {
        this.$store.commit('updateResponseArray', actionName);
      },
    },
  };
</script>

<style scoped>

</style>
