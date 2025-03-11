<template>
  <div>
    <div class="container-fluid wrap">
      <div class="row">
        <div class="col-sm-3">
        <list-group title="Compute Domains" :headingType="`primary`" :items="racmDomains" :onClick="selectDomain" :selectedId="selectedId">
          <span slot="headerContent">
             <button type="button" class="btn btn-primary" @click="loadAllInfo" title="Refresh">
                  <i class="fa fa-refresh" :class="refreshSpinner" aria-hidden="true"></i>
                </button>
                <span class="pull-right">
                <button type="button" class="btn btn-success btn-sm" @click="newDomain" title="Create New Compute Domain">
                <i class="fa fa-plus" aria-hidden="true"></i>
              </button>
                </span>
          </span>
        </list-group>
        </div>
        <div class="col-sm-9">
          <compute-domain
            @on-save="loadAllInfo"
            :selected-id="shownId"
            v-if="shownId" />
          <div v-else class="jumbotron">
            <h1>No Compute Domains Registered</h1>
            <p>In order to use SciServer Compute, please <a @click="newDomain">create</a>
            one or more compute domains.</p>
          </div>
        </div>
      </div>
      <new-domain-wizard ref="newDomainWizard" @on-save="loadAllInfo" />
    </div>
  </div>
</template>

<script>
import head from 'lodash/head';
import keys from 'lodash/keys';
import values from 'lodash/values';
import ComputeDomain from './ComputeDomain';
import NewDomainWizard from './compute/NewDomainWizard';
import listGroup from '../sharedComponents/listGroup';

export default {
  name: 'ComputeAdmin',
  components: {
    ComputeDomain,
    NewDomainWizard,
    listGroup,
  },
  data: () => ({
    selectedId: undefined,
    isLoading: false,
  }),
  computed: {
    shownId() {
      if (this.selectedId) return this.selectedId;
      if (this.racmDomains.length) {
        return head(keys(this.$store.state.compute.racmComputeDomains));
      }
      return undefined;
    },
    racmDomains() {
      return values(this.$store.state.compute.racmComputeDomains);
    },
    refreshSpinner() {
      return {
        'fa-spin': this.isLoading,
      };
    },
  },
  methods: {
    selectDomain(id) {
      this.selectedId = id;
    },
    newDomain() {
      this.$refs.newDomainWizard.startDialog();
    },
    async loadAllInfo() {
      this.isLoading = true;
      await Promise.all([
        this.$store.dispatch('loadCompute'),
        this.$store.dispatch('loadDomainsFromCompute'),
        this.$store.dispatch('loadImagesFromCompute'),
        this.$store.dispatch('loadVolumesFromCompute'),
        this.$store.dispatch('loadNodesFromCompute'),
        this.$store.dispatch('loadCollaborations')]);
      this.isLoading = false;
    },
  },
  mounted() {
    this.loadAllInfo();
  },
};
</script>
<style scoped>
.panel-headings-with-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
