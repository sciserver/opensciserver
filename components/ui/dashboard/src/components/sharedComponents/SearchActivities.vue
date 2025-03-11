<template>
       <div class="col-xs-12 col-md-4 search-wrap">  
            <span class="fa fa-search" aria-hidden="true"></span>
            <label class="thin">
            <input type="text" class="form-control main-search" placeholder="Type here to filter the list" v-model="searchTerm">
            </label>
        </div>
</template>

<script>
export default {
  name: 'SearchActivities',
  data: () => ({
    searchTerm: '',
  }),
  props: ['myKey', 'myDir'],
  watch: {
    searchTerm() {
      this.$emit('searchRecords', this.searchTerm);
    },
  },
  methods: {
    requestKeyChange(value) {
      this.$emit('keyChange', value);
    },
    requestDirChange(value) {
      this.$emit('dirChange', value);
    },
  },
};
</script>
<style>
.search-wrap {
		position: relative;
		overflow: hidden;
		padding-right: 0;
		margin-left: -10px;
  }
  .search-wrap .fa-search {
		position: absolute;
		left:25px;
		top:10px;
		color: #9F9999;
		font-size: 14px;
  }
  .search-wrap .form-control{
		text-indent: 16px;
		border-radius: 50px;
  }
  .search-wrap .fa-times-circle {
		position: absolute;
		right: -26px;
		font-size: 24px;
		top: 5px;
		color: #B3B3B3;
		cursor: pointer;
		-webkit-transition: all 0.1s ease-in-out;
	    -moz-transition: all 0.1s ease-in-out;
	    -o-transition: all 0.1s ease-in-out;
	    transition: all 0.1s ease-in-out;
  }
.search-wrap label { font-weight: normal;max-width:none;display:block}
.thin {
	font-weight: 300 !important;
}
</style>