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
		var tbl = $("#resourceTypeList")
		tbl.DataTable();

	});

	function buttonSaveBack_clickHandler() {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "back"
		//nextaction.value = "redirect:/cctree/contextClassList"
	}

	function buttonSaveCreate_clickHandler() {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "createresourcetype"
	}
	
	function buttonSaveEdit_clickHandler(selectedRTId) {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "editresourcetype"
		var e = document.getElementById("selectedRTId")
		e.value = selectedRTId
	}

	function buttonSaveDelete_clickHandler(selectedRTId) {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "deleteresourcetype"
			var e = document.getElementById("selectedRTId")
			e.value = selectedRTId
	}

	//This method didn't work on Chrome browser.
	function clickLinkEditResourceType(id) {
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "editresourcetype"
		var selectedRTId = document.getElementById("selectedRTId")
		selectedRTId.value = id
		alert("ID " + id);
		
		document.forms["form"].submit();
		
		
	}
</script>

</head>
<body>
	<div class="row">
		<div class="col-xs-12">
			<a href="<spring:url value="/cctree/contextClassList"/>">Back to
				Context Class List</a>
		</div>
	</div>
		<form:form id="form"  name="form"  modelAttribute="contextClassModel"
			style="max-width:350px;">
	<div class="container">
		<h1>Context Class</h1>
			<div class="form-group">
				<form:label path="name" for="name">Context class name</form:label>
				<form:input path="name" type="text" class="form-control" id="name" />
			</div>
			<div class="form-group">
				<!--<form:label path="id" for="id">Context ID</form:label>-->

				<form:input path="id" readonly="true" type="hidden"
					class="form-control" />
			</div>
			<div class="form-group">
				<form:label path="description" for="description">Description</form:label>
				<form:input path="description" type="text" class="form-control"
					id="descriptiontext" />
			</div>
			<div class="form-group">
				<form:label path="release" for="release">release</form:label>
				<form:input path="release" type="text" class="form-control"
					id="release" />
			</div>
			<c:if test="${not contextClassModel.valid}">
				<div class="alert alert-info">
					<strong>${contextClassModel.errorMessage}</strong>
				</div>
			</c:if>
			<input type="hidden" name="nextaction" id="nextaction" value="nextaction" />
			<input type="hidden" name="selectedRTId" id="selectedRTId" value=-1 />

			<div class="row">
				<div class="col-xs-12">
					<button id="buttonSaveBack" type="submit"
						onclick="buttonSaveBack_clickHandler();" class="btn btn-success">Save</button>
					<button id="buttonCancel" type="button"
						onclick="window.location='<spring:url value="/cctree/contextClassList"/>';"
						class="btn btn-success">Cancel</button>
					<button id="buttonSaveNewRT" type="submit"
						onclick="buttonSaveCreate_clickHandler();" class="btn btn-success">Create
						Resource Type</button>
				</div>
			</div>
			<p></p>
	</div>
	<br />
	<div class="container">

		<c:if test="${not empty contextClassModel.resourceTypes}">
			<table id="resourceTypeList">
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
					<c:forEach var="o" items="${contextClassModel.resourceTypes}">
						<tr>
							<td>${o.name}</td>
							<td>${o.description}</td>
							<td>${o.id}</td>
							<td><button  onclick="buttonSaveEdit_clickHandler(${o.id});" type="submit">Edit</button>
								</td>
							<td>
									<button onclick="buttonSaveDelete_clickHandler(${o.id});"type="submit">Delete</button>
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
		"regexp", function(value, element, regexp) {
			var re = new RegExp(regexp);
			return re.test(value);
		}, "Please use only [a-zA-Z0-9_].");

		$('#form').validate({ //this validation occures only after submit
			rules : {
				name : {
					required : true,
					regexp : "^[a-zA-Z0-9_]+$"
				},
				release : {
					required : true
				},
				description : {
					required : false
				}
			},
			messages : {
				release : "Enter release."
			}
		});

		$('#submit').click(function() {
			$('#form').valid();
		});
	</script>
</body>
</html>