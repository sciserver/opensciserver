<template>
  <div class="panel panel-primary">
    <div class="panel-heading panel-headings-with-buttons">
      <span>
      <span class="panel-title">Members</span>
         <a rel="noopener" class= "contextualHints" target="_blank" :href="inviteUserLink" title="How to invite" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">How to invite</span>
					  </a>
      </span>
          <users-list v-if="canEditMemberList" :workspace="collaboration" :members="members" @submitAddUsersRequest="changeMembership"/>
      </div>
      <ul class="list-group" :style="{ 'padding-bottom': isEditting ? '60px' : 'inherit' }">
        <li v-if="canEditMemberList">
            <div v-if="invitedUserId">
              <div class="invitation-support-box checkbox">
                <label>
                  <input v-model="invitedUserIsAdmin" type="checkbox">
                  Allow to edit group and invite others?
                </label>
              </div>
              <p v-if="isDeclinedUser(invitedUserId)" class="text-danger invitation-support-box">This user previously declined an invitation.</p>
              <div style="display: flex">
                <button @click="inviteUser(false)" style="flex: 1" class="btn btn-success">Invite User</button>
                <button @click="inviteUser(true)" style="flex: 1" v-if="canForceAdd" class="btn btn-warning">Force Add</button>
              </div>
            </div>
        </li>
        <li
        v-for="member in members"
        :key="member.user"
        v-if="['INVITED', 'ACCEPTED', 'OWNER'].includes(member.status)"
        :title="titleTextForUser(users[member.id])"
        class="list-group-item"
        :class="{'member-updating': member.updating}">
          <template v-if="!isEditting">
            <span v-if="member.status === 'INVITED'" class="badge">INVITED</span>
            <span v-else class="badge">{{ member.role }}</span>
            {{ users[member.id].username }}
          </template>
          <template v-else>
            {{ displayNameWhenEditting(users[member.id]) }}
            <br>
            <dropdown v-if="member.role !== 'OWNER'" style="width: 100%;">
              <btn type="default" class="dropdown-toggle" style="width: 100%;">
                <template v-if="member.role === 'ADMIN'">Can Edit Group &amp; Invite Others</template>
                <template v-else>Member</template>
                &nbsp;<span class="caret"></span>
              </btn>
              <template slot="dropdown">
                <li v-if="member.role === 'MEMBER'">
                  <a @click="promoteToAdmin(member.id, 'ADMIN')" role="button">Promote to Admin</a>
                </li>
                <li v-if="member.role === 'ADMIN'">
                  <a @click="switchToNonAdmin(member.id, 'MEMBER')" role="button">Switch to non-Admin member</a>
                </li>
                <li>
                  <a @click="removeMember(member)" role="button">
                    <template v-if="member.status === 'INVITED'">Rescind Invitation</template>
                    <template v-else>Remove</template>
                  </a>
                </li>
              </template>
            </dropdown>
          </template>
        </li>
      </ul>
    <div class="panel-footer text-right" style="padding: 0px;" v-if="canEditMemberList && collaboration.members">
        <button @click="isEditting = !isEditting" type="button" class="btn btn-link">
            <small v-if="!isEditting" class="text-danger">Edit Member List</small>
            <small v-if="isEditting">Finish Editting Member List</small>
        </button>
    </div>
  </div>
</template>
<script>
import { mapState, mapGetters, mapActions } from 'vuex';
import firstby from 'thenby';
import some from 'lodash/some';
import isNil from 'lodash/isNil';
import VueSelect2 from '../sharedComponents/VueSelect2';
import usersList from './addMembersButton';

