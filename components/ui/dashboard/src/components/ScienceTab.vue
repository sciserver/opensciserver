<template>
     <div id="scienceTab">
        <div class="container-fluid wrap">
        <div class = "row">
            <div class="col-sm-3">
              <list-group title="Science Domains" :headingType="`primary`">
                  <router-link  :to="`/science/${currentListOfGroups[collaborationId].groupId}`"
                  v-for="collaborationId in sortedCollaborationIds"
                  :key="collaborationId"
                  v-on:click.native="selectedCollaborationId = collaborationId"
                  class="list-group-item"
                  :class="{active: shownCollaborationId === collaborationId}"
                  type="button"
                  role="tab">
                  <span style="font-size: 2rem;">{{ currentListOfGroups[collaborationId].name }}</span>
                  <i id="checkbox" class="fa fa-check" v-if="!currentListOfGroups[collaborationId]._links.join" style="color: #5cb85c; font-size: 2em;" aria-hidden="true"></i>
                  </router-link>
            </list-group>
              <notifications position="top left" width="300" group="foo2" />
              <div class="alert alert-success" id="success-alert" v-if="alertStatus">
              {{ alertMessage }} <strong> {{ currentDomain }} </strong>
            </div>
           </div>
            <div class="col-sm-9">
             <div v-if="shownCollaborationObject" class="main-screen">
                    <div class="panel panel-primary">
                    <div class="panel panel-heading ">
                        {{ shownCollaborationObject.name }}
                    </div>
                    <div class="panel panel-body">
                        <div class="col-md-2">
                          <table>
                            <tr>
                              <td>
                                 <img v-if="shownCollaborationObjectLogo" :src=shownCollaborationObjectLogo
                                  class="img-responsive science-image"/>
                                  <img v-else src="../../src/assets/casjobs.png"
                                  class="img-responsive science-image"/>
                                <div style="padding-top: 10%; text-align: center;">
                                  <button type="button" v-if="joinStatus" class="btn btn-success" @click="joinPublicCollaboration"> Join </button>
                                  <button type="button"  v-else class="btn btn-danger" @click="leaveCollaboration" > Leave </button>
                                </div>
                              </td>
                            </tr>
                          </table>
                        </div>
                          <div class="col-md-10">
                          <div class="markdown-render">
                            <vue-markdown :source="markedDescription" :breaks=false :html=true></vue-markdown>
                          </div>
                        </div>
                  </div>
              </div>
               <div v-for="value in getTitles" v-if="value !== 'FileService.UserVolume' && value !== 'privilege' && value !== 'VolumeContainer'">
                 <div class="panel panel-primary">
                   <div class="panel panel-heading">
                     <span v-if = "value === 'Casjobs.DatabaseContext'" class="panel-title"> Casjobs Database </span>
                     <span v-if = "value === 'DockerImage'" class="panel-title"> Compute Image </span>
                     <span v-if = "value === 'FileService.DataVolume'" class="panel-title"> Data Set </span>
                     <span v-if = "value === 'DatabaseContext'" class="panel-title"> Database Context </span>
                   </div>
                     <div class="panel-body resource-container">
                       <div class="col-md-2" v-for="resourceObject in shownCollaborationObject.resources" v-if="value === resourceObject.resourceType && value !== 'privilege'">
                         <div style="text-align: center">
                              <i style='color:#666666' :class="getImage(value)" ></i>
                           <br>
                             <span> {{ resourceObject.name }} </span>
                         </div>
                         <br>
                       </div>
                     </div>
                  </div>
               </div>
             </div>
            </div>
        </div>
         </div>
        </div>
    </div>
</template>
<script>
import VueMarkdown from 'vue-markdown';
import { mapState } from 'vuex';
import listGroup from './sharedComponents/listGroup';

const apiCall = require('../apiCall').default;
const _ = require('lodash');

