<template>
<div id="NotebookTab" ref="notebookApp">
  <div id="notebookSideBar">
    <div class="sidebaritem sidebarheader">
      <router-link to="/notebook"><span class="headertitle"> Notebooks </span></router-link>
      <span class="headerrel"> ALPHA </span>
      <a href="#" style="padding-left: 10px; color: silver">
        <span class="glyphicon glyphicon-question-sign" title="Limited availability. Sorry, no documentation yet, but please report issues!"></span>
      </a>
    </div>
    <div class="sidebaritem sidebartitle" v-bind:class="{ nbwarning: isTempFile}" id="nbname" @click="openMoveDialog" title="click to rename/move notebook">
      <span style='visibility:hidden'>X</span>
      <span v-if="isTempFile" class="glyphicon glyphicon-warning-sign" title="this looks like a temporary notebook (in scratch volume or has default name), consider renaming/moving!"></span>
      {{ notebookName }}
    </div>
    <div v-if="notebookContents">
      <div class="sidebaritem">
        <div class="btn-group btn-group-justified">
          <router-link :to="`${$route.path}?copy=true&empty=true`" target="_blank" style="color:#333333">
            <button class="btn btn-default" style="margin-right: 1px" title="create new notebook in new tab">
              new <span class="glyphicon glyphicon-open-file"></span>
            </button>
          </router-link>
          <router-link :to="`${$route.path}?copy=true`" target="_blank" style="color:black">
            <button class="btn btn-default" style="margin-right: 1px" title="copy this notebook and open in new tab">
              copy <span class="glyphicon glyphicon-duplicate"></span>
            </button>
          </router-link>
          <a href="#" style="color:black">
            <button class="btn btn-default" @click="getLink()" title="get a live shareable link. Note the user must have at least read access to the containing volume">
              link <span class="glyphicon glyphicon-share"></span>
            </button>
          </a>
        </div>
      </div>
      <div class="sidebaritem sidebarsection"> notebook information </div>
      <div class="sidebaritem">
        <div class="sidebaritemtitle">path</div> {{ notebookDir }}
      </div>
      <div class="sidebaritem">
        <div class="sidebaritemtitle">edit mode</div>
        <span v-if="readOnlyMode" style="color:red;font-weight:bold"> Read Only </span>
        <span v-if="!readOnlyMode" style="font-weight:bold"> Read Write </span>
        <span v-if="ready && !writable" style="color: #888888"> (no write-permission) </span>
        <span v-if="writable" @click="toggleReadMode" title="toggle read mode" style="float: right; margin-right: 8px; cursor: pointer">
          <span class="glyphicon" v-bind:class="[ readOnlyMode ? 'glyphicon-floppy-saved' : 'glyphicon-floppy-remove' ]"></span>
        </span>
      </div>
      <div class="sidebaritem sidebarsection"> compute environment </div>
      <div class="sidebaritem">
        <div class="sidebaritemtitle"> domain </div> {{ computeDomainName }}
      </div>
      <div class="sidebaritem">
        <div class="sidebaritemtitle" title="this is a test"> image name </div> {{ computeImageName }}
      </div>
      <div class="sidebaritem">
        <div class="sidebaritemtitle"> user volumes </div>
        <div v-for="uservol in computeUserVolumes">
          -
          <router-link :to="`/files/uservolumes/${uservol.id}`" target="_blank" title="browse user volume in new tab"> {{ uservol.name }} </router-link>
          <span style="padding-left: 20px"></span>
          <span class="glyphicon glyphicon-copy" aria-hidden="true" style="cursor: pointer; float: right; margin-right: 8px" :title="`copy ${uservol.name} path to clipboard`"
                @click="toClipboard(`/home/idies/workspace/${uservol.rootVolumeName}/${uservol.owner}/${uservol.name}`)"></span>
        </div>
      </div>
      <div class="sidebaritem">
        <div class="sidebaritemtitle"> data volumes </div>
        <div v-for="datavol in computeDataVolumes">
          -
          <router-link :to="`/files/datavolumes/${computeDVtoFSId(datavol)}`" target="_blank" title="browse data volume in new tab"> {{ datavol.name }} </router-link>
          <span style="padding-left: 20px"></span>
          <span class="glyphicon glyphicon-copy" aria-hidden="true" style="cursor: pointer; float: right; margin-right: 8px" :title="`copy ${datavol.name} path to clipboard`"
                @click="toClipboard(`/home/idies/workspace/${datavol.name}`)"></span>
        </div>
      </div>
      <div class="sidebaritem">
        <button @click="openImageChooser" class="btn btn-default"> modify compute environment <span class="glyphicon glyphicon-tasks"></span></button>
      </div>
      <div class="sidebaritem sidebarsection"></div>
    </div>
    <div v-if="ready" class="sidebaritem">
      <button class="btn btn-default" @click="showTerminal = showTerminal ? false: true">show terminal</button>
      <a v-if="isAdmin" :href="computeNotebookUrl" class="btn btn-default"> break out </a>
    </div>
  </div>
  <div id="displayWrapper">
    <div id="terminalFrameWrapper">
      <terminal ref="terminal" :computeId="computeId"></terminal>
    </div>
    <div id="notebookFrameWrapper" v-bind:class="{shortened: showTerminal, nbwarning: warning}">
      <iframe id="notebookFrame"  @load="notebookLoad" ref="notebookFrame" v-bind:src="computeNotebookUrl"></iframe>
    </div>
    <div v-if="warning" id="warningBox">{{ warning }}</div>
    <div v-if="!ready" id="notebookLoadingCurtain" class="full-overlay">
      <div id="loadingInfo">
        <img v-if="!ready && !error" src="../../src/assets/sciserver-notebook-loading.gif">
        <div v-if="loadMessage" style="padding-top: 20px"> {{ loadMessage }}</div>
        <span class="glyphicon glyphicon-warning-sign" style="font-size: 60px; margin: auto; color: #337ab7" v-if="error"></span>
        <div v-if="error" style="padding-top: 20px"> {{ error }} </div>
      </div>
    </div>
  </div>
  <recent-notebook-list v-show="false" ref="recentNotebookList" :disabled="true"></recent-notebook-list>
  <compute-domain-chooser ref="imageChooser" :currentImageInfo="imageInfo" @imageInfoModified="imageInfoModified"></compute-domain-chooser>
  <rename-move-dialog ref="moveDialog"
                      :currentVolume="sourceVolume" :currentPath="$route.params.path" :validVolumes="mountedVolumes" :autoExtension="'.ipynb'"
                      @renamed="moveNotebook"></rename-move-dialog>
