<template>
  <div>
    <p>Total Progress:</p>
    <div class="progress">
      <div class="progress-bar progress-bar-striped active"
        role="progressbar"
        :aria-valuenow="totalProgress" aria-valuemin="0" aria-valuemax="100"
        :style="totalProgressStyle">
        {{ totalProgress }}%
      </div>
    </div>
    <p>Remaining Tasks:</p>
    <ol class="remaining-list">
      <li v-if="!registeredDomainInCompute">Registering Domain in Compute&hellip;</li>
      <li v-if="!registeredNodesInCompute">Registering Nodes in Compute&hellip;</li>
      <li v-if="!registeredSlotsInCompute">Registering Slots for all Nodes in Compute&hellip;</li>
      <li v-if="!registeredImagesInCompute">Registering Images in Compute&hellip;</li>
      <li v-if="!registeredVolumesInCompute">Registering Volumes in Compute&hellip;</li>
      <li v-if="!registeredInRACM">Registering Domain in RACM&hellip;</li>
      <li v-if="!sharedAllWithAdminGroup">Granting Access to the admin group&hellip;</li>
    </ol>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import zipWith from 'lodash/zipWith';

import {
  registerDomainInCompute,
  registerNodeInCompute,
  registerImageInCompute,
  registerVolumeInCompute,
  registerDomainInRACM,
  saveAllToAdminGroup,
  registerSlotsForNode,
} from
  '@/components/admin/compute/new-domain-wizard/domain-register';

export default {
  name: 'DomainSaver',
  props: ['finalModel'],
  data: () => ({
    registeredDomainInCompute: false,
    registeredNodesInCompute: false,
    registeredSlotsInCompute: false,
    registeredImagesInCompute: false,
    registeredVolumesInCompute: false,
    registeredInRACM: false,
    sharedAllWithAdminGroup: false,
  }),
  computed: {
    totalProgress() {
      const tasksCompleted =
        (+this.registeredDomainInCompute) +
        (+this.registeredNodesInCompute) +
        (+this.registeredSlotsInCompute) +
        (+this.registeredImagesInCompute) +
        (+this.registeredVolumesInCompute) +
        (+this.registeredInRACM) +
        (+this.sharedAllWithAdminGroup);
      return Math.round(100 * (tasksCompleted / 7));
    },
    totalProgressStyle() {
      return {
        minWidth: '2em',
        width: `${this.totalProgress}%`,
      };
    },
    adminGroup() {
      if (!this.myCollaborations) return undefined;
      return this.myCollaborations
        .map(x => this.collaborations[x])
        .find(x => x.name === 'admin' && x.type === 'GROUP');
    },
    ...mapState(['token', 'collaborations', 'myCollaborations']),
  },
  methods: {
    async saveDomain() {
      const vm = this;
      let runningModel = vm.finalModel;
      const addIds = (list, ids) => zipWith(list, ids, (object, id) => ({
        id,
        ...object,
      }));

      const domainIds = await registerDomainInCompute(vm.token, runningModel);

      vm.registeredDomainInCompute = true;
      runningModel = { domain_id: domainIds, ...runningModel };
      const allNodes = Promise.all(
        runningModel.nodes.map(nodeObj =>
          registerNodeInCompute(vm.token, runningModel, nodeObj),
        )).then((nodeIds) => {
          vm.registeredNodesInCompute = true;
          runningModel = { ...runningModel, nodes: addIds(runningModel.nodes, nodeIds) };
          return Promise.all(nodeIds.map(nodeId => registerSlotsForNode(vm.token, nodeId)));
        });
      const allImages = Promise.all(
        runningModel.images.map(imageObj =>
          registerImageInCompute(vm.token, runningModel, imageObj),
        )).then((imageIds) => {
          vm.registeredImagesInCompute = true;
          runningModel = { ...runningModel, images: addIds(runningModel.images, imageIds) };
        });
      const allVolumes = Promise.all(
        runningModel.volumes.map(volumeObj =>
          registerVolumeInCompute(vm.token, runningModel, volumeObj),
        )).then((volumeIds) => {
          vm.registeredVolumesInCompute = true;
          runningModel = { ...runningModel, volumes: addIds(runningModel.volumes, volumeIds) };
        });

      await Promise.all([allNodes, allImages, allVolumes]);

      const racmObj = await registerDomainInRACM(vm.token, runningModel);
      vm.registeredInRACM = true;

      await saveAllToAdminGroup(vm.adminGroup, vm.token, racmObj);
      vm.sharedAllWithAdminGroup = true;
    },
  },
};
</script>

<style scoped>
.remaining-list {
  padding-left: 40px;
  list-style: decimal;
}
</style>
