<template>
<div class="container theme-showcase" role="main">
    <div class="container-fluid">
        <div class="row">
            <!-- Form -->
            <div class="form-group row">
                <label for="inputGroupName" class="col-sm-2 col-form-label">Group Name</label> 
                <div class="col-sm-10">
                    <input name="name" type="text" class="form-control" id="groupname" />
                </div>
            </div>
            <div class="form-group row">
                <label for="inputGroupName" class="col-sm-2 col-form-label">Description</label> 
                <div class="col-sm-10">
                    <input name="description" type="text" class="form-control" id="groupdescription" />
                </div>
            </div>
        </div>

        <div class ="row">
            <!-- Users Table -->
            <div class="col-sm-5">
                <div class = "panel panel-primary">
                    <div class = "panel-heading">
                        <h5 class = "panel-title">Available SciServer Users</h5>
                    </div>
                    <div class = "panel-body">
                        <div style="overflow-x:auto;">
                        <table class="table-bordered responsive hover order-column compact sciserver-datatable">
                            <thead class="thead-default">
                                <tr>
                                    <th>Username</th>
                                    <th>Email</th>
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
            <div class="col-sm-1">
                <br/>
                <br/>
                <div class="btn-group-vertical">
                    <div class="dropdown">
                        <button title="invite user" class="dropdown-toggle" data-toggle="dropdown"
                            aria-haspopup="true" aria-expanded="false">
                            Invite User
                        </button>
                        <ul class="dropdown-menu">
                            <li><a href="#" id="inviteADMIN">Admin</a></li>
                            <li><a href="#" id="inviteMEMBER">Member</a></li>
                        </ul>
                    </div>
                    <br/>
                    <br/>
                    <div v-show="isAdmin"> <!--  only admins are allowed to "Add" iso only "Invite" users to a group -->
                        <div class="dropdown">
                            <button title="Add User" class="dropdown-toggle" data-toggle="dropdown" role="button"
                                aria-haspopup="true" aria-expanded="false">
                                Add User
                            </button>
                            <ul class="dropdown-menu">
                                <li><a href="#" id="addADMIN">Admin</a></li>
                                <li><a href="#" id="addMEMBER">Member</a></li>
                            </ul>
                        </div>
                        <br/>
                        <br/>
                    </div>


                </div>
            </div>

            <!-- Members Table -->
            <div class="col-sm-6">
                <div class = "panel panel-primary">
                    <div class = "panel-heading">
                    <h5 class = "panel-title">Group Members</h5>
                </div>
                <div class = "panel-body">
                    <div style="overflow-x:auto;">
                    <table  class="table-bordered responsive hover order-column compact sciserver-datatable">
                        <thead class="thead-default">
                            <tr>
                                <th>Username</th>
                                <th>email</th>
                                <th>Member Role</th>
                                <th>Member Status</th>
                            </tr>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
                    </div>
                </div>
                <div class="panel-footer">
                    <button id="removeGroupMembersButton" title="Remove Member" class="btn btn-danger">Remove Member</button>
                </div>
                </div>
            </div>
            <!-- Members Table End -->
        </div>
    </div>
</div>
</template>

<script>
import { mapGetters } from 'vuex';
import createdialog from '../../create-dialog';

export default {
  name: 'GroupEditDialog',
  mounted() {
    createdialog('#groupEditDialog', null);
  },
  computed: {
    ...mapGetters(['isAdmin']),
  },
};
</script>