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
                Add Member
              </button>
              <ul class="dropdown-menu">
                <li><a href="#" id="inviteADMIN" @click="addUsers('addTA', 'INVITED')">TA</a></li> <br>
                <li><a href="#" id="inviteMEMBER" @click="addUsers('addStudent', 'INVITED')">STUDENT</a></li>
              </ul>
            </div>
            <br/>
            <br/>
          </div>
        </div>
        <!-- Members Table -->
        <div class="col-sm-5">
          <h5> Please select a member to remove from class. </h5>
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
                  <tr v-for="member in getMembers" @click="getSelected($event)" v-bind:id="member.name+' '+member.role">
                  <td>{{member.name}}</td>
                    <td>{{member.role}}</td>
                    <td>{{member.status}}</td>
                    <td><i class="fa fa-trash" aria-hidden="true"></i></td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <ul v-if="currentSelectedUserName"> Would you like to remove {{ currentSelectedUserName }}?
            <button @click="deleteUser"> Yes </button>
            <button @click="clearDeleteSelection"> Cancel </button>
          </ul>
        </div>
      </div>
    </div>
  </modal>
</template>
<script>
    import axios from 'axios';

    const $ = require('jquery');
    // eslint-disable-next-line import/first
    import { mapState, mapGetters, mapActions } from 'vuex';

    export default {
        name: 'newWorkspaceModal',
        data: () => ({
            checkedUsers: [],
            usersRow: [],
            open: false,
            membersObjects: [],
            count: 0,
            currentSelectedUserName: '',
            currentSelectedUserRole: '',
        }),
        props: ['response'],
        created() {
        },
        computed: {
            getMembers() {
                console.log(this.response);
                this.parseResponse(this.response);
                return this.membersObjects;
            },
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
        },
        methods: {
            clearDeleteSelection() {
                this.currentSelectedUserName = null;
            },
            getSelected(e) {
                const temp = e.currentTarget;
                const selectedUserInfo = temp.id;
                const spacePosition = selectedUserInfo.search(' ');
                this.currentSelectedUserName = selectedUserInfo.slice(0, spacePosition);
                // eslint-disable-next-line max-len
                this.currentSelectedUserRole = selectedUserInfo.slice(spacePosition + 1, selectedUserInfo.length);
                console.log(this.currentSelectedUserName);
                console.log(this.currentSelectedUserRole);
            },
            startDialog() {
                this.updateUsersTable();
                this.open = true;
            },
            addUsers(newRole, newStatus) {
                let user = '';
                const users = [];
                const data = $('#usersTable').DataTable().rows({
                    selected: true,
                }).data();
                for (let i = 0; i < data.length; i += 1) {
                    user = data[i][1];
                    users.push(this.users[user]);
                }
                const userObject = [];
                userObject.push(users[0].username);
                this.submitAddUsersRequest(
                    userObject,
                    newRole,
                    newStatus,
                    this.response.resourceUUID,
                );
            },
            parseResponse(response) {
                let count = '';
                const membersObject = [];
                // eslint-disable-next-line guard-for-in,no-restricted-syntax
                for (count in response.teachers) {
                    const memberObject = ({
                        name: '',
                        role: '',
                        status: '',
                    });
                    memberObject.name = response.teachers[count].name;
                    memberObject.role = response.teachers[count].role;
                    memberObject.status = response.teachers[count].status;
                    membersObject.push(memberObject);
                }
                count = '';
                // eslint-disable-next-line guard-for-in,no-restricted-syntax
                for (count in response.tas) {
                    const memberObject = ({
                        name: '',
                        role: '',
                        status: '',
                    });
                    memberObject.name = response.tas[count].name;
                    memberObject.role = response.tas[count].role;
                    memberObject.status = response.tas[count].status;
                    membersObject.push(memberObject);
                }
                count = '';
                // eslint-disable-next-line guard-for-in,no-restricted-syntax
                for (count in response.students) {
                    const memberObject = ({
                        name: '',
                        role: '',
                        status: '',
                    });
                    memberObject.name = response.students[count].name;
                    memberObject.role = response.students[count].role;
                    memberObject.status = response.students[count].status;
                    membersObject.push(memberObject);
                }
                this.membersObjects = membersObject;
            },
            submitAddUsersRequest(user, newRole, newStatus, courseresourceUUID) {
                console.log(user);
                console.log(courseresourceUUID);
                const config = { headers: { 'X-Auth-Token': this.$store.state.token } };
                axios.post(`https://scitest12.pha.jhu.edu/courseware/courseware/course/${courseresourceUUID}/members?action=${newRole}`, user, config).then((response) => {
                    this.$emit('update', 'Got it!');
                    console.log(response);
                }, (error) => {
                    console.log(error);
                });
            },
            deleteUser() {
                const courseresourceUUID = this.response.resourceUUID;
                const user = [];
                user.push(this.currentSelectedUserName);
                const config = { headers: { 'X-Auth-Token': this.$store.state.token } };
                let action = '';
                if (this.currentSelectedUserRole === 'STUDENT') {
                    action = 'removeStudent';
                } else if (this.currentSelectedUserRole === 'TEACHER') {
                    action = 'removeTeacher';
                } else {
                    action = 'removeTA';
                }
                axios.post(`https://scitest12.pha.jhu.edu/courseware/courseware/course/${courseresourceUUID}/members?action=${action}`, user, config).then((response) => {
                    this.$emit('update', 'Got it!');
                    this.currentSelectedUserName = null;
                    console.log(response);
                }, (error) => {
                    console.log(error);
                });
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
  .active {
    background-color: green;
  }
</style>
