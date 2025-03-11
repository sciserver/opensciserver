<template>
<div>
  <modal v-model="open" class="my-sciserver-modal-dialog" title="Copy Folder/File" auto-focus>
    <form @submit.prevent>
                     <div class="panel panel-primary">
                <div class="panel-heading">
                    <span class="panel-title">Volumes</span>
                </div>
        <root-volume-template :currentRootVolumes="currentRootVolumes" :selectedVolumeType="selectedVolumeType" @changeVolumesType="changeVolumesType"/>
            </div>
      <div class="form-group">
      <bread-crumb :currentUserVolume="currentUserVolume" :path="path" @changeUV="changeUV" @showFiles="showFiles" @resetPath="resetPath"/>
              <!-- https://laracasts.com/discuss/channels/vue/vuejs-transition-for-loading-data -->
              <i v-show="loading" class="fa fa-spinner fa-spin fa-3x" aria-hidden="true"></i>
        <div v-show="false" id="spinner2" style="text-align: center">Copying ... <i class="fa fa-spinner fa-spin fa-2x"></i> </div>
        <table id="copyFilesTable" class="table table-condensed table-striped table-responsive" v-show="!loading">
                <thead>
                  <tr>
                     <template v-if="isShowingFiles">
                      <th class="sorted ascending" scope="col">Name</th>
                    </template>
                    <template v-else>
                      <th v-for="column in columnFields" :key="column">
                      {{column}}
                    </th>
                    </template>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="volume in sortedVolumes" :key="volume.name" v-if="volume.write || isShowingFiles">
                    <td>
                      <div class = "userVolume" v-if="(volume.type === 'uservolumes' || volume.type === 'datavolumes')">
                        <a href="#" @click="changeUV(volume.id)">
                         <span v-if="volume.owner!==username" class="fa-stack fa-lg"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                          <span class="fa-stack fa-lg" v-if="volume.owner===username && !volume.sharedUsers.length"><i class="fa fa-folder fa-2x" style="color:#008ae6"></i></span>
                          <span v-if="volume.owner===username && volume.sharedUsers.length" class="fa-stack fa-lg" style="color:#008ae6"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                          {{decodeURIComponent(volume.displayName ? volume.displayName : volume.name)}}
                        </a>
                      </div>
                      <div class="volume" v-if="volume.type === 'dir'">
                        <a href="#" @click="goToFiles(volume.name)"> <span class="fa-stack fa-lg"><i class="fa fa-folder fa-2x" style="color:#008ae6"></i></span>{{volume.name}}</a>
                      </div>
                      <div class="file" v-if="volume.type === 'file'">
                        <span class="fa-stack fa-lg"><i class="fa fa-file-o fa-2x"></i></span>{{decodeURIComponent(volume.name)}}
                      </div>
                    </td>
                   <template v-if="isShowingFiles">
                      <td> {{volume.modified}}</td>
                      <td>{{volume.size}}</td>
                    </template>
                    <template v-if="volume.type === 'uservolumes'">
                       <td>{{volume.rootVolumeObj.name}}</td>
                      <td>{{volume.owner}}</td>
                    </template>
                     <template v-if="volume.type === 'datavolumes'">
                       <td>{{volume.name}}</td>
                      <td></td>
                    </template>
                  </tr>
                </tbody>
              </table>
      </div>
    </form>
    <div slot="footer">
      <button type="button" class="btn btn-primary" @click="paste()">Paste</button>
      <button type="button" class="btn btn-default" @click="open = false" data-action="auto-focus">Cancel</button>
    </div>
  </modal>
  <pop-up-dialog ref="alertPopUp"></pop-up-dialog>
  </div>
</template>
<script>
import axios from 'axios';
import { mapGetters, mapState } from 'vuex';
import find from 'lodash/find';
import concat from 'lodash/concat';
import get from 'lodash/get';
import map from 'lodash/map';
import RootVolumeTemplate from './RootVolumes';
import BreadCrumb from './BreadcrumbForFile';
import popUpDialog from '../sharedComponents/alertDialog';

