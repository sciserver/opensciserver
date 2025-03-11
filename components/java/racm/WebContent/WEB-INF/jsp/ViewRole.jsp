<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>    
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<spring:url value="/" var="context" htmlEscape="true"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script type="text/javascript" src="https://cdn.datatables.net/1.10.12/js/jquery.dataTables.js"></script>
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/jquery.validate.js"></script>
    <script src="<spring:url value="/static/js/bootstrap.min.js"/>"></script>
    <script src="js/jquery.validate.js"></script> 
<link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.10.12/css/jquery.dataTables.min.css">
<link href="<spring:url value="/static/css/bootstrap.min.css"/>" rel="stylesheet"> 
    
<style>
   	label.error {
   		font-weight: normal;
   		color: red;
   	}
</style>

<script>
function selectAllAssignedActions(){
	// select all assigned actions to ensure they are submitted
	var x = document.getElementById("assignedActions");
	for (var i = 0; i < x.options.length; i++) {
	    x.options[i].selected =true
	}
	// no need to send available actions
	x = document.getElementById("availableActions");
	for (var i = 0; i < x.options.length; i++) {
	    x.options[i].selected =false
	}
}

function moveMultipleSelectedValue(fromSelect, toSelect)
{
  var from = document.getElementById(fromSelect);
  var to = document.getElementById(toSelect);
  var l = from.options.length; //initialize
 
  for (var i = 0; i < l; i++) {
     if(from.options[i].selected ==true){
		 var o = from.options[i];
		 from.remove(i);
    	 to.add(o); 
    	 i = i-1;
    	 l = l-1;
      }
  }
}


</script>

</head>
<body>
<div class="row">
		<div class="col-xs-12">
			<a href="${context}cctree/contextClassList">Back to Context Class List</a><br/>
		</div>
	</div>
  <div class="container">
  	<h1>Role</h1>
    <form:form id="form" modelAttribute="roleModel" >
			<div class="form-group">
				<form:input path="id" type="hidden" id="roleId" />
				<form:input path="resourceTypeId" type="hidden"/>
			</div>
			<div class="form-group">
				<form:label path="name" for="name">Role name</form:label>
				<form:input path="name" type="text" class="form-control" id="name" />
			</div>
			<div class="form-group">
				<form:label path="description" for="description">Description</form:label>
				<form:input path="description" type="text" class="form-control"
					id="descriptiontext" />
			</div>
					<div>
			<div style="float: left; width: 33%;">
				<form:select path="availableActions" id="availableActions" size="10"
					multiple="true">
					<form:options items="${roleModel.availableActions}" itemValue="id" itemLabel="name"  />

				</form:select>
			</div>
			
			<div style="float: left; width: 33%;">
			<br/><br/><br/>
			
				<button type="button" onclick='moveMultipleSelectedValue("availableActions", "assignedActions")'>Add</button>
				<button type="button"
					onclick='moveMultipleSelectedValue("assignedActions", "availableActions")'>Remove</button>
			</div>
			<div style="float: left; width: 33%;">
				<form:select path="assignedActions" id="assignedActions" size="10"	multiple="true">
					<form:options items="${roleModel.assignedActions}" itemValue="id" itemLabel="name"  />
				</form:select>		
				</div>

			</div>
			
			<c:if test="${not roleModel.valid}">
				<div class="alert alert-info">
					<strong>${roleModel.errorMessage}</strong>
				</div>
			</c:if>
			<div class="row">
				<div class="col-xs-12">
					<button id="submit"  type="submit" onclick="selectAllAssignedActions();" class="btn btn-success">Save</button>
					<button id="buttonCancel" type="button" onclick="window.location='${context}cctree/resourceType/${roleModel.resourceTypeId}';" class="btn btn-success">Cancel</button>
				</div>
			</div>
		</form:form>
  </div>

    <script>
	    $.validator.addMethod( //this validates as user enters field
	            "regexp",
	            function(value, element, regexp) {
	                var re = new RegExp(regexp);
	                return re.test(value);
	            },
	            "Please use only [a-zA-Z0-9_]."
	    );
    
	    $('#form').validate( {  //this validation occures only after submit
			rules: {
				name: {
					required: true,
					regexp: "^[a-zA-Z0-9_]+$"
				},
				release: {
					required: true
				},
				description: {
					required:false
				}
			},
			messages: {
			}
		});

		$('#submit').click(function() {
			$('#form').valid();
		});
		
    </script>
  </body>
</html>