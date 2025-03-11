 <template>
<div class="jumbotron">
    <h1>{{label}}</h1>
    <p>{{description}}</p>
    <p>
        <slot>
        <button type="button" class="btn btn-success btn-sm" :class="buttonClasses" v-if="action"><i class="fa fa-plus"></i>   
        </button>
        </slot>
    </p>
 </div>
 </template>
 <script>
 export default {
   props: ['label', 'description', 'action'],
 };
 </script>
 