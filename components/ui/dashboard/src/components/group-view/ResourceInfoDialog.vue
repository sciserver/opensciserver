<template>
  <modal v-model="open" :title="title" class="my-sciserver-modal-dialog" auto-focus>
    <slot :info="info" :groupInfo="groupInfo"/>
  </modal>
</template>
<script>
export default {
  props: ['title'],
  data: () => ({
    open: false,
    info: {},
    groupInfo: {},
  }),
  methods: {
    startDialog(info, groupInfo) {
      this.info = info;
      this.groupInfo = groupInfo;
      this.open = true;
    },
  },
};
</script>

