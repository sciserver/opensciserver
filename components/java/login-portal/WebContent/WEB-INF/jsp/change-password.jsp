<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
  <h1>Change password for ${username}</h1>
  <form:form id="form" modelAttribute="passwordInfo" style="max-width:350px;">
    <div class="form-group" style="margin-bottom:2px;">
      <form:label path="password">New password</form:label>
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
        <button id="submit" type="submit" class="btn btn-success">Submit</button>
      </div>
    </div>
  </form:form>
<%@ include file="../jspf/password-policy.jspf" %>
  </div>
  </div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
<script>
  window.onload = function() {
      var input = document.getElementById("password").focus();
  }
</script>
<%@ include file="../jspf/validation.jspf" %>
<%@ include file="../jspf/frame-buster.jspf" %>
</body>
</html>