const $ = require('jquery');
const moment = require('moment');
const _ = require('lodash');
const fileUtils = require('../../files-utils').default;
const mathUtils = require('../../math-utils').default;

export default {
  name: 'copyFiles',
  data: () => ({
    path: '',
    owner: '',
    rootName: '',
    rootVName: '',
    shared: false,
    url: '',
    loading: false,
    parentURL: '',
    selected: [],
    open: false,
    selectedVolumeType: 'uservolumes',
    selectedUVId: '',
  }),
  components: {
    RootVolumeTemplate,
    BreadCrumb,
    popUpDialog,
  },
  created() {
    this.getVolumes();
  },
  computed: {
    isShowingFiles() {
      return this.currentUserVolume;
    },
    currentRootVolumes() {
      const rv = [];
      if (this.myRootVolumeObjects) {
        rv.push({ name: 'User Volumes', type: 'uservolumes', id: 1 });
      }
      if (this.myDataVolumeObjects) {
        rv.push({ name: 'Data Volumes', type: 'datavolumes', id: 2 });
      }
      return rv;
    },
    currentListOfUV() {
      if (this.selectedVolumeType === 'uservolumes') {
        return fileUtils.getCurrentUVList(this.myUserVolumeObjects);
      } else if (this.selectedVolumeType === 'datavolumes') {
        return fileUtils.getCurrentDVList(this.myDataVolumeObjects);
      }
      return undefined;
    },
    currentUserVolume() {
      const id = this.selectedUVId;
      if (id === undefined || this.myUserVolumeObjects.length === 0) {
        return undefined;
      }
      return find(this.currentListOfUV, { id: parseInt(id, 10) });
    },
    listToDisplay() {
      if (this.currentUserVolume) {
        return this.currentFiles;
      }
      return this.currentListOfUV;
    },
    sortedVolumes() {
      const sortAll = _.orderBy(this.listToDisplay, ['type', 'name']);
      return sortAll;
    },
    username() {
      return this.userProfile.username;
    },
    currentFiles() {
      if (!this.currentUserVolume) return [];
      const jsonTree = get(this.$store.state.files.jsonTreeCache,
        [this.currentUserVolume.id, this.path]);
      return concat(
        map(get(jsonTree, 'root.folders', []), folder => ({
          name: folder.name,
          type: 'dir',
          owner: this.currentUserVolume.owner,
          size: folder.size ? mathUtils.bytesToSize(folder.size) : '',
          modified: folder.lastModified === null ? '' : moment(folder.lastModified).format('YYYY-MM-DD HH:mm:ss'),
        })),
        map(get(jsonTree, 'root.files', []), file => ({
          name: file.name,
          type: 'file',
          size: file.size === null ? '' : mathUtils.bytesToSize(file.size),
          modified: file.lastModified === null ? '' : moment(file.lastModified).format('YYYY-MM-DD HH:mm:ss'),
        })),
      );
    },
    columnFields() {
      if (this.selectedVolumeType === 'uservolumes') {
        return ['Volume', 'Root Volume', 'Owner'];
      } else if (this.selectedVolumeType === 'datavolumes') {
        return ['Data Sets', 'Folders', ''];
      }
      return undefined;
    },
    ...mapState(['userProfile', 'token']),
    ...mapGetters(['myUserVolumeObjects', 'myRootVolumeObjects', 'myDataVolumeObjects']),
  },
  watch: {
    currentUserVolume(uv) {
      if (uv) {
        this.getFiles();
      }
    },
    path() {
      if (this.currentUserVolume) {
        this.getFiles();
      }
    },
  },
  // All the methods you want the view to have access to, basically an object of functions
  methods: {
    getVolumes() {
      this.$store.dispatch('loadAllUserVolumes');
    },
    getFiles() {
      this.$store.dispatch('loadJsonTreeById', { userVolumeId: this.currentUserVolume.id, path: this.path, type: this.currentUserVolume.type });
    },
    changeUV(id) {
      this.path = '';
      this.selectedUVId = id;
    },
    goToFiles(name) {
      this.path += `/${name}`;
    },
    showFiles(index) {
      const allPath = this.path.split('/');
      const len = allPath.length - 1;
      allPath.splice(index + 1, len);
      const strPath = allPath.join('/');
      this.path = `${strPath}`;
    },
    copy(selectedFile, url) {
      this.parentURL = url;
      this.resetPath();
      this.selected = selectedFile;
      this.open = true;
    },
    paste() {
        this.$store.commit('loadingStatus', ['Copying', 'True']);
        $('#spinner2').show();
        if (Array.isArray(this.selected)) {
        $.each(this.selected, (index0, text0) => {
          this.confirmPaste(text0.name);
        });
      } else {
        this.confirmPaste(this.selected.name);
      }
    },
    confirmPaste(selectedName) {
      const filepath = `${this.currentUserVolume.type}/${this.currentUserVolume.name}${this.path}`;
      const actionUrl = `${this.currentUserVolume.type}/${this.$route.params.userVolumeId}${this.path}`;
      let actionObject = ({
          actionName: 'Copying',
          actionFileName: selectedName,
          actionLocation: filepath,
          actionStatus: 'True',
      });
      this.$store.commit('updateMessage', actionObject);
      const selected = this.checkIfFileFolderExists(selectedName);
      const encodedSelectedName = encodeURIComponent(selectedName);
      const config = { headers: { 'X-Auth-Token': this.$store.state.token } };
      const that = this;
      axios.put(`${this.parentURL}/${encodedSelectedName}?replaceExisting=false&doCopy=true`, { destinationPath: `${this.path}/${selected}`,
        destinationRootVolume: this.currentUserVolume.type === 'uservolumes' ? this.currentUserVolume.rootVolumeObj.name : null,
        destinationUserVolume: this.currentUserVolume.type === 'uservolumes' ? decodeURIComponent(this.currentUserVolume.name) : null,
        destinationDataVolume: this.currentUserVolume.type === 'uservolumes' ? null : decodeURIComponent(this.currentUserVolume.name),
        destinationOwnerName: this.currentUserVolume.type === 'uservolumes' ? this.currentUserVolume.owner : null,
        destinationFileService: null },
      config).then(() => {
        this.getFiles();
          this.$emit('copiedSuccess');
          that.$store.commit('loadingStatus', '');
          actionObject = ({
              actionName: 'Copying',
              actionFileName: selectedName,
              actionLocation: `${filepath}`,
              actionStatus: 'False',
          });
          that.$store.commit('updateMessage', actionObject);
          $('#spinner2').hide();
      }, (error) => {
          actionObject = ({
            actionName: 'Copying',
            actionFileName: selectedName,
            actionLocation: `${filepath}`,
            actionStatus: 'False',
            actionRequired: 'True',
            actionurl: `${actionUrl}`,
            actionMessage: error.response.data.error,
          });
          that.$store.commit('updateMessage', actionObject);
          $('#spinner2').hide();
      });
    },
    close() {
      this.$emit('close');
    },
    checkIfFileFolderExists(fileFolderName) {
      let found = this.listToDisplay.find(uv => uv.name === fileFolderName);
      let newName = fileFolderName;
      let index = 1;
      while (found) {
        newName = `${fileFolderName} (${index})`;
        index += 1;
        found = this.ifFileExists(newName);
      }
      return newName;
    },
    ifFileExists(newName) {
      const found = this.listToDisplay.find(uv => uv.name === newName);
      return found;
    },
    changeVolumesType(type) {
      this.selectedVolumeType = fileUtils.changeVolumesType(type);
      this.resetPath();
    },
    resetPath() {
      this.selectedUVId = undefined;
      this.path = '';
    },
  },
};
</script>
