<template>
  <modal class="my-sciserver-modal-dialog" v-model="open" title="Share Compute Images">
    <form @submit.prevent>
      <div class="form-group">
        <label>Compute Images</label>
          <select v-model="selectedResources" class="form-control" v-focus="focused" @focus="focused = true" @blur="focused = false">
            <optgroup v-for="domainResources in dockerResources"
            :label="domainResources[0].dockerComputeDomain.name"
            :key="domainResources[0].dockerComputeDomain.name">

            <option v-for="item in domainResources" :key="item.entityId" :value="item">
              {{ item.name }}
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
            <input type="radio" name="privilege" value="createContainer" v-model="checkedPrivilege">
            Create Containers
          </label>
          <label class="radio">
            <input type="radio" name="privilege" value="createContainer,grant" v-model="checkedPrivilege">
            Create Containers and Share this Image
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
  name: 'ShareDockerImages',
  mixins: [focusMixin],
  computed: {
    dockerResources() {
      return groupBy(this.getGrantableResources('DOCKERIMAGE').sort(
        firstBy(di => di.dockerComputeDomain.name)
        .thenBy('name')),
        'dockerComputeDomain.name');
    },
    isValid() {
      return this.selectedResources && this.checkedPrivilege;
    },
    ...mapGetters(['getGrantableResources']),
  }, // computed
  created() {},
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
      this.$emit('linkDIResources', this.selectedResources, this.checkedPrivilege);
      this.open = false;
    },
    ...mapActions(['loadResources']),
  },
}; // default
</script>
