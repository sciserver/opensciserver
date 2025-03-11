<template>
  <div style="position:absolute; width:100%; height: 100%; top: 0px; left: 0px">
    <compute-frame :imageInfo="imageInfo" :path="path" :noReuse="forceNew"/>
  </div>
</template>

<script>
import computeFrame from './ComputeFrame';

const _ = require('lodash');

export default {
    name: 'ComputeFrameTest',
    components: {
        computeFrame,
    },
    data: () => ({
        imageInfoDefault: {
          domain: 6,
          name: 'SciServer Essentials 3.0',
          userVolumes: [],
          dataVolumes: [],
        },
        forceNew: false,
    }),
    created() {
        this.$store.dispatch('loadUserComputeDomains');
        this.forceNew = _.get(this.$route.query, 'fn', false);
        this.$router.replace({ query: _.omit(this.$route.query, 'fn') });
    },
    computed: {
        imageInfo() {
            if (_.get(this.$route.query, 'ij')) {
                try {
                    return JSON.parse(atob(this.$route.query.ij));
                } catch (e) {
                    console.error('could not parse image info json');
                }
            }
            const img = {};
            img.domain = parseInt(_.get(this.$route.query, 'd', this.imageInfoDefault.domain), 10);
            img.name = _.get(this.$route.query, 'n', this.imageInfoDefault.name);
            if (_.get(this.$route.query, 'uv')) {
                img.userVolumes = _.map(this.$route.query.uv.split(','), x => parseInt(x, 10));
            } else {
                img.userVolumes = this.imageInfoDefault.userVolumes;
            }
            if (_.get(this.$route.query, 'dv')) {
                img.dataVolumes = _.map(this.$route.query.dv.split(','), x => parseInt(x, 10));
            } else {
                img.dataVolumes = this.imageInfoDefault.dataVolumes;
            }
            console.log('final resolved image info:', img);
            return img;
        },
        path() {
            return _.get(this.$route.query, 'p', '');
        },
    },
};
</script>


<style>
main {
  position: relative;
}
</style>
