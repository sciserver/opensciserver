const jQuery = require('jquery');
const apiCall = require('./apiCall').default;

const $ = jQuery;

// todo write documentation
export default function (_config, _divid, _groupsManager) {
  const that = this;
  this.config = _config;
  this.username = _config.username;
  this.groupsManager = _groupsManager;
  this.token = _config.token;
  this.rootdiv = jQuery(`#${_divid}`);
  this.usersTable = jQuery(that.rootdiv.find('#usersTable'));
  this.membersTable = jQuery(that.rootdiv.find('#membersTable'));
  this.allUsers = that.config.users;
  this.currentGroup = null;
  this.users_initialized = false;

  function refreshUsersTable() {
    // storing the info in a global variable for later use.
    const rows = [];
    const table = that.usersTable.DataTable();
    $.each(that.allUsers, (index, value) => {
      rows.push([value.username, value.contactEmail]);
    });
    table.rows.add(rows).draw(false);
    that.users_initialized = true;
  }

  function refreshGroupMembers() {
    const mytable = that.membersTable.DataTable();
    mytable.clear();

    if (that.currentGroup != null) {
      $.each(that.currentGroup.memberUsers, (index0, value) => {
        if (value.status !== 'WITHDRAWN') {
          const row = [value.user.username, value.user.contactEmail, value.role, value.status];
          mytable.row.add(row).draw(false);
        }
      });
    }
  }

  function init() {
    that.usersTable.DataTable({
      select: true,
    });
    that.membersTable.dataTable({
      select: true,
    });

    function getExistMember(username) {
      for (let i = 0; i < that.currentGroup.memberUsers.length; i += 1) {
        if (that.currentGroup.memberUsers[i].user.username.trim() === username
          .trim()) {
          return that.currentGroup.memberUsers[i];
        }
      }
      return null;
    }

    function removeMember(username) {
      for (let i = 0; i < that.currentGroup.memberUsers.length; i += 1) {
        if (username.trim() === that.currentGroup.memberUsers[i].user.username
          .trim()) {
          // that.currentGroup.memberUsers.splice(i, 1);
          that.currentGroup.memberUsers[i].status = 'WITHDRAWN';
          break;
        }
      }
    }

    // mode = INVITED or ACCEPTED
    function addUsers(mode, role) {
      const data = that.usersTable.DataTable().rows({
        selected: true,
      }).data();

      for (let i = 0; i < data.length; i += 1) {
        const existMember = getExistMember(data[i][0]);
        if (existMember != null) {
          if (existMember.status === 'DECLINED') {
            alert('Illegal attempt made to add/invite a user to the group who previously decline membership.');
            return;
          }

          existMember.status = mode;
          existMember.role = role;
        } else {
          const userinfo = {};
          userinfo.username = data[i][0];
          userinfo.contactEmail = data[i][1];
          const memberModel = {};
          memberModel.user = userinfo;
          memberModel.status = mode;
          memberModel.role = role;
          that.currentGroup.memberUsers.push(memberModel);
        }
      }
      that.usersTable.DataTable().rows().deselect();
      // that.refresh(that.currentGroup);
      refreshGroupMembers();
    }

    $('#inviteMEMBER').on('click', () => addUsers('INVITED', 'MEMBER'));

    $('#inviteADMIN').on('click', () => addUsers('INVITED', 'ADMIN'));

    $('#addADMIN').on('click', () => addUsers('ACCEPTED', 'ADMIN'));

    $('#addMEMBER').on('click', () => addUsers('ACCEPTED', 'MEMBER'));

    $('#removeGroupMembersButton').on('click', () => {
      const data = that.membersTable.DataTable().rows({
        selected: true,
      }).data();
      for (let i = 0; i < data.length; i += 1) {
        const username = data[i][0].trim();
        if (username === that.currentGroup.owner.username.trim()) {
          alert('Can not remove owner');
        } else {
          removeMember(username);
        }
      }
      that.usersTable.DataTable().rows().deselect();
      // that.refresh(that.currentGroup);
      refreshGroupMembers();
    });
  }

  function createNewGroup() {
    const owner = {
      user: that.config.user,
      status: 'OWNER',
      role: 'OWNER',
    };
    return {
      memberUsers: [owner],
    };
  }

  function updateCurrentGroup(json) {
    that.currentGroup = json;
    that.groupsManager.updateCurrentGroup(that.currentGroup);
  }

  function saveGroup() {
    that.currentGroup.groupName = $('#groupname').val();
    that.currentGroup.description = $('#groupdescription').val();

    const body = JSON.stringify(that.currentGroup);
    apiCall(that.config.submitGroupsUrl, that.token, 'POST', body,
      updateCurrentGroup, null, null);
  }

  this.edit = (_currentGroup = createNewGroup()) => {
    if (!that.users_initialized) {
      refreshUsersTable();
    }
    that.refresh(_currentGroup);
    const d = jQuery('#groupEditDialog').dialog({
      buttons: [{
        text: 'Save',
        click() {
          saveGroup();
          jQuery(this).dialog('close');
        },
      }, {
        text: 'Cancel',
        click() {
          jQuery(this).dialog('close');
        },
      }],
    });
    d.dialog('open');
  };

  this.refresh = (_currentGroup) => {
    that.currentGroup = $.extend(true, {}, _currentGroup);
    $('#groupname').val(that.currentGroup.groupName);
    if ('id' in _currentGroup) {
      $('#groupname').prop('readonly', true);
    } else {
      // TBD currently group names can not be changed
      $('#groupname').prop('readonly', false);
    }
    $('#groupdescription').val(that.currentGroup.description);

    refreshGroupMembers();
  };

  init();
}
