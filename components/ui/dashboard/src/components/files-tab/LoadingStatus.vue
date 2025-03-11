<template>
  <div>
<!--    <div v-if="loadingStatusIcon" style="text-align: center"> {{ loadingAction }} ...<i class="fa fa-circle-o-notch fa-spin fa-2x"></i></div>-->
  <span class="text-align: center" style="margin: auto;width: 50%; padding-left: 20%; padding-right: 20%"> loadingAction ...<i class="fa fa-spinner fa-spin fa-2x"></i></span>
  </div>
</template>

<script>
    import { mapState } from 'vuex';

    export default {
        name: 'LoadingStatus',
        computed: {
            ...mapState(['loadingAction', 'loadingStatusIcon']),
        },
    };
</script>

<style scoped>

</style>
