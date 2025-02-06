<template>
<div>
     <table id="tableForFiles" class="table table-condensed table-striped table-responsive" v-show="!loading">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th v-if="!isShowingFiles">Root Volume</th>
                    <th v-if="!isShowingFiles">Owner</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="volume in sortedVolumes" :key="volume.name">
                    <td>
                      <div class = "userVolume" v-if="(volume.type === 'uservolumes' || volume.type === 'datavolumes')">
                        <a href="#" @click="changeUV(volume.id)">
                         <span v-if="volume.owner!==username" class="fa-stack fa-lg"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                          <span class="fa-stack fa-lg" v-if="volume.owner===username && !volume.sharedUsers.length"><i class="fa fa-folder fa-2x" style="color:#008ae6"></i></span>
                          <span v-if="volume.owner===username && volume.sharedUsers.length" class="fa-stack fa-lg" style="color:#008ae6"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                          {{decodeURIComponent(volume.name)}} 
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
                    <template v-else>
                       <td>{{volume.rootVolumeObj.name}}</td>
                      <td>{{volume.owner}}</td>
                    </template>
                  </tr>
                </tbody>
              </table>
</div>
</template>
<script>
import concat from 'lodash/concat';
import get from 'lodash/get';
import map from 'lodash/map';
import find from 'lodash/find';
import { mapState, mapGetters } from 'vuex';

const moment = require('moment');
const fileUtils = require('../../files-utils').default;

export default {
  props: ['sortedVolumes', 'path'],
  data: () => ({
    selectedVolumeType: 'uservolumes',
  }),
  computed: {
    currentUserVolume() {
      const id = this.$route.params.userVolumeId;
      if (id === undefined || this.userVolumes) {
        return undefined;
      }
      return find(this.userVolumes, { id: parseInt(id, 10) });
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
          modified: folder.lastModified === null ? '' : moment(folder.lastModified).format('YYYY-MM-DD HH:mm:ss'),
        })),
        map(get(jsonTree, 'root.files', []), file => ({
          name: file.name,
          type: 'file',
          modified: file.lastModified === null ? '' : moment(file.lastModified).format('YYYY-MM-DD HH:mm:ss'),
        })),
      );
    },
    listToDisplay() {
      if (this.currentUserVolume) {
        return this.currentFiles;
      }
      return this.currentListOfUV;
    },
    currentListOfUV() {
      if (this.selectedVolumeType === 'uservolumes') {
        return fileUtils.getCurrentUVList(this.myUserVolumeObjects);
      } else if (this.selectedVolumeType === 'datavolumes') {
        return fileUtils.getCurrentDVList(this.myDataVolumeObjects);
      }
      return undefined;
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
    path() {
      return '';
    },
    ...mapState(['userProfile', 'token']),
    ...mapGetters(['myUserVolumeObjects', 'myRootVolumeObjects', 'myDataVolumeObjects']),
  },
  methods: {
    getVolumes() {
      this.$store.dispatch('loadAllUserVolumes');
    },
    getFiles() {
      this.$store.dispatch('loadJsonTreeById', { userVolumeId: this.currentUserVolume.id, path: this.path });
    },
    changeVolumesType(type) {
      this.selectedVolumeType = 'uservolumes';
      if (type === 'uservolumes') {
        this.selectedVolumeType = 'uservolumes';
      } else {
        this.selectedVolumeType = 'datavolumes';
      }
    },
    changeUV(id) {
      this.path = '';
      this.selectedUVId = id;
    },
    goToFiles(name) {
      this.path += `/${name}`;
    },
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
};
</script>