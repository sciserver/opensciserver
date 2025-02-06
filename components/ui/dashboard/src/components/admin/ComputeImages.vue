<template>
  <ul class="list-group">
    <compute-image v-for="image in computeOnlyIds" :key="`compute:${image}`" :imageId="`compute:${image}`" />
    <compute-image v-for="image in imageIds" :key="image" :imageId="image" />
  </ul>
</template>

<script>
import ComputeImage from './ComputeImage';

export default {
  name: 'ComputeImages',
  components: {
    ComputeImage,
  },
  props: ['imageIds', 'computeOnlyIds'],
};
</script>