</div>
</template>

<script>
import axios from 'axios';
import { mapGetters } from 'vuex';
import Cookies from 'js-cookie';
import arrayUnion from 'array-union';
import clipboard from 'clipboard-copy';
import notebookUtils from '../notebook-utils';
import titleMixin from '../titleMixin';
import ComputeDomainChooser from './notebook/ComputeDomainChooser';
import RenameMoveDialog from './notebook/RenameMoveDialog';
import Terminal from './notebook/Terminal';
import RecentNotebookList from './notebook/RecentNotebookList';

const _ = require('lodash');

const TITLE_PREFIX = 'SciServer Notebooks';
const NOTEBOOK_INIT_TIMEOUT = 30; // number of poll intervals (not seconds)
const NOTEBOOK_RELOAD_TRIES = 3;
const NOTEBOOK_POLL_INTERVAL = 3000; // milliseconds between poll actions
const BEACON_INTERVAL = 60000; // 1 minute
const INACTIVE_TIMEOUT = 3600000; // 1 hour
const RECENT_EDIT = INACTIVE_TIMEOUT;
const loadstate = {
    NONE: 0,
    LOADING: 1,
    LOADED: 2,
};

export default {
    name: 'NotebookTab',
    components: {
        ComputeDomainChooser,
        RenameMoveDialog,
        Terminal,
        RecentNotebookList,
    },
    mixins: [
        titleMixin,
    ],
    mounted() {
    },
    data: () => ({
        pageTitle: TITLE_PREFIX,
        connected: false,
        notebookState: loadstate.NONE,
        persistentVol: null,
        scratchVol: null,
        showTerminal: false,
        imageInfo: null,
        readOnlyMode: false,
        writable: true,
        lastEditTime: 0,
        pendingMetadataSave: false,
        notebookContents: null,
        computeId: null,
        jupyter: null,
        poller: null,
        notebookReloadTries: NOTEBOOK_RELOAD_TRIES,
        notebookInitTimeout: NOTEBOOK_INIT_TIMEOUT,
        error: null,
        warning: null,
        loadMessage: 'loading...',
        lastActivity: Date.now(),
        lastBeacon: Date.now(),
    }),
    created() {
        Cookies.set('computeCookie', this.$store.state.token, { secure: true });
        Cookies.set('computeToken', this.$store.state.token, { secure: true });
        this.$store.dispatch('loadUserComputeDomains');
    },
    computed: {
        user() {
            return this.$store.state.userProfile.username;
        },
        sourceVolume() {
            const volid = parseInt(this.$route.params.volId, 10);
            if (this.$route.params.volType === 'datavolumes') {
                return this.myDataVolumeObjects.filter(vol => vol.id === volid).pop();
            }
            return this.myUserVolumeObjects.filter(vol => vol.id === volid).pop();
        },
        notebookPath() {
            if (!this.$route.params.path) {
                this.$router.replace('/notebook');
                return null;
            }
            if (this.sourceVolume && !this.notebookContents) {
                let url = `${this.sourceVolume.apiEndpoint}api/file`;
                if (this.$route.params.volType === 'datavolumes') {
                    url = `${url}/${this.sourceVolume.name}`;
                } else {
                    url = `${url}/${this.sourceVolume.urlSegment}`;
                }
                if (this.$route.params.path === null) { console.warning('short circuited null notebook'); return null; }
                url = `${url}/${this.$route.params.path}.ipynb?TaskName=Dashboard.Notebook.ReadNotebook`;
                const conf = { headers: { 'X-Auth-Token': this.$store.state.token } };
                axios.get(url, conf)
                    .then((response) => {
                        this.notebookContents = response.data;
                    })
                    .catch(() => {
                        this.$refs.recentNotebookList.removeFromList(this.$route.params);
                        this.$router.replace('/notebook?nf=1');
                    });
            }
            if (this.$route.query.copy) {
                if (this.notebookContents) {
                    const nbname = `.sciserver/new-notebook-${Date.now()}`;
                    const url = `${this.myPersistentVol.apiEndpoint}api/file/${this.myScratchVol.urlSegment}${nbname}.ipynb?TaskName=Dashboard.Notebook.CopyNotebook`;
                    const conf = { headers: { 'X-Auth-Token': this.$store.state.token,
                                              'Content-Type': 'application/octet-stream' } };
                    if (this.$route.query.empty) {
                        this.notebookContents.cells = [];
                    }
                    _.set(this.notebookContents, 'metadata.sciserver.copySource', this.$route.params);
                    // unset notebook ID so we can assign a new one via shared logic
                    _.set(this.notebookContents, 'metadata.sciserver.notebookId', undefined);
                    _.set(this.notebookContents, 'metadata.sciserver.notebookId', this.notebookId);
                    axios.put(url, this.notebookContents, conf)
                        .then(() => { this.$router.replace(`/notebook/uservolumes/${this.myScratchVol.id}/${nbname}`); });
                }
            } else if (this.sourceVolume) {
                if (this.sourceVolume.type === 'datavolumes') {
                    return `${this.sourceVolume.name}/${this.$route.params.path}.ipynb`;
                }
                return `${this.sourceVolume.urlSegment}${this.$route.params.path}.ipynb`;
            }
            return null;
        },
        computeBootstrapped() {
            if (this.notebookContents && !this.imageInfo && this.notebookPath) {
                this.imageInfo = _.get(this.notebookContents, 'metadata.sciserver.imageInfo');
                if (!this.imageInfo) {
                    this.setError('Error: notebook has no image information, please modify compute environment');
                    setTimeout(this.$refs.imageChooser.startDialog, 750);
                    return false;
                }
            }
            if (this.imageInfo && this.computeDataVolumes) {
                this.pendingMetadataSave = true;
                // we have obtained all we need, and systems go. add to notbook index
                this.$refs.recentNotebookList.addToList(
                    this.notebookId, this.$route.params, this.notebookName, this.imageInfo,
                );
                const cachedContainer = _.get(this.imageInfo, `cachedContainer.${this.user}`);
                if (cachedContainer) {
                    // really need to check if container is still valid here.
                    this.computeId = cachedContainer;
                    return true;
                }
                this.loadMessage = 'creating your container...';
                const url = `${COMPUTE_URL}/api/domains/${this.imageInfo.domain}/containers`;
                const conf = { headers: {
                    'X-Auth-Token': this.$store.state.token,
                    'Content-Type': 'application/json',
                    'X-Description': 'NOTEBOOK' } };
                // persistent and scratch added each time. Flag to prevent adding if they were
                // removed by user?
                this.imageInfo.userVolumes = arrayUnion(
                    this.imageInfo.userVolumes,
                    [this.myPersistentVol.id, this.myScratchVol.id, this.sourceVolume.id],
                );
                const data = {
                    dockerImageName: this.imageInfo.name,
                    volumeContainers: this.computeDataVolumes,
                    userVolumes: this.imageInfo.userVolumes.map(i => ({ userVolumeId: i })) };
                axios.post(url, data, conf)
                    .then((response) => {
                        this.computeId = parseInt(response.data, 10);
                        _.set(this.imageInfo, `cachedContainer.${this.user}`, this.computeId);
                    });
                return true;
            }
            return false;
        },
        computeNotebookUrl() {
            if (this.computeBootstrapped && this.computeId) {
                this.resetLoadStatus();
                return `${COMPUTE_URL}/go?id=${this.computeId}&path=/notebooks/${this.notebookPath}`;
            }
            return null;
        },
        ready() {
            return this.notebookState === loadstate.LOADED &&
                !this.error;
        },
        notebookId() {
            return _.get(
                this.notebookContents, 'metadata.sciserver.notebookId', btoa(this.user + Date.now().toString()),
            );
        },
        notebookName() {
            if (this.notebookPath) {
                const name = this.notebookPath.split('/').pop().replace('.ipynb', '');
                this.pageTitle = `${TITLE_PREFIX} - ${name}`;
                return name;
            }
            return null;
        },
        isTempFile() {
            if (!this.notebookName) { return false; }
            if (this.sourceVolume === this.myScratchVol) { return true; }
            return this.notebookName.startsWith('new-notebook-');
        },
        notebookDir() {
            if (this.notebookPath) {
                return this.notebookPath.split('/').slice(0, -1).join('/');
            }
            return null;
        },
        computeDomain() {
            return this.imageInfo ? this.getUserComputeDomainById(this.imageInfo.domain) : null;
        },
        computeDomainName() {
            return this.computeDomain ? this.computeDomain.name : null;
        },
        computeImageName() {
            return this.imageInfo ? this.imageInfo.name : null;
        },
        computeUserVolumes() {
            if (this.computeDomain) {
                return this.computeDomain.userVolumes.filter(
                    i => this.imageInfo.userVolumes.includes(i.id));
            }
            return null;
        },
        computeDataVolumes() {
            if (this.computeDomain) {
                return this.computeDomain.volumes.filter(
                    i => this.imageInfo.dataVolumes.includes(i.publisherDID));
            }
            return null;
        },
        mountedVolumes() {
            if (this.imageInfo) {
                return this.myUserVolumeObjects.filter(
                    i => this.imageInfo.userVolumes.includes(i.id));
            }
            return [];
        },
        ...mapGetters([
            'isAdmin',
            'myUserVolumeObjects', 'myDataVolumeObjects', 'getUserVolumeById', 'getDataVolumeById',
            'myPersistentVol', 'myScratchVol', 'getUserComputeDomainById']),
    },
    methods: {
        setError(msg) {
            this.loadMessage = null;
            this.error = msg;
        },
        resetLoadTimers() {
            this.loadMessage = 'opening notebook session...';
            this.error = null;
            this.notebookInitTimeout = NOTEBOOK_INIT_TIMEOUT;
            this.notebookReloadTries = NOTEBOOK_RELOAD_TRIES;
        },
        resetLoadStatus() {
            this.resetLoadTimers();
            this.notebookState = loadstate.NONE;
            clearInterval(this.poller);
            this.poller = setInterval(() => this.polling(), NOTEBOOK_POLL_INTERVAL);
        },
        polling() {
            if (this.notebookState !== loadstate.LOADED && this.notebookInitTimeout > 0) {
                this.notebookInitTimeout -= 1;
                if (this.notebookInitTimeout < 25) {
                    this.loadMessage = 'still working on loading your notebook...';
                }
                if (this.notebookInitTimeout < 15) {
                    this.loadMessage = '...hmmm, this is taking a bit long, but still trying...';
                }
            } else if (this.notebookState !== loadstate.LOADED) {
                this.notebookState = loadstate.NONE;
                this.setError('We\'re sorry, there seems to have been a problem loading your notebook');
            }
            if (this.notebookState === loadstate.LOADING &&
                this.$refs.notebookFrame.contentDocument.readyState === 'complete') {
                try {
                    // a hack, until we can get state by API
                    const errdd = this.$refs.notebookFrame.contentDocument.getElementsByTagName('dd');
                    if (errdd && errdd.length === 3 && errdd[1].innerText === 'Container not found') {
                        _.set(this.imageInfo, `cachedContainer.${this.user}`, null);
                        this.computeId = null;
                        return;
                    }
                    notebookUtils.jupyterClassicVisualModifications(this.$refs.notebookFrame);
                    this.jupyter = this.$refs.notebookFrame.contentWindow.Jupyter;
                    if (!this.jupyter.notebook._fully_loaded) {
                        console.log('Notebook frame loaded, but notebook not fully loaded!');
                        throw new Error('Notebook not fully loaded');
                    }
                    console.log('Notebook is fully loaded and ready!');
                    // The underlying writablilty of the file on filesystem. This is presumably
                    // immutable, so we keep separate from the readOnlyMode setting (and gate by it)
                    this.writable = this.jupyter.notebook.writable;
                    console.log('writable flag from jupyter: ', this.jupyter.notebook.writable);
                    if (this.writable) {
                        // If recently edited by another user, automatically start in RO
                        const lastEditUser = _.get(this.notebookContents, 'metadata.sciserver.lastEdit.user');
                        const lastEditTime = _.get(this.notebookContents, 'metadata.sciserver.lastEdit.time');
                        if (lastEditUser !== this.user && Date.now() - lastEditTime < RECENT_EDIT) {
                            this.readOnlyMode = true;
                            this.$notify({
                                group: 'top_center_notify',
                                type: 'warn',
                                text: `Notebook was edited by user <b>${lastEditUser}</b> within the last hour. Opening readonly - toggle if you are sure you want to write.`,
                                duration: 8000,
                            });
                        }
                    } else {
                        this.$notify({
                            group: 'top_center_notify',
                            type: 'warn',
                            text: 'No write permission on notebook, opening read-only. Not edits will be saved!',
                            duration: 5000,
                        });
                        this.readOnlyMode = true;
                    }
                    this.notebookState = loadstate.LOADED;
                    this.resetLoadTimers();
                    this.addActivityTimers();
                    this.$refs.notebookFrame.contentWindow.onbeforeunload = () => { this.notebookState = loadstate.NONE; };
                } catch (err) {
                    console.log('notebook loading error: ', err);
                    if (this.notebookReloadTries > 0) {
                        this.notebookReloadTries -= 1;
                        this.$refs.notebookFrame.src = this.computeNotebookUrl;
                    }
                }
            }
            // large notebooks may complete the html/js load before
            // they are mutable. Wait for loaded condition to write
            // updated image info.
            if (this.pendingMetadataSave && this.notebookState === loadstate.LOADED) {
                console.log('notebook fully loaded:', this.jupyter.notebook._fully_loaded);
                if (this.jupyter.notebook._fully_loaded) {
                    _.set(this.jupyter.notebook, 'metadata.sciserver.imageInfo', this.imageInfo);
                    _.set(this.jupyter.notebook, 'metadata.sciserver.notebookId', this.notebookId);
                    this.jupyter.notebook.dirty = !this.readOnlyMode;
                    this.pendingMetadataSave = false;
                }
            }
            // Main long poll. Keep container going by pinging compute
            // if activity detected and timeout session when inactive
            // for configured amount of time.
            //
            // The ping should automatically cause a redirect to login
            // when token expires, but should also be checking for
            // connectivity and put warning up when disconnected
            const beaconTime = Date.now() - this.lastBeacon;
            const inactiveTime = Date.now() - this.lastActivity;
            if (this.notebookState === loadstate.LOADED && beaconTime > BEACON_INTERVAL &&
                inactiveTime < BEACON_INTERVAL) {
                axios.post(`${COMPUTE_URL}/api/container/${this.computeId}/ping`);
                this.lastBeacon = Date.now();
            }
            if (inactiveTime > INACTIVE_TIMEOUT) {
                clearInterval(this.poller);
                this.setError('session paused due to inactivity, press any key to continue');
                this.$refs.notebookApp.addEventListener('keydown', this.reload);
                this.$refs.notebookApp.addEventListener('mouseup', this.reload);
            } else if (inactiveTime > INACTIVE_TIMEOUT * 0.85) {
                this.warning = 'are you there? Your session will timeout due to inactivity soon';
            } else {
                this.warning = null;
            }
            // save the notebook if it gets dirty (meaning edited and not reflected on disk) and
            // hasn't been saved recently
            if (this.notebookState === loadstate.LOADED) {
                this.jupyter.notebook.writable = !this.readOnlyMode;
                if (this.jupyter.notebook.dirty) {
                    if (Date.now() - this.lastEditTime > 15000) {
                        this.lastEditTime = Date.now();
                        _.set(this.jupyter.notebook.metadata, 'sciserver.lastEdit', { user: this.user, time: this.lastEditTime });
                        this.jupyter.notebook.save_notebook();
                    }
                }
            }
        },
        reload() {
            this.$router.go();
        },
        notebookLoad() {
            this.resetActivity();
            this.notebookState = loadstate.LOADING;
        },
        resetActivity() {
            this.lastActivity = Date.now();
        },
        addActivityTimers() {
            this.$refs.notebookApp.addEventListener('mousemove', this.resetActivity);
            this.$refs.notebookApp.addEventListener('keydown', this.resetActivity);
            this.$refs.notebookFrame.contentWindow.addEventListener('mousemove', this.resetActivity);
            this.$refs.notebookFrame.contentWindow.addEventListener('keydown', this.resetActivity);
        },
        toClipboard(data) {
            clipboard(data);
            this.$notify({ group: 'top_center_notify', text: 'copied to clipboard', duration: 1000 });
            return false;
        },
        toggleReadMode() {
            if (this.readOnlyMode) {
                this.readOnlyMode = !confirm('Any changes made during read-only mode will be written out. Are you sure?');
            } else {
                this.readOnlyMode = true;
            }
        },
        getLink() {
            this.toClipboard(window.location.origin + window.location.pathname);
            return false;
        },
        computeDVtoFSId(datavol) {
            return _.get(_.find(this.myDataVolumeObjects, { displayName: datavol.name }), 'id');
        },
        openImageChooser() {
            this.$refs.imageChooser.startDialog();
        },
        imageInfoModified(newInfo) {
            this.error = null;
            const cachedContainer = newInfo.cachedContainer;
            newInfo.cachedContainer = _.get(this.imageInfo, 'cachedContainer');
            _.set(this.imageInfo, `cachedContainer.${this.user}`, cachedContainer);
            this.imageInfo = newInfo;
        },
        openMoveDialog() {
            if (this.notebookState !== loadstate.LOADED) {
                this.$notify({ group: 'top_center_notify',
                               text: 'Please wait until fully loaded to rename or move',
                               type: 'warning',
                               duration: 1000 });
            } else {
                this.$refs.moveDialog.startDialog();
            }
        },
        moveNotebook(location) {
            const fsPath = `${location.volume.urlSegment}${location.path}.ipynb`;
            // this is not pretty, but jupyter only allows relative renames within notebook
            const currentNParts = this.notebookPath.split('/').slice(0, -1).length;
            const renamePath = `${'../'.repeat(currentNParts)}${fsPath}`;
            const url = `/notebook/uservolumes/${location.volume.id}/${location.path}`;
            this.notebookState = loadstate.NONE;
            this.loadMessage = 'renaming notebook, please hold tight...';
            this.jupyter.notebook.save_notebook()
                .then(() => {
                    this.jupyter.notebook.rename(renamePath)
                        .then(() => {
                            this.jupyter.notebook.notebook_path = fsPath;
                            this.jupyter.notebook.session.rename_notebook(fsPath);
                            this.$router.replace(url);
                        })
                        .catch(() => {
                            this.$notify({ group: 'top_center_notify',
                                           type: 'error',
                                           text: 'error renaming notebook!' });
                            this.notebookLoad();
                        });
                })
                .catch(() => {
                    this.$notify({ group: 'top_center_notify',
                                   type: 'error',
                                   text: 'could not save notebook prior to move!' });
                    this.notebookLoad();
                });
        },
    },
};
</script>

