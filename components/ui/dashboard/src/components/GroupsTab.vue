<template>
<div id="workspace-tab">
  <div class="container-fluid wrap">
  <div class="row" v-if="resourcesLoaded && shownCollaborationObject"
       style="display: flex;align-items: flex-start;">
    <div class="col-sm-3" style="position: sticky; top: 8px;">
      <list-group title="Groups" :headingType="`primary`" style="border-color: #337ab7;">
         <span slot="headerContent">
             <a rel="noopener" class= "contextualHints" target="_blank" :href="createGroup" title="More Info on Groups" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">More Info on Groups</span>
					  </a>
            <button type="button" class="btn btn-primary" @click="loadResources" title="Refresh"><i class="fa fa-refresh" :class="refreshGroupViewSpinner" aria-hidden="true"></i></button>
            <span class="pull-right" v-on:click="nonStickyDiv">
          <new-group-button @selectNewGroup="selectNewGroup" @closedModal="stickyDiv"><i class="fa fa-plus" aria-hidden="true" ></i></new-group-button>
          </span>
          </span>
        <input type="text" name="search" class="form-control main-search" placeholder="Filter..." v-model="searchTerm">
        <div id="list_group_div">
        <transition-group name="group-list">
            <router-link :to="`/groups/${collaborations[collaborationId].groupId}`"
            v-for="collaborationId in searchedGroups"
            :key="collaborationId"
            v-on:click.native="selectedCollaborationId = collaborationId"
            class="list-group-item"
            :class="{active: shownCollaborationId === collaborationId}"
            type="button"
            role="tab">
            {{ collaborations[collaborationId].name }}
            <span v-if="myStatus(collaborationId) === 'INVITED'" class="glyphicon glyphicon-exclamation-sign"></span>
        <button type="button" class="btn btn-link pull-right" @click="leaveCollaboration" v-if="collaborations[collaborationId]._links.leave && shownCollaborationId === collaborationId">
            <small class="text-danger">Leave {{shownCollaborationObject.type.toLowerCase()}}</small>
        </button>
        <button type="button" @click="deleteCollaboration" class="btn btn-link pull-right" v-if="collaborations[collaborationId]._links.delete && shownCollaborationId === collaborationId">
          <small class="text-danger">Delete {{shownCollaborationObject.type.toLowerCase()}}</small>
        </button>
          </router-link>
          </transition-group>
        </div>
        </list-group>
    </div>
    <div :class="[invitedToShownCollaboration ? 'col-sm-9' : 'col-sm-6']">
      <collaboration-info :collaboration="shownCollaborationObject"/>
      <resources-list :selectedResources="shownResources" :collaboration="shownCollaborationObject" :collabType="type" :collabOwner="collaborationOwner" v-if="!invitedToShownCollaboration" @shareResources="addResources" @unshareResource="deleteResources"/>
      <invitation-response
        v-if="invitedToShownCollaboration  && displayInvite"
        :collaboration="shownCollaborationObject"
        @acceptInvitation="acceptInvitation"
        @declineInvitation="declineInvitation" />
    </div>
    <div class="col-sm-3" v-if="!invitedToShownCollaboration" style="position: sticky; top: 8px;">
       <members-list :collaboration="shownCollaborationObject"/>
    </div>
  </div>
  <empty-state :label="`Collaborate with colleagues`" :description="`Groups allow you to share files and other resources within a collaboration.`" v-if="!shownCollaborationId">
    <new-group-button :button-classes="['btn-lg']">Create New Group</new-group-button>
  </empty-state>
    <div id="groupEditDialog">
    <group-edit-dialog/>
  </div>
</div>
  </div>
</template>

<script>
/*
The workspace page is
currently changed to group view.
Group view is a way to display
users group and the resources
shared with the user.
*/
import { mapState, mapGetters } from 'vuex';
import Vue from 'vue';
import find from 'lodash/find';
import firstby from 'thenby';
import ResourcesList from './group-view/ResourcesList';
import MembersList from './group-view/MembersList';
import InvitationResponse from './group-view/InvitationResponse';
import NewGroupButton from './group-view/NewGroupButton';
import CollaborationInfo from './group-view/CollaborationInfo';
import GroupEditDialog from './group-view/GroupEditDialog';
import EmptyState from './sharedComponents/emptyState';
import listGroup from './sharedComponents/listGroup';
import { EventBus } from '../main';

