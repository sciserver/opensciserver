<template>
  <div class="form clearfix">
    <div class="form-group" :class="{ 'has-error': $v.name.$error }">
      <label>Name</label>
      <input
        v-model.trim="name"
        type="text"
        @input="$v.name.$touch()"
        class="form-control"
        placeholder="Data Volume Name" />
      <span class="help-block" v-if="$v.name.$error && !$v.name.required">
        Name is required
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.description.$error }">
      <label>Description</label>
      <input
        v-model.trim="description"
        @input="$v.description.$touch()"
        type="text"
        class="form-control"/>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.dockerRef.$error }">
      <label>Docker Volume Container Name</label>
      <input
        v-model.trim="dockerRef"
        @input="$v.dockerRef.$touch()"
        type="text"
        placeholder="volume_myvolume"
        class="form-control"/>
      <span class="help-block" v-if="$v.dockerRef.$error && !$v.dockerRef.required">
        A name of a container containing the mounts for this data volume must be specified
      </span>
    </div>

    <div class="pull-right">
      <button
        type="submit"
        class="btn btn-success"
        :disabled="$v.form.$error"
        @click="attemptSave">{{ submitButtonMessage }}</button>
      <button type="button" class="btn btn-default" @click="$emit('cancel')">Cancel</button>
    </div>
  </div>
</template>

<script>
import { validationMixin } from 'vuelidate';
import { required } from 'vuelidate/lib/validators';

export default {
  name: 'VolumeConfigurer',
  mixins: [validationMixin],
  props: {
    isEditing: {
      type: Boolean,
      default: false,
    },
    initialData: {
      type: Object,
      required: false,
    },
  },
  data() {
    if (this.initialData) {
      return { ...this.initialData };
    }
    return {
      name: '',
      description: '',
      dockerRef: '',
    };
  },
  computed: {
    submitButtonMessage() {
      if (this.isEditing) {
        return 'Save Changes';
      }
      return 'Add Data Volume';
    },
  },
  validations: {
    name: {
      required,
    },
    description: {
    },
    dockerRef: {
      required,
    },
    form: ['name', 'description', 'dockerRef'],
  },
  methods: {
    attemptSave() {
      this.$v.form.$touch();
      const isValid = !this.$v.form.$invalid;
      if (isValid) {
        this.$emit('on-save', this.$data);
      }
    },
  },
};
</script>
