<template>
<div v-if="linkedAccounts.length">
    <ul id="settings-emails" v-for="account in linkedAccounts" :key="account.key" class="Box list-style-none">
    <li class="Box-row clearfix css-truncate settings-email">
      <span class="css-truncate-target" :title="account.value">{{account.value}}</span>
      <span class="email-actions">
          <button class="btn-link settings-remove-email" title="unlink account" @click="unlinkAccount(account.key, account.value)"><span class="fa fa-trash fa-lg"></span></button>
      </span>
    </li>
  </ul>
  <p class="note">
      To link other accounts, <a :href="signOutLink" class="btn-link">logout</a> from the dashboard. In the login screen, click 'Sign in with Globus' button.
    </p>
  </div>
  <div v-else>
      <label>There are no linked accounts.</label>
      <p class="note">
      To link other accounts, <a :href="signOutLink" class="btn-link">logout</a> from the dashboard. In the login screen, click 'Sign in with Globus' button.
    </p>
  </div>
</template>
<script>
import axios from 'axios';
import { mapState } from 'vuex';

export default {
  name: 'manageLinkedAccount',
  data: () => ({}),
  computed: {
    signOutLink() {
      const href = this.$router.resolve({ route: this.$route }).href;
      const callback = `${window.location.origin}${href}`;
      return `${LOGIN_PORTAL_URL}/logout?callbackUrl=${callback}`;
    },
    ...mapState(['linkedAccounts']),
  },
  created() {
    this.getLinkedAccounts();
  },
  methods: {
    getLinkedAccounts() {
      this.$store.dispatch('loadLinkedAccounts');
    },
    unlinkAccount(accountID, accountName) {
      const config = { headers: { 'X-Auth-Token': this.$store.state.token, 'Content-Type': 'application/json', Accept: '*/*' } };
      const url = `${LOGIN_PORTAL_URL}api/accounts/${accountID}`;
      if (confirm(`Are you sure you want to unlink ${accountName}?`)) {
        axios.delete(url, config).then(() => {
          this.getLinkedAccounts();
        });
      }
    },
  },
};
</script>
<style scoped>
.Box {
    background-color: #fff;
    border: 1px solid #d1d5da;
    border-radius: 5px;
}
ol, ul {
    margin-bottom: 0;
    margin-top: 0;
    padding-left: 0;
}
.Box-row {
    border-top: 2px solid #e1e4e8;
    margin-top: -1px;
    padding: 10px;
}
.email-actions {
    float: right;
}
.settings-email .css-truncate-target {
    max-width: 300px;
}
.css-truncate.css-truncate-target, .css-truncate .css-truncate-target {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    vertical-align: top;
    white-space: nowrap;
}
.settings-email .email-actions .settings-remove-email {
    color: #cb2431;
    cursor: pointer;
    float: right;
    line-height: 24px;
    margin-left: 5px;
    padding-left: 7px;
    padding-right: 7px;
}
.btn-link {
    -moz-appearance: none;
    -moz-user-select: none;
    -ms-user-select: none;
    -webkit-appearance: none;
    -webkit-user-select: none;
    appearance: none;
    background-color: transparent;
    border: 0;
    color: #0366d6;
    cursor: pointer;
    display: inline-block;
    font-size: inherit;
    padding: 0;
    text-decoration: none;
    user-select: none;
    white-space: nowrap;
}
.note {
    color: #586069;
    font-size: 12px;
    margin: 4px 0 2px;
    min-height: 17px;
}
</style>
