<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<title>Dask Clusters</title>
</head>
<body onload="update()">
	<%@ include file="../jspf/navbar.jspf" %>
	<%@ include file="../jspf/delete-cluster-dialog.jspf" %>
	<%@ include file="../jspf/create-cluster-dialog.jspf" %>
	<main>
		<div class="container">
			<div class="panel panel-primary">
				<div class="panel-heading">Dask Clusters</div>
				<div class="panel-body">
					<div class="table-responsive">
						<table class="table table-hover table-bordered">
							<tr>
								<th>Ref. ID</th>
								<th>Dashboard</th>
								<th></th>
							</tr>
							<c:forEach var="cluster" items="${clusters}">
								<tr>
									<td>${cluster.getExternalRef()}</td>
									<td><a href="${cluster.getDashboardUrl()}" target="_blank">${cluster.getDashboardUrl()}</a></td>
									<td>
										<div class="btn-group btn-group-xs" role="group">
											<a onclick="confirmDelete('<spring:url value="/dask/delete"/>?id=${cluster.getId()}')" href="#" class="btn btn-link" title="Delete">
												<span class="glyphicon glyphicon-remove glyphicon-red" aria-hidden="true"></span>
											</a>
										</div>
									</td>
								</tr>
							</c:forEach>
						</table>
					</div>
					<div>
						<a onclick="return $('#createClusterDialog').modal('show');" href="#" class="btn btn-success">Create cluster</a>
					</div>
				</div>
			</div>
		</div>
	</main>
	<script>
		function confirmDelete(url) {
			$('#confirmDeleteButton').attr('href', url);
			return $('#confirmDeleteDialog').modal('show');
		}
	</script>

<%@ include file="../jspf/footer.jspf" %>
</body>
</html>
