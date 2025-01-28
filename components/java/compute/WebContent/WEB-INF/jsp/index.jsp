<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<title>Welcome</title>
</head>
<body>
	<%@ include file="../jspf/navbar.jspf" %>
	<main>
    <spring:eval expression="T(org.sciserver.compute.AppConfig).getInstance().getAppSettings().getApplicationName()" var="applicationName"/>
	<div class="container" style="margin-top: 36px">
		<h3>Welcome to ${applicationName} Compute!</h3>
		<p>Personal Jupyter notebooks in Python and R.</p>
		<div>
			<a class="btn btn-success" href="<spring:url value="/login"/>">Sign in</a>
		</div>
	</div>
	</main>
<%@ include file="../jspf/footer.jspf" %>
</body>
</html>
