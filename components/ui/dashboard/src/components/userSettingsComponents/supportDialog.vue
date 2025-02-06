<template>
 <modal v-model="open" class="my-sciserver-modal-dialog" title="Support Links" size="sm" auto-focus>
  <div class="container-fluid">
          <div class="row">
            <div class="col-md-9">
              <a :href="userGuides" title="Go to User Guide" rel="noopener" target="_blank" style=" text-decoration: underline; font-size: 14px;">User guides</a>
              <br>
              <a :href="apiDocumentation" title="Go to API Documentation" rel="noopener" target="_blank" style=" text-decoration: underline; font-size: 14px;">API documentation</a>
              <br>
              <a :href="helpDesk" title="Email to help desk" style=" text-decoration: underline; font-size: 14px;">Help desk</a>
              <br>
              <a :href="bugReportForm" title="Report Bug" rel="noopener" target="_blank" style=" text-decoration: underline; font-size: 14px;">Bug report form</a>
            </div>
          </div>
        </div>
    <div slot="footer">
        <button type="button" class="btn btn-default" @click="open = false"  data-action="auto-focus">Close</button>
    </div>
  </modal>
</template>
<script>
export default {
  name: 'supportDialog',
  computed: {},
  data: () => ({
    open: false,
    userGuides: USER_GUIDES,
    apiDocumentation: API_DOCUMENTATION,
    helpDesk: HELP_DESK,
    bugReportForm: BUG_REPORT_FORM,
  }),
  methods: {
    startDialog() {
      this.open = true;
    },
  },
};
</script>