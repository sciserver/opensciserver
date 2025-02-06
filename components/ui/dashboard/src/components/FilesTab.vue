<template>
<div id ="FilesTab">
  <div class="container-fluid wrap">
    <div class = "row">
        <!-- Volumes Section -->
         <div class="col-sm-2">
           <list-group title="Volumes" :headingType="`primary`">
            <router-link :to="`/files/${item.type}`"
                v-for="item in currentRootVolumes"
                :key="item.name"
                class="list-group-item"
                type="button"
                :class="{active: item.type === selectedVolumeType}">
                {{ item.name }}
            </router-link>
          </list-group>
        </div>
        <div class = "col-sm-10">
              <div class="panel panel-primary">
                <!--File Service Header-->
              <div class="panel-heading panel-heading-style">
                   <span class = "panel-title">Files Service</span>
                   <a rel="noopener" class= "contextualHints" target="_blank" :href="fileManagement" title="More Info on Files" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
                    <span class="sr-only">More Info on Files</span>
                  </a>
                <button class="btn btn-primary" @click="isShowingFiles ? getFiles() : getVolumes()" title="Refresh"><i class="fa fa-refresh" :class="refreshFilesSpinner"></i></button>
                <div class="pull-right"><input type="text" name="search" class="form-control main-search" placeholder="Filter..." v-model="searchTerm">
                </div>
                <btn type="primary" class="float-right" v-if="!isShowingFiles && listToDisplay && listToDisplay.length > 0 && selectedVolumeType !== 'datavolumes'" @click="createNewVolume">Create User Volume</btn>
                 <btn type="primary" v-if="selectedVolumeType !== 'datavolumes'" @click="viewQuotas">View Quotas</btn>
                <span v-if="loadingStatusIcon" style="margin: auto;width: 50%; padding-left: 20%; padding-right: 20%"> {{ loadingAction }} ...<i class="fa fa-spinner fa-spin fa-2x"></i></span>

              </div>
                <div class="panel-body">
                <div class="row">
                  <!-- Navigation Buttons -->
                  <div :class="[isShowingFiles && writeAccess ? 'col-sm-8' : 'col-sm-12']">
                    <div class="pull-right" style="padding: 8px;">
                      <div class="btn-group" role="group" aria-label="Button Group">
                        <label for="uploadedFile" type="button" class="btn btn-default btn-sm" v-if="isShowingFiles && writeAccess">Upload</label>
                        <input type="file" name = "datafile" id= "uploadedFile"  style="display: none;" @click="fileUploadClicked" @change="uploadFile()" multiple>
                        <label type="button" class="btn btn-default btn-sm"  v-if="isShowingFiles && writeAccess" @click="createNewSubFolder">New Folder</label>
                      </div>
                    </div>

                    <!-- Breadcrumb section with  the Home icon-->
                    <ul class="breadcrumb">
                      <li><router-link :to="`/files/${selectedVolumeType}`" title="Main User Volume Directory"><i class="fa fa-home fa-2x"></i></router-link></li>
                      <template v-if="currentUserVolume && currentUserVolume.type === 'uservolumes'">
                        <li v-if="currentUserVolume.rootVolumeObj.name">{{ currentUserVolume.rootVolumeObj.name }}</li>
                        <li v-if="currentUserVolume.owner">{{ currentUserVolume.owner }}</li>
                        <li><router-link :to="`/files/${selectedVolumeType}/${currentUserVolume.id}`" :title="`${currentUserVolume.name}`">{{ currentUserVolume.name }}</router-link></li>
                      </template>
                      <template v-if="currentUserVolume && currentUserVolume.type === 'datavolumes'">
                        <li><router-link :to="`/files/${selectedVolumeType}/${currentUserVolume.id}`" :title="`${currentUserVolume.name}`">{{ currentUserVolume.name }}</router-link></li>
                      </template>
                      <template v-if="path">
                      <li v-for="(pathSegment, index) in path.split('/').filter(x => x)" :key="index">
                        <router-link  :title="`${pathSegment}`" :to="`/files/${selectedVolumeType}/${currentUserVolume.id}/${encodeURIComponent(path.split('/').filter(x => x).slice(0, index + 1).join('/'))}`">
                          {{ pathSegment }}
                        </router-link>
                      </li>
                      </template>
                    </ul>

               <div class="btn-group" role="group" aria-label="Button Group" v-if="isShowingFiles && selectedItem.length">
                      <label type="button" v-if="downloadAction" class="btn btn-default btn-sm"  @click="downloadMultipleFiles(selectedItem, selectedItem.length - 1)">Download</label>
                      <label type="button" v-if="copyAction" class="btn btn-default btn-sm"  @click.prevent="childCopy(selectedItem)">Copy</label>
                      <label type="button" v-if="moveAction" class="btn btn-default btn-sm"  @click.prevent="childMove(selectedItem)">Move</label>
                      <label type="button" v-if="deleteAction" class="btn btn-default btn-sm"  @click.prevent="sendTodelete(selectedItem)">Delete</label>
               </div>

              <div class="btn-group" role="group" aria-label="Button Group" v-if="!isShowingFiles && selectedItem.length">
                    <label class="btn btn-default btn-sm" v-if="deleteUVAction" @click="sendTodeleteUserVolume(selectedItem)" title="Delete User Volume">
                      <span class="fa fa-trash" aria-hidden="true"></span>Delete
                    </label>
                    <label  class="btn btn-default btn-sm" v-if="!isShowingFiles && selectedItem.length === 1 && grantUVAction" @click="sendToshareVolume(selectedItem)" title="share user volume with other users or groups">
                      <span class="fa fa-users" aria-hidden="true"></span> Sharing
                    </label>
                    <label class="btn btn-default btn-sm" v-if="!isShowingFiles && selectedItem.length === 1 && editUVAction" @click="sendToeditVolume(selectedItem)" title="Edit User Volume">
                      <span class="fa fa-pencil" aria-hidden="true"></span>Edit
                    </label>
             </div>

                    <!-- https://laracasts.com/discuss/channels/vue/vuejs-transition-for-loading-data -->
                    <i v-show="loading" class="fa fa-spinner fa-spin fa-5x"></i>
                    <div v-if="!isShowingFiles" v-show="!loading && listToDisplay && listToDisplay.length <= 0">
                      <h4><b>Create</b> your first userVolume <button class="btn btn-primary btn-xs" @click="createNewVolume">Get Started</button></h4>
                    </div>
                    <div v-if="isShowingFiles && writeAccess" v-show="!loading && listToDisplay && listToDisplay.length <= 0">
                      <div id="filelist" style="margin:auto;text-align:center;color:grey">File list empty.</div>
                    </div>
                    <div v-if="isShowingFiles" v-show="listToDisplay && listToDisplay.length" id="readme-container" class="markdown-render" v-html="readmeData">
                    </div>
                    <div class="table-responsive">
                      <table id="filesTable" class="table table-hover sciserver-datatable sortable" v-show="!loading && listToDisplay && listToDisplay.length">
                      <thead>
                          <tr>
                            <template v-if="isShowingFiles">
                              <th class="no-sort">
                                <div class="pretty p-default p-curve p-thick p-smooth">
                                  <input type="checkbox" v-model='isSelectedAll' @click="selectedAll"/>
                                  <div class="state p-primary">
                                    <label></label>
                                  </div>
                                </div>
                              </th>
                              <th :class="nameIcon" data-sort="string-ins" v-on:click="sortingMethod('name')">Name</th>
                              <th :class="modifiedIcon" data-sort="string-ins" v-on:click="sortingMethod('modified')">Last Modified</th>
                              <th :class="sizeIcon" data-sort="int" v-on:click="sortingMethod('rsize')">Size</th>
                              <th class="no-sort"></th>
                              <th class="no-sort"></th>
                            </template>
                            <template v-else>
                              <th class="no-sort">
                                <div class="pretty p-default p-curve p-thick p-smooth">
                                  <input type="checkbox" v-model='isSelectedAll' @click="selectedAll"/>
                                  <div class="state p-primary">
                                    <label></label>
                                  </div>
                                </div>
                              </th>
                              <template v-if="selectedVolumeType === 'uservolumes'">
                                <th :class="volumeIcon" data-sort="string-ins" v-on:click="getHeaderName('Volume')">Volume</th>
                                <th :class="rootVolumeIcon" data-sort="string-ins" v-on:click="getHeaderName('Root Volume')">Root Volume</th>
                                <th :class="ownerIcon" data-sort="string-ins" v-on:click="getHeaderName('Owner')">Owner</th>
                                <th  data-sort="string-ins" v-on:click="sortingMethod('name')"></th>
                                <th  data-sort="string-ins" v-on:click="sortingMethod('name')"></th>
                              </template>
                              <template v-else>
                                <th :class="dataSetsIcon" data-sort="string-ins" v-on:click="getHeaderName('Data Sets')">Data Sets</th>
                                <th :class="foldersIcon" data-sort="string-ins" v-on:click="getHeaderName('Folders')">Folders</th>
                                <th data-sort="string-ins"></th>
                                <th  data-sort="string-ins"></th>
                                <th  data-sort="string-ins"></th>
                              </template>
                            </template>
                          </tr>
                        </thead>

                        <tbody v-if="listToDisplay">
                          <tr v-for="volume in sortedVolumes" :key="volume.id">
                            <td v-if="volume.type === 'uservolumes' || volume.type === 'datavolumes'">
                              <div class="pretty p-default p-curve p-thick p-smooth">
                                <input type="checkbox" @click="select" v-model="selectedItem" :value="volume"/>
                                <div class="state p-primary">
                                  <label></label>
                                </div>
                              </div>
                            </td>
                            <td v-if="volume.type === 'dir' || volume.type === 'file'">
                              <div class="pretty p-default p-curve p-thick p-smooth">
                                <input type="checkbox" @click="select" v-model="selectedItem" :value="volume"/>
                                <div class="state p-primary">
                                  <label></label>
                                </div>
                              </div>
                            </td>
                            <td>
                          <!--Persistent table section and the fileName Section -->
                              <div class="userVolume" v-if="volume.type === 'uservolumes'">
                                <router-link :to="volume.id.toString()" :title="`${volume.name}`" append>
                                  <span v-if="volume.owner!==username" class="fa-stack fa-lg"  style="color:#000000"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                                  <span class="fa-stack fa-lg" v-if="volume.owner===username && !volume.sharedWith.length"><i class="fa fa-folder fa-2x" style="color:#337AB7"></i></span>
                                  <span v-if="volume.owner===username && volume.sharedWith.length" class="fa-stack fa-lg" style="color:#337AB7"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                                    {{volume.displayName ? volume.displayName : volume.name}}
                                </router-link>
                              </div>

                              <!--Scratch table section and the fileName Section -->
                              <div class="datavolumes" v-if="volume.type === 'datavolumes'">
                                <router-link :to="volume.id.toString()" :title="`${volume.name}`" append>
                                  <span v-if="volume.owner!==username" class="fa-stack fa-lg"  style="color:#000000"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                                  <span class="fa-stack fa-lg" v-if="volume.owner===username && !volume.sharedWith.length"><i class="fa fa-folder fa-2x" style="color:#337AB7"></i></span>
                                  <span v-if="volume.owner===username && volume.sharedWith.length" class="fa-stack fa-lg" style="color:#337AB7"><i class="fa fa-folder-o fa-stack-2x" ></i><i class="fa fa-users fa-stack-1x"></i></span>
                                    {{volume.displayName ? volume.displayName : volume.name}}
                                </router-link>
                              </div>

                              <div class="volume" v-if="volume.type === 'dir'">
                                <router-link :to="encodeURIComponent(volume.name)" :title="`${volume.name}`" append>
                                  <span class="fa-stack fa-lg"><i class="fa fa-folder fa-2x" style="color:#337AB7"></i></span>
                                 {{volume.name}}
                                </router-link>
                              </div>

                              <div class="file" v-if="volume.type === 'file'" @dblclick="editFile(volume)">
                                <span class="fa-stack fa-lg"><i class="fa fa-file-o fa-2x"></i></span>{{volume.name}}
                              </div>
                            </td>

                            <template v-if="isShowingFiles">
                              <td>{{volume.modified}}</td>
                              <td>{{volume.size}}</td>
                            </template>

                            <template v-if="volume.type === 'uservolumes'">
                               <td :data-sort-value="volume.rootVolumeObj.name" v-if="volume.rootVolumeObj">{{volume.rootVolumeObj.name}}</td>
                              <td :data-sort-value="volume.owner" v-if="volume.owner">{{volume.owner}}</td>
                            </template>

                             <template v-if="volume.type === 'datavolumes'">
                               <td>{{volume.name}}</td>
                              <td></td>
                            </template>

                            <td>
                              <div class="dropdown" v-if="!isShowingFiles && (volume.grant || volume.delete || (volume.owner===username && volume.type==='uservolumes'))">
                                <button class="btn btn-default btn-xs dropdown-toggle" type="button" id="fileMenuDropDown" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                                  <i class="fa fa-ellipsis-h" aria-hidden="true"></i>
                                </button>
                                <ul class="dropdown-menu" aria-labelledby="fileMenuDropDown">
                                  <li v-if="volume.grant"><a href="#" title="Share User Volume" @click="shareVolume(volume)">Sharing</a></li>
                                  <li v-if="volume.delete"> <a href="#" title="Delete User Volume"  @click="deleteUserVolume(volume.id)">Delete</a></li>
                                  <li v-if="volume.owner===username"> <a href="#" title="Edit User Volume"  @click="editVolume(volume)">Edit</a></li>
                                </ul>
                                </div>
                                     <div class="dropdown" v-if="isShowingFiles">
                                <button class="btn btn-default btn-xs dropdown-toggle" type="button" id="fileMenuDropDown" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                                  <i class="fa fa-ellipsis-h" aria-hidden="true"></i>
                                </button>
                                <ul class="dropdown-menu" aria-labelledby="fileMenuDropDown">
                                  <li v-if="volume.type==='file' && isShowingFiles">
                                    <router-link :to="`/editor/?f=${downloadFilePath}/${encodeURIComponent(volume.name)}`">View/Edit</router-link>
                                  </li>
                                  <li v-if="volume.type==='file' && isShowingFiles"><a :href="downloadFilePath + '/' + encodeURIComponent(volume.name)" title="Download">Download</a></li>
                                  <li v-if="isShowingFiles"><a href="#" title="Copy file/folder" @click.prevent="childCopy(volume)">Copy</a></li>
                                  <li v-if="isShowingFiles && writeAccess"> <a href="#" title="Rename file/folder" @click="renameFileOrFolder(volume.name)">Rename</a></li>
                                  <li v-if="isShowingFiles && writeAccess"><a href="#" title="Move file/folder" @click.prevent="childMove(volume)">Move</a></li>
                                  <li v-if="isShowingFiles && writeAccess"><a href="#" title="Delete file/folder" @click="deleteFile(volume.name)">Delete</a></li>
                                </ul>
                                </div>
                              </td>
                              <td>
                              <div>
                              <router-link :to="`${notebookBasePath}/${volume.name.replace('.ipynb', '')}`" v-if="volume.name.endsWith('.ipynb') && isNotebookFirstAlphaGroupMember"><label type="button" class="btn btn-default btn-xs"><span class="fa fa-notebook-o" aria-hidden="true"></span>Open Notebook</label></router-link>
                              <a :href="downloadFilePath + '/' + encodeURIComponent(volume.name)" title="Download" v-if="volume.type==='file'"><label type="button" class="btn btn-default btn-xs"><span class="fa fa-download" aria-hidden="true"></span> Download</label></a>
                              <a href="#" title="Copy file/folder" v-if="isShowingFiles"><label type="button" class="btn btn-default btn-xs" @click.prevent="childCopy(volume)"><span class="fa fa-files-o" aria-hidden="true"></span> Copy</label></a>
                              <a href="#" title="Rename file/folder" v-if="isShowingFiles && writeAccess"><label type="button" class="btn btn-default btn-xs" @click="renameFileOrFolder(volume.name)"><span class="fa fa-pencil-square-o" aria-hidden="true"></span> Rename</label></a>
                              <a href="#" title="Move file/folder" v-if="isShowingFiles && writeAccess"><label type="button" class="btn btn-default btn-xs" @click.prevent="childMove(volume)"><span class="fa fa-share-square-o" aria-hidden="true"></span> Move</label></a>
                              <a href="#" title="Delete file/folder" @click="deleteFile(volume.name)" v-if="isShowingFiles && writeAccess"><label type="button" class="btn btn-default btn-xs"><span class="fa fa-trash" aria-hidden="true"></span> Delete</label></a>
                              <label  class="btn btn-default btn-sm" v-if="volume.grant" @click="shareVolume(volume)" title="share user volume with other users or groups"><span class="fa fa-users" aria-hidden="true"></span> Sharing</label>
                              <label class="btn btn-default btn-sm" v-if="volume.delete" @click="deleteUserVolume(volume.id)" title="Delete User Volume"><span class="fa fa-trash" aria-hidden="true"></span> Delete</label>
                               <label class="btn btn-default btn-sm" v-if="!isShowingFiles && volume.owner===username && editUVAction" @click="editVolume(volume)" title="Edit User Volume"><span class="fa fa-pencil" aria-hidden="true"></span> Edit</label>
                              </div>
                              </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    </div>
                     <div class="col-sm-4" v-show="isShowingFiles && writeAccess">
                       <vue-dropzone v-show="isShowingFiles && writeAccess" ref="vueDropzone" id="dropzone"
                                     :options="dropzoneOptions" @vdropzone-error="comeback"
                                     @vdropzone-processing="dropzoneChangeUrl" @vdropzone-sending="dropzoneSendRequest"
                                     @vdropzone-complete="fileUploadComplete"></vue-dropzone>
                     </div>
                    </div>
                    </div>
                </div>
           </div>
    </div>
    <move-files ref="moveButton" @movedSuccess="getFiles" @changeVolumesType="changeVolumesTypeFromParent" @showFiles="this.$emit('showFiles', index)"
    @changeUV="this.$emit('changeUV', id)" @resetPath="this.$emit('resetPath')"></move-files>
    <copy-file ref="copyButton"  @copiedSuccess="getFiles" @changeVolumesType="changeVolumesTypeFromParent" @showFiles="this.$emit('showFiles', index)"
    @changeUV="this.$emit('changeUV', id)" @resetPath="this.$emit('resetPath')"></copy-file>
    <share-user-volume-dialog ref="shareUserVolumeDialog" @sharedVolume="getVolumes" />
    <create-folder-dialog :userVolume="currentUserVolume" :path="path" @newSubFolderCreated="aftercalled" ref="createFolderDialog"/>
    <create-volume-dialog @newVolumeCreated="getVolumes" ref="createVolumeDialog" />
    <edit-volume-dialog  @userVolumeEdited="getVolumes" ref="editVolumeDialog" />
    <rename-file-dialog :userVolume="currentUserVolume" :path="path" @fileRenamed="getFiles" ref="renameFileDialog" />
    <file-usage-dialog ref="fileUsageDialog" />
    <delete-file-dialog ref="deleteFileDialog" @itemDeleted="refreshFileService" />
    <pop-up-dialog ref="alertPopUp"/>
  </div>
