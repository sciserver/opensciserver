<template>
  <div class="panel panel-primary" v-if="nodeIds">
    <div class="panel-heading">Compute Nodes</div>
    <div class="panel-body">
      {{ nodeNames }}
    </div>
  </div>
</template>
<script>
export default {
  name: 'ComputeNodes',
  props: ['nodeIds'],
  computed: {
    nodeNames() {
      return this.nodeIds.map(
        x => this.$store.state.compute.computeNodes[x].name,
      ).join(', ');
    },
  },
};
</script>