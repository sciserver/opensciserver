<template>
  <modal v-model="open" class="my-sciserver-modal-dialog" title="Delete Dialog">
    <form @submit.prevent>
      <div class="form-group">
        <p>Are you sure you want to delete {{numFiles}} files?</p>
      </div>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="deleteItem">Yes</button>
      <button type="button" class="btn btn-default" @click="open = false">Cancel</button>
    </div>
  </modal>
</template>
<script>
import axios from 'axios';
import { mapState } from 'vuex';
import Vue from 'vue';
import { mixin as focusMixin } from 'vue-focus';

export default {
  mixins: [focusMixin],
  data: () => ({
    open: false,
    name: undefined,
    url: '',
    title: `Delete ${name}`,
    numFiles: 0,
  }),
  computed: {
    ...mapState(['userProfile', 'token']),
  },
  methods: {
    startDialog(name, url) {
      this.open = true;
      this.name = name;
      this.url = url;
    },
    deleteItem() {
      this.open = false;
      const config = { headers: { 'X-Auth-Token': this.token } };
      axios.delete(this.url, config)
      .then(() => {
        Vue.notify({
          group: 'top_center_notify',
          text: 'Delete Successful!',
          duration: 1000,
          type: 'error',
          ignoreDuplicates: true,
        });
        this.$emit('itemDeleted');
      }, (error) => {
        alert(`Error Message: ${error.response.data.error}`);
      });
    },
  },
};
</script>
