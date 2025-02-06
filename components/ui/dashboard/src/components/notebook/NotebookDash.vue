<template>
  <div id="NotebookDash" ref="notebookDash">
    <div class="full-overlay" style="min-width: 400px; padding-top: 60px;">
      <div style="width:80%; max-width:1000px; margin: auto">
        <div v-if="$route.query.nf" style="display: block; padding-bottom: 12px"> Could not find notebook! </div>
        <router-link v-if="oneclickNotebookPath" :to="`/notebook/${oneclickNotebookPath}?copy=true`"><button class="btn btn-primary"> Quick Notebook </button> </router-link>
        <recent-notebook-list style="padding-top: 12px" ref="recentNotebookList"></recent-notebook-list>
      </div>
    </div>
  </div>
</template>

<script>
import applicationConfiguration from '../../applicationConfiguration';
import titleMixin from '../../titleMixin';
import RecentNotebookList from './RecentNotebookList';

export default {
    name: 'NotebookDash',
    components: {
        RecentNotebookList,
    },
    mixins: [
        titleMixin,
        applicationConfiguration,
    ],
    data: () => ({
        title: 'Notebooks',
    }),
};
</script>

<style>
main {
    position: relative;
}
</style>

<style scoped>
.full-overlay {
    position: absolute;
    left: 0px;
    right: 0px;
    bottom: 0px;
    top: 0px;
    border: 0px;
    background-color: #f5faff;
    overflow-y: auto;
}
</style>
