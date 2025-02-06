<template>
  <modal v-model="open" class="my-sciserver-modal-dialog">
     <span slot="title">Files Service
       <a rel="noopener"  class= "contextualHints" target="_blank" :href="createUV" title="Help Creating User Volume"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">Help Creating User Volume</span>
					</a>
     </span>
    <form @submit.prevent>
      <div class="form-group">
          <label>User Volume Name</label>
          <input type="text" v-model="newVolumeName" class="form-control" placeholder="User Volume Name" @keyup.enter="createUserVolume" v-focus="focused" @focus="focused = true" @blur="focused = false">
      </div>
      <div class="form-group">
          <label>Description</label>
          <textarea class="form-control" rows="2" v-model="description" placeholder="Description" @keyup.enter="createUserVolume"></textarea>
      </div>
       <div class="form-group">
          <label>Root Volume: </label>
          <p class="help-block">Select a Root Volume to choose where this user volume is mounted. Different root volumes may have different data storage options.</p>
          <select v-model="selectedRootV" class="form-control" >
            <option disabled :value="{}">Select a Root Volume</option>
            <option v-for="rootV in sortedRootVolumes" :key="rootV.id" :value="rootV">
              {{ rootV.name }} - {{rootV.description}}
            </option>
          </select>
      </div>
      <span v-show="invalidUVName">User volume name cannot contain /.</span>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="createUserVolume" :disabled="!isValid || invalidUVName"> Create User Volume</button>
      <button type="button" class="btn btn-default" @click="open = false">Cancel</button>
    </div>
  </modal>
</template>
<script>
import includes from 'lodash/includes';
import sortBy from 'lodash/sortBy';
import { mapState, mapGetters } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';
import FilesTab from '../FilesTab';
import popUpDialog from '../sharedComponents/alertDialog';

export default {
  mixins: [focusMixin],
  components: {
      FilesTab,
      popUpDialog,
  },
  data: () => ({
    newVolumeName: '',
    description: '',
    open: false,
    selectedRootV: {},
    createUV: CREATE_UV,
    focused: false,
  }),
  computed: {
    // workspace root volumes does not have create permission
    hasCreatePerm() {
      return includes(this.myRootVolumeObjects.allowedActions, 'write');
    },
    isValid() {
      return this.newVolumeName && this.selectedRootV.name;
    },
    invalidUVName() {
      return this.newVolumeName.includes('/');
    },
    sortedRootVolumes() {
      return sortBy(this.myRootVolumeObjects.filter(x => includes(x.allowedActions, 'create')), 'name');
    },
    ...mapGetters(['myFileSystemObjects', 'myRootVolumeObjects']),
    ...mapState(['userProfile', 'token']),
  },
  methods: {
    startDialog() {
      this.newVolumeName = '';
      this.selectedRootV = {};
      this.description = '';
      this.focused = true;
      this.open = true;
    },
    createUserVolume() {
      if (!this.invalidUVName) {
        this.open = false;
        const newOwner = this.userProfile.username;
        const config = { headers: { 'X-Auth-Token': this.token } };
        const encodedUVName = encodeURIComponent(this.newVolumeName);
        const URL = `${this.selectedRootV.fileserviceObj.apiEndpoint}api/volume/${this.selectedRootV.name}/${newOwner}/${encodedUVName}`;
        this.$store.dispatch('createUV', { description: this.description, path: URL, config });
      }
    },
  },
};
</script>
