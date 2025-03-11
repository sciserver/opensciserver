<template>
  <modal v-model="open" class="my-sciserver-modal-dialog" title="Create Course">
    <form @submit.prevent>
      <div class="form-group">
          <label>Course name**</label>
          <input v-model="name" type="text" class="form-control" placeholder="Course Name" v-focus="focused" @focus="focused = true" @blur="focused = false">
      </div>
      <div class="form-group">
          <label>Description</label>
          <textarea v-model="description" class="form-control" rows="3" placeholder="Course Description"></textarea>
        </div>
        <div class="form-group">
          <label>Course Number</label>
          <input v-model="courseID" type="text" class="form-control" placeholder="Course Number">
        </div>
        <div class="form-group">
          <label>Course Term</label>
          <input v-model="term" type="text" class="form-control" placeholder="Course term">
        </div>
        <div class="form-group">
          <label>Course Year</label>
          <input v-model="year" type="text" class="form-control" placeholder="Course year">
        </div>
        <div class="form-group">
          <label> Teachers (Current user is added as teacher by default. Add more if needed.)  </label>
          <textarea v-model="teachers" class="form-control" rows="3" :placeholder= "userProfile.username"></textarea>
        </div>
        <div class="form-group">
          <label>Teacher Assistant </label>
          <textarea v-model="TA" class="form-control" rows="3" placeholder="Type Teacher Assistant SciServer Username"></textarea>
        </div>
        <div class="form-group">
          <label>Students </label>
          <textarea v-model="student" class="form-control" rows="3" placeholder="Type Students SciServer Username"></textarea>
        </div>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="initCourse" :disabled="!isValid">Create Course</button>
      <button type="button" class="btn btn-default" @click="open = false">Cancel</button>
    </div>
  </modal>
</template>

<script>
import axios from 'axios';
import { mapState, mapGetters } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';
// eslint-disable-next-line no-unused-vars
import classroom from '../Classroom_New';

export default {
  name: 'Create-Classroom-Form',
  mixins: [focusMixin],
  data: () => ({
    open: false,
    name: '',
    description: '',
    courseID: '',
    term: '',
    year: '',
    student: '',
    TA: '',
    teachers: '',
    focused: false,
  }),
  computed: {
    pubUsers() {
      return this.publicUsers.map(this.getUserById);
    },
    created() {
        this.startDialog();
    },
    isValid() {
      return this.name;
    },
    ...mapState(['publicUsers', 'userProfile']),
    ...mapGetters(['getUserById']),
  },
  methods: {
    // startDialog gets called by parent component Classroom.vue on create button click
    startDialog() {
      this.resetAll();
    },
    // when a user submits create course form then init course is called
    initCourse() {
      // closes form dialog
      this.open = false;
      // course name cannot be empty
      if (this.name !== '') {
        let courseName = '';
        // course id provided then append it to coursename
        if (this.id) {
          courseName = `${this.name}_${this.id}`;
        } else {
          courseName = `${this.name}`;
        }
        const upperCaseCourseName = courseName.toUpperCase();
        // get each student name separated by comma, space, semicolon, or .
        const students = this.student.split(/[ ,;.]+/);
        // check if student username is a valid sciserver user account
        const studentsList = this.findUserInSciServerUserList(students);
         // get each TA name separated by comma, space, semicolon, or .
        const TAs = this.TA.split(/[ ,;.]+/);
        // check if TA username is a valid sciserver user account
        const TAsList = this.findUserInSciServerUserList(TAs);
        // get each Teacher name separated by comma, space, semicolon, or .
        const teachers = this.teachers.split(/[ ,;.]+/);
        // check if Teacher username is a valid sciserver user account
        const teachersList = this.findUserInSciServerUserList(teachers);
        this.createCourse(upperCaseCourseName, TAsList, studentsList, teachersList);
      } else {
        throw new Error('Course Name is empty');
      }
    },
    // Create course defines course object and posts it to course api
    createCourse(courseName, TAs, students, teachers) {
      const studentMembers = students.length > 0 ? this.createMembersList(students, 'STUDENT') : [];
      const TAMembers = TAs.length > 0 ? this.createMembersList(TAs, 'TA') : [];
      const teacherMembers = teachers.length > 0 ? this.createMembersList(teachers, 'TEACHER') : [];
      const config = { headers: { 'X-Auth-Token': this.$store.state.token } };

      const course = {
        name: courseName,
        description: this.description,
        courseNumber: this.courseID,
        term: this.term,
        year: this.year,
        teachers: teacherMembers,
        tas: TAMembers,
        students: studentMembers,
      };
      const currentuserteacher = {
          name: this.$store.state.userProfile.username,
          role: 'TEACHER',
      };
      course.teachers.push(currentuserteacher);
      axios.post(`${COURSEWARE_URL}/courses`, course, config).then((response) => {
          console.log(response);
          this.$emit('update', 'Got it!');
      }, (error) => {
        alert(error.response.data.error);
      });
    },
    // for each member assigns name and role
    createMembersList(members, role) {
      return members.map(member => ({
        name: member.username,
        role,
      }));
    },
    findUserInSciServerUserList(users) {
      const allUser = [];
      for (let i = 0; i < users.length; i += 1) {
        const student = this.pubUsers.find(k => k.username === users[i]);
        if (student) {
          allUser.push({ username: student.username, userid: student.id });
        }
      }
      return allUser;
    },
    resetAll() {
      this.focused = true;
      this.open = true;
      this.name = '';
      this.student = '';
      this.TA = '';
      this.teachers = '';
      this.description = '';
      this.courseID = '';
      this.term = '';
      this.year = '';
    },
  },
};
</script>
