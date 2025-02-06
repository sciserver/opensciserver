<template>
  <div class="my-component">
    <button type="button" class="btn btn-success btn-sm" :class="buttonClasses" @click="newGroup" title="Create New Group"><slot></slot></button>
    <modal v-model="open" title="New Group" class="my-sciserver-modal-dialog">
      <form @submit.prevent>
        <div class="form-group">
          <label>Group Name</label>
          <input v-model="name" type="text" class="form-control" placeholder="Group Name" @keyup.enter="saveNewGroup" v-focus="focused" @focus="focused = true" @blur="focused = false">
        </div>
        <div class="form-group">
          <label>Description</label>
          <textarea v-model="description" class="form-control" rows="3" placeholder="Group Description" @keyup.enter="saveNewGroup"></textarea>
        </div>
      </form>
      <template slot="footer">
        <btn @click="open = false">Cancel</btn>
        <btn @click="saveNewGroup" type="success" :disabled="!isValid">Create</btn>
      </template>
      <div>
        <h5>*If creating a group to share tables in CASJobs, note that the following naming conventions apply: </h5>
        <ul class="bulletList">
          <li>group name may only contain letters and numbers</li>
          <li>must start with a letter</li>
        </ul>
      </div>
    </modal>
  </div>
</template>

<script>
import axios from 'axios';
import Vue from 'vue';
import { mapState, mapGetters } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';

export default {
  mixins: [focusMixin],
  props: ['buttonClasses'],
  data: () => ({
    open: false,
    name: '',
    description: '',
    focused: false,
  }),
  computed: {
    isValid() {
      return this.name;
    },
    ...mapState(['userProfile', 'token', 'collaborationLinks', 'publicGroups']),
    ...mapGetters(['getGroupById']),
  },
  methods: {
    newGroup() {
      this.focused = true;
      this.open = true;
      this.name = '';
      this.description = '';
    },
    saveNewGroup() {
      const group = {
        groupName: this.name,
        description: this.description,
        memberUsers: [],
      };
      if (this.isDuplicateGroupName(group)) {
        alert(`'${group.groupName}' name already exists. Pick another group name.`);
      } else {
        this.open = false;
        const config = { headers: { 'X-Auth-Token': this.token } };
        axios.post(this.collaborationLinks.createGroup.href, group, config)
        .then(() => {
          this.$store.dispatch('loadCollaborations').then(() => {
          // https://forum.vuejs.org/t/accessing-vuex-store-after-dispatch/14686/2
            this.$emit('selectNewGroup', this.name);
            Vue.notify({
              group: 'top_center_notify',
              text: `Successfully created ${group.groupName}!`,
              duration: 1000,
              type: 'success',
            });
          });
        }, (error) => {
          alert(`Error Message: ${error.response.data.error}`);
        });
      }
    },
    isDuplicateGroupName(group) {
      let exists = false;
      const found = this.publicGroups.map(this.getGroupById)
      .find(g => g.groupName.toLowerCase() === group.groupName.toLowerCase());
      if (found) {
        exists = true;
      } else {
        exists = false;
      }
      return exists;
    },
  },
  watch: {
    open() {
     if (!this.open) {
       this.$emit('closedModal', '');
     }
    },
  },
};
</script>

<style scoped>
div.my-component {
  color: black;
}
ul.bulletList {
    list-style-type: circle;
    list-style-position: inside;
}
</style>
