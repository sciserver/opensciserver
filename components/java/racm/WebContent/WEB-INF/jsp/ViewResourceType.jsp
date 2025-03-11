<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<spring:url value="/" var="context" htmlEscape="true" />

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<script type="text/javascript"
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script type="text/javascript"
	src="https://cdn.datatables.net/1.10.12/js/jquery.dataTables.js"></script>
<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
<!-- Include all compiled plugins (below), or include individual files as needed -->
<script
	src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/jquery.validate.js"></script>
<script src="<spring:url value="/static/js/bootstrap.min.js"/>"></script>
<script src="js/jquery.validate.js"></script>
<link rel="stylesheet" type="text/css"
	href="https://cdn.datatables.net/1.10.12/css/jquery.dataTables.min.css">
<link href="<spring:url value="/static/css/bootstrap.min.css"/>"
	rel="stylesheet">

<style>
label.error {
	font-weight: normal;
	color: red;
}
</style>

<script>
$(document).ready(function(){
	var tbl = $("#actionList")
	tbl.DataTable();
        
	var tbl = $("#roleList")
	tbl.DataTable();

});

function buttonSaveBack_clickHandler() {
	var nextaction = document.getElementById("nextaction")
	nextaction.value = "back"
}

function buttonSaveCreateRole_clickHandler() {
	var nextaction = document.getElementById("nextaction")
	nextaction.value = "createrole"
}

function buttonSaveCreateAction_clickHandler() {
	var nextaction = document.getElementById("nextaction")
	nextaction.value = "createaction"
}

function buttonSaveEditRole_clickHandler(selectedId) {
	var nextaction = document.getElementById("nextaction")
	nextaction.value = "editrole"
	var e = document.getElementById("selectedRoleId")
	e.value = selectedId
}

function buttonSaveDeleteRole_clickHandler(selectedId) {
	var nextaction = document.getElementById("nextaction")
	nextaction.value = "deleterole"
		var e = document.getElementById("selectedRoleId")
		e.value = selectedId
}
	
function buttonSaveEditAction_clickHandler(selectedId) {
	var nextaction = document.getElementById("nextaction")
	nextaction.value = "editaction"
	var e = document.getElementById("selectedActionId")
	e.value = selectedId
}

function buttonSaveDeleteAction_clickHandler(selectedId) {
	var nextaction = document.getElementById("nextaction")
	nextaction.value = "deleteaction"
		var e = document.getElementById("selectedActionId")
		e.value = selectedId
}

</script>

</head>
<body>
	<div class="row">
		<div class="col-xs-12">
			<a href="${context}cctree/contextClassList">Back to Context Class
				List</a><br /> <a
				href="${context}cctree/contextClass/${resourceTypeModel.contextClassId}">Back
				to Context Class ${resourceTypeModel.contextClassId}</a>

		</div>
	</div>
	<form:form id="form" modelAttribute="resourceTypeModel">
	<div class="container">
		<h1>Resource Type</h1>
			<form:input path="id" type="hidden" id="rtId" />
			<form:input path="contextClassId" type="hidden"/>

			<div class="form-group">
				<form:label path="name" for="name">Resource Type name</form:label>
				<form:input path="name" type="text" class="form-control" id="name" />
			</div>
			<div class="form-group">
				<form:label path="description" for="description">Description</form:label>
				<form:input path="description" type="text" class="form-control"
					id="descriptiontext" />
			</div>
			<c:if test="${not resourceTypeModel.valid}">
				<div class="alert alert-info">
					<strong>${resourceTypeModel.errorMessage}</strong>
				</div>
			</c:if>
			<input type="hidden" name="nextaction" id="nextaction" value="nextaction"/>
			<input type="hidden" name="selectedRoleId" id="selectedRoleId" value=-1 />
			<input type="hidden" name="selectedActionId" id="selectedActionId" value=-1 />

			<div class="row">
				<div class="col-xs-10">
					<button id="buttonSave" type="submit" onclick="buttonSaveBack_clickHandler();" class="btn btn-success">Save</button>
					<button id="buttonCancel" type="button" onclick="window.location='${context}cctree/contextClass/${resourceTypeModel.contextClassId}';" 
						class="btn btn-success">Cancel</button>

					<button id="buttonSaveNewRole" type="submit" onclick="buttonSaveCreateRole_clickHandler();" class="btn btn-success">Create Role</button>
					<button id="buttonSaveNewAction" type="submit" onclick="buttonSaveCreateAction_clickHandler();" class="btn btn-success">Create Action</button>
				</div>
			</div>
	</div>
	<br/><br/>
	
	<div class="container">
		<c:if test="${not empty resourceTypeModel.roles}">
			<h3>Roles</h3>
			<br/>
			<table id="roleList">
				<thead>
					<tr>
						<th>Name</th>
						<th>Description</th>
						<th>ID</th>
						<th>Edit</th>
						<th>Delete</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="o" items="${resourceTypeModel.roles}">
						<tr>
							<td>${o.name}</td>
							<td>${o.description}</td>
							<td>${o.id}</td>
							<td><button  onclick="buttonSaveEditRole_clickHandler(${o.id});" type="submit">Edit</button>
								</td>
							<td>
									<button onclick="buttonSaveDeleteRole_clickHandler(${o.id});"type="submit">Delete</button>
								</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		</c:if>
	</div>
	<br />
	<br/>
	<br />
	<br/>
	
	<div class="container">
		<c:if test="${not empty resourceTypeModel.actions}">
			<h3>Actions</h3>
			<br/>
			<table id="actionList">
				<thead>
					<tr>
						<th>Name</th>
						<th>Description</th>
						<th>ID</th>
						<th>Edit</th>
						<th>Delete</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="o" items="${resourceTypeModel.actions}">
						<tr>
							<td>${o.name}</td>
							<td>${o.description}</td>
							<td>${o.id}</td>
							<td><button  onclick="buttonSaveEditAction_clickHandler(${o.id});" type="submit">Edit</button>
								</td>
							<td>
									<button onclick="buttonSaveDeleteAction_clickHandler(${o.id});"type="submit">Delete</button>
								</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		</c:if>
	</div>
	</form:form>

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
					regexp: "^[a-zA-Z0-9_.]+$"
				},
				release: {
					required: true
				},
				description: {
					required: false
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