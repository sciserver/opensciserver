<template>
  <modal
    v-if="loaded"
    v-model="open"
    class="my-sciserver-modal-dialog"
    title="Register Compute Image"
    size="lg"
    :footer="false"
    :transition-duration="0">

    <image-configurer
      :is-editing="false"
      v-if="!isSaving"
      @on-save="saveImage" @cancel="closeDialog" />
    
    <image-saver
      ref="imageSaver"
      :racm-domain-id="racmDomainId"
	  :compute-domain-id="computeDomainId"
      :image-model="imageModel"
      v-if="isSaving" />
  </modal>
</template>

<script>
import Vue from 'vue';

import ImageConfigurer from './new-domain-wizard/ImageConfigurer';
import ImageSaver from './new-domain-wizard/ImageSaver';

export default {
  name: 'RegisterImage',
  components: {
    ImageConfigurer,
    ImageSaver,
  },
  data: () => ({
    open: false,
    loaded: false,
    imageModel: {},
    isSaving: false,
  }),
  props: ['racmDomainId', 'computeDomainId'],
  methods: {
    saveImage(newImage) {
      this.imageModel = newImage;
      this.isSaving = true;
      const vm = this;

      // Need to run this after imageSaver has a chance to be
      // mounted after changing isSaving
      Vue.nextTick(() =>
        vm.$refs.imageSaver.saveImage().then(() => {
          vm.$emit('on-save');
          vm.open = false;
          vm.isSaving = false;
          vm.imageModel = {};
          vm.loaded = false;
        }),
      );
    },
    closeDialog() {
      this.open = false;
      this.loaded = false;
    },
    openDialog() {
      this.open = true;
      this.loaded = true;
    },
  },
};
</script>
