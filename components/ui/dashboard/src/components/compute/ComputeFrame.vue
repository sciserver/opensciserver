<template>
  <div ref="computeApp" class="container">
    <iframe ref="computeFrame" class="expanded" @load="frameLoadCallback" v-bind:src="computeContainerUrl"></iframe>
    <div v-if="!loaded" class="loading expanded curtain">
      <div v-if="imageErrors.length > 0">
        <span class="glyphicon glyphicon-warning-sign" style="font-size: 60px; margin: auto; color: #337ab7; padding-bottom: 14px"></span>
        <div v-for="err in imageErrors">
          {{ err }}
        </div>
      </div>
      <div v-else>
        <span class="fa fa-spinner fa-spin" style="font-size: 70px"></span>
        <p style="padding-top: 15px; font-style: italic"> {{ loadMessage }} </p>
      </div>
    </div>
    <div v-if="needsprobe" class="transparent expanded curtain"></div>
    <div v-if="inactive" class="cloaked expanded curtain">
      <div style='background-color:#ffffffde; padding: 60px; border-radius: 20px; font-weight: bold; max-width: 40%; margin: auto'>
        <p style='margin-bottom: 16px'> Your session has been paused due to inactivity </p>
        <span class="btn btn-primary" @click="reload()"> reload </span>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import Cookies from 'js-cookie';
import { mapGetters } from 'vuex';

const _ = require('lodash');

