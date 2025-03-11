<template>
<div id = "resourcesTab">
<div class="container-fluid wrap">
<div class="row" id="resourcestab">
    <div id="resourcesDiv" class="col-lg-12 col-md-12 col-sm-12 ">
      <div class="panel panel-primary">
        <div class="panel-heading panel-heading-style">
          <span class = "panel-title">Grantable Resources</span>
          <button class="btn btn-primary" id="resourcesRefreshLink" title="Refresh"><i class="fa fa-refresh" aria-hidden="true"></i></button>
        </div>
        <div class="panel-body">
          <p>In this pre-production release, this page provides a listing of the resources you are able to grant to other users (for example, to allow them to write to a user volume, or make containers with your Docker images).</p>
          <div class="table-responsive">
            <table id="resourcesTable" class="table-bordered hover order-column compact sciserver-datatable">
              <tbody id="resourcesTableBody">
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div id="grantPrivilegesDialog">
    <grant-privileges-dialog/>
  </div>
  </div>
</div>
</template>
<script>
import { mapState, mapGetters } from 'vuex';

import GrantPrivilegesDialog from './resource-view/GrantPrivilegesDialog';

const Config = require('../config-loader').default;

export default {
  name: 'ResourcesTab',
  components: { GrantPrivilegesDialog },
  created() {
    this.$store.dispatch('loadScienceDomains', this.userProfile);
  },
  data: () => ({
  }),
  computed: {
    ...mapGetters(['isAdmin']),
    ...mapState(['userProfile', 'token', 'configLinks', 'publicUsers', 'publicGroups']),
  },
  mounted() {
    window.Config = new Config(this.userProfile, this.token);
  },
};
</script>
