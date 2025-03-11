<template>
<div id ="Editor">
  <div id="editWrapper">
    <div id="editorControls">
      <div class="editorControl" id="filename" style="background-color: #337ab7; color:white; height:24px; text-align:middle; padding-left:8px; padding-top:2px;">
        <span style="margin:auto; font-weight:bold">{{ filename }}</span>
        <span style="float:right; cursor:pointer; margin-right: 10px; margin-top: 2px" @click="close" class="glyphicon glyphicon-remove"></span>
      </div>
      <div v-show="loaded" style="background-color: #ffffff; color: #000000; height:24px; padding: 2px">
        <button v-if="editMode" class="btn btn-secondary btn-xs" @click="setViewMode"> view </button>
        <button v-else class="btn btn-primary btn-xs"> viewing </button>
        <span v-if="writeable">
          <button v-if="editMode" class="btn btn-primary btn-xs"> editing </button>
          <button v-else class="btn btn-secondary btn-xs"  @click="setEditMode"> edit </button>
          <span v-if="hasRenderFunction">
            <button v-if="dualView" class="btn btn-primary btn-xs" @click="setDualMode"> dual </button>
            <button v-else class="btn btn-secondary btn-xs"  @click="setDualMode"> dual </button>
          </span>
        </span>
        <span v-if="editMode">
          <button v-if="autoSave" class="btn btn-primary btn-xs" @click="toggleAutoSave"> autosave </button>
          <button v-else class="btn btn-secondary btn-xs" @click="toggleAutoSave"> autosave </button>
          <button v-if="inSave || !dirty" class="btn btn-secondary btn-xs"> save </button>
          <button v-else class="btn btn-primary btn-xs" @click="save"> save </button>
          <span style="color:#666666">
            <span v-if="inSave"><em> saving... </em></span>
            <span v-else> last save: {{ lastSaveTimeStamp }} </span>
          </span>
        </span>
      </div>
    </div>
    <div v-if="shouldInline" class="content" style="top:24px">
      <iframe class="contentFrame" :src="`${filepath}?inline=true`"></iframe>
    </div>
    <div v-else-if="finalError" class="content contentMessage">
      There was an unrecoverable error in loading or viewing this file: <br>
      {{ finalError }} <br>
      <button class="btn btn-success" @click="close"> Close </button>
    </div>
    <div v-else-if="promptLoad" class="content contentMessage">
      <span v-if="largeFile"> You are trying to load a large file, you may experience long load times and difficulty in editing. <br></span>
      <span v-if="unkownType">  There is no built in rendering for this file type and it does not appear to be text.  <br></span>
      Would you still like to load? <br>
      <button class="btn btn-success" @click="acceptedLoad = true"> OK </button>
      <button class="btn btn-success" @click="downloadAndClose"> Download </button>
      <button class="btn btn-default" @click="close"> Close </button>
    </div>
    <div v-else-if="validatedPath" class="content" style="display:flex" @mousemove="doresize" @mouseup="stopresize" @keydown.ctrl.83.prevent="save">
      <div v-if="loading && !loaded" class="content contentMessage">
        Loading... <i class="fa fa-spinner fa-spin fa-2x"></i>
      </div>
      <codemirror v-if="showEditor" v-model="code" :options="cmOptions" @ready="cmReady" @input="contentUpdate" style="top:8px;flex:1;min-width:50%" ref="editor"></codemirror>
      <div v-show="showRender && showEditor" style="flex:1; min-width:5px; max-width:5px; margin: 3px; background-color:silver;cursor:col-resize" @mousedown="startresize"></div>
      <div v-show="showRender" class="markdown-render" style="padding:10px;flex:1;overflow:auto;position:relative" ref="renderbox" id="renderbox" :status="rendering"></div>
    </div>
  </div>
</div>
</template>

<script>
import { codemirror } from 'vue-codemirror';
import { mapGetters } from 'vuex';
import axios from 'axios';
import 'codemirror/lib/codemirror.css';
import 'codemirror/mode/python/python';
import 'codemirror/mode/markdown/markdown';
import 'codemirror/mode/clike/clike';
import 'codemirror/mode/shell/shell';
import 'codemirror/mode/javascript/javascript';
import 'codemirror/mode/stex/stex';
import 'codemirror/mode/sql/sql';
import 'codemirror/mode/yaml/yaml';
import 'codemirror/mode/xml/xml';
import 'codemirror/mode/r/r';
import 'codemirror/mode/go/go';
import viewEditUtils from '../view-edit-utils';
import fileUtils from '../files-utils';

const _ = require('lodash');

const AUTO_SAVE_INTERVAL = 15000;
const AUTO_SAVE_CHARS = 40;
const WARN_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