export default {
  components: { VueSelect2, usersList },
  data: () => ({
    invitedUserId: undefined,
    invitedUserIsAdmin: false,
    isEditting: false,
  }),
  props: ['collaboration'],
  computed: {
    inviteUserLink() {
      return INVITE_USER;
    },
    canEditMemberList() {
      return this.collaboration._links.editMemberList;
    },
    members() {
      if (this.collaboration.type === 'COURSEWARE') {
        return this.collaboration.members;
      }
      return this.collaboration.members.sort(
        firstby(member => member.status === 'INVITED')
        .thenBy(member => ({ OWNER: 1, ADMIN: 2, MEMBER: 3 }[member.role]))
        .thenBy((a, b) => this.users[a.id].username.localeCompare(this.users[b.id].username)),
      );
    },
    selectUserData() {
      return this.publicUsers
        // .filter(id => !some(this.collaboration.members, { id, status: 'ACCEPTED' }))
        // .filter(id => !some(this.collaboration.members, { id, status: 'OWNER' }))
        .map(this.getUserById)
        .sort(firstby((a, b) => a.username.localeCompare(b.username)))
        .map(u => ({
          text: u.username,
          ...u,
        }));
    },
    canForceAdd() {
      return this.collaboration._links.forceAddMember;
    },
    ...mapState(['users', 'publicUsers', 'token']),
    ...mapGetters(['getUserById']),
  },
  methods: {
    formatUserSelection(user) {
      if (user.id === undefined) return user.text;
      let output = '';
      const fullname = user.fullname ? ` (${user.fullname})` : '';
      output += `<p>${user.username}${fullname}</p>`;
      if (!isNil(user.affiliation)) {
        output += `<p class="small text-muted">${user.affiliation}</p>`;
      }

      return output;
    },
    inviteUser(force) {
      const user = [];
      user.push(this.users[this.invitedUserId]);
      this.changeMembership(
        user,
        this.invitedUserIsAdmin ? 'ADMIN' : 'MEMBER',
        force ? 'ACCEPTED' : 'INVITED');

      this.resetInvitation();
    },
    changeMembership(user, newRole, newStatus) {
      this.changeCollaborationMembership({
        collaborationId: this.collaboration._links.self.href,
        editMemberListEndpoint: this.collaboration._links.editMemberList.href,
        user,
        newRole,
        newStatus,
      });
    },
    promoteToAdmin(memberID, role) {
      const user = [];
      user.push(this.users[memberID]);
      this.changeMembership(
          user,
          role);
    },
    switchToNonAdmin(memberID, role) {
      const user = [];
      user.push(this.users[memberID]);
      this.changeMembership(
          user,
          role);
    },
    resetInvitation() {
      this.invitedUserId = undefined;
      this.invitedUserIsAdmin = false;
    },
    resetEditting() {
      this.isEditting = false;
    },
    displayNameWhenEditting(user) {
      const fullnameAddition = user.fullname ? ` (${user.fullname})` : '';
      return `${user.username}${fullnameAddition}`;
    },
    titleTextForUser(user) {
      return [user.fullname, user.affiliation].join('\n');
    },
    isDeclinedUser(userId) {
      return some(this.collaboration.members, { status: 'DECLINED', id: userId });
    },
    removeMember(member) {
      if (confirm(`Are you sure you want to remove ${this.displayNameWhenEditting(this.users[member.id])} from this group?`)) {
        const user = [];
        user.push(this.users[member.id]);
        this.changeMembership(
          user,
          member.role,
          'WITHDRAWN');
      }
    },
    matchInvitedUser(params, data) {
      const term = params.term.toLowerCase().trim();

      if (term.trim() === '') {
        return data;
      }
      if (typeof data.text === 'undefined' || data.text === '') {
        return null;
      }
      if (data.username.toLowerCase().indexOf(term) > -1
        || (data.fullname && data.fullname.toLowerCase().indexOf(term) > -1)
        || (data.affiliation && data.affiliation.toLowerCase().indexOf(term) > -1)) {
        return data;
      }

      return null;
    },
    ...mapActions(['changeCollaborationMembership']),
  },
  watch: {
    collaboration(newCollaboration, oldCollaboration) {
      if (newCollaboration.id !== oldCollaboration.id) {
        this.resetInvitation();
        this.resetEditting();
      }
    },
  },
};
</script>

<style scoped>
  @media only screen and (min-width: 992px) {
    .list-group {
      overflow-y: auto;
      max-height: 75vh !important;
    }
  }

.invitation-support-box {
  display: block;
  padding: 10px 15px;
  margin-bottom: 1px;
  margin-left: auto;
  margin-right: auto;
}
.dropdown-toggle {
  width: 100%;
  text-overflow: ellipsis;
  overflow: hidden;
}

/* subtle hint that we are saving this change of status */
.member-updating {
  background:
    /* On "top" */
    repeating-linear-gradient(
      45deg,
      transparent,
      transparent 10px,
      #eee 10px,
      rgb(247, 247, 247) 20px
    ),
    /* on "bottom" */
    linear-gradient(
      to bottom,
      #eee,
      rgb(255, 255, 255)
    );
}
.panel-headings-with-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
