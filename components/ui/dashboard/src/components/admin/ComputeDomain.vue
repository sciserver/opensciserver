<template>
  <div v-if="racmDomain">
    <div class="panel panel-primary">
      <div class="panel-heading">
        <h3 class="panel-title">{{ racmDomain.name }}</h3>
      </div>
      <div class="panel-body">
        <span class="badge">RACM ID: {{ racmDomain.id }}</span>
        <span class="badge">Compute ID: {{ racmDomain.publisherDID }}</span>
        <dl class="dl-horizontal">
          <template v-if="computeDomain">
            <dt>Name in Compute DB</dt>
            <dd>
              {{ computeDomain.name }}
              <span v-if="computeDomain.name !== racmDomain.name" class="text-danger">- Different from RACM Name</span>
            </dd>
            <dt>Memory Limit</dt>
            <dd>{{ computeDomain.max_memory | humanReadableLimit }}</dd>
          </template>
          <dt>API Endpoint</dt>
          <dd>{{ racmDomain.apiEndpoint }}</dd>
          <dt>Description</dt>
          <dd>{{ racmDomain.description }}</dd>
        </dl>
      </div>
    </div>

    <compute-nodes :node-ids="nodeIds" />

    <div class="panel panel-primary">
      <div class="panel-heading panel-headings-with-buttons">
        <span>Compute Images</span>

        <button
          type="button"
          class="btn btn-success btn-sm"
          v-if="computeDomain && racmDomain"
          @click="newImage" title="Create New Compute Image">
          <i class="fa fa-plus" aria-hidden="true"></i>
        </button>
      </div>
      <compute-images :image-ids="this.racmDomain.images" :compute-only-ids="imagesNotInRACM" />
    </div>

    <div class="panel panel-primary">
      <div class="panel-heading">Data Volumes</div>
      <compute-volumes :volume-container-ids="this.racmDomain.volumes" :compute-only-ids="volumeContainersNotInRACM" />
    </div>

    <div class="panel panel-primary">
      <div class="panel-heading">Linked Root Volumes</div>
      <compute-root-volumes :root-volume-on-compute-domain-ids="this.racmDomain.rootVolumes"/>
    </div>

    <register-image
      ref="registerNewImage"
      @on-save="$emit('on-save')"
      :compute-domain-id="computeDomainId"
      :racm-domain-id="selectedId" />
  </div>
</template>

<script>
import difference from 'lodash/difference';
import filter from 'lodash/filter';
import map from 'lodash/map';
import mathUtils from '@/math-utils';
import ComputeImages from './ComputeImages';
import ComputeVolumes from './ComputeVolumes';
import ComputeNodes from './ComputeNodes';
import ComputeRootVolumes from './ComputeRootVolumes';
import RegisterImage from './compute/RegisterImage';

export default {
  name: 'ComputeDomain',
  components: {
    ComputeImages,
    ComputeVolumes,
    ComputeNodes,
    ComputeRootVolumes,
    RegisterImage,
  },
  props: ['selectedId'],
  computed: {
    racmDomain() {
      return this.$store.state.compute.racmComputeDomains[this.selectedId];
    },
    computeDomain() {
      if (!this.racmDomain) return undefined;
      return this.$store.state.compute.computeComputeDomains[
        this.computeDomainId];
    },
    computeDomainId() {
      return parseInt(this.racmDomain.publisherDID, 10);
    },
    imagesNotInRACM() {
      if (!this.computeImageIds) return [];
      return difference(this.computeImageIds, this.racmImagesPublisherDIDs);
    },
    computeImageIds() {
      if (!this.computeDomain) return [];
      return map(
        filter(this.$store.state.compute.computeImages, { domain_id: this.computeDomainId }),
        'id');
    },
    racmImagesPublisherDIDs() {
      if (!this.racmDomain) return [];
      return map(this.racmDomain.images, imageId =>
        parseInt(this.$store.state.compute.racmImages[imageId].publisherDID, 10));
    },

    volumeContainersNotInRACM() {
      if (!this.computeVolumeContainerIds) return [];
      return difference(this.computeVolumeContainerIds, this.racmVolumeContainerPublisherDIDs);
    },
    computeVolumeContainerIds() {
      if (!this.computeDomain) return [];
      return map(
        filter(this.$store.state.compute.computeVolumeContainers,
          { domain_id: this.computeDomainId }),
        'id');
    },
    racmVolumeContainerPublisherDIDs() {
      if (!this.racmDomain) return [];
      return map(this.racmDomain.volumes, volumeId =>
        parseInt(this.$store.state.compute.racmVolumeContainers[volumeId].publisherDID, 10));
    },
    nodeIds() {
      if (!this.computeDomain) return [];
      return map(
        filter(
          this.$store.state.compute.computeNodes,
          { domain_id: this.computeDomainId }),
        'id');
    },
  },
  methods: {
    newImage() {
      this.$refs.registerNewImage.openDialog();
    },
  },
  filters: {
    humanReadableLimit(value) {
      if (value === 0) return 'No Limit';
      return mathUtils.bytesToSize(value);
    },
  },
};
</script>

<style scoped>
.panel-body > .badge {
  float: right;
  margin-right: 5px;
}

.panel-headings-with-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
