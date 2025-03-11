<template>
  <li class="list-group-item" v-if="foundVolume">
    <span v-if="racmVolume" class="badge">RACM ID: {{ racmVolume.id }}</span>
    <span v-else class="badge alert-danger">Not Registered in RACM</span>
    <span class="badge">Compute ID: {{ computeVolumeId }}</span>
    <h4 class="list-group-item-heading">{{ foundVolume.name }}</h4>
    <dl class="dl-horizontal">
      <dt>Description</dt>
      <dd>{{ foundVolume.description }}</dd>
      <template v-if="computeVolume">
        <dt>Docker volume container</dt>
        <dd>{{ computeVolume.docker_ref }}</dd>
      </template>
    </dl>
    <div class="panel-body" :class="classForConsistency">
      <template v-if="racmVolume && computeVolume">
        <span v-if="consistentMetadata">
          ✅ Name and Description are consistent between RACM and Compute databases
        </span>
        <p v-if="!consistentMetadata && computeVolume.name !== racmVolume.name">
          A different name is registered in Compute: "{{ computeVolume.name }}"
        </p>
        <p v-if="!consistentMetadata && computeVolume.description !== racmVolume.description">
          A different description is registered in Compute: "{{ computeVolume.description }}"
        </p>
        <p v-if="!consistentMetadata && !isGrantedToAdmin">
          The "admin" group does not have grant privileges on this volume
        </p>
      </template>
      <template v-if="racmVolume && !computeVolume">
        Not registered in Compute!
      </template>
      <template v-if="!racmVolume && computeVolume">
        Not registered in RACM!
      </template>
    </div>
  </li>
</template>

<script>
import trimStart from 'lodash/trimStart';
import find from 'lodash/find';
import { mapState } from 'vuex';

export default {
  name: 'ComputeVolume',
  props: ['volumeId'],
  computed: {
    foundVolume() {
      return this.racmVolume || this.computeVolume;
    },
    registeredInRACM() {
      return Number.isInteger(this.volumeId);
    },
    volumeName() {
      if (this.racmVolume) return this.racmVolume.name;
      if (this.computeVolume) return this.computeVolume.name;
      return undefined;
    },
    computeVolumeId() {
      if (this.racmVolume) {
        return parseInt(this.racmVolume.publisherDID, 10);
      } else if (this.volumeId) {
        return parseInt(trimStart(this.volumeId, 'compute:'), 10);
      }
      return undefined;
    },
    racmVolume() {
      if (!this.registeredInRACM) return undefined;
      return this.$store.state.compute.racmVolumeContainers[this.volumeId];
    },
    computeVolume() {
      if (!this.volumeId) return undefined;
      return this.$store.state.compute.computeVolumeContainers[
        this.computeVolumeId];
    },
    classForConsistency() {
      return {
        'alert-danger': !(this.racmVolume && this.computeVolume),
        'alert-success': this.racmVolume && this.computeVolume && this.consistentMetadata,
        'alert-warning': this.racmVolume && this.computeVolume && !this.consistentMetadata,
      };
    },
    consistentMetadata() {
      if (this.computeVolume === undefined || this.racmVolume === undefined) return false;
      return this.computeVolume.name === this.racmVolume.name &&
        this.computeVolume.description === this.racmVolume.description &&
        this.isGrantedToAdmin;
    },
    isGrantedToAdmin() {
      if (!this.myCollaborations || !this.registeredInRACM) return undefined;
      const adminGroup = this.myCollaborations
        .map(x => this.collaborations[x])
        .find(x => x.name === 'admin' && x.type === 'GROUP');
      if (!adminGroup) return undefined;
      const thisVolume = find(adminGroup.resources, { entityId: this.volumeId, type: 'VOLUMECONTAINER' });
      return thisVolume ? thisVolume.actions.includes('grant') : false;
    },
    ...mapState(['collaborations', 'myCollaborations']),
  },
};
</script>
