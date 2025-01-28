<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<title>Info</title>
</head>
<body>
<%@ include file="../jspf/navbar.jspf" %>
<main>
	<div class="container">

		<div class="panel panel-primary">
			<div class="panel-heading">Container info</div>
			<div class="panel-body">
				<dl class="dl-horizontal">
					<dt>Container ID</dt>
					<dd>${container.getId()}</dd>
					<dt>Container name</dt>
					<dd>${fn:escapeXml(container.getDisplayName())}</dd>
					<dt>Created at</dt>
					<dd>${container.getCreatedAt()}</dd>
					<dt>External Ref.</dt>
					<dd>${container.getExternalRef()}</dd>
					<dt>Docker Ref.</dt>
					<dd>${container.getDockerRef()}</dd>
					<dt>Image name</dt>
					<dd>${container.getImageName()}</dd>
					<dt>Node name</dt>
					<dd>${container.getNodeName()}</dd>
				</dl>
			</div>
		</div>

		<div class="panel panel-primary">
			<div class="panel-heading">Actions</div>
			<div class="panel-body">
				<ul class="menu-list">
					<li><a href="<spring:url value="/start"/>?id=${container.getId()}">Start</a></li>
					<li><a href="<spring:url value="/stop"/>?id=${container.getId()}">Stop</a></li>
				</ul>
			</div>
		</div>

		<div class="panel panel-primary">
			<div class="panel-heading">JSON</div>
			<div class="panel-body">
				<pre>${json}</pre>
			</div>
		</div>

	</div>
</main>

<%@ include file="../jspf/footer.jspf" %>
</body>
</html>