const $ = require('jquery');
const apiCall = require('../apiCall').default;

export default {
  name: 'WorkspaceTab',
  components: {
    ResourcesList,
    MembersList,
    InvitationResponse,
    NewGroupButton,
    CollaborationInfo,
    GroupEditDialog,
    EmptyState,
    listGroup,
  },
  created() {
    this.loadResources();
    this.$store.dispatch('loadScienceDomains', this.userProfile);
    EventBus.$on('addMembers', () => {
      this.nonStickyDiv();
    });
    EventBus.$on('closeAddMembers', () => {
      this.stickyDiv();
    });
  },
  data: () => ({
    selectedCollaborationId: undefined,
    searchTerm: '',
    groupsList_border: '',
    createGroup: CREATE_GROUP,
    type: 'Group',
    displayInvite: true,
  }),
  beforeRouteEnter(to, from, next) {
    next((vm) => {
      if (vm.lastGroupRoute && !vm.groupid) {
        next(`groups/${vm.lastGroupRoute}`);
      } else if (vm.groupid && !vm.lastGroupRoute) {
        if (vm.collaborations && vm.collaborations[vm.sortedCollaborationIds[0]]
        && vm.collaborations[vm.sortedCollaborationIds[0]].groupId) {
          next(`groups/${vm.collaborations[vm.sortedCollaborationIds[0]].groupId}`);
        }
      } else {
        next();
      }
    });
  },
  computed: {
    searchedGroups() {
      return this.sortedCollaborationIds.filter(id =>
        (this.collaborations[id].name.toLowerCase().match(this.searchTerm.toLowerCase())));
    },
    findCurrentGroupKey() {
      const key = Object.keys(this.collaborations);
      return key.find(k => this.collaborations[k].groupId.toString() === this.groupid);
    },
    shownCollaborationId() {
      if (this.findCurrentGroupKey) {
        return this.findCurrentGroupKey;
      }
      return this.sortedCollaborationIds[0];
    },
    groupid() {
      return this.$route.params.groupId;
    },
    shownCollaborationObject() {
      return this.collaborations[this.shownCollaborationId];
    },
    sortedCollaborationIds() {
      return this.myCollaborations
        /* Only show groups. */
        .filter(id => this.collaborations[id].type === 'GROUP')
        .sort(
          firstby(id => this.myStatus(id) !== 'INVITED')
          .thenBy((a, b) => this.collaborations[a].name.localeCompare(this.collaborations[b].name)),
        );
    },
    resourcesLoaded() {
      return this.collaborationLoadingStatus === 'LOADED' ||
        this.collaborationLoadingStatus === 'RELOADING';
    },
    resourcesLoading() {
      return this.collaborationLoadingStatus === 'LOADING' ||
        this.collaborationLoadingStatus === 'RELOADING';
    },
    invitedToShownCollaboration() {
      if (this.shownCollaborationObject._links.acceptInvitation) {
        this.displayInvite = true;
        return true;
      }
      return false;
    },
    shownResources() {
      if (!this.shownCollaborationObject) return [];
      return this.shownCollaborationObject.resources;
    },
    refreshGroupViewSpinner() {
      return {
        'fa-spin': this.resourcesLoading,
      };
    },
    isDeletable() {
      return this.shownCollaborationObject._links.delete;
    },
    isLeavable() {
      return this.shownCollaborationObject._links.leave;
    },
    collaborationOwner() {
      return this.shownCollaborationObject.members.find(k => k.id === this.userProfile.id);
    },
    ...mapState(['userProfile', 'token', 'users',
      'collaborationLoadingStatus', 'collaborations', 'myCollaborations', 'collaborationLoadingStatus', 'collaborationLinks']),
    ...mapGetters(['lastGroupRoute']),
  },
  watch: {
    groupid() {
      this.$store.dispatch('loadLastGroupPath', this.groupid);
    },
  },
  methods: {
    loadResources() {
      this.$store.dispatch('loadCollaborations');
    },
    declineInvitation(collaboration) {
      if (confirm('Are you sure you want to decline membership?') === false) {
        return;
      }
      EventBus.$emit('declineClicked');
      apiCall(collaboration._links.declineInvitation.href, this.token, 'POST', '', () => {
        this.$store.dispatch('loadCollaborations').then(() => {
          Vue.notify({
            group: 'top_center_notify',
            text: `${collaboration.name} declined!`,
            duration: 1000,
            type: 'warn',
            ignoreDuplicates: true,
          });
          this.displayInvite = false;
          this.unSelectDeletedGroup();
        });
      }, null);
    },
    acceptInvitation(collaboration) {
      EventBus.$emit('acceptClicked');
      apiCall(collaboration._links.acceptInvitation.href, this.token, 'POST', '', () => {
        Vue.notify({
          group: 'top_center_notify',
          text: `${collaboration.name} joined!`,
          duration: 1000,
          type: 'success',
          ignoreDuplicates: true,
        });
        this.displayInvite = false;
        this.loadResources();
      }, null);
    },
    leaveCollaboration() {
      if (confirm(`Are you sure you want to leave the group '${this.shownCollaborationObject.name}'?`)) {
        apiCall(this.shownCollaborationObject._links.leave.href, this.token, 'POST', '', () => {
          this.$store.dispatch('loadCollaborations').then(() => {
            this.unSelectDeletedGroup();
          });
          this.selectedCollaborationId = undefined;
        }, null);
      }
    },
    deleteCollaboration() {
      if (!confirm(`Do you really want to delete the group '${this.shownCollaborationObject.name}' ?`)) {
        return;
      }
      apiCall(this.shownCollaborationObject._links.delete.href, this.token, 'DELETE', '', () => {
        this.$store.dispatch('loadCollaborations').then(() => {
          this.unSelectDeletedGroup();
        });
        this.selectedCollaborationId = undefined;
      }, null);
    },
    unSelectDeletedGroup() {
      if (!this.groupid) {
        this.$router.replace(`groups/${this.shownCollaborationObject.groupId}`);
      } else {
        this.$router.replace(`${this.shownCollaborationObject.groupId}`);
      }
    },
    myStatus(collaborationId) {
      return find(this.collaborations[collaborationId].members, { id: this.userProfile.id }).status;
    },
    addResources(resource, privilege, collaboration) {
      const url = collaboration._links.shareResource.href.split('?');
      const path = url[0];
      apiCall(`${path}?resourceType=${resource.type}&actions=${privilege}&entityId=${resource.entityId}`, this.token, 'PUT', '', this.loadResources, null);
    },
    deleteResources(resources) {
      const url = this.shownCollaborationObject._links.shareResource.href.split('?');
      const path = url[0];
      if (confirm(`Are you sure you want to remove ${resources[0].name}?`) === false) {
        return;
      }
      resources.forEach((resource) => {
        apiCall(`${path}?resourceType=${resource.type}&actions=&entityId=${resource.entityId}`, this.token, 'PUT', '', this.loadResources, null);
      });
    },
    selectNewGroup(newGroupName) {
      $('.col-sm-3').css('position', 'sticky');
      const id = this.findCurrentGroupKeyByName(newGroupName);
      const groupObj = this.findCurrentGroupObj(id);
      if (groupObj && groupObj.groupId) {
        if (!this.groupid) {
          this.$router.replace(`groups/${groupObj.groupId}`);
        } else {
          this.$router.replace(`${groupObj.groupId}`);
        }
      }
    },
    nonStickyDiv() {
      $('.col-sm-3').css('position', '');
      $('.col-sm-3').css('top', '0px');
    },
    stickyDiv() {
      $('.col-sm-3').css('position', 'sticky');
      $('.col-sm-3').css('top', '8px');
    },
    findCurrentGroupKeyByName(groupName) {
      const key = Object.keys(this.collaborations);
      const foundGroup = key.find(k => this.collaborations[k].name === groupName);
      return foundGroup;
    },
    findCurrentGroupObj(groupId) {
      return this.collaborations[groupId];
    },
  },
};
</script>

<style scoped>
  @media only screen and (min-width: 992px) {
      .col-sm-3 {
        max-height: 80vh !important;
      }
      #list_group_div {
        max-height: 74vh !important;
        background-color: #F5FAFF;
        overflow-y: auto;
      }
  }
</style>
