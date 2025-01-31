<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.sciserver.sso.AppConfig, org.sciserver.sso.AppSettings"%>
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
<div class="container">
<div class="row">
<div class="center-block">
  <c:if test="${info_message != null}">
	<div class="alert alert-info" role="alert">${info_message}</div>
  </c:if>
  <h1>Registration</h1>
  <form:form id="form" modelAttribute="registrationInfo" style="max-width:350px;">
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
      <%@ include file="../jspf/password-policy.jspf" %>
    <div class="form-group">
      <form:label path="confirmPassword">Confirm password</form:label>
      <form:input path="confirmPassword" type="password" class="form-control" id="confirmPassword"/>
    </div>
    <p><input type="checkbox" required name="terms"> I have read and agree to the <a href="<%=appSettings.getPoliciesUrl()%>" target="_blank"><%= appSettings.getApplicationName() %> Data Storage and Non-Commercial Use Policy</a>.</p>
    <div class="row">
      <div class="col-xs-12">
        <button id="submit" type="submit" class="btn btn-success">Create account</button>
      </div>
    </div>
  </form:form>
  </div>
  </div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
<script>
  window.onload = function() {
      var input = document.getElementById("username").focus();
  }
</script>
<%@ include file="../jspf/validation.jspf" %>
<%@ include file="../jspf/frame-buster.jspf" %>
</body>
</html>