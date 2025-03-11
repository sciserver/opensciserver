<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.sciserver.compute.Utilities" %>
<!DOCTYPE html>
<html>
<head>
<style>
table {
	border-collapse: collapse;
}

th, td {
	border: 1px solid black;
	padding-left: 5px;
	padding-right: 5px;
}

.status-ok {
	background-color: #32cd32;
	padding: 0.75em;
}

.status-error {
	background-color: #ff4500;
	padding: 0.75em;
}

.status-disabled {
	background-color: #ffd000;
	padding: 0.75em;
}

.node-name {
	vertical-align: top;
}
</style>
</head>
<body style="font-family: sans-serif; font-size: 0.875em;">
	<h2>Settings:</h2>
	<table>
		<tr>
			<td>Build</td>
			<td>${build}</td>
		</tr>
		<tr>
			<td>Maintenance mode</td>
			<td>${maintenanceMode}</td>
		</tr>
		<tr>
			<td>Cleanup enabled</td>
			<td>${cleanupEnabled}</td>
		</tr>
		<tr>
			<td>Cleanup interval</td>
			<td>${cleanupInterval}</td>
		</tr>
		<tr>
			<td>Cleanup period of inactivity</td>
			<td>${cleanupInactive}</td>
		</tr>
		<tr>
			<td>Max no. of containers per user</td>
			<td>${maxContainers}</td>
		</tr>
		<tr>
			<td>Default domain ID</td>
			<td>${defaultDomain}</td>
		</tr>
	</table>
	<p>Last cleanup: ${lastCleanup}</p>
	<h2>Node status:</h2>
	
	<% Utilities.renderHealth(out, response, Boolean.valueOf(request.getParameter("includeDisabledNodes"))); %>
</body>
</html>
