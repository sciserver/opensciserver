<template>
  <modal class="my-sciserver-modal-dialog-xxl" v-model="open" auto-focus>
    <div class="container-fluid">
      <div class="row">
        <!-- Users Table -->
        <div class="col-sm-5">
          <div class="panel panel-primary">
            <div class="panel-heading">
              <h5 class="panel-title">Available SciServer Users</h5>
            </div>
            <div class="panel-body">
              <div style="overflow-x:auto;">
                <table id="usersTable" style="color:black" class="table-bordered responsive hover order-column compact sciserver-datatable">
                  <thead class="thead-default">
                    <tr>
                       <th style="color:black">Name</th>
                    </tr>
                  </thead>
                  <tbody>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
        <!-- Buttons in the Center -->
        <div class="col-sm-2 setFreeSpace">
          <div class="btn-group-vertical">
                    <div class="dropdown" style="color:black">
                        <button title="invite user" class="dropdown-toggle" data-toggle="dropdown"
                            aria-haspopup="true" aria-expanded="false" >
                            Invite User
                        </button>
                        <ul class="dropdown-menu">
                            <li><a href="#" id="inviteADMIN" @click="addUsers('INVITED', 'ADMIN')">Admin</a></li>
                            <li><a href="#" id="inviteMEMBER" @click="addUsers('INVITED', 'MEMBER')">Member</a></li>
                        </ul>
                    </div>
                    <br/>
                    <br/>
                    <div v-show="isAdmin"> <!--  only admins are allowed to "Add" iso only "Invite" users to a group -->
                        <div class="dropdown" style="color:black">
                            <button title="Add User" class="dropdown-toggle" data-toggle="dropdown" role="button"
                                aria-haspopup="true" aria-expanded="false">
                                Add User
                            </button>
                            <ul class="dropdown-menu">
                                <li><a href="#" id="addADMIN" @click="addUsers('ACCEPTED', 'ADMIN')">Admin</a></li>
                                <li><a href="#" id="addMEMBER" @click="addUsers('ACCEPTED', 'MEMBER')">Member</a></li>
                            </ul>
                        </div>
                        <br/>
                        <br/>
                    </div>
                </div>
              </div>
        <!-- Members Table -->
        <div class="col-sm-5" style="overflow-y: auto">
          <div class="panel panel-primary">
            <div class="panel-heading">
              <h5 class="panel-title">Members</h5>
            </div>
            <div class="panel-body" style="color:black">
              <div class="table-responsive">
              <table id="userModalfilesTable" class="table table-hover sciserver-datatable">
                <thead>
                  <tr>
                     <th>Username</th>
                      <th>Member Role</th>
                      <th>Member Status</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="member in gMembers" :key="member.id">
                    <td>{{users[member.id].username}}</td>
                    <td>{{member.role}}</td>
                    <td>{{member.status}}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
    </div>
  </modal>
</template>
<script>
import { mapState, mapGetters, mapActions } from 'vuex';
import { EventBus } from '../../main';

const $ = require('jquery');

export default {
  name: 'newWorkspaceModal',
  data: () => ({
    checkedUsers: [],
    usersRow: [],
    open: false,
  }),
  props: ['workspace', 'gMembers'],
  computed: {
    allUsers() {
      return this.publicUsers.map(this.getUserById).map(u => [u.username, u.id, 'USER']);
    },
    workspaceId() {
      return this.workspace._links.self.href;
    },
    workspaceEditMember() {
      return this.workspace._links.editMemberList.href;
    },
    ...mapState(['userProfile', 'publicUsers', 'publicGroups', 'users', 'groups', 'token', 'collaborations']),
    ...mapGetters(['getUserById', 'getGroupById', 'isAdmin']),
    ...mapActions(['changeCollaborationMembership']),
  },
  watch: {
    allUsers() {
      if ($.fn.dataTable.isDataTable('#usersTable')) {
        const table = $('#usersTable').DataTable();
        table.clear();
        table.rows.add(this.allUsers).draw(false);
      } else {
        $('#usersTable').DataTable({
          data: this.allUsers,
          processing: true,
          bFilter: true,
          stateSave: false,
          select: true,
          paging: true,
        });
      }
    },
    open() {
      if (!this.open) {
        EventBus.$emit('closeAddMembers');
      }
    },
  },
  methods: {
    startDialog() {
      this.updateUsersTable();
      this.open = true;
    },
    addUsers(newStatus, newRole) {
      let user = '';
      const users = [];
      const data = $('#usersTable').DataTable().rows({
        selected: true,
      }).data();
      for (let i = 0; i < data.length; i += 1) {
        user = data[i][1];
        users.push(this.users[user]);
      }
      this.submitAddUsersRequest(
          users,
          newRole,
          newStatus);
    },
    submitAddUsersRequest(user, newRole, newStatus) {
      this.$emit('submitAddUsersRequest', user, newRole, newStatus);
    },
    updateUsersTable() {
      if ($.fn.dataTable.isDataTable('#usersTable')) {
        $('#usersTable').DataTable().clear().destroy();
      }
      $('#usersTable').DataTable({
        data: this.allUsers,
        processing: true,
        bFilter: true,
        stateSave: false,
        select: true,
        paging: true,
      });
    },
  },
};
</script>
<style scoped>
.setFreeSpace {
   margin-bottom: 20px;
   margin-top: 20px;
}
</style>
