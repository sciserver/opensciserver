<template>
  <modal v-model="open" class="my-sciserver-modal-dialog" title="Create Folder">
    <form @submit.prevent>
      <div class="form-group">
          <label>Folder Name</label>
          <input type="text" v-model="newFolderName" class="form-control" placeholder="New Folder Name" @keyup.enter="createSubFolder" v-focus="focused" @focus="focused = true" @blur="focused = false">
          <span v-show="invalidUVName">Folder name cannot contain /.</span>
      </div>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="createSubFolder()" :disabled="invalidUVName">Create Folder</button>
      <button type="button" class="btn btn-default" @click="open = false">Cancel</button>
    </div>
  </modal>
</template>
<script>
import axios from 'axios';
import Vue from 'vue';
import { mapState } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';
import filesUtils from '../../files-utils';

export default {
  mixins: [focusMixin],
  data: () => ({
    newFolderName: '',
    open: false,
    focused: false,
  }),
  props: ['userVolume', 'path'],
  computed: {
    invalidUVName() {
      return this.newFolderName.includes('/');
    },
    ...mapState(['userProfile', 'token']),
  },
  methods: {
    startDialog() {
      this.newFolderName = '';
      this.focused = true;
      this.open = true;
    },
    createSubFolder() {
      this.open = false;
      const config = { headers: { 'X-Auth-Token': this.token } };
      const url = filesUtils.joinURLWithFileName(this.userVolume, 'api/folder/', this.path, this.newFolderName);
      axios
        .put(url, '', config)
        .then(() => {
          this.$emit('newSubFolderCreated');
          Vue.notify({
            group: 'top_center_notify',
            text: 'Successfully created new folder',
            duration: 1000,
            type: 'success',
          });
        }, (error) => {
          Vue.notify({
            group: 'top_center_notify',
            text: `${error.response.data.error}`,
            duration: 1000,
            type: 'warn',
          });
        });
      this.newFolderName = '';
      this.$forceUpdate();
    },
  },
};
</script>