</div>
</template>

<script>
import axios from 'axios';
import vueDropzone from 'vue2-dropzone';
import find from 'lodash/find';
import Vue from 'vue';
import includes from 'lodash/includes';
import concat from 'lodash/concat';
import get from 'lodash/get';
import map from 'lodash/map';
import has from 'lodash/has';
// import orderBy from 'natural-orderby';
import firstby from 'thenby';
import { mapState, mapGetters } from 'vuex';
import moveFiles from './files-tab/moveFiles';
import copyFile from './files-tab/copyFile';
import CreateVolumeDialog from './files-tab/CreateVolumeDialog';
import CreateFolderDialog from './files-tab/CreateFolderDialog';
import EditVolumeDialog from './files-tab/EditVolumeDialog';
import RenameFileDialog from './files-tab/RenameFileDialog';
import ShareUserVolumeDialog from './files-tab/ShareUserVolumeDialog';
import FileUsageDialog from './files-tab/FileUsageDialog';
import filesUtils from '../files-utils';
import RootVolumeTemplate from './files-tab/RootVolumes';
import DeleteFileDialog from './files-tab/DeleteFileDialog';
import popUpDialog from './sharedComponents/alertDialog';
import listGroup from './sharedComponents/listGroup';
import LoadingStatus from './files-tab/LoadingStatus';

