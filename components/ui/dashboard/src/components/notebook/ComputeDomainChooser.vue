<template>
<modal v-model="open" class="my-sciserver-modal-dialog" title="compute environment settings" size="lg" auto-focus>
  <div class="row">
    <div class="col-lg-5">
      <b>Domain:</b>
      <select v-model="domain" class="form-control" @change="resetForm()">
        <option v-for="dom in validDomains" :value="dom" :title="dom.description"> {{ dom.name }} </option>
      </select>
      <p> {{ domainDescription }} </p>
      <b>Image:</b>
      <select v-model="image" class="form-control">
        <option v-for="img in domainImages" :value="img"> {{ img.name }} </option>
      </select>
      <p> {{ imageDescription }} </p>
    </div>
    <div class="col-lg-7" style="max-height: 300px; overflow-y: auto">
      <b>User Volumes:</b>
      <label v-for="uservol in domainUserVolumes" style="margin: 4px">
        <input v-model="userVols" type="checkbox" class="test-input" :value="uservol" style="display: none">
        <span class="btn btn-default"
              :title="`user volume in ${uservol.rootVolumeName} owned by ${uservol.owner}. ${uservol.description}`">
          {{ uservol.name }}
        </span>
      </label>
      <hr/>
      <b>Data Volumes:</b>
      <label v-for="datavol in domainDataVolumes" style="margin: 4px">
        <input v-model="dataVols" type="checkbox" class="test-input" :value="datavol" style="display: none">
        <span class="btn btn-default"
              :title="`${datavol.description}`">
          {{ datavol.name }}
        </span>
      </label>
    </div>
  </div>
  <div v-if="isAdmin" class="alert alert-info">
    Or, use pre-existing container (admin only - use sparingly! Note
    that the container metadata and real container spec may differ if
    used). ID: <input v-model="containerId" type="input">
  </div>
  <div v-if="imageInfoModified" class="alert alert-warning">
    <span class="glyphicon glyphicon-exclamation-sign"></span>
    Modifying image info requires switching to a new container in a
    new process - cells will have to be rerun. Please ensure any data
    written out has been saved to a mounted user volume or data volume
    (including <b>Temporary</b>. Only data stored directly on the container
    file system will be lost)
  </div>
  <div slot="footer">
    <button type="button" class="btn btn-default" @click="open = false"> Cancel </button>
    <button v-if="isApplyable" type="button" class="btn btn-primary" @click="applyImageInfo"> Apply</button>
  </div>
</modal>
</template>

<script>
import { mapGetters } from 'vuex';
import { mixin as focusMixin } from 'vue-focus';

const _ = require('lodash');

export default {
    name: 'ComputeDomainChooser',
    mixins: [focusMixin],
    props: [
        'currentImageInfo',
    ],
    data: () => ({
        open: false,
        focused: false,
        domains: null,
        domain: null,
        image: null,
        userVols: [],
        dataVols: [],
        containerId: null,
        change: false,
    }),
    computed: {
        validDomains() {
            return this.domains ?
                _.sortBy(this.domains.filter(i => i.images.length > 0), i => i.name) : null;
        },
        domainImages() {
            return this.domain ? _.sortBy(this.domain.images, i => i.name) : [];
        },
        domainUserVolumes() {
            return this.domain ? _.sortBy(this.domain.userVolumes, i => i.name) : null;
        },
        domainDataVolumes() {
            return this.domain ? _.sortBy(this.domain.volumes, i => i.name) : null;
        },
        domainDescription() {
            return this.domain ? this.domain.description : null;
        },
        imageDescription() {
            return this.image ? this.image.description : null;
        },
        newImageInfo() {
            return {
                domain: parseInt(_.get(this.domain, 'publisherDID', ''), 10),
                name: _.get(this.image, 'name'),
                userVolumes: this.userVols.map(i => i.id),
                dataVolumes: this.dataVols.map(i => i.publisherDID),
                cachedContainer: this.containerId ? parseInt(this.containerId, 10) : null,
            };
        },
        imageInfoModified() {
            if (!this.currentImageInfo) {
                return true;
            }
            if (this.newImageInfo.domain !== _.get(this.currentImageInfo, 'domain')) {
                return true;
            }
            if (this.newImageInfo.name !== _.get(this.currentImageInfo, 'name')) {
                return true;
            }
            if (_.xor(_.get(this.newImageInfo, 'userVolumes', []),
                      _.get(this.currentImageInfo, 'userVolumes', [])).length !== 0) {
                return true;
            }
            if (_.xor(_.get(this.newImageInfo, 'dataVolumes', []),
                      _.get(this.currentImageInfo, 'dataVolumes', [])).length !== 0) {
                return true;
            }
            if (this.containerId &&
                this.containerId !== _.get(this.currentImageInfo, 'cachedContainer', '').toString()) {
                return true;
            }
            console.log(this.newImageInfo);
            return false;
        },
        isApplyable() {
            if (!this.imageInfoModified) {
                return false;
            }
            return this.newImageInfo.name && this.newImageInfo.domain;
        },
        ...mapGetters(['isAdmin']),
    },
    methods: {
        startDialog() {
            this.focused = true;
            this.open = true;
            this.domains = this.$store.state.compute.userComputeDomains;
            if (this.domains && this.currentImageInfo) {
                this.domain = this.domains.find(
                    i => i.publisherDID === _.get(this.currentImageInfo, 'domain', '').toString());
                this.resetForm();
            }
        },
        resetForm() {
            if (this.domain) {
                this.image = this.domain.images.find(
                    i => i.name === _.get(this.currentImageInfo, 'name'));
                console.log('image at startup: ', this.image);
                this.userVols = this.domain.userVolumes.filter(
                    i => _.get(this.currentImageInfo, 'userVolumes', []).includes(i.id));
                this.dataVols = this.domain.volumes.filter(
                    i => _.get(this.currentImageInfo, 'dataVolumes', []).includes(i.publisherDID));
            }
        },
        isSelected(i) {
            return i === this.selected;
        },
        applyImageInfo() {
            if (this.imageInfoModified) {
                console.log('image info changed!');
                this.$emit('imageInfoModified', this.newImageInfo);
            }
            this.open = false;
            this.focused = false;
        },
    },
};
</script>
<style scoped>
input.test-input:checked + span {
    background-color: #5cb85c;
    color: white;
}
p {
    padding: 10px;
    font-style: italic;
}
</style>
