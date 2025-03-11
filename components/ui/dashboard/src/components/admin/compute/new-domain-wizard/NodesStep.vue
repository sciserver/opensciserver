<template>
  <div>
    <p v-if="!isEditing">
      Nodes (a.k.a DockerVMs) are configured through using the <code>scripts/node-setup.sh</code> in the
      <a href="https://github.com/sciserver/sciserver-compute-java/">sciserver-compute-java</a>
      repository. This script generates private and signing keys in <code>/etc/certs</code> that are needed
      for allowing Compute to communicate with the nodes.
    </p>
    <p v-if="!isEditing">
      Containers are created such that the proxy may communicate to them on any ports
      between 10000 and 10100, not including port 10010. Be sure no other processes are using these ports.
    </p>
    <ul v-if="!isEditing && nodes" class="list-group">
      <li
        class="list-group-item clearfix"
        v-for="(node, index) of nodes"
        :key="node.name">

        {{ node.name }}
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

      Add New Node
    </button>
    <node-configurer
      :initial-data="nodes[currentlyEditingIndex]"
      v-if="isEditing"
      :is-editing="currentlyEditingIndex !== undefined"
      @on-save="saveNode" @cancel="cancelEdit" />

      <p class="text-danger" v-if="$v.form.$error">
        One or more nodes must be configured
      </p>
  </div>
</template>

<script>
import { validationMixin } from 'vuelidate';
import { required, minLength } from 'vuelidate/lib/validators';

import NodeConfigurer from './NodeConfigurer';

export default {
  name: 'NodesStep',
  mixins: [validationMixin],
  components: {
    NodeConfigurer,
  },
  data: () => ({
    nodes: [],
    isEditing: false,
    currentlyEditingIndex: undefined,
  }),
  validations: {
    nodes: {
      required,
      minLength: minLength(1),
    },
    form: ['nodes'],
  },
  methods: {
    validate() {
      this.$v.form.$touch();
      const isValid = !this.$v.form.$invalid;
      this.$emit('on-validate', this.$data, isValid);
      return isValid;
    },
    saveNode(newNode) {
      if (this.currentlyEditingIndex === undefined) {
        this.nodes.push(newNode);
      } else {
        this.nodes[this.currentlyEditingIndex] = newNode;
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
