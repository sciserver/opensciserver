<template id="vue-datepicker">
   <div>
    <label for="dateStart">Start date</label>
    <input name="dateStart" id="dateStart" v-model="dateFrom" style="width:170px;"/>
    <label for="dateEnd">End date</label>
    <input name="dateEnd" v-model="dateTo" id="dateEnd" style="width:178px;"/>
  </div>          
</template>

<script>
import $ from 'jquery';

export default {
  name: 'JqueryCalendar',
  computed: {
  },
  props: ['from', 'to'],
  data: () => ({
    dateFrom: this.from,
    dateTo: this.to,
  }),
  watch: {},
  mounted() {
    $('input[name=dateStart]').datetimepicker({
      onChangeDateTime: (dp, $input) => {
        this.$emit('changedFrom', $input.val());
        this.dateFrom = $input.val();
      },
    });
    $('input[name=dateEnd]').datetimepicker({
      onChangeDateTime: (dp, $input) => {
        this.$emit('changedTo', $input.val());
        this.dateTo = $input.val();
      },
    });
  },
  methods: {
    clearAll() {
      this.$emit('changedFrom', '');
      this.dateFrom = '';
      this.$emit('changedTo', '');
      this.dateTo = '';
    },
  },
};
</script>
<style>
</style>