<style>
#notebookSideBar {
    position: absolute;
    left: 0px;
    width: 280px;
    bottom: 0px;
    top: 0px;
    border-right: 4px solid var(--navColor);
    background-color: white;
    color: black;
    overflow-y: hidden;
}
#notebookSideBar:hover {
    overflow-y: auto;
}
#displayWrapper {
    position: absolute;
    left: 280px;
    right: 0px;
    top: 0px;
    bottom: 1px; /* non-zero due to some scrollbar oddity with notebook */
}
#warningBox {
    position: absolute;
    top: 0px;
    height: 32px;
    right: 0px;
    left: 0px;
    color: white;
    padding-top: 6px;
    text-align: center;
}
#notebookFrameWrapper {
    position: absolute;
    left: 0px;
    right: 0px;
    bottom: 0px;
    top: 0px;
    border: 0px;
}
#notebookFrameWrapper.shortened {
    bottom: 50%;
}
#notebookFrameWrapper.nbwarning {
    top: 32px;
}
#notebookFrame {
    display: block;
    position: absolute;
    left: 0px;
    width: 100%;
    height: 100%;
    border: 0px;
}
#terminalFrameWrapper {
    position: absolute;
    left: 0px;
    right: 0px;
    bottom: 0px;
    height: 50%;
    border: 0px;
}
.full-overlay {
    position: absolute;
    left: 0px;
    right: 0px;
    bottom: 0px;
    top: 0px;
    border: 0px;
    background-color: #f5faff;
    overflow-y: auto;
}
#loadingInfo {
    display: block;
    margin-left: auto;
    margin-right: auto;
    margin-top: 10%;
    margin-bottom:  auto;
    text-align: center
}
.sidebaritem {
    display: block;
    width: 100%;
    padding: 10px;
    background-color: #ffffff;
    color: black;
    white-space: nowrap;
    text-overflow: ellipsis;
    overflow: hidden;
}
.sidebaritem:hover {
    background-color: #eeeeee;
}
.sidebarheader {
    background-color: var(--navColor);
    padding: 2px;
    padding-top: 0px;
    padding-left: 16px;
    font-weight: bolder;
}
.sidebarheader:hover {
    background-color: var(--navColor);
}
.sidebarheader .headertitle {
    font-size:1.21em;
    color:white;
    font-style: italic;
}
.sidebarheader .headerrel {
    color:#5cb85c;
}
.sidebartitle {
    background-color: #337ab7;
    color: white;
    font-weight: bold;
    cursor: pointer;
}
.sidebartitle:hover {
    background-color: #1667ad;
}
.sidebarsection {
    border-top: 1px solid #5f626d;
    color: #5f626d;
    font-size: small;
    font-variant-caps: all-small-caps;
    padding-top: 2px;
    padding-bottom: 2px;
    text-align: center;
}
.sidebaritemtitle {
    width: 100%;
    color: #5f626d;
    font-size: x-small;
    font-style: italic;
    text-align: left;
    margin-left: -6px;
    margin-top: -6px;
}
main {
 position: relative;
}
.nbwarning {
    background-color: #d6a435;
    color: black;
}
</style>
