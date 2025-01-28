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
<title>List of COMPMs</title>
<script type="text/javascript"
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script type="text/javascript"
	src="https://cdn.datatables.net/1.10.12/js/jquery.dataTables.js"></script>
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
    var tbl = $("#compms")
    tbl.DataTable();
        
    $("p").click(function(){
        $(this).hide();
    });
});
</script>

</head>
<body>
	<div class="container">
	<h3>Hello ${user}, the following COMPMS are visible to you</h3>
	<div class="row">
		<div class="col-xs-8">
			<a href="<spring:url value="/compm/mvc/new"/>">Register new COMPM</a>
		</div>
	</div>
	<hr/>
	<div class="row">
	<c:choose>
		<c:when test="${not empty compms}">
			<table id="compms">
				<thead>
					<tr>
						<th>Label</th>
						<th>UUID</th>
						<th>Creator</th>
						<th>Description</th>
						<th>Default Timeout (sec)</th>
						<th>Jobs Per User</th>
						<th>Action</th>
					</tr>
				</thead>
				<c:forEach var="o" items="${compms}">
					<tr>
						<td>${o.label}</td>
						<td>${o.uuid}</td>
						<td>${o.creatorUserid}</td>
						<td>${o.description}</td>
						<td>${o.defaultJobTimeout}</td>
						<td>${o.defaultJobsPerUser }</td>
						<td><a href="<spring:url value="/compm/mvc/view/${o.uuid}"/>">Edit</a></td>
					</tr>
				</c:forEach>
			</table>
		</c:when>
		<c:otherwise>
			<div class="container">
				<h4>There are as yet no COMPMs defined. Create one.</h4>
			</div>
		</c:otherwise>
	</c:choose>
	</div>
</div>
</body>



</html>