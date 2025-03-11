<template>
  <div>
    <div class="form-group" :class="{ 'has-error': $v.name.$error }">
      <label>Name</label>
      <input
        v-model.trim="name"
        type="text"
        class="form-control"
        placeholder="Compute Domain Name"
        @input="$v.name.$touch()"
        v-focus="focused"
        @focus="focused = true"
        @blur="focused = false" />
      <span class="help-block" v-if="$v.name.$error && !$v.name.required">
        Name is required
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.description.$error }">
      <label>Description</label>
      <input
        v-model.trim="description"
        type="text"
        class="form-control"
        placeholder="For example: compute capabilities of this domain"
        @input="$v.description.$touch()" />
    </div>

    <div class="form-group" v-bind:class="{ 'has-error': $v.maxMemory.$error }">
      <label>Memory Limit Per Container (in bytes)</label>
      <input
        v-model.trim="maxMemory"
        type="number"
        class="form-control"
        @input="$v.maxMemory.$touch()" />
      <span class="help-block" v-if="$v.maxMemory.$error && !$v.maxMemory.required">
        Must specify an integer value. Set to 0 to not restrict memory.
      </span>
    </div>
  </div>
</template>

<script>
import { validationMixin } from 'vuelidate';
import { required, numeric } from 'vuelidate/lib/validators';

import { mixin as focusMixin } from 'vue-focus';

export default {
  name: 'DomainStep',
  mixins: [validationMixin, focusMixin],
  data: () => ({
    name: '',
    description: '',
    maxMemory: 0,
    focused: false,
  }),
  validations: {
    name: {
      required,
    },
    description: {
    },
    maxMemory: {
      required,
      numeric,
    },
    form: ['name', 'description', 'maxMemory'],
  },
  methods: {
    validate() {
      this.$v.form.$touch();
      const isValid = !this.$v.form.$invalid;
      this.$emit('on-validate', this.$data, isValid);
      return isValid;
    },
  },
};
</script>
