<template>
  <modal
    v-if="loaded"
    v-model="open"
    class="my-sciserver-modal-dialog"
    title="Create Compute Domain"
    size="lg"
    :footer="false"
    :transition-duration="0">

  <form-wizard color="#3498db" shape="tab" title="" subtitle="" finishButtonText="Create Domain" @on-complete="finishDomain">
    <tab-content title="New Domain" :before-change="()=>validateStep('step1')">
      <domain-step ref="step1" @on-validate="mergePartialModels" />
    </tab-content>
    <tab-content title="Nodes" :before-change="()=>validateStep('nodesStep')">
      <nodes-step ref="nodesStep" @on-validate="mergePartialModels" />
    </tab-content>
    <tab-content title="Images" :before-change="()=>validateStep('imagesStep')">
      <images-step ref="imagesStep" @on-validate="mergePartialModels" />
    </tab-content>
    <tab-content title="Data Volumes" :before-change="()=>validateStep('volumesStep')">
      <volumes-step ref="volumesStep" @on-validate="mergePartialModels" />
    </tab-content>
    <tab-content title="User Volumes" :before-change="()=>validateStep('userVolumesStep')">
      <user-volumes-step ref="userVolumesStep" @on-validate="mergePartialModels" />
    </tab-content>
    <tab-content title="Summary">
      <template v-if="!savingDomain">
        Once this wizard is finished, the "{{ finalModel.name }}" domain will be created as follows:
        <ul class="summary-list">
          <li v-if="finalModel.description">Description: {{ finalModel.description }}</li>
          <li v-else>No Description</li>

          <li v-if="parseInt(finalModel.maxMemory, 10)">RAM Limit Per Container: {{ finalModel.maxMemory | humanReadableLimit }}</li>
          <li v-else>No RAM Limit</li>

          <li v-if="finalModel.nodes">{{ finalModel.nodes.length }} {{ finalModel.nodes.length | pluralize('node') }}</li>
          <li v-if="finalModel.images">{{ finalModel.images.length }} {{ finalModel.images.length | pluralize('image')}}</li>
          <li v-if="finalModel.volumes">{{ finalModel.volumes.length }} data {{ finalModel.volumes.length | pluralize('volume') }}</li>
          <li v-if="finalModel.rootVolumesOnCD">linked with {{ finalModel.rootVolumesOnCD.length }} root {{ finalModel.rootVolumesOnCD.length | pluralize('volume') }}</li>
        </ul>
      </template>
      <domain-saver v-else
        :final-model="finalModel"
        ref="domainSaver" />
    </tab-content>
  </form-wizard>
  </modal>
</template>

<script>
import Vue from 'vue';
import { FormWizard, TabContent } from 'vue-form-wizard';

import 'vue-form-wizard/dist/vue-form-wizard.min.css';

import mathUtils from '@/math-utils';

import DomainStep from './new-domain-wizard/DomainStep';
import NodesStep from './new-domain-wizard/NodesStep';
import ImagesStep from './new-domain-wizard/ImagesStep';
import VolumesStep from './new-domain-wizard/VolumesStep';
import UserVolumesStep from './new-domain-wizard/UserVolumesStep';
import DomainSaver from './new-domain-wizard/DomainSaver';

export default {
  name: 'NewDomainWizard',
  components: {
    FormWizard,
    TabContent,
    DomainStep,
    NodesStep,
    ImagesStep,
    VolumesStep,
    UserVolumesStep,
    DomainSaver,
  },
  data: () => ({
    open: false,
    focused: false,
    loaded: false,
    savingDomain: false,

    finalModel: {},
  }),
  methods: {
    startDialog() {
      this.loaded = true;
      this.focused = true;
      this.open = true;
    },
    validateStep(name) {
      return this.$refs[name].validate();
    },
    mergePartialModels(model, isValid) {
      if (isValid) {
        this.finalModel = Object.assign({}, this.finalModel, model);
      }
    },
    finishDomain() {
      const vm = this;
      if (vm.savingDomain) return;
      vm.savingDomain = true;

      // Need to run this after domainSaver has a chance to be
      // mounted after changing savingDomain
      Vue.nextTick(() =>
        vm.$refs.domainSaver.saveDomain().then(() => {
          vm.$emit('on-save');
          vm.open = false;
          vm.savingDomain = false;
          vm.finalModel = {};
          vm.loaded = false;
        }),
      );
    },
  },
  filters: {
    humanReadableLimit(value) {
      return mathUtils.bytesToSize(value);
    },
  },
};
</script>

<style scoped>
.summary-list {
  list-style: disc;
  padding-left: 40px;
}
</style>
