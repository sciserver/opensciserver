<template>
  <modal v-model="open" class="my-sciserver-modal-dialog" title="View Quotas" auto-focus>
    <div
      v-for="quotaGroup in sortedQuotas"
      :key="quotaGroup[0].rootVolumeId"
      class="panel panel-default">
  <div class="panel-heading">
    <template v-if="quotaGroup[0].rootVolume && quotaGroup[0].rootVolume.name ">
      {{ quotaGroup[0].rootVolume.name }}
    </template>
     on {{ quotaGroup[0].fileservice.name }} 
  </div>
      <div
        v-for="quota in quotaGroup"
        :key="quota.rootVolumeId + quota.type + quota.username + quota.userVolumeId"
        class="list-group-item">
        <template v-if="quota.username">
          {{ quota.username }}'s user volumes:
        </template>
        <template v-else>
          {{ get(getUserVolumeById(quota.userVolumeId), 'name') }}
        </template>
        <div class="file-usage-full-box">
          <div class="file-usage-used-box" :style="styleForUsageBox(quota)"></div>
        </div>
        <br/>{{ quota.numberOfBytesUsed | filesize }} used out of {{ quota.numberOfBytesQuota | filesize }}
      </div>
    </div>
    <div slot="footer">
      <p class="text-muted">File usage information can take up to 30 minutes to first appear or update.</p>
      <btn @click="open = false" data-action="auto-focus">Close</btn>
    </div>
  </modal>
</template>
<script>
import { mapState, mapGetters } from 'vuex';
import get from 'lodash/get';
import flatMap from 'lodash/flatMap';
import groupBy from 'lodash/groupBy';
import sortBy from 'lodash/sortBy';
import firstBy from 'thenby';
import filesize from 'filesize';

export default {
  data: () => ({
    open: false,
  }),
  computed: {
    myFileserviceIdentifiers() {
      return this.$store.state.files.myFileserviceIdentifiers;
    },
    quotasForFileserviceIdentifier() {
      return identifier => get(this.$store.state.files.quotasPerFileservice, identifier, []);
    },
    sortedQuotas() {
      return sortBy(groupBy(flatMap(this.myFileserviceIdentifiers
        .map(this.getFileserviceByIdentifier),
         fileservice =>
          this.quotasForFileserviceIdentifier(fileservice.identifier)
            .map(q => ({
              fileservice,
              rootVolume: this.getRootVolumeById(q.rootVolumeId),
              ...q,
            }))), 'rootVolumeId'), listOfQuotas => get(listOfQuotas[0], 'fileservice.name'))
            .map(listOfQuotas => listOfQuotas.sort(
              firstBy('type')
              .thenBy(quota => quota.username !== this.userProfile.username)
              .thenBy('username')
              .thenBy(quota => get(this.getUserVolumeById(get(quota, 'userVolumeId')), 'name'))));
    },
    ...mapState(['userProfile']),
    ...mapGetters(['getRootVolumeById', 'getFileserviceByIdentifier', 'getUserVolumeById']),
  },
  methods: {
    startDialog() {
      this.open = true;
    },
    styleForUsageBox: quota => ({
      width: `${100 * Math.min(1, quota.numberOfBytesUsed / quota.numberOfBytesQuota)}%`,
      background: quota.numberOfBytesUsed >= quota.numberOfBytesQuota ? 'red' :
                    'lime',
    }),
  },
  filters: {
    filesize,
  },
};
</script>

<style scoped>
  .file-usage-full-box {
    width:100%;
    height:24px;
    border: thin solid;
  }
  .file-usage-used-box {
    border-right: thin solid;
    height:100%;
  }
</style>