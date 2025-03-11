<template>
  <ul class="list-group">
    <compute-root-volume v-for="volume in rootVolumeOnComputeDomainIds" :key="volume"
      :root-volume-on-compute-domain-id="volume" />
  </ul>
</template>

<script>
import ComputeRootVolume from './ComputeRootVolume';

export default {
  name: 'ComputeRootVolumes',
  components: {
    ComputeRootVolume,
  },
  props: ['rootVolumeOnComputeDomainIds'],
};
</script>
