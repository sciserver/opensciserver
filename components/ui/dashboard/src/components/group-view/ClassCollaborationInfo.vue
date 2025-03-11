<template>
  <div>
    <div v-if="this.$route.params.groupId == null">
       Please select a course to view information.
    </div>
    <div v-else class="panel panel-primary">
      <div class="panel-heading panel-headings-with-buttons">
        <span class = "panel-title collaboration-name" > {{ courseName }} </span>
        <button type="button" class="btn btn-primary" aria-label="Edit Collaboration" >
          <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span>
        </button>
      </div>
      <div class="panel-body" >
        <span> Course Information: </span> <br>
        <span> Course Year: 2015 </span> <br>
        <div v-model="getPath"> {{ description }} </div>
      </div>
    </div>
  </div>
</template>
<script>
import { mixin as focusMixin } from 'vue-focus';
import popUpDialog from '../sharedComponents/alertDialog';

export default {
  props: ['courses'],
  mixins: [focusMixin],
  components: {
    popUpDialog,
  },
  data: () => ({
    description: '',
    courseName: '',
  }),
  created() {
  },
  computed: {
      display() {
          if (this.courses.length > 0) {
              return true;
          }
          return false;
      },
      getPath() {
          let val = '';
          // eslint-disable-next-line radix
          const currentGroupId = this.$route.params.groupId;
          if (this.courses[0]) {
              // eslint-disable-next-line no-restricted-syntax,guard-for-in
              for (val in this.courses) {
                  if (currentGroupId === this.courses[val].resourceUUID) {
                      this.description = this.courses[val].description;
                      this.courseName = this.courses[val].name;
                  }
              }
          }
      },
  },
  methods: {
  },
};
</script>

<style scoped>
.panel-headings-with-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h1.collaboration-name {
  display: inline;
  margin: 0;
}

.group-footer {
  padding: 0;
}
</style>
