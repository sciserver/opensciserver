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
    var tbl = $("#resourceContexts")
    tbl.DataTable();
        
    $("p").click(function(){
        $(this).hide();
    });
});
</script>

</head>
<body>
	<h3>Access Control Management</h3>
	<div class="row">
		<div class="col-xs-8 text-right">
			<a href="${context}rctree/resourceContext">Create Resource Context</a>
		</div>
	</div>

	<c:choose>
		<c:when test="${not empty resourceContexts}">
			<table id="resourceContexts">
				<thead>
					<tr>
						<th>Type</th>
						<th>Label</th>
						<th>Endpoint</th>
						<th>UUID</th>
						<th>Modified</th>
						<th>Edit</th>
						<th>Delete</th>
					</tr>
				</thead>
				<c:forEach var="o" items="${resourceContexts}">
					<tr>
						<td>${o.contextClass.name}</td>
						<td>${o.label}</td>
						<td>${o.racmEndpoint}</td>
						<td>${o.uuid}</td>
						<td>${o.modificationDate}</td>
						<td><a href="${context}rctree/resourceContext/${o.id}">Edit</a></td>
							<td><form:form action="${context}rctree/resourceContext/${o.id}/delete" method="POST">
								<button id="deleteResourceContextButton" type="submit">Delete</button>
							</form:form></td>
						
					</tr>
				</c:forEach>
			</table>
		</c:when>
		<c:otherwise>
			<div class="container">
				<h4>There is no ResourceContext defined. Create one.</h4>
			</div>
		</c:otherwise>
	</c:choose>

</body>



</html>