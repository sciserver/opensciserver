<template>
  <modal class="my-sciserver-modal-dialog-xxl" v-model="open" auto-focus>
    <span slot="title"> {{userVolumeName}}
       <a rel="noopener"  class= "contextualHints" target="_blank" :href="shareUV" title="Help Sharing User Volume"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">Help Sharing User Volume</span>
					</a> 
     </span>
    <div class="container-fluid">
      <div class="row">
        <!-- Users Table -->
        <div class="col-sm-5">
          <div class="panel panel-primary">
            <div class="panel-heading">
              <h5 class="panel-title">Available SciServer Groups And Users</h5>
            </div>
            <div class="panel-body">
              <div style="overflow-x:auto;">
                <table id="usersTable-shareFile" class="table-bordered responsive hover order-column compact sciserver-datatable">
                  <thead class="thead-default">
                    <tr>
                      <th>Name</th>
                      <th>ID</th>
                      <th>Type</th>
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
          <btn type="primary" size="lg" @click="addMembers" block data-action="auto-focus">Add</btn>
          <btn type="primary" size="lg" @click="removeMembers" block>Remove Access</btn>
        </div>
        <!-- Members Table -->
        <div class="col-sm-5">
          <div class="panel panel-primary">
            <div class="panel-heading">
              <h5 class="panel-title">Access Groups And Users</h5>
            </div>
            <div class="panel-body">
              <div style="overflow-x:auto;">
                <table id="membersTable-shareFile" class="table-bordered hover order-column compact sciserver-datatable">
                  <thead class="thead-default">
                    <tr>
                      <th>Name</th>
                      <th>ID</th>
                      <th>Type</th>
                      <th>Permissions</th>
                    </tr>
                  </thead>
                  <tbody>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <span>Make sure to click 'Save Changes' button to save your changes.</span>
    <div slot="footer">
      <button type="button" class="btn btn-primary"  @click="saveShared">Save Changes</button>
      <button type="button" class="btn btn-default"  @click="open = false">Close</button>
    </div>
  </modal>
</template>
<script>
import axios from 'axios';
import { mapState, mapGetters } from 'vuex';
import includes from 'lodash/includes';
import concat from 'lodash/concat';
import xor from 'lodash/xor';
import find from 'lodash/find';
import filesUtils from '../../files-utils';

const $ = require('jquery');

