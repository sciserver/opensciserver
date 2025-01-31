<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
<style>
table, th, td {
  border: 1px solid black;
  border-collapse: collapse;
}
</style>
</head>
<body>
	<table>
		<tr>
			<th>Created at</th>
			<th>Name</th>
			<th>Email</th>
			<th>IP address</th>
			<th>Extra</th>
			<th></th>
			<th></th>
		</tr>
		<c:forEach var="req" items="${approvalRequests}">
			<tr>
			<td>${req.getCreatedAt()}</td>
			<td>${req.getName()}</td>
			<td>${req.getEmail()}</td>
			<td>${req.getIpAddress()}</td>
			<td>${req.getExtra()}</td>
			<td><a href="<spring:url value="/process-approval-request"/>?userId=${req.getKeystoneUserId()}&action=accept">Accept</a></td>
			<td><a href="<spring:url value="/process-approval-request"/>?userId=${req.getKeystoneUserId()}&action=reject">Reject</a></td>
			</tr>
		</c:forEach>
	</table>
</body>
</html>
