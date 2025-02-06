<template>
<div id = "activityTab">
<div class="container-fluid wrap">
 <div class="row" v-if="sciserverMessage">
     <div class = "col-md-3">
     <div class="panel panel-primary">
      <div class="panel-heading panel-heading-style">
         <span class = "panel-title">Filter</span>
      </div>
       <div class="panel-body activityBody">
         <form  class="form-group">
           <div class="form-group">
          <label for="dateStart">Start date</label>
    <input name="dateStart" id="dateStart" v-model="from"/>
    </div>
    <div class="form-group">
    <label for="dateEnd">End date</label>
    <input name="dateEnd" v-model="to" id="dateEnd"/>
    </div>
    <div class="form-group">
    <label>Activities</label>
     <input v-model="numberOfSciserverMessage" type="number" placeholder="100" min="1" oninput="validity.valid||(value='');"/>
     </div>
    <form @submit.prevent>
      <div class="form-group">
        <label>SciServer Applications</label>
          <select v-model="checkedApplication" id="applicationMenu" class="form-control" multiple>
            <option v-for="item in sortedApps" :key="item.app" :value="item.app">
              <label>{{item.name}}</label>
            </option>
        </select>
      </div>
    </form>
          </form> 
        <button class="btn btn-primary" id="submitJobStartEnd" @click="loadMessages">Filter</button>
         <button class="btn btn-default" @click ="clearAll">Clear All</button>
        </div>
  </div>
     </div>
<div class = "col-md-9">
   <div class="panel panel-primary">
      <div class="panel-heading">
         <span class = "panel-title">Activity Log</span>
         <a rel="noopener" class= "contextualHints" target="_blank" :href="activityLog" title="More info on Activity Log" style="text-decoration: none;"> <i aria-hidden="true" class="fa fa-question-circle"></i>
						<span class="sr-only">More info on Activity Log</span>
					  </a>
          <button class="btn btn-primary" title="Refresh" @click="refreshActivity"><i class="fa fa-refresh" :class="refreshActivitiesSpinner" aria-hidden="true"></i>
          </button>
      </div>
<div class="activityBody">
  <div style="overflow-x:auto;">
        <table class="table-bordered sciserver-datatable" id="sciserverTable">
        <thead>
          <tr>
          <th class="tableHeader">Applications</th>
          <th class="tableHeader">Activities</th>
          <th class="tableHeader">Time</th> 
          </tr>
        </thead>
        <tbody>
          <i v-show="loading" class="fa fa-spinner fa-spin fa-5x" aria-hidden="true"></i>
        </tbody>
        </table>
        </div>
</div>
</div>
   </div>
 </div>
 <empty-state :label="`Your Activities`" :description="`No activities to show.`" :action="false" v-if="!sciserverMessage"/>
<pop-up-dialog ref="alertPopUp"/>
 </div>
 </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex';
import jqueryCalendar from './sharedComponents/JqueryCalendar';
import popUpDialog from './sharedComponents/alertDialog';
import EmptyState from './sharedComponents/emptyState';
import applicationConfiguration from '../applicationConfiguration';

const $ = require('jquery');
const moment = require('moment');
const _ = require('lodash');

