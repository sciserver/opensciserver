<template>
  <div>
    <div class="panel panel-primary" v-if="hasGrantableResource('USERVOLUME') || sharedUserVolumes.length > 0">
      <div class="panel-heading">
        <span class="panel-title">Shared <span v-if="collabType === 'Group'">Files</span><span v-else>Course Materials</span></span>
         <a rel="noopener" class= "contextualHints" target="_blank" :href="shareResource" title="How to share files" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">How to share files</span>
					  </a>
        <button v-if="hasGrantableResource('USERVOLUME') && this.collaboration.type !== 'PUBLIC GROUP'" class="btn btn-success btn-xs pull-right" @click="openShareUserVolumeDialog"><span class="fa fa-plus"></span></button>
      </div>
      <div class="panel-body resource-container">
        <div
          class="btn-group text-center resource-item"
          v-for="uv in sharedUserVolumes"
          :key="uv.resourceId">

          <div class="btn btn-default resource-name dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <i class="fa fa-folder fa-2x" style="color:#008ae6" aria-hidden="true"></i><br/>{{ uv.name }}
            <span class="sr-only">Toggle Menu</span>
          </div>
          <ul class="dropdown-menu">
            <li><router-link :to="`/files/uservolumes/${uv.entityId}`" :title="`Go to ${uv.name}`">Go To</router-link></li>
            <li><a href="#" title="View More Info" @click="openInfo('userVolumeInfoDialog', getResourceDetails('USERVOLUME', uv.entityId), uv)">More Info</a></li>
            <li v-if="isGrantableThroughAnyMeans('USERVOLUME', uv.entityId)" @click="unshare([getResourceDetails('USERVOLUME', uv.entityId)])"><a href="#" title="Remove Resource">Remove</a></li>
          </ul>
        </div>
        <div v-show="!sharedUserVolumes.length" class="text-muted center-block">Share user volumes with this group to see them here.</div>
      </div>
    </div>
    <resource-info-dialog ref="userVolumeInfoDialog" title="User Volume Info">
      <template slot-scope="slotProps">
        <h4 class="text-center">{{ slotProps.info.name }}</h4>
        <dl class="dl-horizontal" v-if="slotProps.info.name">
          <dt>Creator</dt><dd>{{ slotProps.info.owner }}</dd>
          <dt>Description</dt><dd>{{ slotProps.info.description }}</dd>
          <dt>Type of Volume</dt><dd>{{ slotProps.info.rootVolume.name }}</dd>
          <dt>Hosted By</dt><dd >{{ slotProps.info.fileService.name }}</dd>
          <dt>Actions Allowed</dt>
          <dd>
            <div v-if="slotProps.groupInfo.actions.includes('read')">Read</div>
            <div v-if="slotProps.groupInfo.actions.includes('write')">Write</div>
            <div v-if="slotProps.groupInfo.actions.includes('delete')">Delete user volume</div>
            <div v-if="slotProps.groupInfo.actions.includes('grant')">Share user volume with others</div>
          </dd>
        </dl>
      </template>
    </resource-info-dialog>

    <div class="panel panel-primary" v-if="hasGrantableResource('DATAVOLUME') || sharedDataVolumes.length > 0">
  <div class="panel-heading">
    <span class="panel-title">Shared Data Volume</span>
    <a rel="noopener" class= "contextualHints" target="_blank" :href="shareResource" title="How to share Data Volume" style="text-decoration: none;"> 
						<span class="sr-only">How to share Data Volume</span>
					  </a>
