<template>
<div>
  <div id="grantPrivileges" class="container theme-showcase" role="main">
  <div class="row">
    <div class="col-sm-5">
      <table>
        <tr>
          <td align="center">Resource Name</td>
          <td>:</td>
          <td id="grant_resourceName"></td>
        </tr>
        <tr>
          <td align="center">Resource Id (in Context)</td>
          <td>:</td>
          <td id="grant_resourceid"></td>
        </tr>
        <tr>
          <td align="center">Context Class</td>
          <td>:</td>
          <td id="grant_contextclass"></td>
        </tr>
        <tr>
          <td align="center">Resource Type</td>
          <td>:</td>
          <td id="grant_resourcetype"></td>
        </tr>
        <tr>
          <td align="center">Resource Context UUID</td>
          <td>:</td>
          <td id="grant_contextuuid"></td>
        </tr>
      </table>
    </div>
  </div>
</div>
<table><tr><td rowspan="2">

      <h3 align="center">Users and Groups</h3>
      <table id="usersAndGroupsTable" class="table-bordered">
        <thead>
          <tr>
            <th>Type (U|G)</th>
            <th>Name</th>
            <th>Description (G)</th>
          </tr>
        </thead>
        <tbody>
        </tbody>
      </table>
</td><td>&nbsp;&nbsp;</td><td>
      <h3 align="center">Actions</h3>
          <table id="resourceActions" class="table-bordered">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
              </tr>
            </thead>
            <tbody>
            </tbody>
          </table>
</td><td>&nbsp;&nbsp;</td><td>
          <div class="btn-group-vertical">
            <br />
            <br />
            <br />
            <button type="button" class="btn btn-primary" id="addPrivilegeButton">Add</button>
            <br />
            <button type="button" class="btn btn-primary" id="removePrivilegeButton">Remove</button>
          </div>
</td><td>&nbsp;&nbsp;</td><td>
          <h3 align="center">Privileges</h3>
          <table id="assignedPrivileges" class="table-bordered">
            <thead>
              <tr>
                <th>ID</th>
                <th>SciS-Entity</th>
                <th>SciS-Type</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
            </tbody>
          </table>
</td></tr>
<tr><td>&nbsp;&nbsp;</td><td>
  <h3 align="center">Roles</h3>
          <table id="resourceRoles" class="table-bordered">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
              </tr>
            </thead>
            <tbody>
            </tbody>
          </table>
</td><td>&nbsp;&nbsp;</td><td>
          <div class="btn-group-vertical">
            <br />
            <br />
            <br />
            <button type="button" class="btn btn-primary" id="addRoleButton">Add</button>
            <br />
            <button type="button" class="btn btn-primary" id="removeRoleButton">Remove</button>
          </div>
</td><td>&nbsp;&nbsp;</td><td>
          <h3 align="center">Assigned Roles</h3>
          <table id="assignedRoles" class="table-bordered">
            <thead>
              <tr>
                <th>ID</th>
                <th>SciS-Entity</th>
                <th>SciS-Type</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
            </tbody>
          </table>
</td></tr></table>
</div>
</template>
<script>
import createdialog from '../../create-dialog';

export default {
  name: 'GrantPrivilegesDialog',
  mounted() {
    createdialog('#grantPrivilegesDialog', null);
  },
};
</script>
