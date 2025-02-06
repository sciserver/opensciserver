<template>
  <div class="lookUp">
      <div class="form-group">
        <label>{{title}}</label>
          <input type="text" name="search" class="form-control main-search" :placeholder="`Enter ${title}`" v-model="searchTerm">
      </div>
      <ul class = "list-group" v-if="searchTerm">
        <a href="#" class="list-group-item" v-for="(item, index) in searchedActivities" :key="index"  @click="addToList(item)">{{item}}</a>
      </ul>
    </div>
</template>
<script>
import axios from 'axios';
import { mapState } from 'vuex';

export default {
  props: ['title', 'selectData'],
  data: () => ({
    open: false,
    searchTerm: '',
    selectedObjects: [],
  }),
  computed: {
    isValid() {
      return this.selectedObjects.length > 0;
    },
    searchedActivities() {
      return this.selectData.filter(selectData =>
        (selectData.username.toLowerCase().match(this.searchTerm.toLowerCase())));
    },
    ...mapState(['userProfile', 'token', 'collaborationLinks']),
  },
  methods: {
    newGroup() {
      this.selectedObjects = [];
      this.searchTerm = '';
      this.open = true;
    },
    addToList(item) {
      this.selectedObjects.push(item);
    },
  },
};
</script>

<style scoped>
div.addInviteUser {
  color: black;
}
.list-group{
    max-height: 300px;
    margin-bottom: 10px;
    overflow-y:scroll;
    -webkit-overflow-scrolling: touch;
}
</style>