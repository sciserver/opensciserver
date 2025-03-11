<template>
  <div>
    <span v-if="recentNotebooksProcessed.length > 0"> Recently opened notebooks:
    <span v-if="shownum">(<router-link title="view more recent notebooks" to="/notebook">see all</router-link>)</span></span>
    <div v-for="notebook in recentNotebooksProcessed" class="panel panel-default" style="text-align: left" v-bind:class="{'tight': tight}">
      <div class="panel-body">
        <span style="font-weight: bolder">
          <router-link title="open notebook" :to="notebook.nbLink">
            {{ notebook.name }}
          </router-link>
        </span>
        <span style="padding-left: 20px"> (in
          <router-link title="open containing path" :to="notebook.filesLink">
            <b>{{ notebook.volName }}</b>/{{ notebook.dir }}
          </router-link>
          )
        </span>
        <span style="padding-left: 20px"> {{ notebook.imageName }} </span>
        <span style="float: right">
          <span> last opened {{ notebook.timeIndicator }} </span> |
          <router-link title="open notebook" :to="notebook.nbLink">
            <span class="glyphicon glyphicon-edit"></span>
          </router-link>
          <router-link title="copy notebook" :to="`${notebook.nbLink}?copy=true`">
            <span class="glyphicon glyphicon-copy"></span>
          </router-link>
          <router-link title="new notebook same config" :to="`${notebook.nbLink}?copy=true&empty=true`">
            <span class="glyphicon glyphicon-export"></span>
          </router-link>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { mapGetters } from 'vuex';

const _ = require('lodash');

const RECENT_NOTEBOOKS_INDEX = '.sciserver/recent-notebooks.json';
const MAX_RECENT = 20;

export default {
    name: 'RecentNotebookList',
    props: [
        'disabled', // parent can toggle this to only update list when visible
        'shownum',
        'tight',
    ],
    data: () => ({
        recentNotebooks: [],
        refreshTimer: null,
    }),
    created() {
        this.refreshTimer = setInterval(() => { this.refreshList(); }, 60000);
        if (this.myPersistentVol) {
            this.refreshList();
        }
    },
    watch: {
        myPersistentVol() {
            this.refreshList();
        },
        disabled() {
            this.refreshList();
        },
    },
    computed: {
        indexUrl() {
            return `${this.myPersistentVol.apiEndpoint}api/file/${this.myPersistentVol.urlSegment}` +
                `/${RECENT_NOTEBOOKS_INDEX}?TaskName=Notebook.Index&quiet=true`;
        },
        recentNotebooksProcessed() {
            return _.filter(this.recentNotebooks, n => n.name).map(
                (n) => {
                    const path = n.pathIdentifiers;
                    const volume = this.volumeInfo(path);
                    const owner = _.get(volume, 'owner', '');
                    const ownerInfo = this.$store.state.userProfile.username === owner ? '' : `${owner}/`;
                    const volName = `${ownerInfo}${_.get(volume, 'name')}`;
                    const dir = _.split(path.path, '/').slice(0, -1).join('/');
                    const filesLink = `/files/${path.volType}/${path.volId}/${dir}`;
                    const nbLink = `/notebook/${path.volType}/${path.volId}/${path.path}`;
                    const imageName = _.get(n.imageInfo, 'name');
                    return {
                        name: n.name,
                        path,
                        imageInfo: n.imageInfo,
                        timeIndicator: this.formatTimeIndicator(n.time),
                        volume,
                        volName,
                        dir,
                        filesLink,
                        nbLink,
                        imageName,
                    };
                }).slice(0, this.shownum == null ? MAX_RECENT : this.shownum);
        },
        ...mapGetters(['myPersistentVol', 'myUserVolumeObjects', 'myDataVolumeObjects']),
    },
    methods: {
        refreshList() {
            if (!this.disabled) {
                const conf = { headers: { 'X-Auth-Token': this.$store.state.token } };
                axios.get(this.indexUrl, conf)
                    .then((response) => { this.recentNotebooks = _.reverse(response.data); })
                    .catch(() => {});
            }
        },
        addToList(id, path, name, imageInfo) {
            if (!name || !id) {
                return;
            }
            this.modifyList(id, path, name, imageInfo, null);
        },
        removeFromList(path) {
            this.modifyList(null, path, null, null, null);
        },
        modifyList(id, path, name, imageInfo) {
            const conf = { headers: { 'X-Auth-Token': this.$store.state.token } };
            const pathIdentifiers = _.pick(path, ['volType', 'volId', 'path']);
            const indexEntry = { id, name, pathIdentifiers, imageInfo, time: Date.now() };
            axios.get(this.indexUrl, conf)
                .then((response) => {
                    const index = Array.isArray(response.data) ? response.data : [];
                    // previous entries identifying same notebook are either the same ID, or same path
                    _.remove(index, { id });
                    _.remove(index, { pathIdentifiers: path });
                    if (name && id) {
                        index.push(indexEntry);
                    }
                    this.publishIndex(_.takeRight(index, MAX_RECENT));
                })
                .catch((error) => {
                    // probably don't have the file, though this returns 500 in such a case...
                    if (_.get(error, 'response.data.error', '').endsWith('File does not exist.') && name) {
                        const index = [indexEntry];
                        this.publishIndex(index);
                    }
                });
        },
        publishIndex(index) {
            const conf = { headers: { 'X-Auth-Token': this.$store.state.token,
                                      'Content-Type': 'application/octet-stream' } };
            axios.put(this.indexUrl, index, conf)
                .then(() => { this.recentNotebooks = _.reverse(index); });
        },
        volumeInfo(path) {
            return _.find(path.volType === 'uservolumes' ? this.myUserVolumeObjects : this.myDataVolumeObjects,
                          { id: parseInt(path.volId, 10) });
        },
        formatTimeIndicator(time) {
            if (Date.now() - time < 60 * 1000) {
                return 'a few seconds ago';
            } else if (Date.now() - time < 600 * 1000) {
                return 'a few minutes ago';
            } else if (Date.now() - time < 3600 * 1000) {
                return 'less than 1 hour ago';
            } else if (Date.now() - time < 86400 * 1000) {
                return 'less than 1 day ago';
            } else if (Date.now() - time < 7 * 86400 * 1000) {
                return 'a few days ago';
            } else if (Date.now() - time < 30 * 86400 * 1000) {
                return 'within the last month';
            }
            return 'more than a month ago';
        },
    },
    beforeDestroy() {
        clearInterval(this.refreshTimer);
    },
};
</script>

<style scoped>
a {
    color: black;
}
a:hover {
    color: inherit;
}
div.panel {
    margin-bottom:10px;
}
div.tight {
    margin-bottom:6px;
}
div.tight div {
    padding-top: 9px;
    padding-bottom: 9px;
}
</style>
