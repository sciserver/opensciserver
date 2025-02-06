<template>
  <div>
    <p>Mark these root volumes as being accessible to users in this domain.</p>
    <p class="text-warning">These root volumes must be mounted in the docker nodes to be mountable in Compute containers.</p>
    <div
      v-for="(rootVolume, index) of myRootVolumeObjects"
      :key="index"
      class="checkbox">

      <label>
        <input type="checkbox" :value="rootVolume" v-model="rootVolumesOnCD">
        {{ rootVolume.name }} Root Volume<br/>
        <small>Hosted on "{{ rootVolume.fileserviceObj.name }}"</small><br/>
        <template v-if="rootVolumesOnCD.includes(rootVolume)">
          Where is this root volume mounted on the compute node?
          <input class="form-control" type="text" placeholder="/srv/path/" v-model="rootVolumePathOnCDs[rootVolume.id]"/>
        </template>
      </label>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import { validationMixin } from 'vuelidate';

export default {
  name: 'UserVolumesStep',
  mixins: [validationMixin],
  data: () => ({
    rootVolumesOnCD: [],
    rootVolumePathOnCDs: {},
  }),
  computed: {
    ...mapGetters(['myRootVolumeObjects']),
  },
  validations: {
    rootVolumesOnCD: {
    },
    rootVolumePathOnCDs: {
    },
    form: ['rootVolumesOnCD', 'rootVolumePathOnCDs'],
  },
  methods: {
    validate() {
      this.$v.form.$touch();
      const isValid = !this.$v.form.$invalid;
      this.$emit('on-validate', this.$data, isValid);
      return isValid;
    },
  },
};
</script>
