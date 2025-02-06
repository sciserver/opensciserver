<template>
  <div id="app">
    <!-- Navigation -->
    <nav class="navbar navbar-default" role="navigation" style="margin-bottom: 0px; border-radius: 0px;">
      <div class="navbar-header">
        <span class="navbar-brand">
          <a :href="applicationHomeUrl" :title="'Go to ' + applicationName" rel="noopener" target="_blank">
            <img :alt="applicationName + ' logo'" src="./assets/sciserverlogo.png"
              style="max-height: 32px; margin-top: -6px">
          </a>
        </span>
        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#page-navbar"
          aria-expanded="false">
          <span class="sr-only">Toggle Navigation</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
      </div>
      <div class="collapse navbar-collapse" id="page-navbar">
        <ul class="nav navbar-nav">
          <li><router-link to="/" title="Home tab">Home</router-link></li>
          <li v-if="isDisplayFilesTab"><router-link to="/files" title="Files tab">Files</router-link></li>
          <li v-if="isDatasetsURLValid"><a :href="this.datasets_URL" title="Datasets">Datasets</a></li>
          <li><router-link to="/groups" title="Groups tab">Groups</router-link></li>
          <li v-if="getScienceDomainLoadStatus"><router-link to="/science" title="Science Domains">Science
              Domains</router-link></li>
          <li v-if="isAdmin" class="dropdown">
            <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true"
              aria-expanded="false">Admin <span class="caret"></span></a>
            <ul class="dropdown-menu">
              <li><router-link to="/admin/compute">Compute</router-link></li>
            </ul>
          </li>
          <li><router-link to="/resource" v-if="isAdmin" title="Resources tab">Resources</router-link></li>
        </ul>
        <notifications position="top center" group="top_center_notify" style="margin-top: 6px;" width="200" />
        <ul v-if="isLoggedIn" class="nav navbar-nav navbar-right">
          <li class="dropdown">
            <a href="#" title="User Profile DropDown" class="dropdown-toggle" data-toggle="dropdown" role="button"
              aria-haspopup="true" aria-expanded="false">
              {{ userProfile.username }}
              <span class="caret"></span>
            </a>
            <ul class="dropdown-menu">
              <li><a href="#" title="View Profile" @click="openProfile">My Account</a></li>
              <li><a href="#" title="Help" @click="openSupport">Help</a></li>
              <li role="separator" class="divider"></li>
              <!-- If we aren't using SciServer-native logins, changing passwords is meaningless. -->
              <li v-if="displaySciserverLogin"><a href="#" title="Change your password" @click="changePassword">Change Password</a></li>
              <li><a :href="signOutLink" title="Sign Out">Sign Out</a></li>
            </ul>
          </li>
        </ul>
        <ul class="nav navbar-nav navbar-right">
          <li><router-link to="/activity_log" title="Activity Log"><span class="fa fa-history fa-lg"
                aria-hidden="true"></span></router-link></li>
        </ul>
        <ul class="nav navbar-nav navbar-right">
          <li class="dropdown">
            <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true"
              aria-expanded="false" title="SciServer Apps">
              <span class="fa fa-th fa-lg" aria-hidden="true"></span>
            </a>
            <ul class="dropdown-menu" id="sciserverLinks">
              <li class="text-left">
                <router-link to="/" title="Home tab">
                  <div class="alert alert-primary" role="alert">
                    <img alt="SciServer logo" src="./assets/sciserver_logo_icon_blue.png"
                      style="max-height: 32px; margin-top: -6px; margin-right: 10px"> Home
                  </div>
                </router-link>
              </li>
              <li v-for="app in appTiles" class="text-left">
                <a :href="app.serviceUrl" rel="noopener" :title="`Go to ${app.name}`" target="_blank">
                  <div class="alert alert-primary" role="alert">
                    <img v-if="'iconUrl' in app" :alt="app.name" :src="app.iconUrl"
                      style="max-height: 32px; margin-top: -6px; margin-right: 10px" />
                    <img v-else-if="'staticIcon' in app" :alt="app.name" :src="staticImages[app.staticIcon]"
                      style="max-height: 32px; margin-top: -6px; margin-right: 10px" />
                    <span v-else :class="app.glyph || 'fa fa-asterisk fa-3x'"
                      style="color:black; vertical-align:middle" />
                    {{ app.name }}
                  </div>
                </a>
              </li>
            </ul>
          </li>
        </ul>
        <ul class="nav navbar-nav navbar-right" style="padding-right: 15px;">
          <notification-tab></notification-tab>
        </ul>
      </div>
    </nav>
    <change-password ref="changePassword"></change-password>
    <profile-dialog ref="profileDialog" />
    <support-dialog ref="supportDialog" />

    <!--Collapsible side menu-->

    <!--End of collapsible-->
    <main>
      <div v-if="alertMessage" :class="alertClasses">
        {{ alertMessage }}
      </div>
      <router-view />
    </main>
    <footer class="footer">
      <span> {{ applicationName }} – {{ sciserver_Version }} <i class="text-muted">Dashboard - {{ version }} </i> </span>
      <span class="logo-holder">
        <span> Powered by: </span>
        <a class="logo" href="https://www.nsf.gov" rel="noopener" title="Go to National Science Foundation Website"
          target="_blank">
          <img style="max-height: 50px" src="./assets/nsf1.gif" alt="National Science Foundation" />
        </a>
        <a class="logo" href="http://idies.jhu.edu" rel="noopener" title="Go to IDIES Website" target="_blank">
          <img style="max-height: 24px" src="./assets/idies-logo-big3.png" alt="IDIES" />
        </a>
        <a class="logo" href="https://jhu.edu" rel="noopener" title="Go to Johns Hopkins University Website"
          target="_blank">
          <img style="max-height: 32px" src="./assets/jhulogo.png" alt="Johns Hopkins University" />
        </a>
      </span>
    </footer>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex';
