/** import axios from 'axios';import find from 'lodash/find';

/**  create teacher group - add TAs
     create student group - add student
    create student uservolume for each student - give rw access to student, give rwgd access to TAs
    create lecture uservolume - give rwgd access to TAs, give r access to student
*/

/** function createCourseware() {
    let teacherGroupName = '';
    let studentGroupName = '';
    if (this.id) {
      teacherGroupName = `${this.name}_${this.id}_teacher`;
      studentGroupName = `${this.name}_${this.id}_student`;
    } else {
      teacherGroupName = `${this.name}_teacher`;
      studentGroupName = `${this.name}_student`;
    }
    const upperCaseTeacherGroupName = teacherGroupName.toUpperCase();
    const upperCaseStudentGroupName = studentGroupName.toUpperCase();
    const students = this.student.split(/[ ,;.]+/);
    const student = this.findStudent(students);
    const TAs = this.TA.split(/[ ,;.]+/);
    const TA = this.findTA(TAs);
    this.createTeacherGroup(upperCaseTeacherGroupName, TA);
    this.createStudentGroup(upperCaseStudentGroupName, student, TA);
    // look at delete user volume and copy the code
    this.createTeacherUserVolume(upperCaseTeacherGroupName, upperCaseStudentGroupName, student);
  }
  function createTeacherGroup(teacherGroup, TA) {
    const members = [];
    for (let i = 0; i < TA.length; i += 1) {
      members.push({
        user: {
          username: TA[i].username,
        },
        status: 'ACCEPTED',
        role: 'ADMIN',
      });
    }
    const group = {
      groupName: teacherGroup,
      description: this.description,
      memberUsers: members,
    };
    this.open = false;
    const config = { headers: { 'X-Auth-Token': this.token } };
    axios.post(this.collaborationLinks.createGroup.href, group, config)
    .then(() => {
      this.$store.dispatch('loadCollaborations').then();
    }, (error) => {
      alert(`Error Message: ${error.response.data.error}`);
    });
  }
  function createStudentGroup(studentGroup, student, TA) {
    const members = [];
    for (let i = 0; i < student.length; i += 1) {
      members.push({
        user: {
          username: student[i].username,
        },
        status: 'ACCEPTED',
        role: 'MEMBER',
      });
    }
    for (let i = 0; i < TA.length; i += 1) {
      members.push({
        user: {
          username: TA[i].username,
        },
        status: 'ACCEPTED',
        role: 'ADMIN',
      });
    }
    const group = {
      groupName: studentGroup,
      description: this.description,
      memberUsers: members,
    };
    this.open = false;
    const config = { headers: { 'X-Auth-Token': this.token } };
    axios.post(this.collaborationLinks.createGroup.href, group, config)
    .then(() => {
      this.$store.dispatch('loadCollaborations').then();
    }, (error) => {
      alert(`Error Message: ${error.response.data.error}`);
    });
  }
  function findStudent(students) {
    const allUser = [];
    for (let i = 0; i < students.length; i += 1) {
      const student = this.pubUsers.find(k => k.username === students[i]);
      if (student) {
        allUser.push({ username: student.username, userid: student.id });
      }
    }
    return allUser;
  }
  function findTA(TAs) {
    const allUser = [];
    for (let i = 0; i < TAs.length; i += 1) {
      const student = this.pubUsers.find(k => k.username === TAs[i]);
      if (student) {
        allUser.push({ username: student.username, userid: student.id });
      }
    }
    return allUser;
  }
  function isDuplicateGroupName(group) {
    let exists = false;
    const found = this.publicGroups.map(this.getGroupById)
    .find(g => g.groupName.toLowerCase() === group.groupName.toLowerCase());
    if (found) {
      exists = true;
    } else {
      exists = false;
    }
    return exists;
  }
  function createTeacherUserVolume(upperCaseTeacherVolName, upperCaseStudentGroupName, student) {
    const newOwner = this.userProfile.username;
    const rootvolumename = 'Storage';
    let rootVolObj = [];
    for (let i = 0; i < this.sortedRootVolumes.length; i += 1) {
      if (this.sortedRootVolumes[i].name === rootvolumename) {
        rootVolObj = this.sortedRootVolumes[i];
      }
    }
    const config = { headers: { 'X-Auth-Token': this.token } };
    axios
      .put(
        `${rootVolObj.fileserviceObj.apiEndpoint}api/volume/
        ${rootVolObj.name}/${newOwner}/${upperCaseTeacherVolName}`,
        { description: this.description }, config)
      .then(() => {
        // const uv = this.findUV(upperCaseTeacherVolName);
        this.shareTeacherUserVolume(upperCaseTeacherVolName, upperCaseStudentGroupName);
        this.createStudentUserVolume(upperCaseStudentGroupName, student,
        upperCaseTeacherVolName, student.length - 1);
      }, (error) => {
        alert(`Error Message: ${error.response.data.error}`);
      });
  }
  function findUV(userVolumeName) {
    let found = false;
    while (!found) {
      this.$store.dispatch('loadAllUserVolumes').then();
      found = find(this.myUserVolumeObjects, { name: userVolumeName });
    }
    return found;
  }
  function shareTeacherUserVolume(userVolume, userVolume1) {
    const newOwner = this.userProfile.username;
    const rootvolumename = 'Storage';
    let rootVolObj = [];
    for (let i = 0; i < this.sortedRootVolumes.length; i += 1) {
      if (this.sortedRootVolumes[i].name === rootvolumename) {
        rootVolObj = this.sortedRootVolumes[i];
      }
    }
    const config = { headers: { 'X-Auth-Token': this.token } };
    const url = `${rootVolObj.fileserviceObj.apiEndpoint}
    api/share/${rootVolObj.name}/${newOwner}/${userVolume}`;
    const id = this.findCurrentGroupKeyByName(userVolume);
    const uv1Id = this.findCurrentGroupKeyByName(userVolume1);
    const groupObj = this.findCurrentGroupObj(id);
    const groupObj1 = this.findCurrentGroupObj(uv1Id);
    const sharedWith = [{
      name: groupObj.name,
      id: groupObj.id,
      type: 'GROUP',
      allowedActions: ['read', 'write', 'grant', 'delete'],
    }];
    sharedWith.push({
      name: groupObj1.name,
      id: groupObj1.id,
      type: 'GROUP',
      allowedActions: ['read'],
    });
    axios.patch(url, sharedWith, config)
      .then(() => {
        this.$emit('sharedVolume');
      }, (error) => {
        alert(`Error while sharing; ${error.response.data.error}`);
      });
  }
  function createStudentUserVolume(upperCaseStudentVolName, student, TA, index) {
    // create user volume for students
    const newOwner = this.userProfile.username;
    const rootvolumename = 'Storage';
    let rootVolObj = [];
    for (let i = 0; i < this.sortedRootVolumes.length; i += 1) {
      if (this.sortedRootVolumes[i].name === rootvolumename) {
        rootVolObj = this.sortedRootVolumes[i];
      }
    }
    const config = { headers: { 'X-Auth-Token': this.token } };
    if (index >= 0) {
      const volName = `${upperCaseStudentVolName}_${student[index].username}`;
      axios.put(
      `${rootVolObj.fileserviceObj.apiEndpoint}api/volume/
      ${rootVolObj.name}/${newOwner}/${volName}`,
      { description: '' }, config).then(() => {
        this.shareStudentUserVolumeToStudent(`${volName}`, student[index], TA);
        this.createStudentUserVolume(upperCaseStudentVolName, student, TA, index - 1);
      }, (error) => {
        alert(`Error Message: ${error.response.data.error}`);
      });
    }
  }
  function shareStudentUserVolumeToStudent(volumeName, student, TA) {
    const newOwner = this.userProfile.username;
    const rootvolumename = 'Storage';
    let rootVolObj = [];
    for (let i = 0; i < this.sortedRootVolumes.length; i += 1) {
      if (this.sortedRootVolumes[i].name === rootvolumename) {
        rootVolObj = this.sortedRootVolumes[i];
      }
    }
    const config = { headers: { 'X-Auth-Token': this.token } };
    const id = this.findCurrentGroupKeyByName(TA);
    const groupObj = this.findCurrentGroupObj(id);
    const url = `${rootVolObj.fileserviceObj.apiEndpoint}api/share/
    ${rootVolObj.name}/${newOwner}/${volumeName}`;
    const sharedWith = [{
      name: student.username,
      id: student.id,
      type: 'USER',
      allowedActions: ['read', 'write'],
    }];
    sharedWith.push({
      name: groupObj.name,
      id: groupObj.id,
      type: 'GROUP',
      allowedActions: ['read', 'write', 'grant'],
    });
    axios.patch(url, sharedWith, config)
      .then(() => {
        this.$emit('sharedVolume');
      }, (error) => {
        alert(`Error while sharing; ${error.response.data.error}`);
      });
  }
  function shareStudentUserVolumeToTA(volName, TA) {
    const newOwner = this.userProfile.username;
    const rootvolumename = 'Storage';
    const sharedWith = [];
    let rootVolObj = [];
    for (let i = 0; i < this.sortedRootVolumes.length; i += 1) {
      if (this.sortedRootVolumes[i].name === rootvolumename) {
        rootVolObj = this.sortedRootVolumes[i];
      }
    }
    const config = { headers: { 'X-Auth-Token': this.token } };
    const url = `${rootVolObj.fileserviceObj.apiEndpoint}api/share/
    ${rootVolObj.name}/${newOwner}/${volName}`;
    for (let i = 0; i < TA.length; i += 1) {
      sharedWith.push({
        name: TA[i].username,
        type: 'USER',
        allowedActions: ['read', 'write', 'grant'],
      });
    }
    axios.patch(url, sharedWith, config)
      .then(() => {
        this.$emit('sharedVolume');
      }, (error) => {
        alert(`Error while sharing; ${error.response.data.error}`);
      });
  }
  function findCurrentGroupKeyByName(groupName) {
    const key = Object.keys(this.collaborations);
    const foundGroup = key.find(k => this.collaborations[k].name === groupName);
    return foundGroup;
  }
  function findCurrentGroupObj(groupId) {
    return this.collaborations[groupId];
  }
*/
