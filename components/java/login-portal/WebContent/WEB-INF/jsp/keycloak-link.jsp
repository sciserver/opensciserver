<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.sciserver.sso.AppConfig"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%! String appNameKeycloak = AppConfig.getInstance().getAppSettings().getApplicationName(); %>

<!DOCTYPE html>
<html lang="en">
<head>
  <%@ include file="../jspf/banner.jspf" %>
  <title>Link Accounts</title>
</head>
<body>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<div class="alert alert-warning">
You have successfully signed in with your Globus account <b>${preferredUsername}</b>. Please choose one of the two options: 
<b>a)</b> if you already have a <%= appNameKeycloak %> account, you can link it with your Globus account by entering your <%= appNameKeycloak %> credentials, or 
<b>b)</b> you can create a new <%= appNameKeycloak %> account. The new account registration form has been pre-filled with your Globus ID information 
and a random password. You may change your user name, email, and password as you like, but they have to meet <%= appNameKeycloak %> requirements and 
may not conflict with existing <%= appNameKeycloak %> accounts.
</div>
<div class="container">
<div class="row">
<c:if test="${error_message != null}">
<div class="alert alert-danger" role="alert">${error_message}</div>
</c:if>
<c:if test="${info_message != null}">
<div class="alert alert-info" role="alert">${info_message}</div>
</c:if>
<div class="col-xs-6">
	<h1>Link an existing <br><%= appNameKeycloak %> account</h1>
	<p>Please enter your <%= appNameKeycloak %> login and password.</p>
	<form:form modelAttribute="loginInfo" style="max-width:330px;">
		<div class="form-group">
			<form:label path="username">User name</form:label>
			<form:input path="username" type="text" class="form-control" id="username1"/>
		</div>
		<div class="form-group">
			<form:label path="password">Password</form:label>
			<form:input path="password" type="password" class="form-control" id="password1" />
		</div>
		<div class="row">
			<div class="col-xs-4">
				<button id="linkSubmit" type="submit" class="btn btn-success">Link</button>
			</div>
		</div>
	</form:form>
</div>
<div class="col-xs-6">
	<h1>Create a new <br><%= appNameKeycloak %> account</h1>
	<p>By registering you agree to the <a href="<%=AppConfig.getInstance().getAppSettings().getPoliciesUrl()%>" target="_blank"><%= appNameKeycloak %> Data Storage and Non-Commercial Use Policy</a>.</p>
	<form:form id="form" modelAttribute="registrationInfo" style="max-width:350px;" action="register">
		<div class="form-group">
		<form:label path="username">User name</form:label>
		<form:input path="username" type="text" class="form-control" id="username"/>
		</div>
		<div class="form-group">
			<form:label path="email">Email</form:label>
			<form:input path="email" type="email" class="form-control" id="email"/>
		</div>
		<div class="form-group" style="margin-bottom:2px;">
			<form:label path="password">Password</form:label>
			<form:input path="password" type="password" class="form-control" id="password"/>
		</div>
		<p><input type="checkbox" id="show-password">
			<label for="show-password" style="font-weight:normal;">Show password</label></p>
		<div class="form-group">
			<form:label path="confirmPassword">Confirm password</form:label>
			<form:input path="confirmPassword" type="password" class="form-control" id="confirmPassword"/>
		</div>
		<div class="row">
			<div class="col-xs-12">
				<button id="submit" type="submit" class="btn btn-success">Create account</button>
			</div>
		</div>
		<form:input path="keycloakUserId" type="hidden"/>
		<form:input path="keycloakUsername" type="hidden"/>
	</form:form>
<%@ include file="../jspf/password-policy.jspf" %>
</div>
</div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
<script>
	window.onload = function(){
		var input = document.getElementById("username").focus();
	}
</script>
<%@ include file="../jspf/validation.jspf" %>
<%@ include file="../jspf/frame-buster.jspf" %>
</body>
</html>
