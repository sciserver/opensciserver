const jQuery = require('jquery');
const apiCall = require('./apiCall').default;

const $ = jQuery;

export default function (_config, _divid, _resourcesManager) {
  const that = this;
  this.config = _config;
  this.rootdiv = jQuery(`#${_divid}`);
  this.resourcesManager = _resourcesManager;
  this.allUsers = that.config.users;
  this.allGroups = that.config.groups;
  this.initialized = false;
  // tables
  const usersAndGroupsTable = $('#usersAndGroupsTable');
  const actionsTable = $('#resourceActions');
  const rolesTable = $('#resourceRoles');
  const privilegesTable = $('#assignedPrivileges');
  const assignedRolesTable = $('#assignedRoles');

  this.resource = null;

  function addPrivilege(priv) {
    // check priv does not alreayd exist
    const privs = that.resource.privileges;
    let found = false;
    $.each(privs, (index, value) => {
      if (value.scisName === priv.scisName && value.scisType === priv.scisType
        && value.actionName === priv.actionName) {
        found = true;
      }
    });
    if (!found) {
      that.resource.privileges.push(priv);
    }
  }

  function refreshPrivileges() {
    const table = $('#assignedPrivileges').DataTable();
    table.clear().draw();

    const privs = that.resource.privileges;
    $.each(privs, (index, value) => {
      table.row.add(
        [(value.id == null ? '' : value.id), value.scisName, value.scisType, value.actionName]).draw(
        false);
    });
  }

  function addPrivileges() {
    let data = usersAndGroupsTable.DataTable().rows({
      selected: true,
    }).data();
    const entities = [];
    for (let i = 0; i < data.length; i += 1) {
      const entity = { scisType: data[i][0], scisName: data[i][1] };
      entities.push(entity);
    }
    data = actionsTable.DataTable().rows({
      selected: true,
    }).data();
    const actions = [];
    for (let i = 0; i < data.length; i += 1) {
      const action = { id: data[i][0], name: data[i][1] };
      actions.push(action);
    }
    const el = entities.length;
    const al = actions.length;
    for (let ie = 0; ie < el; ie += 1) {
      const entity = entities[ie];
      for (let ia = 0; ia < al; ia += 1) {
        const action = actions[ia];
        const priv = {
          scisName: entity.scisName,
          scisType: entity.scisType,
          actionName: action.name,
          id: null };
        addPrivilege(priv);
      }
    }
    refreshPrivileges();
  }

  function removePrivilege(priv) {
    $.each(that.resource.privileges, (index, value) => {
      if (value.scisName === priv.scisName
        && value.scisType === priv.scisType
        && value.actionName === priv.actionName) {
        that.resource.privileges.splice(index, 1);
        return false;
      }
      return undefined;
    });
  }

  function removePrivileges() {
    const data = privilegesTable.DataTable().rows({
      selected: true,
    }).data();
    for (let i = 0; i < data.length; i += 1) {
      const priv = { scisName: data[i][1], scisType: data[i][2], actionName: data[i][3] };
      removePrivilege(priv);
    }
    refreshPrivileges();
  }

  function getSelectedUsersAndGroups() {
    const data = usersAndGroupsTable.DataTable().rows({
      selected: true,
    }).data();
    const entities = [];
    for (let i = 0; i < data.length; i += 1) {
      const entity = { scisType: data[i][0], scisName: data[i][1] };
      entities.push(entity);
    }
    return entities;
  }

  function addRole(role) {
    // check priv does not alreayd exist
    const roles = that.resource.roles;
    let found = false;
    $.each(roles, (index, value) => {
      if (value.scisName === role.scisName && value.scisType === role.scisType
        && value.roleName === role.roleName) {
        found = true;
      }
    });
    if (!found) {
      that.resource.roles.push(role);
    }
  }

  function refreshRoles() {
    const table = $('#assignedRoles').DataTable();
    table.clear().draw();
    const privs = that.resource.roles;
    $.each(privs, (index, value) => {
      table.row.add(
        [value.id, value.scisName, value.scisType, value.roleName]).draw(
        false);
    });
  }

  function addRoles() {
    const entities = getSelectedUsersAndGroups();
    const data = rolesTable.DataTable().rows({
      selected: true,
    }).data();
    const roles = [];
    for (let i = 0; i < data.length; i += 1) {
      const role = { id: data[i][0], name: data[i][1] };
      roles.push(role);
    }
    const el = entities.length;
    const al = roles.length;
    for (let ie = 0; ie < el; ie += 1) {
      const entity = entities[ie];
      for (let ia = 0; ia < al; ia += 1) {
        const role = roles[ia];
        const priv = {
          scisName: entity.scisName,
          scisType: entity.scisType,
          roleName: role.name,
          id: null };
        addRole(priv);
      }
    }
    refreshRoles();
  }

  function removeRole(role) {
    $.each(that.resource.roles, (index, value) => {
      if (value.scisName === role.scisName
        && value.scisType === role.scisType
        && value.roleName === role.roleName) {
        that.resource.roles.splice(index, 1);
        return false;
      }
      return undefined;
    });
  }

  function removeRoles() {
    const data = assignedRolesTable.DataTable().rows({
      selected: true,
    }).data();
    for (let i = 0; i < data.length; i += 1) {
      const role = { scisName: data[i][1], scisType: data[i][2], roleName: data[i][3] };
      removeRole(role);
    }
    refreshRoles();
  }

  function init() {
    $('#addPrivilegeButton').on('click', () => addPrivileges());
    $('#removePrivilegeButton').on('click', () => removePrivileges());
    $('#addRoleButton').on('click', () => addRoles());
    $('#removeRoleButton').on('click', () => removeRoles());
    if ($.fn.dataTable.isDataTable('#resourceActions')) { // trying to refresh the table
      $('#resourceActions').DataTable().clear();
    } else {
      $('#resourceActions').DataTable({
        select: true, bFilter: false, bInfo: false, bPaginate: false,
      });
    }
    if ($.fn.dataTable.isDataTable('#resourceRoles')) { // trying to refresh the table
      $('#resourceRoles').DataTable().clear();
    } else {
      $('#resourceRoles').DataTable({
        select: true, bFilter: false, bInfo: false, bPaginate: false,
      });
    }
    if ($.fn.dataTable.isDataTable('#assignedPrivileges')) { // trying to refresh the table
      $('#assignedPrivileges').DataTable().clear();
    } else {
      $('#assignedPrivileges').DataTable({
        select: true, bInfo: false,
      });
    }
    if ($.fn.dataTable.isDataTable('#assignedRoles')) { // trying to refresh the table
      $('#assignedRoles').DataTable().clear();
    } else {
      $('#assignedRoles').DataTable({
        select: true, bInfo: false,
      });
    }
    if ($.fn.dataTable.isDataTable('#usersAndGroupsTable')) { // trying to refresh the table
      $('#usersAndGroupsTable').DataTable().clear();
    } else {
      usersAndGroupsTable.DataTable({
        select: true, bInfo: false,
      });
    }
  }

  function refreshUsersAndGroupsTable() {
    // storing the info in a global variable for later use.
    const table = usersAndGroupsTable.DataTable();
    const rows = [];

    table.clear();
    $.each(that.allGroups, (index, value) => {
      rows.push(['G', value.groupName, value.description ? value.description : '']);
    });
    $.each(that.allUsers, (index, value) => {
      rows.push(['U', value.username, '']);
    });
    table.rows.add(rows).draw();
  }

  function refresh(json) {
    that.resource = $.parseJSON(JSON.stringify(json));
    $('#grant_resourceName').text(that.resource.resourceName);
    $('#grant_resourceid').text(that.resource.resourceId);
    $('#grant_contextclass').text(that.resource.contextClass);
    $('#grant_resourcetype').text(that.resource.rtm.name);
    $('#grant_contextuuid').text(that.resource.contextuuid);

    const rtm = that.resource.rtm;

    let table = $('#resourceActions').DataTable();
    table.clear();
    $.each(rtm.actions, (index, value) => {
      table.row.add([value.id, value.name]).draw(false);
    });

    table = rolesTable.DataTable();
    table.clear();
    $.each(rtm.roles, (index, value) => {
      table.row.add([value.id, value.name]).draw(false);
    });

    refreshPrivileges();
    refreshRoles();
  }

  function saveResourcePrivileges() {
    const body = JSON.stringify(that.resource);
    apiCall(that.config.postResourcesUrl, that.config.token, 'POST',
      body, refresh, null, null);
  }

  this.grant = (resourceuuid) => {
    if (!that.initialized) {
      refreshUsersAndGroupsTable();
      that.initialized = true;
    }
    const d = jQuery('#grantPrivilegesDialog').dialog({
      buttons: [{
        text: 'Save',
        click() {
          saveResourcePrivileges();
          // updateCurrentGroup(that.groupManager.currentGroup);
        },
      }, {
        text: 'Close',
        click() {
          jQuery(this).dialog('close');
        },
      }],
    });
    d.dialog('open');
    /*
    var req = {
    "resourceuuid" : resourceuuid
    }
    req = JSON.stringify(req);
    */
    apiCall(`${that.config.getPrivilegesUrl}/?resourceuuid=${resourceuuid}`, that.config.token, 'GET', null,
      refresh, null);
  };

  init();
}
