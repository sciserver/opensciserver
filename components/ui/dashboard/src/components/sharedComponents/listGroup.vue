<template>
<nav class="list">
     <h3 :class="`list-heading-${headingType}`">
       {{ title }}
       <slot name="headerContent" class="headerContentStyle">
       </slot>
    </h3>
  <slot>
    <a href="#"
      type="button"
      class="list-group-item"
      v-for="item in items"
      :key="item.id"
      @click="onClick(item.id)"
      :class="{active: item.id === selectedId}">
      {{ item.name }}
    </a>
  </slot>
</nav>
</template>
​
<script>
export default {
  name: 'listGroup',
  props: ['items', 'onClick', 'title', 'headingType', 'selectedId'],
};
</script>

<style scoped>
.list-heading-default {
    background-color: #f3f5f8;
    border-bottom: 1px solid #e1e4e8;
    color: #586069;
    display: block;
    font-size: 13px;
    font-weight: 600;
    line-height: 20px;
    margin-bottom: 0;
    margin-top: 0;
    padding: 8px 10px;
}
.list-heading-primary {
    background-color: #337ab7;
    border-bottom: 1px solid #337ab7;
    color: #ffffff;
    display: block;
    font-size: 13px;
    font-weight: 600;
    line-height: 20px;
    margin-bottom: 0;
    margin-top: 0;
    padding: 10px 10px;
}
.list {
    background-color: #fff;
    border: 1px solid #d1d5da;
    border-radius: 3px;
    list-style: none;
    margin-bottom: 15px;
}
.list-item {
    border-bottom: 1px solid #e1e4e8;
    display: block;
    padding: 8px 10px;
    position: relative;
}
</style>
