<template>
  <div class="form clearfix">
    <div class="form-group" :class="{ 'has-error': $v.name.$error }">
      <label>Name</label>
      <input
        v-model.trim="name"
        type="text"
        @input="$v.name.$touch()"
        class="form-control"
        placeholder="Image Name" />
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
      <label>Docker Image Reference</label>
      <input
        v-model.trim="dockerRef"
        @input="$v.dockerRef.$touch()"
        type="text"
        placeholder="my.docker.registry.io:port/image-name:version"
        class="form-control"/>
      <span class="help-block" v-if="$v.dockerRef.$error && !$v.dockerRef.required">
        A reference that Docker understands as an image name is required
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.containerManagerClass.$error }">
      <label>Executable Manager</label>
      <span class="help-block">
        SciServer code to use to run images. This only needs to be changed for GPU-enabled images or those using the
        MATLAB installation at JHU.
      </span>
      <select
        v-model="containerManagerClass"
        @input="$v.containerManagerClass.$touch()"
        class="form-control">

        <option value="org.sciserver.compute.core.container.DefaultExecutableManager">Default</option>
        <option value="org.sciserver.compute.core.container.DsAppsExecutableManager">Load ds_apps (for MATLAB @ JHU)</option>
        <option value="org.sciserver.compute.core.container.GpuExecutableManager">GPU</option>
        <option value="org.sciserver.compute.core.container.JetsonExecutableManager">Jetson</option>
        <option value="org.sciserver.compute.core.container.NvidiaExecutableManager">NVIDIA</option>
        <option value="org.sciserver.compute.core.container.KubernetesExecutableManager">Kubernetes</option>
      </select>

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
  name: 'ImageConfigurer',
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
      containerManagerClass: 'org.sciserver.compute.core.container.DefaultExecutableManager',
    };
  },
  computed: {
    submitButtonMessage() {
      if (this.isEditing) {
        return 'Save Changes';
      }
      return 'Add Image';
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
    containerManagerClass: {
      required,
    },
    form: ['name', 'description', 'dockerRef', 'containerManagerClass'],
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
