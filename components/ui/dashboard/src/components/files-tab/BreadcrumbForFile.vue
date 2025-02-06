<template>
    <ul class="breadcrumb">
        <li>
            <a title="Main Directory" href="#" @click="resetPath"><i class="fa fa-home fa-2x" aria-hidden="true"></i></a>
        </li>
        <template v-if="currentUserVolume && currentUserVolume.type === 'uservolumes'">
            <li>{{ currentUserVolume.rootVolumeObj.name }}</li>
            <li>{{ currentUserVolume.owner }}</li>
            <li>
              <a href="#" @click="changeUV(currentUserVolume.id)">{{ currentUserVolume.name }}</a>
            </li>
        </template>
        <template v-if="currentUserVolume && currentUserVolume.type === 'datavolumes'">
          <li><a href="#" @click="changeUV(currentUserVolume.id)">{{ currentUserVolume.name }}</a></li>
        </template>
        <template v-if="path">
            <li v-for="(pathSegment, index) in path.split('/').filter(x => x)" :key="index">
              <a href="#"  @click="showFiles(index + 1)">{{ pathSegment }}</a>
            </li>
        </template>
    </ul>
</template>
<script>
export default {
  props: ['currentUserVolume', 'path'],
  methods: {
    changeUV(id) {
      this.$emit('changeUV', id);
    },
    showFiles(index) {
      this.$emit('showFiles', index);
    },
    resetPath() {
      this.$emit('resetPath');
    },
  },
};
</script>