export default {
    name: 'ComputeFrame',
    props: [
        'imageInfo',
        'customLoader',
        'noReuse',
        'path',
        'sessionTimeout',
    ],
    created() {
        Cookies.set('computeCookie', this.$store.state.token, { secure: true });
        Cookies.set('computeToken', this.$store.state.token, { secure: true });
        this.timeout = this.sessionTimeout || this.timeout;
    },
    data: () => ({
        loaded: false,
        loadMessage: 'loading...',
        containerId: null,
        imageErrors: [],
        lastActive: 0,
        inactive: false,
        needsprobe: false,
        timeout: 3600000,
        timer: null,
    }),
    watch: {
        imageInfo() {
            this.loaded = false;
            this.containerId = null;
        },
        resolvedImageInfo() {
            if (this.resolvedImageInfo) {
                if (this.noReuse) {
                    this.startContainer();
                } else {
                    this.loadMessage = 'getting sessions...';
                    axios.get(`${COMPUTE_URL}/api/containers`)
                        .then((response) => {
                            this.findOrStartContainer(response.data);
                        })
                        .catch((error) => {
                            console.error(`error getting list of running containers ${error}`);
                            this.startContainer();
                        });
                }
            }
        },
        loaded() {
            if (this.loaded) {
                this.$refs.computeApp.addEventListener('mousemove', this.resetActivity);
                this.$refs.computeApp.addEventListener('keydown', this.resetActivity);
                this.$refs.computeFrame.contentWindow.addEventListener('mousemove', this.resetActivity);
                this.$refs.computeFrame.contentWindow.addEventListener('keydown', this.resetActivity);
                this.lastActive = Date.now();
                setTimeout(this.pingCompute, 60000);
            }
        },
    },
    computed: {
        resolvedImageInfo() {
            if (!this.$store.state.compute.userComputeDomainsLoaded) {
                return null;
            }
            if (!(this.myPersistentVol && this.myScratchVol)) {
                console.log('no persistent or scratch vol');
                return null;
            }
            this.imageErrors = [];
            const domain = this.getUserComputeDomainById(this.imageInfo.domain);
            if (!domain) {
                this.imageErrors.push(`no such domain ${this.imageInfo.domain}`);
                return false;
            }
            const defaultUv = [this.myPersistentVol.id, this.myScratchVol.id];
            const domainUv = domain.userVolumes.map(v => v.id);
            const domainDv = domain.volumes.map(v => parseInt(v.publisherDID, 10));
            const missingUv = _.difference(this.imageInfo.userVolumes, domainUv);
            const missingDv = _.difference(this.imageInfo.dataVolumes, domainDv);
            const domainImages = domain.images.map(v => v.name);
            if (missingUv.length !== 0) {
                this.imageErrors.push(`no access to uservolumes: ${JSON.stringify(missingUv)}`);
            }
            if (missingDv.length !== 0) {
                this.imageErrors.push(`no access to datavolumes: ${JSON.stringify(missingDv)}`);
            }
            if (!domainImages.includes(this.imageInfo.name)) {
                this.imageErrors.push(`no access to image ${this.imageInfo.name}`);
            }
            if (this.imageErrors.length !== 0) {
                return false;
            }
            return {
                domain: this.imageInfo.domain,
                dockerImageName: this.imageInfo.name,
                // data volumes (volumeContainers) are done in this way to preserve the writability flag, which comes
                // from the racm domain data
                volumeContainers: _.intersectionBy(
                    domain.volumes,
                    _.map(this.imageInfo.dataVolumes, i => ({ publisherDID: String(i) })),
                    'publisherDID'),
                userVolumes: _.union(
                    defaultUv,
                    _.get(this.imageInfo, 'userVolumes', [])).map(i => ({ userVolumeId: i })),
            };
        },
        computeContainerUrl() {
            if (this.containerId) {
                const path = this.path ? `&path=${this.path}` : '';
                return `${COMPUTE_URL}/go?id=${this.containerId}&${path}`;
            }
            return '';
        },
        ...mapGetters([
            'myUserVolumeObjects', 'myDataVolumeObjects', 'getUserVolumeById', 'getDataVolumeById',
            'myPersistentVol', 'myScratchVol', 'getUserComputeDomainById',
        ]),
    },
    methods: {
        findOrStartContainer(containerList) {
            const matched = containerList.filter(
                c => (_.get(c, 'domainId') === this.resolvedImageInfo.domain &&
                      _.get(c, 'json.dockerImageName') === this.resolvedImageInfo.dockerImageName &&
                      _.difference(
                          this.resolvedImageInfo.userVolumes.map(i => i.userVolumeId),
                          _.get(c, 'json.userVolumes', []).map(i => i.userVolumeId)).length === 0 &&
                      _.difference(
                          this.resolvedImageInfo.volumeContainers.map(i => i.publisherDID),
                          _.get(c, 'json.volumeContainers', []).map(i => i.publisherDID)).length === 0),
            );
            console.log('found matching containers:', 'imageInfo:', this.resolvedImageInfo, 'matched:', matched);
            if (matched.length === 0) {
                this.startContainer();
            } else {
                this.loadMessage = 'loading application...';
                this.containerId = _.sample(matched).id;
            }
        },
        startContainer() {
            this.loadMessage = 'starting compute session...';
            const createUrl = `${COMPUTE_URL}/api/domains/${this.resolvedImageInfo.domain}/containers`;
            const conf = { headers: {
                'Content-Type': 'application/json', 'X-Description': 'NOTEBOOK', 'X-auth-token': this.$store.state.token,
            } };
            axios.post(createUrl, this.resolvedImageInfo, conf)
                .then((response) => {
                    this.loadMessage = 'starting application...';
                    this.containerId = parseInt(response.data, 10);
                })
                .catch((error) => {
                    this.imageErrors.push(`error creating container ${error.data}`);
                });
        },
        frameLoadCallback() {
            if (!this.containerId) {
                return;
            }
            this.loadMessage = 'loading application...';
            const framedoc = this.$refs.computeFrame.contentDocument;
            // a bit hacky, but a number of things could mean we won't immediately get sent to the final url, and thus
            // we need to retry. Most likely compute has not completed container setup. We try to identify various
            // conditions here
            const preloadCond = framedoc.title === '503 Service Temporarily Unavailable' ||
                  framedoc.title === 'SciServer User Dashboard' ||
                  (_.includes(_.get(framedoc.getElementsByTagName('dd'), 1, '').innerText, '503 Service Unavailable')
                   && framedoc.title === 'Error');
            if (!preloadCond) {
                if (this.customLoader) {
                    this.customLoader(this);
                } else {
                    this.loaded = true;
                }
            } else {
                console.log('compute container not ready or error loading, retrying...');
                setTimeout(this.retryContainerUrl, 2000);
            }
        },
        retryContainerUrl() {
            console.log('retrying loading container url');
            this.$refs.computeFrame.src = this.computeContainerUrl;
        },
        resetActivity() {
            this.needsprobe = false;
            this.lastActive = Date.now();
        },
        pingCompute() {
            if (this._isDestroyed) {
                return;
            }
            const idle = Date.now() - this.lastActive;
            console.log(`polling activity, idle time is now ${idle}`);
            if (idle < 70000) {
                console.log('pinging compute container');
                axios.post(`${COMPUTE_URL}/api/container/${this.containerId}/ping`)
                    .catch(error => console.error(`failed to ping compute: ${error}`));
            } else if (idle < this.timeout) {
                console.log('no recent activity, marking as needing probe');
                this.needsprobe = true;
            } else if (idle >= this.timeout) {
                // stop the cycle, no recovery from here
                this.inactive = true;
                return;
            }
            this.timer = setTimeout(this.pingCompute, 60000);
        },
        reload() {
            this.$router.go();
        },
        beforeDestroy() {
            console.log('component being destroyed, clearing timeout');
            clearTimeout(this.timer);
        },
    },
};
</script>

<style scoped>
.container {
    position: relative;
    width: 100%;
    height: 100%;
}
.expanded {
    position: absolute;
    top: 0px;
    left: 0px;
    width: 100%;
    height: 100%;
    border: 0px;
}
.expanded iframe {
    overflow: hidden;
}
.curtain {
    background-color: #f5faff;
    text-align: center;
    padding-top: 12vh;
}
.cloaked {
    background-color:#000000bb;
}
.transparent {
    background-color:#00000000;
}
.loading {
    padding-top: 30vh;
}
</style>
