<template>
  <li class="list-group-item" v-if="racmRootVolumeOnCD">
    <h4 class="list-group-item-heading">{{ racmRootVolumeOnCD.displayName }}</h4>
    <dl class="dl-horizontal">
      <dt>Path on Nodes</dt>
      <dd>{{ racmRootVolumeOnCD.pathOnCD }}</dd>
      <template v-if="rootVolume && rootVolume.fileserviceObj">
        <dt>Root Volume</dt>
        <dd>"{{ rootVolume.name }}" on the "{{ rootVolume.fileserviceObj.name }}" file service</dd>
      </template>
    </dl>
    <div class="panel-body" :class="classForConsistency">
      <p v-if="consistentMetadata">
        ✅ The display name is consistent with the root volume's name.
      </p>
      <p v-else>
        Display name in Compute does not match the name of the root volume
      </p>
    </div>
  </li>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'ComputeRootVolume',
  props: ['rootVolumeOnComputeDomainId'],
  computed: {
    racmRootVolumeOnCD() {
      if (!this.rootVolumeOnComputeDomainId) return undefined;
      return this.$store.state.compute.racmRootVolumesOnCD[this.rootVolumeOnComputeDomainId];
    },
    rootVolume() {
      return this.getMyRootVolumeById(this.racmRootVolumeOnCD.rootVolumeId);
    },
    classForConsistency() {
      return {
        'alert-success': this.consistentMetadata,
        'alert-warning': !this.consistentMetadata,
      };
    },
    consistentMetadata() {
      if (this.rootVolume === undefined || this.rootVolume.fileserviceObj === undefined) {
        return false;
      }
      return this.rootVolume.name === this.racmRootVolumeOnCD.displayName;
    },
    ...mapGetters(['getMyRootVolumeById']),
  },
};
</script>
