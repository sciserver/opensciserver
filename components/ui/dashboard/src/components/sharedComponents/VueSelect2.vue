
<!-- Based on https://vuejs.org/v2/examples/select2.html , modified for webpack, style and bootstrap -->
<template>
  <select>
    <option></option>
    <slot></slot>
  </select>
</template>

<script>
import $ from 'jquery';
import 'select2';
import 'select2/dist/css/select2.css';
import 'select2-bootstrap-theme/dist/select2-bootstrap.css';
import isEqual from 'lodash/isEqual';
import omit from 'lodash/omit';

export default {
  name: 'vue-select2',
  props: ['options', 'value', 'templateResult', 'placeholder', 'allowClear', 'matcher', 'searchMessage'],
  computed: {
    select2args() {
      return {
        data: this.options,
        theme: 'bootstrap',
        templateResult: this.templateResult,
        placeholder: this.placeholder,
        escapeMarkup: m => m,
        minimumInputLength: 1,
        allowClear: this.allowClear,
        matcher: this.matcher,
        language: {
          inputTooShort: () => this.searchMessage,
        },
      };
    },
  },
  mounted() {
    const vm = this;
    $(this.$el)
      // init select2
      .select2(this.select2args)
      .val(this.value)
      .trigger('change')
      // emit event on change.
      .on('change', e =>
        vm.$emit('input', $(e.currentTarget).val()));
  },
  watch: {
    value(value) {
      // update value
      $(this.$el)
        .val(value)
        .trigger('change');
    },
    select2args(select2args, oldargs) {
      // skip fields where new methods are generated in the computed select2args's
      const ignoredFields = ['language', 'templateResult', 'escapeMarkup'];
      if (isEqual(omit(select2args, ignoredFields), omit(oldargs, ignoredFields))) {
        return;
      }
      // update options
      $(this.$el).empty()
        .select2(select2args)
        .val(this.value)
        .trigger('change');
    },
  },
  destroyed() {
    $(this.$el).off().select2('destroy');
  },
};
</script>
<style>
.select2-results__option--highlighted .text-muted {
  color: white !important;
}
</style>