export default {
  name: 'ActivityLog',
  mixins: [applicationConfiguration],
  components: {
    jqueryCalendar,
    popUpDialog,
    EmptyState,
  },
  data: () => ({
    files_URL: '/files',
    activityLog: ACTIVITY_LOG,
    numberOfSciserverMessage: '',
    loading: false,
    sciserverMessage: [],
    checkedApplication: [],
    from: '',
    to: '',
    applications: [{ app: 'skyserver', name: 'SkyServer' }, { app: 'casjobs', name: 'CasJobs' }, { app: 'jobm', name: 'Jobs' },
    { app: 'fileservice', name: 'FileService' }, { app: 'scidrive', name: 'SciDrive' }, { app: 'skyquery', name: 'SkyQuery' },
    { app: 'compute', name: 'Compute' }, { app: 'authentication', name: 'LoginPortal' }, { app: 'User Profile', name: 'User Profile' },
    { app: 'Groups', name: 'Groups' }, { app: 'Resource', name: 'Resource' }] }),
  computed: {
    refreshActivitiesSpinner() {
      return {
        'fa-spin': this.loading === true,
      };
    },
    constructSciServerMessagesUrl() {
      let newUrl = `${LOGGING_URL}/api/messages/applications?type=${this.checkedApplications}&doShowInUserHistory=true`;
      const pageLength = 100;
      if (this.from) {
        const startDate = new Date(this.from);
        const startMilliseconds = startDate.getTime();
        newUrl = `${newUrl}&start=${startMilliseconds}`;
      }
      if (this.to) {
        const endDate = new Date(this.to);
        const endMilliseconds = endDate.getTime();
        newUrl = `${newUrl}&end=${endMilliseconds}`;
      }
      if (this.numberOfSciserverMessage) {
        newUrl = `${newUrl}&top=${this.numberOfSciserverMessage}`;
      } else {
        newUrl = `${newUrl}&top=${pageLength}`;
      }
      return newUrl;
    },
    checkedApplications() {
      let apps = '';
      if (this.transformedApps.length > 0) {
        apps = this.transformedApps.toString();
      } else {
        apps = 'skyserver,casjobs,jobm,fileservice,scidrive,skyquery,compute,authentication,racm';
      }
      return apps;
    },
    transformedApps() {
      const apps = [];
      $.each(this.checkedApplication, (index0, text0) => {
        if (text0 === 'User Profile' || text0 === 'Groups' || text0 === 'Resource') {
          if (!apps.includes('racm')) {
            apps.push('racm');
          }
        } else {
          apps.push(text0);
        }
      });
      return apps;
    },
    sortApps() {
      const sorted = Object.keys(this.applications)
      .sort().reduce((acc, key) => {
        acc[key] = this.applications[key];
        return acc;
      }, {});
      return sorted;
    },
    sortedApps() {
      return _.sortBy(this.applications, ['name']);
    },
    ...mapState(['token', 'publicGroups']),
    ...mapGetters(['myUserVolumeObjects', 'getGroupById']),
  },
  created() {
    this.loadMessages();
  },
  mounted() {
    $('input[name=dateStart]').datetimepicker({
      onChangeDateTime: (dp, $input) => {
        this.from = $input.val();
      },
    });
    $('input[name=dateEnd]').datetimepicker({
      onChangeDateTime: (dp, $input) => {
        const startDate = document.getElementById('dateStart').value;
        const endDate = document.getElementById('dateEnd').value;
        if ((Date.parse(startDate) >= Date.parse(endDate))) {
          this.$refs.alertPopUp.showAlert('End date should be greater than Start date', 'Error message');
          document.getElementById('dateEnd').value = '';
        }
        this.to = $input.val();
      },
    });
  },
  methods: {
    loadMessages() {
      const that = this;
      that.loading = true;
      this.$http.get(this.constructSciServerMessagesUrl).then((response) => {
        that.sciserverMessage = [];
        $.each(response.data, (index0, text0) => {
          const val = $.parseJSON(text0.content);
          let app = text0.application_type;
          let initialApp = text0.application_type;
          let message = '';
          let content = '';
          let verb = '';
          let predicate = '';
          let subject = '';
          let activity = '';
          let racm = '';
          if (val.Action != null) {
            message = val.Action;
          } else if (val.action != null) {
            message = val.action;
          }
          if (val && val.sentence && val.sentence.verb) {
            verb = val.sentence.verb;
          }
          if (val && val.sentence && val.sentence.subject) {
            subject = val.sentence.subject;
          }
          if (val && val.sentence && val.sentence.predicate) {
            predicate = val.sentence.predicate;
          }
          if (val && val.EntryPoint && app === 'skyserver') {
            message = val.EntryPoint.substring(24, val.EntryPoint.length);
          }
          if (val && val.SqlCmd && app === 'skyserver') {
            content = val.SqlCmd;
          }
          if (val && val.group && val.sentence && val.sentence.verb !== 'shared') {
            racm = 'Groups';
            initialApp = 'Groups';
          } else if (val && val.sentence && val.sentence.predicate === 'user profile') {
            racm = 'User Profile';
            initialApp = 'User Profile';
          } else if (val && val.sentence && val.resource) {
            racm = 'Resource';
            initialApp = 'Resource';
          } else if (val && val.group && val.sentence && val.sentence.verb === 'shared') {
            racm = 'Groups';
            initialApp = 'Groups';
            if (val.entityId && val.resourceType === 'USERVOLUME') {
              const uvName = that.findUserVolName(val.entityId);
              const groupName = that.findGroupName(val.group);
              if (uvName || groupName) {
                predicate = `uservolume '${uvName}' with group '${groupName}'`;
                message = `${subject} shared uservolume '${uvName}' with group '${groupName}'`;
              }
            }
          }
          if (app === 'skyserver') {
            app = `<a href=${this.skyserver_URL}  title="Go to SkyServer" rel="noopener" 
            target="_blank" role="button" class="btn btn-default"><b>SkyServer</b></a>`;
          } else if (app === 'casjobs') {
            app = `<a href=${this.casJobs_URL} title="Go to CasJobs" rel="noopener" 
            target="_blank" role="button" class="btn btn-default"><b>CasJobs</b></a>`;
          } else if (app === 'authentication') {
            app = '<b>LoginPortal</b>';
          } else if (app === 'racm') {
            app = `<b>${racm}</b>`;
          } else if (app === 'jobm') {
            app = `<a href=${this.computeJobs_URL} title="Go to Compute Jobs" rel="noopener"
            target="_blank" role="button" class="btn btn-default"><b>Jobs</b></a>`;
          } else if (app === 'fileservice') {
            app = '<b>FileService</b>';
          } else if (app === 'scidrive') {
            app = `<a href=${this.scidrive_URL} title="Go to SciDrive" rel="noopener"
            target="_blank" role="button" class="btn btn-default"><b>SciDrive</b></a>`;
          } else if (app === 'skyquery') {
            app = `<a href=${this.skyquery_URL} title="Go to SkyQuery" rel="noopener"
            target="_blank" role="button" class="btn btn-default"><b>SkyQuery</b></a>`;
          } else if (app === 'compute') {
            app = `<a href=${this.compute_URL} title="Go to Compute" rel="noopener"
            target="_blank" role="button" class="btn btn-default"><b>Compute</b></a>`;
          } else {
            app = text0.application_type;
          }
          if (verb !== '' && predicate !== '') {
            activity = `<span title="${message}"><b>${verb}</b> ${predicate}</span>`;
          } else if (content !== '') {
            activity = `<span title="${content}"> <b>${message}</b> : ${content}</span>`;
          } else {
            activity = `<span title="${message}">${message}</span>`;
          }
          if (that.checkedApplication.length < 1) {
            that.sciserverMessage.push({ 0: app, 1: activity, 2: moment(text0.time_string).format('DD MMM YYYY hh:mm:ss a') });
          } else if (that.checkedApplication.includes(racm)) {
            that.sciserverMessage.push({ 0: app, 1: activity, 2: moment(text0.time_string).format('DD MMM YYYY hh:mm:ss a') });
          } else if (that.transformedApps.includes(initialApp)) {
            that.sciserverMessage.push({ 0: app, 1: activity, 2: moment(text0.time_string).format('DD MMM YYYY hh:mm:ss a') });
          }
        });
        that.loading = false;
        if ($.fn.dataTable.isDataTable('#sciserverTable')) { // trying to refresh the table
          const table = $('#sciserverTable').DataTable();
          table.clear();
          table.rows.add(that.sciserverMessage).draw(false);
        } else { // instantiate it for the first time:
          $('#sciserverTable').DataTable(
            {
              data: that.sciserverMessage,
              processing: true,
              bFilter: true, // text search
              stateSave: false,
              select: false,
              paging: true,
              responsive: true,
              columnDefs: [
                { className: 'wrapword', targets: [1] },
                { className: 'changeColumnWidth', targets: [2] },
                { type: 'date', targets: [2] },
              ],
              order: [[2, 'desc']],
              language: {
                loadingRecords: 'Loading...',
                processing: 'Processing...',
                sLengthMenu: 'Rows _MENU_  per page',
              },
            },
          );
        }
      });
    },
    refreshActivity() {
      this.clearAll();
      this.loadMessages();
    },
    clearAll() {
      this.from = '';
      this.to = '';
      this.numberOfSciserverMessage = '';
      $('#applicationMenu').val('');
      this.checkedApplication = [];
    },
    findUserVolName(uvID) {
      let uv = [];
      let uvName = '';
      const that = this;
      if (that.myUserVolumeObjects) {
        uv = that.myUserVolumeObjects.find(m => m.id === uvID);
        if (uv && uv.name) {
          uvName = uv.name;
        }
      }
      return uvName;
    },
    findGroupName(grID) {
      let group = [];
      let groupName = '';
      if (this.publicGroups) {
        group = this.publicGroups.map(this.getGroupById).find(g => g.id === grID);
        if (group && group.groupName) {
          groupName = group.groupName;
        }
      }
      return groupName;
    },
  },
};
</script>
<style>
.filterPanel {
  border: 1px solid #E9EBEE;
  box-sizing: border-box;
  background-color: #E9EBEE;
}
.activityBody {
  border: 1px solid #E9EBEE;
  box-sizing: border-box;
  background-color: #E9EBEE;
}
.tableHeader{
  font: 15px/15px Helvetica;
  font-weight: 550;
}
.wrapword {
    white-space: -moz-pre-wrap !important;  /* Mozilla, since 1999 */
    white-space: -pre-wrap;      /* Opera 4-6 */
    white-space: -o-pre-wrap;    /* Opera 7 */
    white-space: pre-wrap;       /* css-3 */
    word-wrap: break-word;       /* Internet Explorer 5.5+ */
    white-space: -webkit-pre-wrap; /* Newer versions of Chrome/Safari*/
    word-break: break-all;
    white-space: normal;
}
.job-descript-cell {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  /* CSS magic
  * I think https://stackoverflow.com/a/26293398 describes what's going on
  */
  max-width: 0;
  width: 50%;
}
.changeColumnWidth {
  width: 15%;
}
</style>
