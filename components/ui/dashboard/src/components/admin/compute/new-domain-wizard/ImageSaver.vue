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
      <li v-if="!registeredInCompute">Registering Image in Compute&hellip;</li>
      <li v-if="!registeredInRACM">Registering Image in RACM&hellip;</li>
      <li v-if="!sharedWithAdminGroup">Granting Access to the admin Group&hellip;</li>
    </ol>
  </div>
</template>

<script>
import { mapState } from 'vuex';
import find from 'lodash/find';

import {
  registerImageInCompute,
  registerImageInRACM,
  saveEntityToAdminGroup,
} from
  '@/components/admin/compute/new-domain-wizard/domain-register';

export default {
  name: 'DomainSaver',
  props: ['imageModel', 'computeDomainId', 'racmDomainId'],
  data: () => ({
    registeredInCompute: false,
    registeredInRACM: false,
    sharedWithAdminGroup: false,
  }),
  computed: {
    totalProgress() {
      const tasksCompleted =
        (+this.registeredInCompute) +
        (+this.registeredInRACM) +
        (+this.sharedWithAdminGroup);
      return Math.round(100 * (tasksCompleted / 3));
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
    async saveImage() {
      const vm = this;
      const runningModel = vm.imageModel;

      const imageId = await registerImageInCompute(vm.token, { domain_id: vm.computeDomainId },
        runningModel);
      vm.registeredInCompute = true;
      runningModel.id = imageId;

      const domainObj = await registerImageInRACM(vm.token, vm.racmDomainId, runningModel);
      vm.registeredInRACM = true;

      await saveEntityToAdminGroup(vm.adminGroup, vm.token, 'DOCKERIMAGE', 'grant',
        find(domainObj.images, { publisherDID: imageId.toString() }).id);

      vm.sharedWithAdminGroup = true;
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
