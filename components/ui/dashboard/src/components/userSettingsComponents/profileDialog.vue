<template>
 <modal v-model="open" class="my-sciserver-modal-dialog" title="My Account" size="lg" auto-focus>
<div class="row">
  <div class="col-sm-4">
      <listGroup :title="`Account Settings`" :headingType="`default`">
        <a href="#" class="list-group-item" v-bind:class="{ 'active' : isSelected(0) }" v-on:click="selected = 0">Profile</a>
        <!-- If we aren't using SciServer-native logins, allowing users to unlink their Keycloak (i.e., Globus) account
            will cause errors and changing passwords is meaningless. -->
        <a v-if="displaySciserverLogin" href="#" class="list-group-item" v-bind:class="{ 'active' : isSelected(1) }" v-on:click="selected = 1">Manage Linked Globus Account</a>
        <a v-if="displaySciserverLogin" href="#" class="list-group-item" v-bind:class="{ 'active' : isSelected(2) }" v-on:click="selected = 2">Change Password</a>
      </listGroup>
  </div>
  <div class="col-sm-8">
      <div  v-if="selected === 0">
        <div class="Subhead mt-0 mb-0">
    <h3 id="public-profile-heading" class="Subhead-heading">My Profile</h3>
  </div>
          <form @submit.prevent>
      <div class="form-group">
              User name:
              <input name="username" type="text" class="form-control" id="username" readonly="readonly" :value="userProfile.username"/> Contact email:
              <input name="email" type="email" class="form-control" id="email" readonly="readonly" :value="userProfile.contactEmail"/> Full name:
              <input name="fullname" type="text" v-model="fullName" class="form-control" id="fullname" :placeholder="initialFullName" v-focus="focused" @focus="focused = true" @blur="focused = false"/> Affiliation:
              <input name="affiliation" type="text" v-model="affiliation" class="form-control" id="affiliation" :placeholder="initialAffiliation"/>
              <template v-if="isAdmin">
                Token:
                <input name="token" type="text" class="form-control" readonly="readonly" :value="token">
              </template>
      </div>
    </form>
    <button type="button" class="btn btn-primary" id="saveProfileButton" @click="saveProfile">Update profile</button>
      </div>
      <div v-if="selected === 1">
          <div class="Subhead mt-0 mb-0">
    <h3 id="public-profile-heading" class="Subhead-heading">Manage Linked Globus Accounts</h3>
          </div>
          <manage-linked-accounts></manage-linked-accounts>
      </div>
       <div v-if="selected === 2">
        <div class="Subhead mt-0 mb-0">
          <h3 id="public-profile-heading" class="Subhead-heading">Change Password</h3>
        </div>
          <change-user-password></change-user-password>
        </div>
  </div>
</div>
    <div slot="footer">
        <button type="button" class="btn btn-default" @click="open = false">Close</button>
    </div>
  </modal>
</template>
<script>
import { mapState, mapGetters } from 'vuex';
import axios from 'axios';
import userProfileGetter from '@/user-profile-getter';
import { mixin as focusMixin } from 'vue-focus';
import changeUserPassword from './changeUserPassword';
import manageLinkedAccounts from './manageLinkedAccount';
import listGroup from '../sharedComponents/listGroup';

const jQuery = require('jquery');

export default {
  name: 'profileDialog',
  mixins: [focusMixin],
  components: {
    changeUserPassword,
    manageLinkedAccounts,
    listGroup,
  },
  computed: {
    manageLinkedAccountURL() {
      return `${LOGIN_PORTAL_URL}linked-accounts`;
    },
    displaySciserverLogin() {
      return `${DISPLAY_SCISERVER_LOGIN}` === 'true';
    },
    ...mapGetters(['isAdmin']),
    ...mapState(['userProfile', 'token', 'configLinks']),
  },
  data: () => ({
    fullName: '',
    initialFullName: '',
    affiliation: '',
    initialAffiliation: '',
    open: false,
    focused: false,
    selected: 0,
  }),
  methods: {
    startDialog() {
      this.focused = true;
      this.open = true;
      this.fullName = this.userProfile.fullname;
      this.initialFullName = this.userProfile.fullname;
      this.affiliation = this.userProfile.affiliation;
      this.initialAffiliation = this.userProfile.affiliation;
    },
    saveProfile() {
      const updatedduser = jQuery.extend({}, this.userProfile);
      updatedduser.fullname = this.fullName;
      updatedduser.affiliation = this.affiliation;
      const body = JSON.stringify(updatedduser);
      const config = { headers: { 'X-Auth-Token': this.token, 'Content-Type': 'application/octet-stream' } };
      axios.post(this.configLinks.manageUserProfileUrl, body, config)
      .then(this.reloadProfile, this.error);
    },
    reloadProfile() {
      userProfileGetter().then(({ userProfile }) => {
        this.$store.commit('setUserProfile', userProfile);
      });
    },
    isSelected(i) {
      return i === this.selected;
    },
  },
};
</script>
<style scoped>
.Subhead {
    border-bottom: 1px solid #e1e4e8;
    display: flex;
    flex-flow: row wrap;
    margin-bottom: 16px;
    padding-bottom: 8px;
}
.Subhead-heading {
    flex: 1 1 auto;
    font-size: 24px;
    font-weight: 400;
}
.menu-heading {
    background-color: #f3f5f8;
    border-bottom: 1px solid #e1e4e8;
    color: #586069;
    display: block;
    font-size: 13px;
    font-weight: 600;
    line-height: 20px;
    margin-bottom: 0;
    margin-top: 0;
    padding: 8px 10px;
}
.menu {
    background-color: #fff;
    border: 1px solid #d1d5da;
    border-radius: 3px;
    list-style: none;
    margin-bottom: 15px;
}
</style>