import { Slide } from 'vue-burger-menu';
import Drawer from 'vue-simple-drawer';
import changePassword from './components/userSettingsComponents/changePassword';
import profileDialog from './components/userSettingsComponents/profileDialog';
import supportDialog from './components/userSettingsComponents/supportDialog';
import LoadingStatus from './components/files-tab/LoadingStatus';
import NotificationTab from './components/files-tab/Notifications';
import SideNotification from './components/files-tab/SideNotifications';
import applicationConfiguration from './applicationConfiguration';

const jQuery = require('jquery');

window.$ = jQuery;
window.jQuery = jQuery;

require('datatables.net');
require('datatables.net-select');
require('jquery-datetimepicker');
require('jquery-tablesort');
require('stupid-table-plugin');
require('jquery-ui/ui/widgets/tabs');
require('bootstrap');
require('./allStyles.css');
require('../static/css/dashboard.css');
require('jquery-datetimepicker/jquery.datetimepicker.css');
require('datatables.net-select-dt/css/select.dataTables.css');
require('natural-orderby');

export default {
  name: 'app',
  mixins: [applicationConfiguration],
  updated() {
    this.assignCSS();
  },
  created() {
    this.assignCSS();
  },
  mounted() {
  },
  computed: {
    isDisplayFilesTab() {
      return this.$store.state.files.hasFileService;
    },
    nameIcon() {
      return 'sorted';
    },
    isLoggedIn() {
      return this.token !== undefined;
    },
    signOutLink() {
      const href = this.$router.resolve({ route: this.$route }).href;
      const callback = `${window.location.origin}${href}`;
      return `${LOGIN_PORTAL_URL}/logout?callbackUrl=${callback}`;
    },
    isDatasetsURLValid() {
      return this.datasets_URL !== null && this.datasets_URL !== undefined && this.datasets_URL !== '';
    },
    ...mapGetters(['isAdmin', 'getNumOfNotifications', 'getScienceDomainLoadStatus']),
    ...mapState(['userProfile', 'token', 'numOfNotifications']),
  },
  components: {
    Slide,
    Drawer,
    LoadingStatus,
    changePassword,
    profileDialog,
    supportDialog,
    NotificationTab,
    SideNotification,
  },
  data: () => ({
    open: false,
  }),
  methods: {
    toggle() {
      this.open = !this.open;
    },
    openNav() {
      jQuery('#mySidebar').style.width = '250px';
      jQuery('#app').style.width = '250px';
    },
    closeNav() {
      jQuery('#mySidebar').style.width = '0';
      jQuery('#app').style.width = '0';
    },
    openProfile() {
      this.$refs.profileDialog.startDialog();
    },
    openSupport() {
      this.$refs.supportDialog.startDialog();
    },
    changePassword() {
      this.$refs.changePassword.startDialog();
    },
  },
};
</script>
<style scoped>
sorted:before {
  color: pink;
  font: normal normal normal 12px/1 FontAwesome;
}

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

.vue-notification {
  padding: 10px;
  margin: 0 5px 5px;

  font-size: 12px;

  color: #ffffff;
  background: #44A4FC;
  border-left: 5px solid #187FE7;

  .warn {
    background: #ffb648;
    border-left-color: #f48a06;
  }

  .error {
    background: #E54D42;
    border-left-color: #B82E24;
  }

  .success {
    background: #68CD86;
    border-left-color: #42A85F;
  }
}
</style>
