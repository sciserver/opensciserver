<template>
<div id = "homeTab">
  <div id="header">
    <div class="header-content">
      <div class="header-content-inner">
        <h1 id="homeHeading">{{applicationName}} Dashboard</h1>
        <hr>
        <p>{{applicationTagline}}</p>
      </div>
    </div>
  </div>
  <div id="banner"></div>
  <div class="container wrap">
    <div v-if="oneclickNotebookPath && isNotebookFirstAlphaGroupMember" class="row row-eq-height">
      <div class="col-md-2">
        <span style="font-style: italic"> New (Alpha)! </span> <router-link :to="`notebook/${oneclickNotebookPath}?copy=true`"><button class="btn btn-primary"> Quick Notebook </button> </router-link>
        <a href="https://docs.google.com/document/d/1jmKkaOefxf2BMSpxYPMPBM3sNsoK_F3UK8k1pHmwVBc/edit" target="_blank">
          <span class="glyphicon glyphicon-question-sign" style="color:#aaaaaa;padding-left:10px" title="you are seeing this as a member of __SciserverNotebookFirstDemo__. Not yet General Availability"></span>
        </a>
      </div>
      <div class="col-md-10">
        <recent-notebook-list ref="recentNotebookList" shownum="3" tight="true"></recent-notebook-list>
      </div>
    </div>
    <header v-show="showApplicationAppRow" class="hero-spacer">
      <h3>Your Activities</h3>
    </header>
    <div class="row row-eq-height text-center">
      <div v-if="isDisplayFilesTab" :class=tabclass>
        <router-link to="/files" title="Files tab" style=" text-decoration: none;">
          <div class="thumbnail dashboard">
            <i v-if="useIconsForActivities" class="fa fa-file fa-3x" aria-hidden="true"></i>
            <img v-else alt="Files" src="../../src/assets/sciserver_icon_files.png" style="width:50px; height:50px;"/>
            <h4>Files</h4>
            <p v-show="!showApplicationAppRow">Upload and access data volumes</p>
            <p>You have {{getSharedVolumes}} Shared User {{getSharedVolumes | pluralize('Volume')}}.</p>
            <p>You have {{getOwnedVolumes}} Owned User {{getOwnedVolumes | pluralize('Volume')}}.</p>
          </div>
        </router-link>
      </div>
      <div :class=tabclass>
        <router-link to="/groups" title="Groups tab" style=" text-decoration: none;">
          <div class="thumbnail dashboard">
            <i v-if="useIconsForActivities" class="fa fa-users fa-3x" aria-hidden="true"></i>
            <img v-else alt="Groups" src="../../src/assets/sciserver_icon_groups.png" style="width:50px; height:50px;"/>
            <h4>Groups</h4>
            <p v-show="!showApplicationAppRow">Share files and other resources within a collaboration</p>
            <p>You have {{invitedGroups}} Group {{invitedGroups | pluralize('Invitation')}}.</p>
            <p>You have {{ownerGroups}} Owned {{ownerGroups | pluralize('Group')}}.</p>
          </div>
        </router-link>
      </div>
      <div v-show="!showApplicationAppRow" :class=tabclass>
        <a :href="compute_URL"  title="Go to Compute" rel="noopener" target="_blank" style="text-decoration: none;">
          <div class="thumbnail dashboard">
            <i v-if="useIconsForActivities" class="fa fa-microchip fa-3x" aria-hidden="true"></i>
            <img v-else alt="Compute" src="../../src/assets/sciserver_icon_compute.png" style="width:50px; height:50px;"/>
            <h4>Compute</h4>
            <p>Analyze data with interactive Jupyter notebooks</p>
          </div>
        </a>
      </div>
      <div :class=tabclass>
        <a :href="computeJobs_URL"  title="Go to Compute Jobs" rel="noopener" target="_blank" style="text-decoration: none;">
          <div class="thumbnail dashboard">
            <i v-if="useIconsForActivities" class="fa fa-server fa-3x" aria-hidden="true"></i>
            <img v-else alt="Compute Jobs" src="../../src/assets/sciserver_icon_jobs.png" style="width:50px; height:50px;"/>
            <h4>Compute Jobs</h4>
            <p v-show="!showApplicationAppRow">Submit non-interactive jobs in Python, R and others</p>
            <p>You have {{ runningJobs }} {{runningJobs | pluralize('Job')}} Running.</p>
            <p>You have {{completedJobs}} {{completedJobs | pluralize('Job')}} Completed in 24 hours.</p>
          </div>
        </a>
      </div>
      <div v-if="getScienceDomainLoadStatus" :class=tabclass>
        <router-link to="/science" title="Science Domains" style="text-decoration: none;">
          <div class="thumbnail dashboard">
            <i class="fa fa-book fa-3x" aria-hidden="true"></i>
            <h4>Science Domains</h4>
            <p>You have joined {{getJoinedScienceDomains}} domains.</p>
            <p>There are {{getTotalScienceDomains}} domains available.</p>
          </div>
        </router-link>
      </div>
    </div>

    <!-- /.row -->
    <!-- Header -->
    <header v-if="showApplicationAppRow && appTiles.length > 0" class="hero-spacer">
      <h3>{{applicationName}} Apps</h3>
    </header>
    <!-- Page Features -->

    <div v-if="showApplicationAppRow" class="row row-eq-height text-center">
      <div v-for="app in appTiles" :class="appCSS">
        <a :href="app.serviceUrl" :title="`Go to ${app.name}`" style="text-decoration: none;" rel="noopener" target="_blank">
          <div class="thumbnail sciserver-apps">
            <div class="image">
              <img v-if="'iconUrl' in app" :alt="app.name" :src="app.iconUrl" style="width: 35%; height: 32%"/>
              <img v-else-if="'staticIcon' in app" :alt="app.name" :src="staticImages[app.staticIcon]" style="width: 35%; height: 32%"/>
              <span v-else :class="app.glyph || 'fa fa-asterisk fa-3x'"/>
            </div>
            <h4>{{ app.name }}</h4>
            <p>{{ app.description }}</p>
          </div>
        </a>
      </div>
    </div>
  </div>
