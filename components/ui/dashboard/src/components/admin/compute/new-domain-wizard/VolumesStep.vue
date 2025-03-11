<template>
  <div>
    <p v-if="!isEditing">
      Data Volumes are currently created by creating a docker container with one or more folders mounted.
      This docker container never has to run, and can be as simple as one using the <code>hello</code> image.
      This docker volume container must be created on all nodes.
    </p>
    <ul v-if="!isEditing && volumes" class="list-group">
      <li
        class="list-group-item clearfix"
        v-for="(volume, index) of volumes"
        :key="volume.name">

        {{ volume.name }}
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

      Add New Data Volume
    </button>
    <volume-configurer
      :initial-data="volumes[currentlyEditingIndex]"
      v-if="isEditing"
      :is-editing="currentlyEditingIndex !== undefined"
      @on-save="saveVolume" @cancel="cancelEdit" />
  </div>
</template>

<script>
import { validationMixin } from 'vuelidate';

import VolumeConfigurer from './VolumeConfigurer';

export default {
  name: 'VolumesStep',
  mixins: [validationMixin],
  components: {
    VolumeConfigurer,
  },
  data: () => ({
    volumes: [],
    isEditing: false,
    currentlyEditingIndex: undefined,
  }),
  validations: {
    volumes: {
    },
    form: ['volumes'],
  },
  methods: {
    validate() {
      this.$v.form.$touch();
      const isValid = !this.$v.form.$invalid;
      this.$emit('on-validate', this.$data, isValid);
      return isValid;
    },
    saveVolume(newVolume) {
      if (this.currentlyEditingIndex === undefined) {
        this.volumes.push(newVolume);
      } else {
        this.volumes[this.currentlyEditingIndex] = newVolume;
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
