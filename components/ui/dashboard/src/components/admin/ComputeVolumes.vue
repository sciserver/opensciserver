<template>
  <ul class="list-group">
    <compute-volume v-for="volume in computeOnlyIds" :key="`compute:${volume}`" :volume-id="`compute:${volume}`" />
    <compute-volume v-for="volume in volumeContainerIds" :key="volume" :volume-id="volume" />
  </ul>
</template>

<script>
import ComputeVolume from './ComputeVolume';

export default {
  name: 'ComputeVolumes',
  components: {
    ComputeVolume,
  },
  props: ['volumeContainerIds', 'computeOnlyIds'],
};
</script>
