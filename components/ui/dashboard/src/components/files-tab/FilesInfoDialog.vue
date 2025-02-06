<template>
  <modal v-model="open" :title="title" class="my-sciserver-modal-dialog" auto-focus>
    <slot :info="info"/>
  </modal>
</template>
<script>
export default {
  props: ['title'],
  data: () => ({
    open: false,
    info: {},
  }),
  methods: {
    startDialog(info) {
      this.info = info;
      this.open = true;
    },
  },
};
</script>
