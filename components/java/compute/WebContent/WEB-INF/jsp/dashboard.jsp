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
<spring:eval expression="T(org.sciserver.compute.AppConfig).getInstance().getAppSettings().getApplicationName()" var="applicationName"/>
<title>${applicationName} Compute - Interactive Work</title>
</head>
<body onload="update()">
	<spring:eval expression="T(org.sciserver.compute.AppConfig).getInstance().getAppSettings().isUiInformationEnabled()" var="informationEnabled"/>
    <spring:eval expression="T(org.sciserver.compute.AppConfig).getInstance().getAppSettings().getPoliciesUrl()" var="policiesUrl"/>
	
	<%@ include file="../jspf/navbar.jspf" %>
	
	<c:if test="${alert != null && alert.length() > 0}">
		<div class="alert alert-warning" style="margin-bottom: 0px">
		${alert}
		</div>
	</c:if>
	
	<%@ include file="../jspf/create-container-dialog.jspf" %>
	<%@ include file="../jspf/delete-container-dialog.jspf" %>
	<%@ include file="../jspf/visit-dashboard-dialog.jspf" %>
	
	<div id="deletingMessage" class="modal fade" role="dialog">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-body">
					<h4>Deleting container. Please wait...</h4>
				</div>
			</div>
		</div>
	</div>

	<div id="creatingMessage" class="modal fade" role="dialog">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-body">
					<h4>Creating container. Please wait...</h4>
				</div>
			</div>
		</div>
	</div>
	<main>
		<div class="container">
			<div class="panel panel-primary">
				<div class="panel-heading">Containers</div>
				<div class="panel-body">
					<div class="table-responsive">
						<table class="table table-hover table-bordered">
							<tr>
								<th>Created At</th>
								<th>Name</th>
								<!--  <th>Volumes</th> -->
								<th>Domain</th>
								<th>Image</th>
								<th>Status</th>
								<th></th>
							</tr>
							<c:forEach var="container" items="${containers}">
								<tr>
									<td>${container.getCreatedAt()}</td>
									<td>
											<a href="<spring:url value="/go"/>?id=${container.getId()}" target="_blank">${fn:escapeXml(container.getDisplayName())}</a>
									</td>
									<!-- <td>${container.getAttachedVolumes()}</td> -->
									<td>${container.getDomainName()}</td>
									<td>${container.getImageName()}</td>
									<td>${container.getStatus()}</td>
									<td>
										<div class="btn-group btn-group-xs" role="group">
											<c:choose>
	  											<c:when test="${container.status != 'running'}">
													<a href="<spring:url value="/start"/>?id=${container.getId()}&target=." class="btn btn-link" title="Start Container">
														<span class="glyphicon glyphicon-play glyphicon-green" aria-hidden="true"></span>
													</a>
												</c:when>
												<c:otherwise>
													<a href="<spring:url value="/stop"/>?id=${container.getId()}&target=." class="btn btn-link" title="Stop Container">
														<span class="glyphicon glyphicon-stop glyphicon-red" aria-hidden="true"></span>
													</a>
												</c:otherwise>
											</c:choose>
											<a href="<spring:url value="/info"/>?id=${container.getId()}" class="btn btn-link" title="Container Info">
												<span class="glyphicon glyphicon-info-sign glyphicon-blue" aria-hidden="true"></span>
											</a>
											<a onclick="confirmDelete('<spring:url value="/delete"/>?id=${container.getId()}')" href="#" class="btn btn-link" title="Delete Container">
												<span class="glyphicon glyphicon-remove glyphicon-red" aria-hidden="true"></span>
											</a>
										</div>
									</td>
								</tr>
							</c:forEach>
						</table>
					</div>
					<div>
						<a onclick="return $('#createContainerDialog').modal('show');" href="#" class="btn btn-success">Create container</a>
					</div>
				</div>
			</div>
			
			<c:if test="${informationEnabled}">
			<div class="panel panel-danger">
				<div class="panel-heading"><h4 class="panel-title">Important Information about Compute Container File Storage</h4></div>
				<div class="panel-body" id="fileStorage">
					<dl class="dl-horizontal">
						<dt>File System</dt>
					<dd>
						Most of the folders in a Container's file system should not be
						used to store your files. Your initial container view is of <u>/home/idies/workspace</u>,
						which may contain volumes under the <u>Storage</u> and <u>Temporary</u>
						folders. Any user volumes you choose to add to the container at
						creation will be present within these folders. Do not store your
						files in <u>workspace</u>, or in any other folder except as
						described here. If a Compute node fails, your incorrectly stored
						files will be <em class="text-danger">lost permanently</em>.
					</dd>
					<dt>
						<u>Storage</u>
					</dt>
					<dd>
						Use <u>Storage</u> volumes for long term storage of your scripts
						and small data files. The volumes in the <u>Storage</u> folder are
						backed up. These volumes are mounted according to the username of
						the user who created them under the path <u>/home/idies/workspace/Storage/<em>username</em>/<em>user
								volume name</em>/
						</u>. Files saved to this folder persist between your containers, even
						in the event that a container fails. Other files and folders cannot be
						placed in any intermediate paths, i.e., under the <u>Storage/<em>username</em></u>
						or <u>Storage/</u> folders. Your <u>Storage</u> volumes are
						subject to size limitations described in the <a href="${policiesUrl}" 
							target="_blank"><strong>${applicationName} Compute Data Storage Policy</strong></a>.
					</dd>
					<dt><u>persistent</u></dt>
					<dd>
						By default, all users start with a <u>Storage</u> volume named <u>persistent</u>.
						The files in this volume correspond to the same <u>persistent</u>
						folder used in previous versions of Compute.
					</dd>
					<dt>
						<u>Temporary</u>
					</dt>
					<dd>
						Use <u>Temporary</u> volumes for temporary large file storage. The
						<u>Temporary</u> volumes persist between containers and are not
						affected by Compute node failure, but is not backed up. These
						volumes are mounted according to the username of the user who
						created them under the path <u>/home/idies/workspace/Temporary/<em>username</em>/<em>user
								volume name</em>/
						</u>. Other files and folders cannot be placed in any intermediate paths,
						i.e., under the <u>Temporary/<em>username</em></u> or <u>Temporary/</u>
						folders. Your <u>Temporary</u> volumes are subject to time limit
						and size limitations described in the <a href="${policiesUrl}"
							target="_blank"><strong>${applicationName} Compute Data Storage Policy</strong></a>.
					</dd>
					<dt><u>scratch</u></dt>
					<dd>
						By default, all users start with a <u>Temporary</u> volume named <u>scratch</u>.
						The files in this volume correspond to the same <u>scratch</u>
						folder used in previous versions of Compute.
					</dd>
				</dl>
					<div class="clearfix"></div>
					<p class="h4 text-center">Do not save your scripts or data files anywhere in your Compute container's file system except in "Storage" or "Temporary".</p>
				</div>
			</div>
			</c:if>
		</div>
	</main>
	<script>
			function confirmDelete(url) {
					$('#confirmDeleteButton').attr('href', url);
					return $('#deleteContainerDialog').modal('show');
			}
	</script>
	
<%@ include file="../jspf/footer.jspf" %>
</body>
</html>
