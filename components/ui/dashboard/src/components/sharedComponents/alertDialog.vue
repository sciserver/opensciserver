<template>
  <modal v-model="open" :title="title">
    <form @submit.prevent>
      <div class="form-group">
          <span>{{message}}</span>
      </div>
    </form>
    <div slot="footer">
    </div>
  </modal>
</template>

<script>
export default {
  name: 'AlertPopUp',
  computed: {},
  data: () => ({
    open: false,
    message: '',
    title: '',
  }),
  watch: {},
  mounted() {},
  methods: {
    showAlert(message, title) {
      this.message = message;
      this.title = title;
      this.open = true;
    },
  },
};
</script>
<style>
</style>