<button v-if="hasGrantableResource('DATAVOLUME') && this.collaboration.type !== 'PUBLIC GROUP'" @click="openShareDataVolumeDialog" class="btn btn-success btn-xs pull-right"><span class="fa fa-plus" aria-hidden="true"></span></button>
      </div>
      <div class="panel-body resource-container">
        <div class="btn-group text-center resource-item"
      v-for="dv in sharedDataVolumes"
      :key="dv.resourceId">
      <div class="resource-name btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <i class="fa fa-archive fa-2x" aria-hidden="true"></i><br/>{{ dv.name }}
        <span class="sr-only">Toggle Menu</span>
          </div>
           <ul class="dropdown-menu">
        <li><router-link :to="`/files/datavolumes/${dv.entityId}`" :title="`Go to ${dv.name}`">Go To</router-link></li>
        <li><a href="#" title="View More Info" @click="openInfo('dataVolumeContainerInfoDialog', getResourceDetails('DATAVOLUME', dv.entityId), allGroupInfos('DATAVOLUME', dv.name, 'dockerComputeDomain.name'))">More Info</a></li>
        <li v-if="isGrantableThroughAnyMeans('DATAVOLUME', dv.entityId)" @click="unshare(allGroupInfos('DATAVOLUME', dv.name, 'dockerComputeDomain.name'))"><a href="#" title="Remove Resource">Remove</a></li>
      </ul>
        </div>
        <div v-show="!sharedDataVolumes.length" class="text-muted center-block">Share Data Volume with this group to see them here.</div>
      </div>
    </div>
    <resource-info-dialog ref="dataVolumeContainerInfoDialog" title="Data Volume Info">
      <template slot-scope="slotProps">
        <h4 class="text-center">{{ slotProps.info.name }}</h4>
        <dl class="dl-horizontal" v-if="slotProps.info.name">
          <dt>Description</dt><dd>{{ slotProps.info.description }}</dd>
          <dt>Actions Allowed</dt>
          <dd>
            <div v-for="resource in slotProps.groupInfo" :key="resource.entityId">
              <div v-if="resource.actions.includes('read')">Read</div>
              <div v-if="resource.actions.includes('write')">Write</div>
              <div v-if="resource.actions.includes('delete')">Delete</div>
              <div v-if="resource.actions.includes('grant')">Share Data Volume with others</div>
            </div>
          </dd>
        </dl>
      </template>
    </resource-info-dialog>

    <div class="panel panel-primary" v-if="(hasGrantableResource('DOCKERIMAGE') || sharedImages.length > 0) && collabType === 'Group'">
      <div class="panel-heading">
        <span class="panel-title">Shared Compute Images</span>
         <a rel="noopener" class= "contextualHints" target="_blank" :href="shareResource" title="How to share compute images" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">How to share compute images</span>
					  </a>
        <button v-if="hasGrantableResource('DOCKERIMAGE') && this.collaboration.type !== 'PUBLIC GROUP'" class="btn btn-success btn-xs pull-right" @click="openShareDockerImagesDialog"><span class="fa fa-plus" aria-hidden="true"></span></button>
      </div>
      <div class="panel-body resource-container">
        <div
          class="btn-group text-center resource-item"
          v-for="image in sharedImages"
          :key="image.resourceId">
          <div class="resource-name btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <i class="fa fa-th fa-2x" aria-hidden="true"></i><br/>{{ image.name }}
            <span class="sr-only">Toggle Menu</span>
          </div>
          <ul class="dropdown-menu">
            <li><a href="#" title="View More Info" @click="openInfo('dockerImageInfoDialog', getResourceDetails('DOCKERIMAGE', image.entityId), allGroupInfos('DOCKERIMAGE', image.name, 'dockerComputeDomain.name'))">More Info</a></li>
            <li v-if="isGrantableThroughAnyMeans('DOCKERIMAGE', image.entityId)" @click="unshare(allGroupInfos('DOCKERIMAGE', image.name, 'dockerComputeDomain.name'))"><a href="#" title="Remove Resource">Remove</a></li>
          </ul>
        </div>
        <div v-show="!sharedImages.length" class="text-muted center-block">Share compute images with this group to see them here.</div>
      </div>
    </div>
    <resource-info-dialog ref="dockerImageInfoDialog" title="Compute Image Info">
      <template slot-scope="slotProps">
        <h4 class="text-center">{{ slotProps.info.name }}</h4>
        <dl class="dl-horizontal" v-if="slotProps.info.name">
          <dt>Description</dt><dd>{{ slotProps.info.description }}</dd>
          <dt>Actions Allowed</dt>
          <dd>
            <div v-for="resource in slotProps.groupInfo" :key="resource.entityId">
              <div><b>For {{ getResourceDetails('DOCKERIMAGE', resource.entityId).dockerComputeDomain.name }}:</b></div>
              <div v-if="resource.actions.includes('createContainer')">Use image for new containers</div>
              <div v-if="resource.actions.includes('grant')">Share image with others</div>
            </div>
          </dd>
        </dl>
      </template>
    </resource-info-dialog>

    <div class="panel panel-primary" v-if="(hasGrantableResource('DATABASE') || sharedDatabases.length > 0) && collabType === 'Group'">
      <div class="panel-heading">
        <span class="panel-title">Shared Databases</span>
         <a rel="noopener" class= "contextualHints" target="_blank" :href="shareResource" title="How to share databases" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">How to share databases</span>
					  </a>
        <button v-if="hasGrantableResource('DATABASE') && this.collaboration.type !== 'PUBLIC GROUP'" class="btn btn-success btn-xs pull-right" @click="openShareDatabaseDialog"><span class="fa fa-plus" aria-hidden="true"></span></button>
      </div>
      <div class="panel-body resource-container">
        <div
          class="btn-group text-center resource-item"
          v-for="database in sharedDatabases"
          :key="database.resourceId">
          <div class="resource-name btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <i class="fa fa-database fa-2x" aria-hidden="true"></i><br/>{{ database.name }}
            <span class="sr-only">Toggle Menu</span>
          </div>
          <ul v-if="database.type === 'DATABASE'" class="dropdown-menu">
            <li><a href="#" title="View More Info" @click="openInfo('databaseInfoDialog', getResourceDetails('DATABASE', database.entityId), allGroupInfos('DATABASE', database.name, 'rdbComputeDomain.name'))">More Info</a></li>
            <li v-if="isGrantableThroughAnyMeans('DATABASE', database.entityId)" @click="unshare(allGroupInfos('DATABASE', database.name, 'rdbComputeDomain.name'))"><a href="#" title="Remove Resource">Remove</a></li>
          </ul>
            <ul v-if="database.resourceType === 'Casjobs.DatabaseContext'" class="dropdown-menu">
            <li><a href="#" title="View More Info" @click="openInfo('casJobsDatabaseInfoDialog', getResourceDetails('RESOURCE', database.entityId), allGroupInfos('RESOURCE', database.name, 'CasJobs'))">More Info</a></li>
            <li v-if="isGrantableThroughAnyMeans('RESOURCE', database.entityId)" @click="unshare(allGroupInfos('RESOURCE', database.name, 'CasJobs'))"><a href="#" title="Remove Resource">Remove</a></li>
          </ul>
        </div>
        <div v-show="!sharedDatabases.length" class="text-muted center-block">Share database context with this group to see them here.</div>
      </div>
    </div>
    <resource-info-dialog ref="databaseInfoDialog" title="Database Info">
      <template slot-scope="slotProps">
        <h4 class="text-center">{{ slotProps.info.name }}</h4>
        <dl class="dl-horizontal" v-if="slotProps.info.name">
          <dt>Description</dt><dd>{{ slotProps.info.description }}</dd>
          <dt>Actions Allowed</dt>
          <dd>
            <div v-for="resource in slotProps.groupInfo" :key="resource.entityId">
              <div><b>For {{ getResourceDetails('DATABASE', resource.entityId).rdbComputeDomain.name }}:</b></div>
              <div v-if="resource.actions.includes('QUERY')">Query</div>
              <div v-if="resource.actions.includes('UPDATE')">Update</div>
              <div v-if="resource.actions.includes('GRANT')">Share database with others</div>
            </div>
          </dd>
        </dl>
      </template>
    </resource-info-dialog>
     <resource-info-dialog ref="casJobsDatabaseInfoDialog" title="Database Info">
      <template slot-scope="slotProps">
        <h4 class="text-center">{{ slotProps.info.name }}</h4>
        <dl class="dl-horizontal" v-if="slotProps.info.name">
          <dt>Description</dt><dd class="resource-description">{{ slotProps.info.description }}</dd>
          <dt>Actions Allowed</dt>
          <dd>
            <div v-for="resource in slotProps.groupInfo" :key="resource.entityId">
              <div v-if="resource.actions.includes('submitQuery')">Submit Query</div>
              <div v-if="resource.actions.includes('viewSchema')">View Schema</div>
              <div v-if="resource.actions.includes('grant')">Share database with others</div>
            </div>
          </dd>
        </dl>
      </template>
    </resource-info-dialog>

    <share-user-volume-dialog  @linkUVResources="addResources" ref="shareUserVolumeDialog"></share-user-volume-dialog>
    <share-volume-container @linkVCResources="addResources" ref="shareVolumeContainerDialog"></share-volume-container>
    <share-docker-images @linkDIResources="addResources" ref="shareDockerImagesDialog"></share-docker-images>
    <share-database-contexts @linkDBResources="addResources" ref="shareDatabaseDialog"/>
    <share-data-volume @linkDVResources="addResources" ref="shareDataVolumeDialog"></share-data-volume>
  </div>