export default {
    components: {
        listGroup,
        VueMarkdown,
    },
    created() {
        this.$store.dispatch('loadScienceDomains', this.userProfile);
    },
    data: () => ({
        selectedCollaborationId: undefined,
        collabObjects: [],
        alertStatus: false,
        alertMessage: '',
        currentDomain: '',
        names: [],
        maps2: [],
        resourceTypes: [],
        resourceNames: [],
        map: [],
        status: 'Join',
        isJoin: '',
        shownCollaborationObjectLogo: '',
    }),
    computed: {
        getTitles() {
            return _.uniq(_.map(this.shownCollaborationObject.resources, x => x.resourceType));
        },
        markedDescription() {
            const currentId = this.$route.params.scienceId || this.sortedCollaborationIds[0];
            const description = this.currentListOfGroups[currentId].description;
            this.setMarkedInformation(currentId, description);
            return description;
        },
        scienceGroups() {
            return this.$store.state.science.scienceDomains;
        },
        sortedCollaborationIds() {
            return this.$store.state.science.scienceDomainsId;
        },
        currentListOfGroups() {
            if (this.$store.state.science.scienceDomains) {
                return this.$store.state.science.scienceDomains;
            }
            return undefined;
        },
        findCurrentGroupKey() {
            const key = Object.keys(this.currentListOfGroups);
            return key.find(k => this.currentListOfGroups[k].groupId.toString() === this.groupid);
        },
        groupid() {
            return this.$route.params.scienceId;
        },
        shownCollaborationId() {
            if (this.findCurrentGroupKey) {
                return this.findCurrentGroupKey;
            }
            return this.sortedCollaborationIds[0];
        },
        shownCollaborationObject() {
            return this.currentListOfGroups[this.shownCollaborationId];
        },
        joinStatus() {
            return this.currentListOfGroups[this.shownCollaborationId]._links.join;
        },
        selectedScienceDomainDescriptionObj() {
            let obj = '';
            try {
                obj = JSON.parse(this.shownCollaborationObject.description);
            } catch (e) {
                // console.log(e);
            }
            if (obj && typeof obj === 'object') {
                return obj;
            }
            obj = this.shownCollaborationObject.description;
            return obj;
        },
        selectedImageLogo() {
            return this.selectedScienceDomainDescriptionObj.logo;
        },
        ...mapState(['userProfile', 'token']),
    },
    methods: {
        setMarkedInformation(currentId, description) {
            const titleInfo = description.match(/\[TITLE\]: # \(([^)]*)\)/);
            if (titleInfo) {
                this.currentListOfGroups[currentId].name = titleInfo[1];
            }
            const logoInfo = description.match(/\[LOGO-URL\]: # \(([^)]*)\)/);
            if (logoInfo) {
                this.shownCollaborationObjectLogo = logoInfo[1];
            }
        },
        getImage(value) {
            if (value === 'Casjobs.DatabaseContext') {
                return 'fa fa-database fa-2x';
            } else if (value === 'DockerImage') {
                return 'fa fa-th fa-2x';
            } else if (value === 'VolumeContainer') {
                return 'fa fa-archive fa-2x';
            } else if (value === 'FileService.DataVolume') {
                return 'fa fa-archive fa-2x';
            } else if (value === 'DatabaseContext') {
                return 'fa fa-database fa-2x';
            }
            return 'fa fa-archive fa-2x';
        },
        leaveCollaboration() {
            if (confirm(`Would you like to leave '${this.shownCollaborationObject.name}'?`)) {
                apiCall(this.shownCollaborationObject._links.leave.href, this.token, 'POST', '',
                        () => {
                            this.$store.dispatch('loadScienceDomains', this.userProfile);
                        },
                        null);
            }
        },
        joinPublicCollaboration() {
            if (confirm(`Would you like to join '${this.shownCollaborationObject.name}'?`)) {
                apiCall(`${RACM_URL}ugm/rest/groups/join?groupId=${this.shownCollaborationObject.groupId}`,
                        this.token, 'POST', '',
                        () => {
                            this.$store.dispatch('loadScienceDomains', this.userProfile);
                        },
                        null);
            }
        },
    },
};
</script>

<style>
ol, ul {
  list-style: inside;
}
.main-screen{
    border: 2px;
    width: auto;
}
.science-title{
padding: 2px;
margin: 2px;
font-size: 2rem;
margin: 10px;
}
.science-description{
font-size: 1.6rem;
padding: 2px;
margin: 10px;
}
.secondary-action{
    font-size: 1.6rem;
    padding: 2px;
    margin: 5px;
}

/*Toggle switch css*/
.switch {
  position: relative;
  display: inline-block;
  width: 60px;
  height: 34px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  -webkit-transition: .4s;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 26px;
  width: 26px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  -webkit-transition: .4s;
  transition: .4s;
}

input:checked + .slider {
  background-color: #2196F3;
}

input:focus + .slider {
  box-shadow: 0 0 1px #2196F3;
}

input:checked + .slider:before {
  -webkit-transform: translateX(26px);
  -ms-transform: translateX(26px);
  transform: translateX(26px);
}

/* Rounded sliders */
.slider.round {
  border-radius: 34px;
}

.slider.round:before {
  border-radius: 50%;
}
#checkbox {
  text-shadow: 1px 1px 1px #ccc;
  font-size: 2em;
}

.panel panel-heading{
  color: #fff;
  background-color: #337ab7;
  border-color: #337ab7;
}
</style>
