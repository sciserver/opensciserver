<template>
  <modal class="my-sciserver-modal-dialog" v-model="open" title="Share User Volumes">
    <form @submit.prevent>
      <div class="form-group">
        <label>User Volumes</label>
          <select v-model="selectedResources" class="form-control" v-focus="focused" @focus="focused = true" @blur="focused = false">
            <optgroup v-for="rootVolumeResources in uvResources"
            :label="`${rootVolumeResources[0].rootVolume.name} Volumes`"
            :key="rootVolumeResources[0].rootVolume.name">

            <option v-for="item in rootVolumeResources" :key="item.id" :value="item">
              {{ item.owner }}: {{ item.name }}
            </option>
          </optgroup>
        </select>
      </div>
    </form>
    <form @submit.prevent class="form-horizontal">
      <div class="form-group">
        <label class="col-sm-4 control-label">Allow this group to:</label>
        <div class="col-sm-8">
          <label class="radio">
            <input type="radio" name="privilege" value="read" v-model="checkedPrivilege">
            Read
          </label>
          <label class="radio">
            <input type="radio" name="privilege" value="read,write" v-model="checkedPrivilege">
            Read/Write
          </label>
          <label class="radio">
            <input type="radio" name="privilege" value="read,write,delete" v-model="checkedPrivilege">
            Read/Write and Delete this User Volume
          </label>
          <label class="radio">
            <input type="radio" name="privilege" value="read,write,delete,grant" v-model="checkedPrivilege">
            Read/Write/Delete and Share this User Volume
          </label>
        </div>
      </div>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="linkResources" :disabled="!isValid">Share</button>
      <button type="button" class="btn btn-default" @click="open = false">Cancel</button>
    </div>
  </modal>
</template>
<script>
import { mapGetters, mapActions } from 'vuex';
import firstBy from 'thenby';
import groupBy from 'lodash/groupBy';
import { mixin as focusMixin } from 'vue-focus';

export default {
  name: 'shareUserVolumeDialog',
  mixins: [focusMixin],
  computed: {
    uvResources() {
      window.tempStuff = this.resources;
      return groupBy(this.getGrantableResources('USERVOLUME')
        .sort(
          firstBy(uv => uv.fileService.name)
          .thenBy(uv => uv.rootVolume.name)
          .thenBy('owner')
          .thenBy('name')), 'rootVolume.name');
    },
    isValid() {
      return this.selectedResources && this.checkedPrivilege;
    },
    ...mapGetters(['getGrantableResources']),
  }, // computed
  data: () => ({
    selectedResources: undefined,
    checkedPrivilege: undefined,
    open: false,
    focused: false,
  }),
  methods: {
    startDialog() {
      this.loadResources();
      this.selectedResources = undefined;
      this.checkedPrivilege = undefined;
      this.focused = true;
      this.open = true;
    },
    linkResources() {
      this.$emit('linkUVResources', this.selectedResources, this.checkedPrivilege);
      this.open = false;
    },
    ...mapActions(['loadResources']),
  },
}; // default
</script>