</template>

<script>
import uniqBy from 'lodash/uniqBy';
import filter from 'lodash/filter';
import sortBy from 'lodash/sortBy';
import some from 'lodash/some';
import get from 'lodash/get';
import concat from 'lodash/concat';
import { mapGetters, mapActions } from 'vuex';

import shareUserVolumeDialog from './shareUserVolumeDialog';
import shareVolumeContainer from './shareVolumeContainer';
import shareDockerImages from './shareDockerImages';
import shareDatabaseContexts from './shareDatabaseContexts';
import shareDataVolume from './shareDataVolumeDialog';
import resourceInfoDialog from './ResourceInfoDialog';

export default {
  props: ['selectedResources', 'collaboration', 'collabOwner', 'collabType'],
  components: {
    shareUserVolumeDialog,
    shareVolumeContainer,
    shareDockerImages,
    shareDatabaseContexts,
    resourceInfoDialog,
    shareDataVolume,
  },
  data: () => ({
    shareResource: SHARE_RESOURCE,
  }),
  computed: {
    sharedVolumes() {
      if (this.isPublicGroup) {
        return this.uniqAndSortBy(filter(this.selectedResources, { type: 'VOLUMECONTAINER' }), 'name');
      }
      return this.uniqAndSortBy(filter(this.selectedResources, { type: 'VOLUMECONTAINER' }), 'name');
    },
    sharedImages() {
      if (this.isPublicGroup) {
        return this.uniqAndSortBy(filter(this.selectedResources, { type: 'DockerImage' }), 'name');
      }
      return this.uniqAndSortBy(filter(this.selectedResources, { type: 'DOCKERIMAGE' }), 'name');
    },
    sharedDataVolumes() {
      if (this.isPublicGroup) {
        return this.uniqAndSortBy(filter(this.selectedResources, { type: 'FileService.DataVolume' }), 'name');
      }
      return this.uniqAndSortBy(filter(this.selectedResources, { type: 'DATAVOLUME' }), 'name');
    },
    sharedUserVolumes() {
      if (this.isPublicGroup) {
        return this.uniqAndSortBy(filter(this.selectedResources, { type: 'FileService.UserVolume' }), uv => uv.name.toLowerCase());
      }
      return sortBy(filter(this.selectedResources, { type: 'USERVOLUME' }), uv => uv.name.toLowerCase());
    },
    sharedDatabases() {
      return concat(this.casJobsDatabases, this.sciQueryDatabases);
    },
    casJobsDatabases() {
      return this.uniqAndSortBy(filter(this.selectedResources, { resourceType: 'Casjobs.DatabaseContext' }), 'name');
    },
    sciQueryDatabases() {
      return this.uniqAndSortBy(filter(this.selectedResources, { type: 'DATABASE' }), 'name');
    },
    isWorkspace() {
      return this.collaboration.type === 'WORKSPACE';
    },
    isPublicGroup() {
      return this.collaboration.type === 'PUBLIC GROUP';
    },
    isOwner() {
      return this.collabOwner.role === 'OWNER';
    },
    hasGrantableResource() {
      return resourceType =>
        this.getGrantableResources(resourceType).length > 0;
    },
    ...mapGetters(['getGrantableResources', 'getResourceDetails']),
  },
  methods: {
    uniqAndSortBy(data, field) {
      return uniqBy(data, field).sort((a, b) => a[field].localeCompare(b[field]));
    },
    unshare(items) {
      this.$emit('unshareResource', items);
    },
    openShareUserVolumeDialog() {
      this.$refs.shareUserVolumeDialog.startDialog();
    },
    openShareVolumeContainerDialog() {
      this.$refs.shareVolumeContainerDialog.startDialog();
    },
    openShareDockerImagesDialog() {
      this.$refs.shareDockerImagesDialog.startDialog();
    },
    openShareDatabaseDialog() {
      this.$refs.shareDatabaseDialog.startDialog();
    },
    openShareDataVolumeDialog() {
      this.$refs.shareDataVolumeDialog.startDialog();
    },
    addResources(resources, privilege) {
      this.$emit('shareResources', resources, privilege, this.collaboration);
    },
    openInfo(dialogRef, info, groupInfo) {
      this.$refs[dialogRef].startDialog(info, groupInfo);
    },
    isGrantableThroughAnyMeans(type, entityId) {
      return some(get(this.getResourceDetails(type, entityId), 'allowedActions'), { category: 'G' });
    },
    allGroupInfos(type, name, sortField) {
      return sortBy(filter(this.selectedResources, { type, name }), sortField);
    },
    ...mapActions(['loadResources']),
  },
  mounted() {
    this.loadResources();
  },
};
</script>

<style scoped>
.resource-container {
  display: flex;
  flex-wrap: wrap;
}
.resource-item {
  flex: 1 1 50%;
  display: flex;
  text-align: center;
  padding-top: 1.5ex;
  padding-bottom: 1.5ex;
  min-width: 10em;
  border: 0;
}
@media (min-width: 992px) {
  .resource-item {
    min-width: 20ch;
    flex: 0 1 0;
  }
}
.resource-name {
  white-space: normal;
  word-break: break-all;
  flex: 1;
  border: 0;
  user-select: text;
}
.resource-name-nolink {
  cursor: auto;
}
.resource-menu {
  border: 0;
}
.resource-name:hover {
    text-decoration: none;
		background: rgba(247, 246, 246, 0.993);
}
.resource-description {
  white-space: normal;
  word-break: break-all;
  flex: 1;
  border: 0;
  user-select: text;
}
</style>
