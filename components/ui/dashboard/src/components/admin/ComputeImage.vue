<template>
  <li class="list-group-item" v-if="foundImage">
    <span v-if="racmImage" class="badge">RACM ID: {{ racmImage.id }}</span>
    <span v-else class="badge alert-danger">Not Registered in RACM</span>
    <span class="badge">Compute ID: {{ computeImageId }}</span>
    <h4 class="list-group-item-heading">{{ foundImage.name }}</h4>
    <dl class="dl-horizontal">
      <dt>Description</dt>
      <dd>{{ foundImage.description }}</dd>
      <template v-if="computeImage">
        <dt>Docker Reference</dt>
        <dd>{{ computeImage.docker_ref }}</dd>
        <dt>Executation Manager</dt>
        <dd>
          {{ computeImage.container_manager_class | abbrivExecutationManager}}
        </dd>
      </template>
    </dl>
    <div class="panel-body" :class="classForConsistency">
      <template v-if="racmImage && computeImage">
        <span v-if="consistentMetadata">
          ✅ Name and Description are consistent between RACM and Compute databases
        </span>
        <p v-if="!consistentMetadata && computeImage.name !== racmImage.name">
          Another image name is registered in Compute: "{{ computeImage.name }}"
        </p>
        <p v-if="!consistentMetadata && computeImage.description !== racmImage.description">
          A different image description is registered in Compute: "{{ computeImage.description }}"
        </p>
        <p v-if="!consistentMetadata && !isGrantedToAdmin">
          The "admin" group does not have grant privileges on this image
        </p>
      </template>
      <template v-if="racmImage && !computeImage">
        Not registered in Compute!
      </template>
      <template v-if="!racmImage && computeImage">
        Not registered in RACM!
      </template>
    </div>
  </li>
</template>
<script>
import startCase from 'lodash/startCase';
import trimStart from 'lodash/trimStart';
import find from 'lodash/find';
import { mapState } from 'vuex';


export default {
  name: 'ComputeImage',
  props: ['imageId'],
  computed: {
    foundImage() {
      return this.racmImage || this.computeImage;
    },
    registeredInRACM() {
      return Number.isInteger(this.imageId);
    },
    imageName() {
      if (this.racmImage) return this.racmImage.name;
      if (this.computeImage) return this.computeImage.name;
      return undefined;
    },
    computeImageId() {
      if (this.racmImage) {
        return parseInt(this.racmImage.publisherDID, 10);
      } else if (this.imageId) {
        return parseInt(trimStart(this.imageId, 'compute:'), 10);
      }
      return undefined;
    },
    racmImage() {
      if (!this.registeredInRACM) return undefined;
      return this.$store.state.compute.racmImages[this.imageId];
    },
    computeImage() {
      if (!this.imageId) return undefined;
      return this.$store.state.compute.computeImages[
        this.computeImageId];
    },
    classForConsistency() {
      return {
        'alert-danger': !(this.racmImage && this.computeImage),
        'alert-success': this.racmImage && this.computeImage && this.consistentMetadata,
        'alert-warning': this.racmImage && this.computeImage && !this.consistentMetadata,
      };
    },
    consistentMetadata() {
      if (this.computeImage === undefined || this.racmImage === undefined) return false;
      return this.computeImage.name === this.racmImage.name &&
        this.computeImage.description === this.racmImage.description &&
        this.isGrantedToAdmin;
    },
    isGrantedToAdmin() {
      if (!this.myCollaborations || !this.registeredInRACM) return undefined;
      const adminGroup = this.myCollaborations
        .map(x => this.collaborations[x])
        .find(x => x.name === 'admin' && x.type === 'GROUP');
      if (!adminGroup) return undefined;
      const thisImage = find(adminGroup.resources, { entityId: this.imageId, type: 'DOCKERIMAGE' });
      return thisImage ? thisImage.actions.includes('grant') : false;
    },
    ...mapState(['collaborations', 'myCollaborations']),
  },
  filters: {
    abbrivExecutationManager(value) {
      return startCase(value.split(/\./).pop());
    },
  },
};
</script>
