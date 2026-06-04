<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0.
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/bootstrap-header.jspf"%>
<link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/v/bs/dt-1.10.15/b-1.4.0/datatables.min.css"/>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jstree/3.2.1/themes/default/style.min.css" />
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/jquery-toast-plugin/1.3.1/jquery.toast.min.css" integrity="sha256-WolrNTZ9lY0QL5f0/Qi1yw3RGnDLig2HVLYkrshm7Y0=" crossorigin="anonymous" />
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/vue-multiselect@2.1.0/dist/vue-multiselect.min.css">
<link rel="stylesheet" href="<spring:url value="/static/css/jobs.css"/>"/>
<spring:eval expression="T(org.sciserver.compute.AppConfig).getInstance().getAppSettings().getApplicationName()" var="applicationName"/>
<title>${applicationName} Compute - Jobs</title>
</head>
<body
	data-token="${fn:escapeXml(token)}"
	data-racm="${fn:escapeXml(racmUrl)}"
	data-username="${fn:escapeXml(username)}"
	data-dashboard="${fn:escapeXml(dashboardUrl)}"
	data-dask-default-workers="${daskDefaultWorkers}"
	data-dask-default-memory="${daskDefaultMemory}"
	data-dask-default-threads="${daskDefaultThreads}"
	data-is-dask-available="${isDaskAvailable}">
	<%@ include file="../jspf/navbar.jspf"%>

	<main>
	<c:if test="${alert != null && alert.length() > 0}">
		<div class="alert alert-warning" style="margin-bottom: 0">
			${alert}</div>
	</c:if>

	<div class="container">
		<div class="panel panel-primary">
			<div class="panel-heading" id="jobs-heading">
				<h2 class="panel-title">Compute Jobs</h2>
				<div id="run-button-groups-holder">
					<button id="run-command-button" class="btn btn-success btn-sm disabled" data-runtype="command">
						Run Command
					</button>
					<button id="run-notebook-button" class="btn btn-success btn-sm disabled" data-runtype="notebook">
						Run Existing Notebook
					</button>
				</div>
			</div>
			<div class="panel-body">
				<div class="table-responsive">
					<table id="jobsTable" class="table table-hover">
					</table>
				</div>
			</div>
			<p id="no-jobs-message" class="panel-body hidden">
				Compute Jobs are available for longer, non-interactive tasks. Click one of the buttons above to create a job.
			</p>
		</div>
	</div>

	<div id="create-job-command-wizard"></div>
	<div id="create-job-notebook-wizard"></div>
	</main>

	<%@ include file="../jspf/footer.jspf"%>
	<%@ include file="../jspf/visit-dashboard-dialog.jspf" %>
	<script type="text/javascript" src="https://cdn.datatables.net/v/bs/dt-1.10.15/b-1.4.0/datatables.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.18.1/moment.min.js" integrity="sha256-1hjUhpc44NwiNg8OwMu2QzJXhD8kcj+sJA3aCQZoUjg=" crossorigin="anonymous"></script>
	<script src="https://cdn.datatables.net/plug-ins/1.10.15/features/conditionalPaging/dataTables.conditionalPaging.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/FileSaver.js/1.3.3/FileSaver.min.js" integrity="sha256-FPJJt8nA+xL4RU6/gsriA8p8xAeLGatoyTjldvQKGdE=" crossorigin="anonymous"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-toast-plugin/1.3.1/jquery.toast.min.js" integrity="sha256-jJcc8SMFEvXl7AqOTKSLOOxo9HxGbyeKZZHEzBVlXMs=" crossorigin="anonymous"></script>
	<script src="https://cdn.jsdelivr.net/npm/lodash@4.17.4/lodash.min.js" integrity="sha256-IyWBFJYclFY8Pn32bwWdSHmV4B9M5mby5bhPHEmeY8w=" crossorigin="anonymous"></script>
	<script src="https://cdn.jsdelivr.net/npm/normalizr/dist/normalizr.browser.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/vue/2.5.2/vue.js" integrity="sha256-z1if//it5+Nk8vE5T+Oj4fQFt3lzPUCZBFB8nfSzBWo=" crossorigin="anonymous"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/vuex/3.0.0/vuex.js" integrity="sha256-qRemu2NyPYJXPb/GlIdqHuQKt0UNGTA8Fpyvp2zn21I=" crossorigin="anonymous"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/uiv/0.21.4/uiv.min.js" integrity="sha256-CAAtHx0AnDoxr2ewKFpA8kEqaf98PcNPt7SgP5ML3Gg=" crossorigin="anonymous"></script>
	<script src="https://cdn.jsdelivr.net/npm/vue-multiselect@2.1.0"></script>
	<script async src="<spring:url value="/static/js/jobs.js"/>"></script>
</body>
</html>