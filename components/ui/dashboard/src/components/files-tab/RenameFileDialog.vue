<template>
  <modal v-model="open" class="my-sciserver-modal-dialog" title="Rename">
    <form @submit.prevent>
      <div class="form-group">
          <label>New Name:</label>
          <input type="text" v-model="newName" class="form-control" :placeholder="initialName" @keyup.enter="confirmRename" v-focus="focused" @focus="focused = true" @blur="focused = false">
      </div>
      <span v-show="invalidFolderName">User volume name cannot contain /.</span>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" :disabled="invalidFolderName" @click="confirmRename">Rename</button>
      <button type="button" class="btn btn-default" @click="open = false">Cancel</button>
    </div>
  </modal>
</template>
<script>
import axios from 'axios';
import { mapState } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';
import filesUtils from '../../files-utils';

export default {
  mixins: [focusMixin],
  data: () => ({
    newName: '',
    initialName: '',
    open: false,
    focused: false,
  }),
  props: ['path', 'userVolume'],
  computed: {
    invalidFolderName() {
      return this.newName.includes('/');
    },
    ...mapState(['userProfile', 'token']),
  },
  methods: {
    startDialog(initialName) {
      this.newName = initialName;
      this.initialName = initialName;
      this.focused = true;
      this.open = true;
    },
    confirmRename() {
      if (!this.invalidFolderName) {
        this.open = false;
        const url = filesUtils.joinURLWithFileName(this.userVolume, 'api/data/', this.path, this.initialName);
        const config = { headers: { 'X-Auth-Token': this.token } };
        axios.put(url, {
          destinationPath: `${this.path}/${this.newName}`,
          destinationRootVolume: this.userVolume.type === 'uservolumes' ? this.userVolume.rootVolumeObj.name : null,
          destinationOwnerName: this.userVolume.type === 'uservolumes' ? this.userVolume.owner : null,
          destinationUserVolume: this.userVolume.type === 'uservolumes' ? this.userVolume.name : null,
          destinationDataVolume: this.userVolume.type === 'uservolumes' ? null : this.userVolume.name,
          destinationFileService: null,
        }, config)
        .then(() => {
          this.$emit('fileRenamed');
        }, (error) => {
          alert(`Error Message: ${error.response.data.error}`);
        });
      }
    },
  },
};
</script>
