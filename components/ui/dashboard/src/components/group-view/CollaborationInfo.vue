<template>
  <div class="panel panel-primary">
    <div class="panel-heading panel-headings-with-buttons">
         <span class = "panel-title collaboration-name" v-if="!isEditting || !canEditName">{{ collaboration.name }}</span>
        <span v-else class="panel-title collaboration-name" style="color:black"><input v-model="newName" placeholder="Name" /></span>
        <div class="btn-group btn-group-sm">
        <button @click="startEditting" type="button" class="btn btn-primary" aria-label="Edit Collaboration" v-if="isEditable && !isEditting">
            <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span>
        </button>
        </div>
    </div>
    <div class="panel-body" v-if="collaboration.description || isEditting">
      <template v-if="!isEditting">
        {{ collaboration.description }}
        <span v-if="collaboration.term"> <b>Term: </b>{{collaboration.term}}</span>
        <span v-if="collaboration.year"> <b>Year: </b>{{collaboration.year}}</span>
      </template>
      <template v-else>
        <textarea
          v-model="newDescription"
          rows="5"
          autocapitalize="sentences"
          spellcheck="true"
          placeholder="Optional Description"
          :style="editCollaborationDescriptionStyle" v-focus="focused" @focus="focused = true" @blur="focused = false"></textarea>
        <btn @click="isEditting = false" class="pull-right" type="default">Cancel</btn>
        <btn @click="save" class="pull-right" type="success">Save</btn>
      </template>
    </div>
    <pop-up-dialog ref="alertPopUp"></pop-up-dialog>
  </div>
</template>
<script>
import { mixin as focusMixin } from 'vue-focus';
import popUpDialog from '../sharedComponents/alertDialog';

export default {
  props: ['collaboration'],
  mixins: [focusMixin],
  components: {
    popUpDialog,
  },
  data: () => ({
    isEditting: false,
    newDescription: '',
    newName: '',
    focused: false,
  }),
  computed: {
    isEditable() {
      return this.canEditName || this.canEditDescription;
    },
    isDeletable() {
      return this.collaboration._links.delete;
    },
    isLeavable() {
      return this.collaboration._links.leave;
    },
    canEditName() {
      return this.collaboration._links.editName;
    },
    canEditDescription() {
      return this.collaboration._links.editDescription;
    },
    canShareResource() {
      return this.collaboration._links.shareResource;
    },
    isWorkspace() {
      return this.collaboration.type === 'WORKSPACE';
    },
    editCollaborationDescriptionStyle: () => ({
      width: '100%',
    }),
  },
  methods: {
    startEditting() {
      this.newDescription = this.collaboration.description;
      this.newName = this.collaboration.name;
      this.focused = true;
      this.isEditting = true;
    },
    save() {
      this.isEditting = false;
      const updatedInfo = {};
      if (this.canEditName) updatedInfo.name = this.newName;
      if (this.canEditDescription) updatedInfo.description = this.newDescription;
      if (this.collaboration.type === 'COURSEWARE') {
        updatedInfo.courseNumber = this.collaboration.courseNumber;
        updatedInfo.term = this.collaboration.term;
        updatedInfo.year = this.collaboration.year;
        this.$http.post(this.collaboration._links.editDescription.href, updatedInfo)
        .then(() => {
          this.$store.dispatch('loadCollaborations');
        }).catch((e) => {
          // TODO: Real error handling
          this.$refs.alertPopUp.showAlert(e, 'Error message');
        });
      } else {
        this.$http.patch(this.collaboration._links.editDescription.href, updatedInfo)
        .then(() => {
          this.$store.dispatch('loadCollaborations');
        }).catch((e) => {
          // TODO: Real error handling
          this.$refs.alertPopUp.showAlert(e, 'Error message');
        });
      }
    },
  },
  watch: {
    collaboration(newCollaboration, oldCollaboration) {
      if (newCollaboration._links.self.href !== oldCollaboration._links.self.href) {
        this.isEditting = false;
      }
    },
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
