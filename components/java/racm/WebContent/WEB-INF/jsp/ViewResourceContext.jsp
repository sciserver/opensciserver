
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
	$(document).ready(function() {
		var tbl = $("#resourceList")
		tbl.DataTable();

	});

	function buttonSaveBack_clickHandler() {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "back"
	}

	function buttonSaveCreate_clickHandler() {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "createresource"
	}
	
	function buttonSaveEdit_clickHandler(selectedResourceId) {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "editresource"
		var e = document.getElementById("selectedResourceId")
		e.value = selectedResourceId
	}

	function buttonSaveDelete_clickHandler(selectedResourceId) {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "deleteresource"
			var e = document.getElementById("selectedResourceId")
			e.value = selectedResourceId
	}

	function buttonNewToken_clickHandler() {
		var secretToken = document.getElementById("secretToken")
		// based on comment to https://stackoverflow.com/a/9719815/239003
		var s = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		secretToken.value = Array(48).join().split(',')
			.map(function() { return s.charAt(Math.floor(Math.random() * s.length)); })
			.join('');
	}
</script>

</head>
<body>
	<div class="row">
		<div class="col-xs-12">
			<a href="<spring:url value="/rctree/resourceContextList"/>">Back to Resource Context List</a>
		</div>
	</div>
	<form:form id="form" name="form" modelAttribute="resourceContextModel"
		style="max-width:350px;">
		<div class="container">
			<h1>Resource Context</h1>
			<div class="form-group">
				<form:label path="contextClassModel.name">Context Class Type</form:label>
				<form:select path="contextClassModel.name" 
					id="availableContextClasseModels" size="10" multiple="false">
					<form:options
						items="${resourceContextModel.availableContextClasseModels}"
						itemValue="name" itemLabel="name" />

				</form:select>
			</div>
			<div class="form-group">
				<form:label path="label" for="label">Label</form:label>
				<form:input path="label" type="text" class="form-control" id="label" />
			</div>
			<div class="form-group">
				<form:label path="endpoint" for="endpoint">Endpoint</form:label>
				<form:input path="endpoint" type="text" class="form-control"
					id="endpoint" />
			</div>
			<div class="form-group">
				<form:label path="uuid" for="uuid">Context UUID: generated for you, save this with the Context instance!</form:label>
				<form:input path="uuid" type="text" class="form-control" id="uuid"
					readonly="true" />
			</div>
			<div class="form-group">
				<form:label path="secretToken" for="secretToken">Token used for authenticating with RACM as this ResourceContext</form:label>
				<div class="input-group">
					<form:input path="secretToken" type="text" class="form-control" id="secretToken" autocomplete="off" />
					<span class="input-group-btn">
						<button class="btn btn-default" type="button" onclick="buttonNewToken_clickHandler();">Generate</button>
					</span>
				</div>
			</div>
			<div class="form-group">
				<!--<form:label path="id" for="id">Context ID</form:label>-->

				<form:input path="id" readonly="true" type="hidden"
					class="form-control" />
			</div>
			<div class="form-group">
				<form:label path="description" for="description">Description</form:label>
				<form:input path="description" type="text" class="form-control"
					id="description" />
			</div>
			<c:if test="${not resourceContextModel.valid}">
				<div class="alert alert-info">
					<strong>${resourceContextModel.errorMessage}</strong>
				</div>
			</c:if>
			<input type="hidden" name="nextaction" id="nextaction"
				value="nextaction" /> <input type="hidden"
				name="selectedResourceId" id="selectedResourceId" value=-1 />

			<div class="row">
				<div class="col-xs-12">
					<button id="buttonSaveBack" type="submit"
						onclick="buttonSaveBack_clickHandler();" class="btn btn-success">Save</button>
					<button id="buttonCancel" type="button"
						onclick="window.location='<spring:url value="/rctree/resourceContextList"/>';"
						class="btn btn-success">Cancel</button>
					<button id="buttonSaveNewResource" type="submit"
						onclick="buttonSaveCreate_clickHandler();" class="btn btn-success">Create
						Resource</button>
				</div>
			</div>
			<p></p>
		</div>
		<br />
		<div class="container">

			<c:if test="${not empty resourceContextModel.resources}">
				<table id="resourceList">
					<thead>
						<tr>
							<th>Context Identifier</th>
							<th>Resource Type</th>
							<th>ID</th>
							<th>Edit</th>
							<th>Delete</th>
						</tr>
					</thead>
					<tbody>
						<c:forEach var="o" items="${resourceContextModel.resources}">
							<tr>
								<td>${o.publisherDID}</td>
								<td>${o.resourceTypeModel.name}</td>
								<td>${o.id}</td>
								<td><button onclick="buttonSaveEdit_clickHandler(${o.id});"
										type="submit">Edit</button></td>
								<td>
									<button onclick="buttonSaveDelete_clickHandler(${o.id});"
										type="submit">Delete</button>
								</td>

							</tr>
						</c:forEach>
					</tbody>
				</table>
			</c:if>
		</div>
	</form:form>



	<script>
		$('#form').validate({ //this validation occures only after submit
			rules : {
				name : {
					required : true
				},
		        contextClassModel.name: {
			        required: true
		       },
		       name1: {
		    	   required: true
		       }
			},
			messages : {
				name : "Select Context Class from the list.",
			    contextClassModel.name: " ccc",
			    name1: "EEE"
			}
		});

		$('#buttonSaveBack').click(function() {
			$('#form').valid();
		});
	</script>
</body>
</html>