export default {
    name: 'Editor',
    components: {
        codemirror,
    },
    mounted() {
    },
    data: () => ({
        filepath: null,
        filename: null,
        fileVolume: null,
        initialFileSize: null,
        fileContentType: null,
        acceptedLoad: false,
        code: '',
        cmOptions: {
            tabSize: 4,
            mode: 'text/plain',
            lineNumbers: true,
            line: true,
            readOnly: true,
            viewportMargin: 10,
            lineWrapping: true,
        },
        autoSave: false,
        lastSaveSize: 0,
        lastSaveTime: Date.now(),
        lastEditTime: Date.now(),
        inSave: false,
        updateCounter: 0,
        saveCounter: 0,
        loading: false,
        loaded: false,
        finalError: null,
        mode: 'view',
    }),
    created() {
        this.filename = this.$route.query.f.split('/').pop();
        if (this.filename === '') {
            this.finalError = 'no filename given!';
        }
        this.filepath = encodeURI(this.$route.query.f);
        window.addEventListener('beforeunload', this.unloadPrompt);
    },
    watch: {
        mode() {
            this.cmOptions.readOnly = this.mode === 'view';
        },
    },
    computed: {
        validatedPath() {
            // there seems to be no way with the current implementation of these store objects
            // to know if we have no objects or if they are not yet loaded...
            if (this.myDataVolumeObjects.length < 1 && this.myUserVolumeObjects.length < 1) {
                return null;
            }
            if (!this.fileVolume) {
                let volume = this.myUserVolumeObjects.filter(vol => this.filepath.includes(
                    encodeURI(`${vol.apiEndpoint}api/file/${vol.rootVolumeName}/${vol.owner}/${vol.name}/`)));
                if (volume.length === 0) {
                    volume = this.myDataVolumeObjects.filter(vol => this.filepath.includes(
                        encodeURI(`${vol.apiEndpoint}api/file/${vol.name}/`)));
                }
                if (volume.length === 0) {
                    this.finalError = 'unrecognized volume, cannot view file!';
                    return false;
                }
                this.fileVolume = volume[0];
                console.log('found volume for file', this.fileVolume);
            }
            if (this.initialFileSize === null) {
                this.initialFileSize = -1;
                axios.head(`${this.filepath}`,
                           { headers: { 'X-Auth-Token': this.$store.state.token } })
                    .then((response) => {
                        this.initialFileSize = _.get(response.headers, 'content-length', 0);
                        this.fileContentType = _.get(response.headers, 'content-type', '');
                    })
                    .catch(() => {
                        console.error('could not detect filesize, continuing anyway');
                        this.initialFileSize = 0;
                        this.fileContentType = '';
                    });
                return null;
            } else if (this.initialFileSize < 0 || this.promptLoad) {
                return null;
            }
            this.cmOptions.mode = viewEditUtils.filenameToModeMimeType(this.filename);
            if (!this.cmOptions.mode) {
                if (_.startsWith(this.fileContentType, 'text/') || this.acceptedLoad) {
                    this.cmOptions.mode = 'text/plain';
                } else {
                    return null;
                }
            }
            if (this.loading) {
                return true;
            }
            this.loading = true;
            axios.get(`${this.filepath}?TaskName=Dashboard.viewfile`,
                      { headers: { 'X-Auth-Token': this.$store.state.token } })
                .then((response) => {
                    this.code = typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
                    this.lastSaveSize = this.code.length;
                    this.loaded = true;
                }).catch((error) => {
                    if (error.response.data.error.includes('File does not exist')) {
                        this.code = '';
                        this.loaded = true;
                        this.cmOptions.readOnly = false;
                    } else {
                        this.finalError = error.response.data.error;
                    }
                });
            setInterval(() => this.periodicSave(), AUTO_SAVE_INTERVAL);
            return true;
        },
        unkownType() {
            if (this.fileContentType === null) {
                return false;
            }
            return !this.fileContentType.startsWith('text/') &&
                viewEditUtils.filenameToModeMimeType(this.filename) === null;
        },
        largeFile() {
            return this.initialFileSize > WARN_FILE_SIZE;
        },
        promptLoad() {
            return (this.largeFile || this.unkownType) && !this.acceptedLoad;
        },
        writeable() {
            return !_.endsWith(this.filename, '.ipynb') && _.get(this.fileVolume, 'allowedActions', []).includes('write');
        },
        editMode() {
            return this.mode !== 'view';
        },
        dualView() {
            return this.mode === 'dual';
        },
        shouldInline() {
            return viewEditUtils.shouldInline(this.filename);
        },
        showEditor() {
            return this.validatedPath && this.loaded && (this.editMode || !this.hasRenderFunction);
        },
        showRender() {
            if (!this.hasRenderFunction) {
                return false;
            }
            return this.dualView || !this.showEditor;
        },
        hasRenderFunction() {
            return viewEditUtils.getRenderFunction(this.filename);
        },
        rendering() {
            if (!(this.loaded && this.$refs.renderbox)) {
                return null;
            }
            if (!this.showRender) {
                return null;
            }
            const renderfunc = viewEditUtils.getRenderFunction(this.filename);
            if (typeof renderfunc !== 'function') {
                return null;
            }
            return renderfunc(this.$refs.renderbox, this.code);
        },
        lastSaveTimeStamp() {
            return new Date(this.lastSaveTime).toString();
        },
        dirty() {
            return this.updateCounter > this.saveCounter;
        },
        ...mapGetters(['myUserVolumeObjects', 'myDataVolumeObjects']),
    },
    methods: {
        startresize() {
            this.inresize = true;
            this.lastresize = Date.now();
            return false;
        },
        doresize(e) {
            if (this.inresize && Date.now() - this.lastresize > 100) {
                this.$refs.editor.$el.style.minWidth = `${e.clientX}px`;
                this.$refs.editor.$el.style.maxWidth = `${e.clientX}px`;
                return false;
            }
            return false;
        },
        stopresize() {
            this.inresize = false;
            return false;
        },
        cmReady(cm) {
            cm.doc.clearHistory();
            cm.refresh();
        },
        contentUpdate() {
            // probably a better method is to get the last edit time of file, or at least size
            if (Date.now() - this.lastEditTime > 3600000) {
                if (!confirm('last edit made more than 1 hour ago, continue?')) {
                    this.$router.go();
                }
            }
            this.updateCounter += 1;
            this.lastEditTime = Date.now();
            if (Math.abs(this.code.length - this.lastSaveSize) > AUTO_SAVE_CHARS && this.autoSave) {
                this.save();
            }
        },
        periodicSave() {
            if (this.updateCounter > this.saveCounter && this.autoSave) {
                this.save();
            }
        },
        save() {
            if (this.cmOptions.readOnly || this.inSave || !this.loaded || !this.dirty) {
                return;
            }
            const _updateCounter = this.updateCounter;
            const _codeLength = this.code.length;
            if (Date.now() - this.lastSaveTime > 3600000) {
                if (!confirm('last save more than 1 hour ago, continue?')) {
                    return;
                }
            }
            this.inSave = true;
            axios.put(`${this.filepath}?quiet=true&TaskName=Dashboard.editfile`, this.code,
                      { headers: { 'X-Auth-Token': this.$store.state.token,
                                   'Content-Type': 'application/octet-stream' } })
                .then(() => {
                    console.log('saved...');
                    this.saveCounter = _updateCounter;
                    this.lastSaveSize = _codeLength;
                    this.lastSaveTime = Date.now();
                })
                .catch((error) => {
                    if (error.response.data.error.includes('permissions')) {
                        this.cmOptions.readOnly = true;
                    }
                    this.$notify({
                        group: 'top_center_notify',
                        type: 'error',
                        text: 'Error saving file!',
                    });
                })
                .finally(() => {
                    this.inSave = false;
                });
        },
        setViewMode() {
            this.mode = 'view';
        },
        setEditMode() {
            this.mode = 'edit';
        },
        setDualMode() {
            this.mode = this.mode === 'dual' ? 'edit' : 'dual';
        },
        toggleReadOnly() {
            this.cmOptions.readOnly = !this.cmOptions.readOnly;
        },
        toggleAutoSave() {
            this.autoSave = !this.autoSave;
        },
        downloadAndClose() {
            fileUtils.downloadFile(this.filepath, this.filename);
            // give a small pause to allow the file download to start in this context before moving on
            setTimeout(this.close, 750);
        },
        unloadPrompt(e) {
            if (this.dirty) {
                e.preventDefault();
                e.returnValue = '';
            } else {
                delete e.returnValue;
            }
        },
        close() {
            // Temporary solution. When this component is configured for inclusion in other layouts, this should
            // probably emit an event, which can be handled by the outer component
            this.$router.push('/files');
        },
    },
    beforeRouteLeave(to, from, next) {
        if (this.dirty && !window.confirm('There are unsaved changes. Do you still wish to exit?')) {
            next(false);
        } else {
            next();
        }
    },
    beforeDestroy() {
        window.removeEventListener('beforeunload', this.unloadPrompt);
    },
};
</script>

<style>
#editWrapper {
    position: absolute;
    left: 0px;
    right: 0px;
    top: 0px;
    bottom: 0px;
    background: #ffffff;
}
.content {
    position: absolute;
    left: 0px;
    right: 0px;
    top: 52px;
    bottom: 0px;
}
.innerContent {
    position: absolute;
    left: 0px;
    right: 0px;
    top: 0px;
    bottom: 0px;
}
.contentFrame {
    position: absolute;
    left: 0px;
    top: 0px;
    width:100%;
    height:100%;
    display:block;
}
.contentMessage {
    text-align: center;
    padding: 15% 20px 20px;
    color: #666666;
}
.CodeMirror, .vue-codemirror {
    height: 100%!important;
}
#renderbox iframe {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
}
main {
    position: relative;
}
</style>
