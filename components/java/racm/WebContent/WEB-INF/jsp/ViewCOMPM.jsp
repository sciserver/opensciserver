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
<title>COMPM - ${compmModel.uuid}</title>
<script type="text/javascript"
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script type="text/javascript"
	src="https://cdn.datatables.net/1.10.12/js/jquery.dataTables.js"></script>
<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
<!-- Include all compiled plugins (below), or include individual files as needed -->
<script
	src="https://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/jquery.validate.js"></script>
<script src="<spring:url value="/static/js/bootstrap.min.js"/>"></script>
<script src="<spring:url value="/static/js/jquery.validate.js"/>"></script>
<link rel="stylesheet" type="text/css"
	href="https://cdn.datatables.net/1.10.12/css/jquery.dataTables.min.css">
<link href="<spring:url value="/static/css/bootstrap.min.css"/>" rel="stylesheet">

<style>
label.error {
	font-weight: normal;
	color: red;
}
</style>

<script>
	function selectAssignedJobTypes() {
		// select all assigned actions to ensure they are submitted
		var x = document.getElementById("assignedJobTypes");
		for (var i = 0; i < x.options.length; i++) {
			x.options[i].selected = true
		}
		// no need to send available actions
		x = document.getElementById("availableJobTypes");
		for (var i = 0; i < x.options.length; i++) {
			x.options[i].selected = false
		}
	}

	function moveMultipleSelectedValues(fromSelect, toSelect) {
		var from = document.getElementById(fromSelect);
		var to = document.getElementById(toSelect);
		var l = from.options.length; //initialize

		for (var i = 0; i < l; i++) {
			if (from.options[i].selected == true) {
				var o = from.options[i];
				from.remove(i);
				to.add(o);
				i = i - 1;
				l = l - 1;
			}
		}
	}
</script>

</head>
<body>

	<h1>COMPM</h1>
	<div class="container"> 
		<form:form id="form" modelAttribute="compmModel" method="post">
			<div class="row">
				<div class="col-sm-12">
					<form:input path="id" type="hidden" id="id" />
					<div class="form-group">
						<form:label path="label" for="label">Your label</form:label>
						<form:input path="label" type="text" class="form-control" id="label"/>
					</div>
					<div class="form-group">
						<form:label path="uuid" for="uuid">COMPM UUID: generated for you, save this with the COMPM instance!</form:label>
						<form:input path="uuid" type="text" class="form-control" id="uuis"	readonly="true" />
					</div>
					<div class="form-group">
						<form:label path="defaultJobTimeout" for="defaultJobTimeout">Default timeout (in seconds)</form:label>
						<form:input path="defaultJobTimeout" type="number" min="0" class="form-control" id="defaultJobTimeout"/>
					</div>
					<div class="form-group">
						<form:label path="defaultJobsPerUser" for="defaultJobsPerUser">Jobs Per User</form:label>
						<form:input path="defaultJobsPerUser" type="number" min="1" class="form-control" id="defaultJobsPerUser"/>
					</div>
					<div class="form-group">
						<form:label path="description" for="description">Description</form:label>
						<form:input path="description" type="text" class="form-control"
							id="descriptiontext" />
					</div>
				</div>
			</div>
			<div class="row">
				<div class="col-sm-5">
					<form:select path="allComputeDomains" size="10" multiple="false">
						<form:options items="${compmModel.allComputeDomains}"
							itemValue="id" itemLabel="apiEndpoint" />
					</form:select>
				</div>
			</div>
<hr/>
			<div class="row">
				<div class="col-xs-12">
					<button id="submit" type="submit" class="btn btn-success">Save</button>
					<button id="cancel" type="button" 
						onclick="window.open('<spring:url value="/compm/mvc/list"/>','_self');" class="btn btn-success">Cancel</button>
				</div>
			</div>

		</form:form>
	</div>

	<script>
		$.validator.addMethod( //this validates as user enters field
		"regexp", function(value, element, regexp) {
			var re = new RegExp(regexp);
			return re.test(value);
		}, "Please use only [a-zA-Z0-9_].");

		$('#form').validate({ //this validation occures only after submit
			rules : {
				description : "required"
			},
			messages : {
				description : "Add description"
			}
		});

		$('#submit').click(function() {
			$('#form').valid();
		});
	</script>
</body>
</html>