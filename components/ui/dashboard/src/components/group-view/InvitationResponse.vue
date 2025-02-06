<template>
  <div class="panel pane-default">
    <div class="panel-body">
      <p>You have been invited to join this group created by {{ ownerUserName }}.</p>
      <button @click="$emit('acceptInvitation', collaboration)" class="btn btn-success" id="acceptButton">Accept Invitation</button>
      <button @click="$emit('declineInvitation', collaboration)" class="btn btn-warning" id="declineButton">Decline Invitation</button>
    </div>
  </div>
</template>
<script>
import { mapGetters } from 'vuex';
import find from 'lodash/find';
import { EventBus } from '../../main';

const $ = require('jquery');

export default {
  props: ['collaboration'],
  computed: {
    ownerUserName() {
      return this.getUserById(find(this.collaboration.members, { role: 'OWNER' }).id).username;
    },
    ...mapGetters(['getUserById']),
  },
  created() {
    EventBus.$on('acceptClicked', () => {
      this.acceptChange();
    });
    EventBus.$on('declineClicked', () => {
      this.declineChange();
    });
  },
  methods: {
    acceptChange() {
      $('#acceptButton').prop('disabled', true);
      $('#declineButton').prop('disabled', true);
      $('#acceptButton').html('Accepting ...');
    },
    declineChange() {
      $('#acceptButton').prop('disabled', true);
      $('#declineButton').prop('disabled', true);
      $('#declineButton').html('Declining ...');
    },
  },
};
</script>
