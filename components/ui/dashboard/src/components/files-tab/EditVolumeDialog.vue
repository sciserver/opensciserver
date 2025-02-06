<template>
  <modal v-model="open" class="my-sciserver-modal-dialog" title="Edit User Volume">
    <form @submit.prevent>
      <div class="form-group">
          <label>User Volume Name</label>
          <input type="text" v-model="newVolumeName" class="form-control" :placeholder="initialVolumeName" @keyup.enter="editUserVolume" v-focus="focused" @focus="focused = true" @blur="focused = false">
      </div>
      <div class="form-group">
          <label>Description</label>
          <textarea v-model="newVolumeDescription" class="form-control" rows="2" :placeholder="initialVolumeDescription" @keyup.enter="editUserVolume"></textarea>
      </div>
      <span v-show="invalidUVName">User volume name cannot contain /.</span>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="editUserVolume()" :disabled="invalidUVName">Save Changes</button>
      <button type="button" class="btn btn-default" @click="open = false">Cancel</button>
    </div>
  </modal>
</template>
<script>
import axios from 'axios';
import { mapState } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';

export default {
  mixins: [focusMixin],
  data: () => ({
    userVolume: {},
    newVolumeName: '',
    newVolumeDescription: '',
    initialVolumeName: '',
    initialVolumeDescription: '',
    focused: false,
    open: false,
  }),
  computed: {
    invalidUVName() {
      return this.newVolumeName.includes('/');
    },
    ...mapState(['userProfile', 'token']),
  },
  methods: {
    startDialog(volume) {
      this.userVolume = volume;
      const initialVolumeName = volume.name;
      const initialVolumeDescription = volume.description;
      this.newVolumeName = initialVolumeName;
      this.initialVolumeName = initialVolumeName;
      this.newVolumeDescription = initialVolumeDescription;
      this.initialVolumeDescription = initialVolumeDescription;
      this.focused = true;
      this.open = true;
    },
    editUserVolume() {
      if (!this.invalidUVName) {
        this.open = false;
        const config = { headers: { 'X-Auth-Token': this.token } };
        axios
        .patch(`${this.userVolume.apiEndpoint}api/volume/${this.userVolume.rootVolumeObj.name}/${this.userVolume.owner}/${encodeURIComponent(this.initialVolumeName)}`,
        { name: this.newVolumeName, description: this.newVolumeDescription },
        config)
        .then(() => {
          this.$emit('userVolumeEdited');
        }, (error) => {
          alert(`Error Message: ${error.response.data.error}`);
        });
      }
    },
  },
};
</script>