const marked = require('marked');
const dompurify = require('dompurify');

const mathUtils = require('../math-utils').default;
const fileUtils = require('../files-utils').default;

const $ = require('jquery');
const moment = require('moment');

export default {
  name: 'FilesTab',
  components: {
    moveFiles,
    copyFile,
    CreateVolumeDialog,
    CreateFolderDialog,
    EditVolumeDialog,
    RenameFileDialog,
    ShareUserVolumeDialog,
    FileUsageDialog,
    DeleteFileDialog,
    vueDropzone,
    LoadingStatus,
    RootVolumeTemplate,
    popUpDialog,
    listGroup,
  },
  mounted() {
  },
  beforeRouteEnter(to, from, next) {
    next((vm) => {
      if (vm.lastRoute !== '' && vm.lastRoute !== '/files' && to.path === '/files') {
        next(`${vm.lastRoute}`);
      } else if (vm.lastRoute !== '' && to.path !== '/files') {
        next(to.path);
      } else if (vm.lastRoute === '' && to.path !== '/files') {
        next(to.path);
      } else {
        next(`/files/${vm.selectedVolumeType}`);
      }
    });
  },
  beforeRouteLeave(to, from, next) {
    this.$store.dispatch('loadLastPath', from.path);
    next();
  },
  data: () => ({
    filelist: false,
    searchTerm: '',
    selectedItem: [],
    isSelectedAll: false,
    fileManagement: FILE_MANAGEMENT,
    file_uploading: false,
    dropzoneOptions: {
      url: 'url',
      thumbnailWidth: 150,
      uploadMultiple: false,
      method: 'put',
      maxFiles: 100,
      parallelUploads: 20,
      timeout: 1800000,
    },
    display_spinner: false,
    sortedObject: '',
    increaseIcon: {
      content: '\f0dc',
      color: 'red',
    },
    titleChanged: false,
    currentList: [],
    isReversed: false,
    currentSelectedHeader: 'name',
    isCurrentIncreasing: true,
    readmeData: '',
    readmePath: '',
  }),
  created() {
    this.getVolumes();
  },
  computed: {
    volumeIcon() {
     if (this.currentSelectedHeader === 'name') {
        if (this.isCurrentIncreasing) {
          return 'sorted ascending';
        }
       return 'sorted descending';
     }
      return 'sorted normal';
    },
    rootVolumeIcon() {
      if (this.currentSelectedHeader === 'rootVolumeName') {
        if (this.isCurrentIncreasing) {
          return 'sorted ascending';
        }
        return 'sorted descending';
      }
      return 'sorted normal';
    },
    ownerIcon() {
      if (this.currentSelectedHeader === 'owner') {
        if (this.isCurrentIncreasing) {
          return 'sorted ascending';
        }
        return 'sorted descending';
      }
      return 'sorted normal';
    },
    dataSetsIcon() {
      if (this.currentSelectedHeader === 'displayName') {
        if (this.isCurrentIncreasing) {
          return 'sorted ascending';
        }
        return 'sorted descending';
      }
      return 'sorted normal';
    },
    foldersIcon() {
      if (this.currentSelectedHeader === 'name') {
        if (this.isCurrentIncreasing) {
          return 'sorted ascending';
        }
        return 'sorted descending';
      }
      return 'sorted normal';
    },
    nameIcon() {
      const temp = this.$store.state.sortingStatus;
      if (temp.substring(1) === 'name') {
        if (temp.charAt(0) === '+') {
          return 'sorted ascending';
        }
        return 'sorted descending';
      }
      return 'sorted normal';
    },
    modifiedIcon() {
      const temp = this.$store.state.sortingStatus;
      if (temp.substring(1) === 'modified') {
        if (temp.charAt(0) === '+') {
          return 'sorted ascending';
        }
        return 'sorted descending';
      }
      return 'sorted normal';
    },
    sizeIcon() {
      const temp = this.$store.state.sortingStatus;
      if (temp.substring(1) === 'rsize') {
        if (temp.charAt(0) === '+') {
          return 'sorted ascending';
        }
        return 'sorted descending';
      }
      return 'sorted normal';
    },
    isShowingFiles() {
      return this.currentUserVolume;
    },
    sortedVolumes() {
      const currentRouterParam = this.$route.params.userVolumeId;
      if (currentRouterParam) {
        return this.fileSorting;
      }
      return this.volumesSorting;
    },
    volumesSorting() {
      const routerParam = this.$route.params.rootVolume;
      if (this.currentSelectedHeader.length === 0 && routerParam === 'uservolumes') {
        return this.searchedActivities.sort(
          firstby('name', { ignoreCase: true }));
      }
      if (this.currentSelectedHeader.length === 0 && routerParam === 'dataVolumes') {
        return this.searchedActivities.sort(
          firstby('displayName', { ignoreCase: true }));
      }
      if (this.currentSelectedHeader !== null) {
        if (this.isCurrentIncreasing) {
          return this.searchedActivities.sort(
            firstby(this.currentSelectedHeader, { ignoreCase: true }));
        }
        return this.searchedActivities.sort(
          firstby(this.currentSelectedHeader, { ignoreCase: true }))
          .reverse();
      }
      return this.searchedActivities;
    },
    fileSorting() {
      const temp = this.$store.state.sortingStatus;
      if (temp.charAt(0) === '+') {
        return this.searchedActivities.sort(firstby('type').thenBy(temp.substring(1), { ignoreCase: true }));
      }
      if (temp.charAt(0) === '-') {
        return this.searchedActivities.sort(firstby('type')
          .thenBy(temp.substring(1), { ignoreCase: true }))
          .reverse();
      }
      return this.searchedActivities;
    },
    username() {
      return this.userProfile.username;
    },
    searchedActivities() {
      this.loadReadme(this.listToDisplay);
      if (this.isShowingFiles) {
        return this.listToDisplay.filter(volume =>
          (volume.name.toLowerCase().match(this.searchTerm.toLowerCase())) ||
          (volume.size.toLowerCase().match(this.searchTerm.toLowerCase())) ||
          (volume.modified.toLowerCase().match(this.searchTerm.toLowerCase())),
        );
      }
      if (this.selectedVolumeType === 'uservolumes') {
        return this.listToDisplay.filter(volume =>
        (volume.displayName ? volume.displayName
        .toLowerCase().match(this.searchTerm.toLowerCase()) : volume.name
        .toLowerCase().match(this.searchTerm.toLowerCase())) ||
        (volume.rootVolumeObj ? volume.rootVolumeObj.name
        .toLowerCase().match(this.searchTerm
        .toLowerCase()) : volume.name.toLowerCase().match(this.searchTerm.toLowerCase())) ||
        (volume.owner.toLowerCase().match(this.searchTerm.toLowerCase())),
        );
      }
      return this.listToDisplay.filter(volume =>
      (volume.displayName.toLowerCase().match(this.searchTerm.toLowerCase()) ||
      volume.name.toLowerCase().match(this.searchTerm.toLowerCase())),
      );
    },
    resetSearch() {
      this.searchTerm = '';
    },
    availableActions() {
      let actions = '';
      if (this.selectedItem.length && this.isShowingFiles) {
        if (find(this.selectedItem, ['type', 'dir']) && this.writeAccess) {
          actions = 'copy,move,delete';
        } else if (find(this.selectedItem, ['type', 'dir']) && !this.writeAccess) {
          actions = 'copy';
        } else if (!find(this.selectedItem, ['type', 'dir']) && this.writeAccess) {
          actions = 'download,copy,move,delete';
        } else {
          actions = 'download,copy';
        }
      }
      return actions;
    },
    getEachAction() {
      let eachAction = [];
      if (this.availableActions !== '') {
        eachAction = this.availableActions.split(',');
      }
      return eachAction;
    },
    deleteAction() {
      return this.getEachAction.includes('delete');
    },
    copyAction() {
      return this.getEachAction.includes('copy');
    },
    downloadAction() {
      return this.getEachAction.includes('download');
    },
    moveAction() {
      return this.getEachAction.includes('move');
    },
    writeAccess() {
      return this.currentUserVolume && includes(this.currentUserVolume.allowedActions, 'write');
    },
    deleteUVAction() {
      if (find(this.selectedItem, ['delete', false])) {
        return false;
      }
      return true;
    },
    grantUVAction() {
      if (find(this.selectedItem, ['grant', false])) {
        return false;
      }
      return true;
    },
    editUVAction() {
      if (find(this.selectedItem, ['write', false]) || this.selectedItem.find(u => u.owner !== this.username)) {
        return false;
      }
      return true;
    },
    refreshFilesSpinner() {
      return {
        'fa-spin': this.loading === true,
      };
    },
    selectedVolumeType() {
      const id = this.$route.params.rootVolume;
      if (id === undefined) {
        return 'uservolumes';
      }
      return id;
    },
    currentUserVolume() {
      const id = this.$route.params.userVolumeId;
      if (id === undefined || this.myUserVolumeObjects.length === 0) {
        return undefined;
      }
      return find(this.currentListOfUV, { id: parseInt(id, 10) });
    },
    currentListOfUV() {
      if (this.$route.params.rootVolume === 'uservolumes') {
        return fileUtils.getCurrentUVList(this.myUserVolumeObjects);
      } else if (this.$route.params.rootVolume === 'datavolumes') {
        return fileUtils.getCurrentDVList(this.myDataVolumeObjects);
      }
      return undefined;
    },
    currentRootVolumes() {
      const rv = [];
      if (this.myRootVolumeObjects.length > 0) {
        rv.push({ name: 'User Volumes', type: 'uservolumes', id: 1 });
      }
      if (this.myDataVolumeObjects.length > 0) {
        rv.push({ name: 'Data Volumes', type: 'datavolumes', id: 2 });
      }
      return rv;
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
          rsize: file.size,
          modified: file.lastModified === null ? '' : moment(file.lastModified).format('YYYY-MM-DD HH:mm:ss'),
        })),
      );
    },
    listToDisplay() {
      if (this.currentUserVolume) {
        return this.currentFiles;
      }
      return this.getNonExcludedVolumes();
    },
    loading() {
      if (this.currentUserVolume) {
        return !has(this.$store.state.files.jsonTreeCache,
          [this.currentUserVolume.id, this.path]);
      }
      return this.myUserVolumeObjects.length === 0;
    },
    path() {
      if (this.$route.params.path === undefined) return '';
      return `/${this.$route.params.path}`;
    },
    config() {
      return { headers: { 'X-Auth-Token': this.$store.state.token } };
    },
    downloadFilePath() {
      const url = fileUtils.joinURLWithPath(this.currentUserVolume, 'api/file/', this.path);
      return url;
    },
    notebookBasePath() {
      const volpath = this.path === '/' ? '' : this.path;
      return `/notebook/${this.selectedVolumeType}/${this.currentUserVolume.id}${volpath}`;
    },
    columnFields() {
      if (this.selectedVolumeType === 'uservolumes') {
        return ['Volume', 'Root Volume', 'Owner', ' ', ' '];
      } else if (this.selectedVolumeType === 'datavolumes') {
        return ['Data Sets', 'Folders', ' ', ' ', ' '];
      }
      return undefined;
    },
    isNotebookFirstAlphaGroupMember() {
      return this.getGroupIds.find(g => get(this.getCollaborationById(g), 'name') === '__SciserverNotebookFirstDemo__') !== undefined;
    },
    ...mapState(['userProfile', 'token', 'loadingUI', 'sortingStatus', 'loadingAction', 'loadingStatusIcon']),
    ...mapGetters(['myUserVolumeObjects', 'lastRoute', 'myRootVolumeObjects', 'myDataVolumeObjects', 'getGroupIds', 'getCollaborationById']),
  },
  watch: {
    $route(to, from) {
      if (!to.params.rootVolume) {
        this.$router.push(from.fullPath);
      }
    },
    currentUserVolume(uv) {
      if (uv) {
        this.getFiles();
      }
    },
    currentListOfUV() {
      if (this.selectedItem) {
        this.selectedItem = [];
      }
      if (this.isSelectedAll) {
        this.selectedAll();
      }
    },
    currentFiles() {
      if (this.selectedItem) {
        this.selectedItem = [];
      }
      if (this.isSelectedAll) {
        this.selectedAll();
      }
      if (this.searchTerm !== '') {
        this.searchTerm = '';
      }
    },
  },
  // All the methods you want the view to have access to, basically an object of functions
  methods: {
    getNonExcludedVolumes() {
      const excludedResources = this.$store.state.files.serviceOwnedResources;
      return this.currentListOfUV.filter(e => !excludedResources.includes(e.resourceUUID));
    },
    getHeaderName(value) {
      const headermap = new Map();
      headermap.set('Data Sets', 'displayName');
      headermap.set('Volume', 'name');
      headermap.set('Folders', 'name');
      headermap.set('Owner', 'owner');
      headermap.set('Root Volume', 'rootVolumeName');
      this.currentSelectedHeader = headermap.get(value);
      this.isCurrentIncreasing = !this.isCurrentIncreasing;
      this.titleChanged = !this.titleChanged;
    },
    sortingMethod(value) {
      const storeValue = this.$store.state.sortingStatus;
      if (storeValue.substring(1) === value) {
        if (storeValue.charAt(0) === '+') {
          this.$store.commit('setSortingStatus', `-${value}`);
        } else {
          this.$store.commit('setSortingStatus', `+${value}`);
        }
      } else {
        this.$store.commit('setSortingStatus', `+${value}`);
      }
    },
    loadReadme(currentList) {
      const currentPath = this.$route.path;
      // eslint-disable-next-line no-restricted-syntax,guard-for-in
      for (const temp of currentList) {
        if (temp.name === 'README.md') {
          const url = filesUtils.joinURLWithFileName(this.currentUserVolume, 'api/file/', this.path, 'README.md');
          const config = { headers: { 'X-Auth-Token': this.$store.state.token }, validateStatus: status => status >= 0 };
          if (currentPath !== this.readmePath) {
            axios.get(`${url}?TaskName=Dashboard.Files.Readme`, config)
              .then((response) => {
                this.readmePath = currentPath;
                if (response.status === 200) {
                  this.readmeData = '[<a href="#filesTable">Skip to file list</a>]<br>';
                  this.readmeData += dompurify.sanitize(marked(response.data));
                } else {
                  this.readmeData = '';
                }
              });
          }
          return null;
        }
      }
      this.readmeData = '';
      this.readmePath = '';
      return null;
    },
    aftercalled() {
      this.getFiles();
    },
    getVolumes() {
      this.$store.dispatch('loadAllUserVolumes');
      this.$store.dispatch('loadScienceDomains', this.userProfile);
    },
    getFiles() {
      this.$store.dispatch('loadJsonTreeById', { userVolumeId: this.currentUserVolume.id, path: this.path, type: this.currentUserVolume.type });
    },
    refreshFileService() {
      if (this.currentUserVolume) {
        this.getFiles();
      } else {
        this.getVolumes();
      }
    },
    downloadSingleFile(file) {
      const url = filesUtils.joinURLWithFileName(this.currentUserVolume, 'api/file/', this.path, file.name);
      filesUtils.downloadFile(url, file.name);
    },
      editFile(file) {
          const url = filesUtils.joinURLWithFileName(this.currentUserVolume, 'api/file/', this.path, file.name);
          this.$router.push(`/editor?f=${url}`);
    },
    downloadMultipleFiles(files, index) {
      if (index >= 0) {
        const url = filesUtils.joinURLWithFileName(this.currentUserVolume, 'api/file/', this.path, files[index].name);
        fileUtils.downloadFile(url, files[index].name);
        setTimeout(() => { this.downloadMultipleFiles(files, index - 1); }, 5000);
      }
    },
    changeSelectedVolumesType(type) {
      this.selectedVolumeType = filesUtils.changeVolumesType(type);
      this.$router.push({ path: '/files' });
    },
    changeVolumesTypeFromParent(type) {
      this.$emit('changeVolumesType', type);
    },
    dropzoneChangeUrl(file) {
      const url = filesUtils.joinURLWithFileName(this.currentUserVolume, 'api/file/', this.path, file.name);
      this.$refs.vueDropzone.setOption('headers', { 'X-Auth-Token': this.token });
      this.$refs.vueDropzone.setOption('url', url);
    },
    comeback(file, message, xhr) {
      console.log(xhr);
      console.log(message);
      let errorMessage = null;
      if (message !== null) {
        Vue.notify({
          group: 'top_center_notify',
          text: 'File upload failed.',
          duration: 1000,
          type: 'error',
          ignoreDuplicates: true,
        });
      }
      if (message.status === 'error') {
        errorMessage = message.error;
      } else {
        errorMessage = `${message} For large file upload, use the upload button.`;
      }
      const actionObject = ({
        actionName: 'Uploading',
        actionFileName: file.name,
        actionStatus: 'False',
        actionRequired: 'True',
        actionMessage: errorMessage,
      });
      this.$store.commit('updateMessage', actionObject);
    },
    dropzoneSendRequest(file, xhr) {
      const actionObject = ({
        actionName: 'Uploading',
        actionFileName: file.name,
        actionStatus: 'True',
        actionRequired: 'False',
      });
      this.$store.commit('updateMessage', actionObject);
      this.$store.commit('loadingStatus', ['Uploading', true]);
      const _send = xhr.send;
      xhr.send = function send() {
        _send.call(xhr, file);
      };
    },
    fileUploadComplete(file) {
      this.getFiles();
      const actionObject = ({
        actionName: 'Uploading',
        actionFileName: file.name,
        actionStatus: 'False',
        actionRequired: 'False',
      });
      this.$store.commit('loadingStatus', '');
      this.$store.commit('updateMessage', actionObject);
      this.$refs.vueDropzone.removeFile(file);
    },
    fileUploadClicked() {
      $('#uploadedFile')[0].value = null;
    },
    uploadFile() {
      const filepath = `${this.currentUserVolume.type}/${this.currentUserVolume.name}${this.path}`;
      const actionUrl = `${this.currentUserVolume.type}/${this.$route.params.userVolumeId}${this.path}`;
      let actionObject = ({
        actionName: 'Uploading',
        actionFileName: null,
        actionLocation: null,
        actionStatus: 'True',
        actionRequired: null,
        actionurl: null,
      });
      const files = $('#uploadedFile')[0].files;
      const config = {
        headers: {
          'X-Auth-Token': this.$store.state.token,
          'Content-Type': 'application/octet-stream',
        },
      };
      const that = this;
      if (files.length > 0) {
        for (let i = 0; i < files.length; i += 1) {
          const file = $('#uploadedFile')[0].files[i];
          if (file) {
            actionObject = ({
              actionName: 'Uploading',
              actionFileName: file.name,
              actionLocation: `${filepath}`,
              actionStatus: 'True',
              actionRequired: null,
              actionurl: `${actionUrl}`,
            });
            that.$store.commit('updateMessage', actionObject);
          }
        }
      }
      if (files.length > 0) {
        for (let i = 0; i < files.length; i += 1) {
          const file = $('#uploadedFile')[0].files[i];
          if (file) {
            this.$store.commit('loadingStatus', ['Uploading', true]);
            let errorMessage = '';
            const url = filesUtils.joinURLWithFileName(that.currentUserVolume, 'api/file/', that.path, file.name);
            // eslint-disable-next-line chai-friendly/no-unused-expressions,no-loop-func
            axios.put(url, file, config)
              .then(() => {
                try {
                  that.getFiles();
                  // eslint-disable-next-line no-empty
                } catch (e) {}
              })
              // eslint-disable-next-line no-loop-func
              .catch((error) => {
                try {
                  errorMessage = error.response.data.error;
                } catch (e) {
                  errorMessage = 'Please make sure if the file already exists.';
                }
              })
              // eslint-disable-next-line no-loop-func
              .finally(() => {
                this.$forceUpdate();
                this.$store.commit('loadingStatus', ['Uploading', false]);
                if (errorMessage.length === 0) {
                  this.storeNotificationUpdate(file.name, filepath, 'False', 'False', url, '');
                } else if (errorMessage === 'true') {
                  this.storeNotificationUpdate(file.name, filepath, 'False', 'True', url, errorMessage);
                } else {
                  this.storeNotificationUpdate(file.name, filepath, 'False', 'True', url, errorMessage);
                }
              });
          }
        }
      }
    },
    storeNotificationUpdate(fileName,
                            filePath, actionStatus, actionrequired, actionUrl, actionmessage) {
      const actionObject = ({
        actionName: 'Uploading',
        actionFileName: fileName,
        actionLocation: filePath,
        actionStatus: 'False',
        actionRequired: actionrequired,
        actionurl: actionUrl,
        actionMessage: actionmessage,
      });
      if (actionmessage.length === 0) {
        Vue.notify({
          group: 'top_center_notify',
          text: 'File upload successful',
          duration: 1000,
          type: 'success',
          ignoreDuplicates: true,
        });
      }
      this.$store.commit('updateMessage', actionObject);
    },
    childMove(selected) {
      const url = filesUtils.joinURLWithPath(this.currentUserVolume, 'api/data/', this.path);
      this.$refs.moveButton.move(selected, url);
    },
    childCopy(files) {
      const url = filesUtils.joinURLWithPath(this.currentUserVolume, 'api/data/', this.path);
      this.$refs.copyButton.copy(files, url);
    },
    sendTodelete(files) {
      if (confirm(`Are you sure you want to delete ${files.length} items?`)) {
        $.each(files, (index0, text0) => {
          this.deleteFiles(text0.name);
          Vue.notify({
            group: 'top_center_notify',
            text: 'Delete Successful!',
            duration: 1000,
            type: 'error',
            ignoreDuplicates: true,
          });
        });
      }
    },
    deleteFiles(fileName) {
      $('#filelist').show();
      if (!this.listToDisplay) { $('#filelist').show(); }
      const url = fileUtils.joinURLWithFileName(this.currentUserVolume, 'api/data/', this.path, fileName);
      axios.delete(url, this.config)
      .then(() => {
        this.getFiles();
      }, (error) => {
        this.$refs.alertPopUp.showAlert(error.response.data.error, 'Error Message');
      });
    },
    deleteFile(fileName) {
      $('#filelist').show();
      if (!this.listToDisplay) { $('#filelist').show(); }
      if (confirm(`Are you sure you want to delete ${fileName}`)) {
        this.deleteFiles(fileName);
      }
    },
    sendTodeleteUserVolume(volumes) {
        if (!this.listToDisplay) { $('#filelist').show(); }
        if (confirm(`Are you sure you want to delete ${volumes.length} items?`)) {
        this.deleteUserVolumes(volumes, volumes.length - 1);
      }
    },
    findVolumeById(id) {
      return find(this.currentListOfUV, { id });
    },
    deleteUserVolumes(volumes, index) {
        if (index >= 0) {
        const userVolume = this.findVolumeById(volumes[index].id);
        const url = filesUtils.joinBaseURL(userVolume, 'api/volume/');
        axios.delete(url, this.config).then(() => {
          this.deleteUserVolumes(volumes, index - 1);
          this.getVolumes();
        });
      }
    },
    deleteUserVolume(userVolumeId) {
      const userVolume = this.findVolumeById(userVolumeId);
      const url = filesUtils.joinBaseURL(userVolume, 'api/volume/');
      this.$refs.deleteFileDialog.startDialog(userVolume.name, url);
    },
    sendToeditVolume(volumes) {
      $.each(volumes, (index0, text0) => {
        this.editVolume(text0);
      });
    },
    sendToshareVolume(volumes) {
      $.each(volumes, (index0, text0) => {
        this.shareVolume(text0);
      });
    },
    sendToUnshareVolume(volumes) {
      $.each(volumes, (index0, text0) => {
        this.unshareVolume(text0);
      });
    },
    createNewVolume() {
        $('#spinner').show();
        this.$refs.createVolumeDialog.startDialog();
      // $('#spinner').hide();
    },
    createNewSubFolder() {
      this.$refs.createFolderDialog.startDialog();
    },
    editVolume(volume) {
      this.$refs.editVolumeDialog.startDialog(volume);
    },
    shareVolume(volume) {
      this.$refs.shareUserVolumeDialog.startDialog(volume);
    },
    renameFileOrFolder(name) {
      this.$refs.renameFileDialog.startDialog(name);
    },
    viewQuotas() {
      this.$refs.fileUsageDialog.startDialog();
    },
    selectedAll() {
      this.isSelectedAll = !this.isSelectedAll;
      this.selectedItem = [];
      if (this.isSelectedAll) { // Check all
        this.selectedItem = Object.values(this.listToDisplay);
      }
    },
    select() {
      this.isSelectedAll = false;
    },
  },
};
</script>
<style>
.someascending{
 background-color: red;
}
th.sorted.ascending:after {
    content: "\f0de";
    font: normal normal normal 12px/1 FontAwesome;
    margin-left: 2px;
}
th.sorted.descending:after {
  content: "\f0dd";
  font: normal normal normal 12px/1 FontAwesome;
  margin-left: 2px;
}
th.sorted.normal:after {
  content: "\f0dc";
  color: lightgray;
  font: normal normal normal 12px/1 FontAwesome;
  margin-left: 2px;
}
.sortable{
  cursor: pointer;
}
/*--- FontAwesome Enhancement ---*/
span i:not(:first-child){
  margin-top:4px;
}
.btnStyle {
  text-align: center;
}
@media (max-width: 767px) {
    .table-responsive .dropdown-menu {
        position: static !important;
    }
}
@media (min-width: 768px) {
    .table-responsive {
        overflow: inherit;
    }
}
table#filesTable tr td label.btn {
  opacity: 0;
  }
table#filesTable tr:hover td label.btn {
  opacity: 1;
  }
</style>
