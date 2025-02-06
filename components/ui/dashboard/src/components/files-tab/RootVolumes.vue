<template>
    <transition-group name="group-list" class="list-group" tag="div">
        <button @click="changeVolumesType(item.type)"
            v-for="item in currentRootVolumes"
            :key="item.name"
            class="list-group-item"
            type="button" 
            :class="{active: item.type === selectedVolumeType}">
            {{ item.name }}
        </button>
    </transition-group>
</template>
<script>
export default {
  props: ['currentRootVolumes', 'selectedVolumeType'],
  methods: {
    changeVolumesType(item) {
      this.$emit('changeVolumesType', item);
    },
  },
};
</script>