</div>
</template>

<script>
import { mapState, mapGetters } from 'vuex';
import filter from 'lodash/filter';
import moment from 'moment';
import removeTokenFromUrl from '../remove-token-from-url';
import jobs from '../jobs';
import applicationConfiguration from '../applicationConfiguration';
import RecentNotebookList from './notebook//RecentNotebookList';

const $ = require('jquery');
const _ = require('lodash');

export default {
    name: 'SciserverTab',
    mixins: [applicationConfiguration],
    components: {
        RecentNotebookList,
    },
    computed: {
        appCSS() {
            let numRows = 2;
            const nApps = _.size(this.appTiles);
            if (nApps === 3) {
                numRows = 4;
            } else if (nApps === 6) {
                numRows = 2;
            } else if (nApps === 4) {
                numRows = 3;
            }
            return `col-md-${numRows} col-sm-12 col-xs-12 hero-feature`;
        },
        tabclass() {
            if (this.getScienceDomainLoadStatus || !this.showApplicationAppRow) {
                return 'col-md-3 col-sm-6 col-xs-12 hero-feature';
            }
            return 'col-md-4 col-sm-6 col-xs-12 hero-feature';
        },
        sortedCollaborationIds() {
            return this.myCollaborations
            /* Only show groups. */
                .filter(id => this.collaborations[id].type === 'GROUP');
        },
        totalGroups() {
            return this.getGroupIds.length;
        },
        invitedGroups() {
            return this.getGroupIds
                .filter(g => this.getStatusIn(g) === 'INVITED')
                .length;
        },
        adminGroups() {
            return this.getGroupIds
                .filter(g => this.getRoleIn(g) === 'ADMIN' && this.getStatusIn(g) === 'ACCEPTED')
                .length;
        },
        ownerGroups() {
            return this.getGroupIds
                .filter(g => this.getRoleIn(g) === 'OWNER')
                .length;
        },
        getSharedVolumes() {
            return []
                .concat(...this.myUserVolumeObjects)
                .filter(uv => uv.owner !== this.$store.state.userProfile.username).length;
        },
        getOwnedVolumes() {
            return []
                .concat(...this.myUserVolumeObjects)
                .filter(uv => uv.owner === this.$store.state.userProfile.username).length;
        },
        jobs() {
            return this.$store.state.jobs.jobs;
        },
        runningJobs() {
            return jobs.runningJobs(this.jobs);
        },
        completedJobs() {
            return jobs.completedJobs(this.jobs);
        },
        loggedIn() {
            return filter(this.$store.state.userActivities, l =>
                          l.task_name === 'LoginPortal.LogIn');
        },
        loggedInTime() {
            if (this.loggedIn) {
                $.each(this.loggedIn, (index0, text0) => {
                    if (this.time === '' && text0.time) {
                        this.time = text0.time;
                    } else if (this.time < text0.time && text0.time) {
                        this.time = text0.time;
                    }
                });
            }
            return moment(this.time).format('DD MMM YYYY hh:mm:ss a');
        },
        scienceGroups() {
            return this.$store.state.science.scienceDomains;
        },
        numScienceGroups() {
            return this.$store.state.science.scienceDomains.length;
        },
        isDisplayFilesTab() {
            return this.$store.state.files.hasFileService;
        },
        isNotebookFirstAlphaGroupMember() {
            return _.find(this.getGroupIds, g => _.get(this.getCollaborationById(g), 'name') === '__SciserverNotebookFirstDemo__') !== undefined;
        },
        ...mapState(['myCollaborations', 'token', 'collaborationLoadingStatus', 'userProfile', 'collaborations', 'users', 'userActivities']),
        ...mapGetters(['getCollaborationById', 'getGroupIds', 'getRoleIn', 'getStatusIn', 'myUserVolumeObjects', 'getScienceDomainLoadStatus', 'getTotalScienceDomains', 'getJoinedScienceDomains']),
    },
    data: () => ({
        totalUserVolumes: 0,
        time: '',
    }),
    updated() {
        this.assignCSS();
    },
    created() {
        this.getTotalGroup();
        this.getActivity();
        this.getJobs();
        this.getScienceGroup();
        this.assignCSS();
    },
    beforeRouteEnter(to, from, next) {
        next(removeTokenFromUrl);
    },
    methods: {
        getTotalGroup() {
            this.$store.dispatch('loadCollaborations');
        },
        getActivity() {
            this.$store.dispatch('loadUserActivities');
        },
        getJobs() {
            this.$store.dispatch('loadJobs');
        },
        getScienceGroup() {
            this.$store.dispatch('loadScienceDomains', this.userProfile);
        },
    },
};
</script>

