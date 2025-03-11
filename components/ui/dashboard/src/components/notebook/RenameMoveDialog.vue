<template>
<modal v-model="open" class="my-sciserver-modal-dialog" title="rename and/or move file" size="lg" auto-focus>
  <a href="#" @click="destVolume = null; destPath = ''" title="select destination volume"><span class="glyphicon glyphicon-folder-close"></span></a>
  <a href="#" @click="destPath = ''" title="go to base path of volume">{{ volumePath }}</a>
  <span v-for="crumb in cookieTrail"><a href="#" @click="destPath = crumb.path" title="go to path">{{ crumb.name }}</a> / </span>
  <input type="text" class="form-control" style="display:inline;width:auto;min-width:300px" v-model="destFilename">
  <a href="#" @click="backDir" title="go back one path component"><span class="glyphicon glyphicon-menu-left"></span></a>
  <div class="dialog">
    <div v-if="!destVolume">
      Select user volume:
      <button v-for="vol in validVolumes.sort()" class="btn btn-default" @click="destVolume = vol">{{ vol.name }}</button>
    </div>
    <div v-if="destVolume">
      Subdirs:
      <div class="newdir">
        <input v-model="newDir" @submit="mkDir" class="form-control" style="width:120px">
        <button class="btn btn-primary" @click="mkDir" title="create directory at path"> + </button>
      </div>
      <button v-for="dir in directories.sort()" class="btn btn-default" @click="destPath = `${destPath}${dir}/`"> {{ dir }} </button>
    </div>
    <div v-if="destVolume" style="margin-top:6px">
      Files in path:
      <span v-for="file in files.sort()" style="font-style:italic; margin-right: 16px; color: #cccccc"> {{ file }} </span>
    </div>
  </div>
  <div v-if="!valid" class="alert alert-warning">
     {{ invalidity }}
   </div>
  <div slot="footer">
    <button type="button" class="btn btn-default" @click="open = false"> Cancel </button>
    <button type="button" class="btn btn-default" @click="startDialog"> Reset </button>
    <button v-if="valid && changed" type="button" class="btn btn-primary" @click="apply"> Apply</button>
  </div>
</modal>
</template>

<script>
import axios from 'axios';
import map from 'lodash/map';
import get from 'lodash/get';
import { mixin as focusMixin } from 'vue-focus';

export default {
    name: 'RenameMoveDialog',
    mixins: [focusMixin],
    props: [
        'currentVolume',
        'currentPath',
        'validVolumes',
        'autoExtension',
    ],
    data: () => ({
        open: false,
        focused: false,
        destFilename: '',
        destPath: '',
        destVolume: null,
        pathContents: null,
        invalidity: '',
        newDir: '',
    }),
    watch: {
        basePath() {
            this.pathContents = null;
            this.getDirs();
        },
    },
    computed: {
        volumePath() {
            if (this.destVolume) {
                console.log(this.destVolume);
                return this.destVolume.urlSegment;
            }
            return '';
        },
        basePath() {
            return `${this.volumePath}${this.destPath}`;
        },
        directories() {
            if (!this.pathContents) { return []; }
            return map(this.pathContents.root.folders, i => i.name);
        },
        files() {
            if (!this.pathContents) { return []; }
            return map(this.pathContents.root.files, i => i.name);
        },
        cookieTrail() {
            let x = '';
            return this.destPath.split('/').slice(0, -1).map((i) => { x = `${x}${i}/`; return { name: i, path: x }; });
        },
        valid() {
            this.invalidity = '';
            if (!this.changed) {
                return true;
            }
            if (this.destVolume == null) {
                this.invalidity = 'please select a destination volume';
            }
            if (this.destFilename.includes('/')) {
                this.invalidity += 'Filename cannot contain "/"! ';
            }
            if (this.files.includes(
                this.destFilename + this.autoExtension)) {
                this.invalidity += 'File already exists! ';
            }
            return this.invalidity === '';
        },
        changed() {
            if (this.destPath + this.destFilename === this.currentPath &&
                this.destVolume === this.currentVolume) {
                return false;
            }
            return true;
        },

    },
    methods: {
        startDialog() {
            this.focused = true;
            this.open = true;
            this.destFilename = this.currentPath.split('/').pop();
            this.destPath = this.currentPath.split('/').slice(0, -1).join('/');
            if (this.destPath !== '') {
                this.destPath += '/';
            }
            this.destVolume = this.currentVolume;
        },
        getDirs() {
            if (this.destVolume) {
                const url = `${this.destVolume.apiEndpoint}api/jsontree/${this.basePath}?level=2`;
                const conf = { headers: { 'X-Auth-Token': this.$store.state.token } };
                axios.get(url, conf).then((response) => { this.pathContents = response.data; });
            }
        },
        backDir() {
            if (this.destPath === '') {
                this.destVolume = null;
            }
            this.destPath = this.destPath.split('/').slice(0, -2).join('/');
            if (this.destPath !== '') {
                this.destPath = `${this.destPath}/`;
            }
        },
        mkDir() {
            if (this.newDir) {
                const url = `${this.destVolume.apiEndpoint}api/folder/${this.basePath}${this.newDir}`;
                const conf = { headers: { 'X-Auth-Token': this.$store.state.token } };
                axios.put(url, null, conf)
                    .then(() => {
                        this.destPath = `${this.destPath}${this.newDir}/`;
                        this.newDir = '';
                        this.getDirs();
                    })
                    .catch((error) => {
                        const msg = get(error, 'response.data.error') || 'Unknown error creating directory!';
                        this.$notify({ group: 'top_center_notify', type: 'error', text: msg });
                    });
            }
        },
        apply() {
            this.$emit('renamed', { volume: this.destVolume, path: this.destPath + this.destFilename });
            this.open = false;
            this.focused = false;
        },
    },
};
</script>
<style scoped>
div.dialog {
    height: 250px;
    overflow-y: auto;
    margin-top: 10px;
    overflow-x: hidden;
    padding-top: 12px;
    border-top: 1px solid #bbbbbb;
}
.dialog .btn {
    margin: 4px;
    margin-right: 12px;
}
.newdir {
    display: inline;
}
.newdir input {
    display: none;
}
.newdir:hover input {
    display: inline;
}
</style>