export default {
  data: () => ({
    userVolume: {},
    open: false,
    sharedUsers: [],
    // https://stackoverflow.com/a/38872723/239003
    id: `shareUserVolume${Math.random().toString(36).substr(2, 10)}`,
    shareUV: SHARE_UV,
  }),
  mounted() {
    // The use of DataTables here means we can't make the
    // click listeners that are managed by vue
    window[this.id] = this;
  },
  computed: {
    sharedUsersRow() {
      return this.sharedUsers.map((sharedUser) => {
        const isReadable = includes(sharedUser.allowedActions, 'read');
        const isWritable = includes(sharedUser.allowedActions, 'write');
        const isGrantable = includes(sharedUser.allowedActions, 'grant');
        const isDeletable = includes(sharedUser.allowedActions, 'delete');
        const role = `
          <label class="checkbox-inline"><input type="checkbox" onchange="${this.id}.toggleAction(${sharedUser.id}, '${sharedUser.type}', 'read')" ${isReadable ? 'checked' : ''}>Read</label>
          <label class="checkbox-inline"><input type="checkbox" onchange="${this.id}.toggleAction(${sharedUser.id}, '${sharedUser.type}', 'write')" ${isWritable ? 'checked' : ''}>Write</label>
          <label class="checkbox-inline"><input type="checkbox" onchange="${this.id}.toggleAction(${sharedUser.id}, '${sharedUser.type}', 'grant')" ${isGrantable ? 'checked' : ''}>Grant</label>
          <label class="checkbox-inline"><input type="checkbox" onchange="${this.id}.toggleAction(${sharedUser.id}, '${sharedUser.type}', 'delete')" ${isDeletable ? 'checked' : ''}>Delete</label>`;
        return [
          sharedUser.name,
          sharedUser.id,
          sharedUser.type,
          role,
        ];
      });
    },
    groupsAndUsers() {
      return concat(
        this.publicUsers.map(this.getUserById).map(u => [u.username, u.id, 'USER']),
        this.publicGroups.map(this.getGroupById).map(g => [g.groupName, g.id, 'GROUP']),
      );
    },
    userVolumeName() {
      return ` Share ${this.userVolume.name}`;
    },
    ...mapState(['userProfile', 'publicUsers', 'publicGroups', 'users', 'groups', 'token']),
    ...mapGetters(['getUserById', 'getGroupById']),
  },
  watch: {
    sharedUsersRow(newSharedUsersRow) {
      if ($.fn.dataTable.isDataTable('#membersTable-shareFile')) {
        const table = $('#membersTable-shareFile').DataTable();
        table.clear();
        table.rows.add(newSharedUsersRow).draw(false);
      } else {
        $('#membersTable-shareFile').DataTable({
          data: newSharedUsersRow,
          processing: true,
          bFilter: true,
          paging: true,
          select: true,
          language: {
            emptyTable: 'No Members To Display',
          },
        });
      }
    },
    groupsAndUsers() {
      if ($.fn.dataTable.isDataTable('#usersTable-shareFile')) {
        $('#usersTable-shareFile').DataTable().clear().destroy();
      }
      $('#usersTable-shareFile').DataTable({
        data: this.groupsAndUsers,
        processing: true,
        bFilter: true,
        stateSave: false,
        select: {
          style: 'single',
        },
        paging: true,
      });
    },
  },
  methods: {
    startDialog(userVolume) {
      this.userVolume = userVolume;
      this.sharedUsers = this.userVolume.sharedUsers;
      this.updateGroupsAndUsersTable();
      this.open = true;
    },
    saveShared() {
      this.open = false;
      const config = { headers: { 'X-Auth-Token': this.token } };
      const url = filesUtils.joinBaseURL(this.userVolume, 'api/share/');
      axios.patch(url, this.sharedUsers, config)
        .then(() => {
          this.$emit('sharedVolume');
        }, (error) => {
          alert(`Error while sharing; ${error.response.data.error}`);
        });
    },
    addMembers() {
      const data = $('#usersTable-shareFile').DataTable().rows({
        selected: true,
      }).data()
      .toArray();
      this.sharedUsers = concat(
        this.sharedUsers,
        data.map(entity => ({
          name: entity[0],
          id: entity[1],
          type: entity[2],
          allowedActions: ['read'],
        })),
      );
    },
    removeMembers() {
      const removed = $('#membersTable-shareFile').DataTable().rows({
        selected: true,
      }).data()
      .toArray();
      $('#membersTable-shareFile').DataTable().rows('.selected').remove()
      .draw();
      const data = $('#membersTable-shareFile').DataTable().rows().data()
      .toArray();
      const allUsers = this.sharedUsers.filter(m => data.find(m2 => m.id === m2[1]));
      const removedUser = removed.map(entity => ({
        name: entity[0],
        id: entity[1],
        type: entity[2],
        allowedActions: [],
      }));
      this.sharedUsers = concat(allUsers, removedUser);
    },
    toggleAction(id, type, action) {
      const sharedUser = find(this.sharedUsers, { id, type });
      sharedUser.allowedActions = xor(sharedUser.allowedActions, [action]);
    },
    updateGroupsAndUsersTable() {
      if ($.fn.dataTable.isDataTable('#usersTable-shareFile')) {
        $('#usersTable-shareFile').DataTable().clear().destroy();
      }
      $('#usersTable-shareFile').DataTable({
        data: this.groupsAndUsers,
        processing: true,
        bFilter: true,
        stateSave: false,
        select: {
          style: 'single',
        },
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

