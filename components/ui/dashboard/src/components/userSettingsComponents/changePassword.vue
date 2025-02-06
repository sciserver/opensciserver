<template>
 <modal v-model="open" class="my-sciserver-modal-dialog" size="sm">
   <span slot="title"> Change Password
        <a rel="noopener" class= "contextualHints" target="_blank" :href="changePassLink" title="More Info on Groups" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">More Info on Groups</span>
				</a>
     </span>
    <form @submit.prevent>
      <div class="form-group">
             New Password
              <input name="changePass1" type="password" v-model="password1" class="form-control" id="changePass1" v-focus="focused" @focus="focused = true" @blur="focused = false"/>
              Confirm New Password
              <input name="changePass2" type="password" v-model="password2" class="form-control" id="changePass2" @keyup.enter="changePassword"/>
             <input type="checkbox" v-model="showPass" @click="showHidePass"><span v-if="showPass">Hide</span> <span v-if="!showPass">Show</span> Password
             <span v-show="!pass1EqualPass2">**Password must match</span>
             <span v-show="!validPassword">Your password must be a minimum of 8 characters long.</span>
      </div>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="changePassword" data-dismiss="modal" :disabled="!pass1EqualPass2 || blankPassField || !validPassword">Update Password</button>
      <button type="button" class="btn btn-default" data-dismiss="modal" @click="clearAll">Cancel</button>
    </div>
  </modal>
</template>
<script>
import axios from 'axios';
import { mapState, mapGetters } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';

export default {
  name: 'changePassword',
  mixins: [focusMixin],
  computed: {
    signOutLink() {
      return `${LOGIN_PORTAL_URL}/logout?callbackUrl=${window.location
        .href}`;
    },
    pass1EqualPass2() {
      return this.password1 === this.password2;
    },
    blankPassField() {
      return this.password1 === '' || this.password2 === '';
    },
    resetPassword() {
      return `${LOGIN_PORTAL_URL}/change-password`;
    },
    validPassword() {
      const re = new RegExp('.{8,}');
      return re.test(this.password1);
    },
    ...mapGetters(['isAdmin']),
    ...mapState(['userProfile', 'token']),
  },
  data: () => ({
    password1: '',
    password2: '',
    showPass: false,
    changePassLink: CHANGE_PASSWORD,
    open: false,
    focused: false,
  }),
  methods: {
    startDialog() {
      this.password1 = '';
      this.password2 = '';
      this.showPass = false;
      this.focused = true;
      this.open = true;
    },
    changePassword() {
      const config = { headers: { 'X-Auth-Token': this.$store.state.token, 'Content-Type': 'application/json', Accept: '*/*' } };
      const url = `${LOGIN_PORTAL_URL}api/password`;
      axios.post(url, { password: this.password2 }, config).then(this.redirect, this.error);
    },
    redirect() {
      this.clearAll();
      alert('Your password has been updated successfully. You will be redirected to log in page.');
      window.location.replace(this.signOutLink);
    },
    clearAll() {
      this.password1 = '';
      this.password2 = '';
      this.open = false;
    },
    showHidePass() {
      const x = document.getElementById('changePass1');
      const y = document.getElementById('changePass2');
      if (x.type === 'password') {
        x.type = 'text';
      } else {
        x.type = 'password';
      }
      if (y.type === 'password') {
        y.type = 'text';
      } else {
        y.type = 'password';
      }
    },
  },
};
</script>