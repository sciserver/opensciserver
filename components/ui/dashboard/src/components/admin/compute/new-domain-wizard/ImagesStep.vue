<template>
  <div>
    <p v-if="!isEditing">
      For performance, remember to run put the docker images on all the nodes.
      For images from a registry, this is done with <kbd>docker pull ...</kbd>.
    </p>
    <ul v-if="!isEditing && images" class="list-group">
      <li
        class="list-group-item clearfix"
        v-for="(image, index) of images"
        :key="image.name">

        {{ image.name }}
        <div class="btn-group pull-right" role="group">
          <button type="button" class="btn btn-default" @click="startEditing(index)">Edit</button>
          <button type="button" class="btn btn-default">Remove</button>
        </div>
      </li>
    </ul>
    <button
      v-if="!isEditing"
      type="button"
      class="btn btn-link"
      @click="startEditing()">

      Add New Image
    </button>
    <image-configurer
      :initial-data="images[currentlyEditingIndex]"
      v-if="isEditing"
      :is-editing="currentlyEditingIndex !== undefined"
      @on-save="saveImage" @cancel="cancelEdit" />

      <p class="text-danger" v-if="$v.form.$error">
        One or more images must be configured
      </p>
  </div>
</template>

<script>
import { validationMixin } from 'vuelidate';
import { required, minLength } from 'vuelidate/lib/validators';

import ImageConfigurer from './ImageConfigurer';

export default {
  name: 'ImagesStep',
  mixins: [validationMixin],
  components: {
    ImageConfigurer,
  },
  data: () => ({
    images: [],
    isEditing: false,
    currentlyEditingIndex: undefined,
  }),
  validations: {
    images: {
      required,
      minLength: minLength(1),
    },
    form: ['images'],
  },
  methods: {
    validate() {
      this.$v.form.$touch();
      const isValid = !this.$v.form.$invalid;
      this.$emit('on-validate', this.$data, isValid);
      return isValid;
    },
    saveImage(newImage) {
      if (this.currentlyEditingIndex === undefined) {
        this.images.push(newImage);
      } else {
        this.images[this.currentlyEditingIndex] = newImage;
        this.currentlyEditingIndex = undefined;
      }
      this.isEditing = false;
    },
    startEditing(index) {
      this.currentlyEditingIndex = index;
      this.isEditing = true;
    },
    cancelEdit() {
      this.isEditing = false;
      this.currentlyEditingIndex = undefined;
    },
  },
};
</script>