<style scoped>
.row:before, .row:after {
    display: none !important;
}

span i:not(:first-child){
    margin-top:3px;
}
.hero-spacer {
    margin-top: 10px;
}
.hero-feature {
    margin-top: 10px;
}
.hero-setFreeSpace {
    margin-bottom: 20px;
}
@media (min-width: 768px) {
    #header {
        min-height: 100%;
    }
    #header .header-content {
        position: absolute;
        top: 50%;
        -webkit-transform: translateY(-50%);
        -ms-transform: translateY(-50%);
        transform: translateY(-50%);
        padding: 0 50px;
    }
    #header .header-content .header-content-inner {
        max-width: 1000px;
        margin-left: auto;
        margin-right: auto;
    }
    #header .header-content .header-content-inner h1 {
        font-size: 60px;
    }
    #header .header-content .header-content-inner p {
        font-size: 18px;
        max-width: 80%;
        margin-left: auto;
        margin-right: auto;
    }
}
.section-heading {
    margin-top: 0;
}
a {
    color: #333;
    text-decoration: none;
}
.row.row-eq-height{
    display: -webkit-box;
    display: -webkit-flex;
    display: -ms-flexbox;
    display: flex;
    flex-wrap: wrap;

}
.thumbnail {
    height: 100%;
}
/** https://befused.com/css/flexbox-prevent-image-shrinking **/
.image {
    flex-shrink: 0;
}
img {
    width: 100%;
}
.alert-primary {
    background-color: #e7ecf3;
    padding: 1em 1.25em;
    border-radius: 2px;
    color: #486491;
    position: relative;
    margin-top: 1em;
    text-align: center;
  }
</style>
