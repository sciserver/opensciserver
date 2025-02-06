<template>
  <div>
    <div v-if="this.$route.params.groupId == null"></div>
    <div v-else class="panel panel-primary">
      <div class="panel-heading panel-headings-with-buttons">
      <span>
      <span class="panel-title">Course Members</span>
         <a rel="noopener" class= "contextualHints" target="_blank" title="How to invite" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">How to invite</span>
					  </a>
            </span>
        <button v-if="!isStudent" class="btn btn-success btn-sm" @click="addMembers"><span class="fa fa-plus" aria-hidden="true"></span></button>
      </div>
      <ul class="list-group">
        <li class="list-group-item" v-for="teacher in teachers">
          <template>
            {{ teacher.name }}
            <span class="badge">Teacher</span>
          </template>
        </li>
        <li class="list-group-item" v-for="TA in tas">
          <template>
            {{ TA.name }}
            <span class="badge">TA</span>
          </template>
        </li>
        <div v-model="getMembers"></div>
        <span type="button" data-toggle="collapse" data-target="#demo"
              class="panel-heading panel-headings-with-buttons"
              style="background-color: #337ab7; text-color: white; color: #fff;}">Students<i class="fa fa-sort-desc" aria-hidden="true"></i></span>
        <li class="list-group-item" style="position: relative;
                    padding: 10px 15px;
                    margin-bottom: -1px;
                    background-color: #fff;
                    border: 1px solid #ddd;
                    border-bottom-color: #337ab7;" v-for="student in students">
          {{ student.name }}
        </li>
      </ul>
      <users-modal ref="inviteUsersModal" :response="currentCourse" :updateAfterSubmit="updateAfterSubmit" @update="updateValue"></users-modal>
    </div>
  </div>
</template>
<script>
    import axios from 'axios';
    import { mapState, mapGetters } from 'vuex';
    import VueSelect2 from '../sharedComponents/VueSelect2';
    import usersList from './classAddMembersButton';
    import usersModal from './ClassUsersModal';

    export default {
        components: { VueSelect2, usersList, usersModal },
        data: () => ({
            students: [],
            teachers: [],
            tas: [],
            currentCourse: {},
            updateAfterSubmit: '',
            isStudent: false,
        }),
        created() {
        },
        props: [],
        computed: {
            getMembers() {
                const resourceUUID = this.$route.params.groupId;
                if (resourceUUID) {
                  const config = { headers: { 'X-Auth-Token': this.$store.state.token } };
                  axios.get(`https://scitest12.pha.jhu.edu/courseware/courseware/course/${resourceUUID}`, config).then((response) => {
                      this.getAllMembers(response.data);
                      this.checkIfStudent(response.data);
                  }, (error) => {
                      console.log(error);
                  });
                }
            },
            ...mapState(['users', 'publicUsers', 'token', 'userProfile']),
            ...mapGetters(['getUserById']),
        },
        methods: {
            updateValue(value) {
                console.log(value);
                const resourceUUID = this.$route.params.groupId;
                if (resourceUUID) {
                    const config = { headers: { 'X-Auth-Token': this.$store.state.token } };
                    axios.get(`https://scitest12.pha.jhu.edu/courseware/courseware/course/${resourceUUID}`, config).then((response) => {
                        this.getAllMembers(response.data);
                    }, (error) => {
                        console.log(error);
                    });
                }
            },
            checkIfStudent(response) {
                const currentUser = this.$store.state.userProfile.username;
                // eslint-disable-next-line guard-for-in,no-restricted-syntax
                for (const val in response.students) {
                    if (response.students[val].name === currentUser) {
                        console.log('Equal');
                        this.isStudent = true;
                        return;
                    }
                }
                this.isStudent = false;
                // eslint-disable-next-line consistent-return
                return null;
            },
            getAllMembers(response) {
                this.students = response.students;
                this.teachers = response.teachers;
                this.tas = response.tas;
                this.currentCourse = response;
            },
            addMembers() {
                this.$refs.inviteUsersModal.startDialog();
            },
        },
    };
</script>

<style scoped>
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
