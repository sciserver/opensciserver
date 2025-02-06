<template>
  <div>
    <ul class= "nav navbar-nav navbar-right">
      <li class="dropdown">
        <a href="#" @click="toggle">
          <span class = "fa fa-bell-o"> </span>
          <span v-if="numOfNotifications > 0" class = "num"> {{ getNumOfNotifications }}</span>
        </a>
      </li>
    </ul>
    <Drawer @close="toggle" align="right" :closeable="false" @click="toggle" :maskClosable="true">
      <div v-if="open">
        <ul>
          <li>
            <span class="label">Notifications</span>
            <button a v-on:click="removeAll" type="button" class="btn btn-danger btn-sm pull-right" style="margin-top: 6px;">Clear All</button>
          </li>
        </ul>
        <br>
        <ul v-if="numOfNotifications === 0">
          <li>
            <div class="alert alert-info" role="alert">
              <span>No new notifications</span>
            </div>
          </li>
        </ul>
        <ul v-for="item in getActionArray">
            <div class="alert alert-success" role="alert">
              <span class = "fa fa-cloud-upload"></span> {{ item.actionName }} {{ item.actionFileName }} <router-link v-if="item.actionurl" :to="`/files/${item.actionurl}`"> to {{ item.actionLocation }}</router-link>
            </div>
          <br>
        </ul>
        <ul v-for="item in actionRequiredItems">
          <div class="alert alert-danger" role="alert">
            Failed {{ item.actionName }}<router-link :to="`/files/${item.actionurl}`"> {{ item.actionFileName }}</router-link>
            . {{ item.actionMessage }}
            <button type="button"  v-on:click="removeAction(item.actionFileName)" class="close" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <br>
        </ul>
      </div>
    </Drawer>
  </div>
</template>

<script>
  // import Vue from 'vue';
  import Drawer from 'vue-simple-drawer';

  // eslint-disable-next-line import/first
  import { mapState, mapGetters } from 'vuex';

    export default {
        name: 'NotificationsTab',
        data: () => ({
          open: false,
        }),
        components: {
          Drawer,
        },
        computed: {
            ...mapState(['loadingUI', 'statusUpdate', 'numOfNotifications']),
            ...mapGetters(['getActionArray', 'getResponseArray', 'getNumOfNotifications']),
          actionRequiredItems() {
              return this.getResponseArray;
          },
        },
        methods: {
            toggle() {
              this.open = !this.open;
            },
            removeAction(actionName) {
              this.$store.commit('updateResponseArray', actionName);
            },
            removeAll() {
              this.$store.commit('removeAllNotifications');
            },
          },
    };
</script>

<style scoped>
  span.num {
    position: absolute;
    /* font-size: 0.3em; */
    top: 2.5px;
    /* color: white; */
    background: darkred;
    text-align: center;
    /* border-bottom-left-radius: inherit; */
    border-radius: 100%;
    width: 50%;
    padding-left: -2px;
    right: 1.5px;
  }
  .label {
    display: inline-block;
    font-size: 160%;
    text-align: left;
    padding-left: 0px;
  }
</style>
