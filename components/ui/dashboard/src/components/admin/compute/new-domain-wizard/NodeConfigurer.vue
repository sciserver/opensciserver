<template>
  <div class="form clearfix">
    <div class="form-group" :class="{ 'has-error': $v.name.$error }">
      <label>Node Name</label>
      <input
        v-model.trim="name"
        type="text"
        @input="$v.name.$touch()"
        class="form-control"
        placeholder="Node Name" />
      <span class="help-block" v-if="$v.name.$error && !$v.name.required">
        Name is required
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.dockerApiUrl.$error }">
      <label>Docker API Server</label>
      <input
        v-model.trim="dockerApiUrl"
        @input="$v.dockerApiUrl.$touch()"
        type="text"
        placeholder="https://10.10.10.10:2376/"
        class="form-control"/>
      <span class="help-block" v-if="$v.dockerApiUrl.$error && !$v.dockerApiUrl.required">
        Docker API Server is required and must be a valid url (e.g., <code>https://10.10.10.10:2376/</code>)
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.proxyApiUrl.$error }">
      <label>Proxy API Server</label>
      <input
        v-model.trim="proxyApiUrl"
        @input="$v.proxyApiUrl.$touch()"
        type="text"
        placeholder="https://10.10.10.10:8001/"
        class="form-control"/>
      <span class="help-block" v-if="$v.proxyApiUrl.$error && !$v.proxyApiUrl.required">
        Proxy API Server is required and must be a valid url (e.g., <code>https://10.10.10.10:8001/</code>)
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.proxyBaseUrl.$error }">
      <label>Base URL for Proxy Server</label>
      <span class="help-block">Address by which the node will be accessible to users</span>
      <input
        v-model.trim="proxyBaseUrl"
        @input="$v.proxyBaseUrl.$touch()"
        type="text"
        placeholder="https://domain.example.com/this-node/"
        class="form-control"/>
      <span class="help-block" v-if="$v.proxyBaseUrl.$error && !$v.proxyBaseUrl.required">
        Proxy Base URL is required and must be a valid url (e.g., <code>https://domain.example.com/this-node/</code>)
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.clientKey.$error }">
      <label>Client Key File Name</label>
      <span class="help-block">Name of the private key used by clients to authenticate to the Docker and proxy APIs</span>
      <input
        v-model.trim="clientKey"
        @input="$v.clientKey.$touch()"
        type="text"
        placeholder="name-of-node-key.pem"
        class="form-control"/>
      <span class="help-block" v-if="$v.clientKey.$error && !$v.clientKey.required">
        Client key name is required
      </span>
    </div>
    <div class="form-group" :class="{ 'has-error': $v.clientCert.$error }">
      <label>Client Certificate File Name</label>
      <span class="help-block">Name of the signed certificate used by clients to authenticate to the Docker and proxy APIs</span>
      <input
        v-model.trim="clientCert"
        @input="$v.clientCert.$touch()"
        type="text"
        placeholder="name-of-node-cert.pem"
        class="form-control"/>
      <span class="help-block" v-if="$v.clientCert.$error && !$v.clientCert.required">
        Client certificate name is required
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
  name: 'NodeConfigurer',
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
      dockerApiUrl: '',
      proxyBaseUrl: '',
      proxyApiUrl: '',
      clientKey: '',
      clientCert: '',
    };
  },
  computed: {
    submitButtonMessage() {
      if (this.isEditing) {
        return 'Save Changes';
      }
      return 'Add Node';
    },
  },
  validations: {
    name: {
      required,
    },
    dockerApiUrl: {
      required,
    },
    proxyApiUrl: {
      required,
    },
    proxyBaseUrl: {
      required,
    },
    clientKey: {
      required,
    },
    clientCert: {
      required,
    },
    form: ['name', 'dockerApiUrl', 'proxyApiUrl', 'proxyBaseUrl', 'clientKey', 'clientCert'],
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
