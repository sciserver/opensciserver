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

	function buttonSaveBack_clickHandler() {
		var selectedRT
		
		var nextaction = document.getElementById("nextaction")
		nextaction.value = "back"
	}

</script>

</head>
<body>
	<div class="row">
		<div class="col-xs-12">
			<a href="<spring:url value="/rctree/resourceContextList"/>">Back
				to Resource Context List</a>
		</div>
	
		<div class="col-xs-12">
			<a href="<spring:url value="/rctree/resourceContext/${resourceModel.containerId}"/>">Back
				to Resource Context ${resourceModel.containerId}</a>
		</div>
	</div>
	<form:form id="form" name="form" modelAttribute="resourceModel"
		style="max-width:350px;">
		<div class="container">
			<h1>Resource</h1>
			<div class="form-group">
			<form:input path="id" type="hidden" id="id" />
			<form:input path="containerId" type="hidden"/>
			
				<form:label path="resourceTypeModel.name">Resource Type</form:label>
				<form:select path="resourceTypeModel.name"
					id="availableResourceTypeModels" size="10" multiple="false">
					<form:options
						items="${resourceModel.availableResourceTypeModels}"
						itemValue="name" itemLabel="name" />

				</form:select>
			<div class="form-group">
				<form:label path="name" for="name">Name</form:label>
				<form:input path="name" type="text" class="form-control" id="name" />
			</div>
				
			<div class="form-group">
				<form:label path="uuid" for="uuid">Resource UUID: generated for you, save this with the Resource instance!</form:label>
				<form:input path="uuid" type="text" class="form-control" id="uuis"
					readonly="true" />
			</div>

			<div class="form-group">
				<form:label path="publisherDID" for="publisherDID">PublisherDID</form:label>
				<form:input path="publisherDID" type="text" class="form-control" id="publisherDID" />
			</div>

			
			<div class="form-group">
				<form:label path="description" for="description">Description</form:label>
				<form:input path="description" type="text" class="form-control"
					id="description" />			
			<c:if test="${not resourceModel.valid}">
				<div class="alert alert-info">
					<strong>${resourceModel.errorMessage}</strong>
				</div>
			</c:if>
			<input type="hidden" name="nextaction" id="nextaction"
				value="nextaction" /> 

			<div class="row">
				<div class="col-xs-12">
					<button id="buttonSaveBack" type="submit"
						onclick="buttonSaveBack_clickHandler();" class="btn btn-success">Save</button>
					<button id="buttonCancel" type="button"
						onclick="window.location='<spring:url value="/rctree/resourceContext/${resourceModel.containerId}"/>';"
						class="btn btn-success">Cancel</button>
				</div>
			</div>
			<p></p>
		</div>
		<br />
	</form:form>



	<script>
		$('#form').validate({ //this validation occures only after submit
			rules : {
				resourceTypeModelId : {
					required : true
				}
			},
			messages : {
				resourceTypeModelName : "Please select Resource Type."
			}
		});

		$('#submit').click(function() {
			$('#form').valid();
		});
	</script>
</body>
</html>