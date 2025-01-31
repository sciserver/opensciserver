<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.sciserver.sso.AppConfig, org.sciserver.sso.AppSettings" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%! AppSettings appSettings = AppConfig.getInstance().getAppSettings(); %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/banner.jspf" %>
<title>Welcome</title>
</head>
<body>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<br>
<br>
<div class="container" style="margin-bottom: 200px;">
	<c:if test="${error_message != null}">
		<div class="alert alert-danger" role="alert">${error_message}</div>
	</c:if>
	<c:if test="${info_message != null}">
		<div class="alert alert-info" role="alert">${info_message}</div>
	</c:if>
	<div class="row" style="justify-content: center; display: flex;">	

		<!-- Disclaimer -->
		<c:if test="<%= appSettings.showDisclaimerInfo() %>">
			<div class="col-md-4">
				<%= appSettings.getDisclaimerInfo() %>
			</div>
		</c:if>

		<!-- Sciserver Login -->
		<c:if test="<%= appSettings.isDisplaySciserverLogin() %>">
			<div class="col-md-4" style="margin-top: -16px;">
				<h1>Login with <%= appSettings.getApplicationName() %></h1>
				<form:form modelAttribute="loginInfo" style="max-width:330px;">
					<div class="form-group">
					<form:label path="username">User name</form:label>
					<form:input path="username" type="text" class="form-control" id="username"/>
					</div>
					<div class="form-group">
					<form:label path="password">Password</form:label>
					<form:input path="password" type="password" class="form-control" id="password" />
					</div>

					<div class="row">
						<div class="col-xs-4">
							<button type="submit" class="btn btn-success">Sign in</button>
						</div>
						<div class="col-xs-8 text-right">
							<a href="<spring:url value="/register?{query}">
								<spring:param name="query" value='<%=request.getAttribute("queryString").toString()%>'/>
								</spring:url>">Create a new account</a>
						</div>
					</div>
					<div class="row">
						<div class="col-xs-4"></div>
						<div class="col-xs-8 text-right">
							<a href="<spring:url value="/reset-password"></spring:url>">Forgot your password?</a>
						</div>
					</div>
				</form:form>
			</div>
		</c:if>

		<!-- Default Login -->
		<div class="col-md-4">
			<c:if test="<%= appSettings.isKeycloakEnabled() %>">
				<div class="center-block">
					<div>
						<%= appSettings.getKeycloakLoginInfoText() %>
					</div>
					<br>
					<div>
						<a href="<spring:url value="/keycloak-sso?{query}"><spring:param name="query" value='<%=request.getAttribute("queryString").toString()%>'/></spring:url>" class="btn btn-primary">
							<%= appSettings.getKeycloakLoginButtonText() %>
						</a>
						<c:url var="globusLogout" value="https://auth.globus.org/v2/web/logout">
							<c:param name="redirect_uri" value="<%= appSettings.getEmailCallback()%>" />
							<c:param name="redirect_name" value="<%= appSettings.getApplicationName()%>" />
						</c:url>
						<c:if test="<%= appSettings.showGlobusSignout() %>">
							<a href="${globusLogout}" class="btn btn-danger">Sign out of Globus</a>
						</c:if>
					</div>
				</div>
			</c:if>
		</div>

	</div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
<script>
window.onload = function() {
	var input = document.getElementById("username").focus();
}
</script>
<%@ include file="../jspf/frame-buster.jspf" %>
</body>
</html>