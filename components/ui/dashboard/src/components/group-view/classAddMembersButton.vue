<template>
  <div id="addMembers">
    <button class="btn btn-success btn-sm"  @click="addMembersToGroup"><span class="fa fa-plus" aria-hidden="true"></span></button>
    <users-modal  ref="inviteUsersModal" :workspace="workspace" @submitAddUsersRequest="requestAdd" :gMembers='members'></users-modal>
  </div>
</template>
<script>
    import { mapState } from 'vuex';
    import usersModal from './ClassUsersModal';

    export default {
        name: 'addMembers',
        components: {
            usersModal,
        },
        props: ['workspace', 'members'],
        computed: {
            ...mapState(['users']),
        },
        data: () => ({
            selectedUsers: [],
            name: '',
            description: '',
            owner: '',
            selectedCourseUUID: '',
        }),
        created() {
        },
        methods: {
            selectUsers(users) {
                this.selectedUsers = users;
            },
            requestAdd(user, newRole, newStatus) {
                this.$emit('submitAddUsersRequest', user, newRole, newStatus);
            },
            addMembersToGroup() {
                this.$refs.inviteUsersModal.startDialog();
            },
        },
    };
</script>
