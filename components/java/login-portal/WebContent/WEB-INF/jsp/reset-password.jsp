<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
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
  <c:if test="${info_message != null}">
	<div class="alert alert-info" role="alert">${info_message}</div>
  </c:if>

  <h1>Reset password</h1>
  <form:form id="form" modelAttribute="resetPasswordInfo" style="max-width:350px;">
    <div class="form-group">
      <form:label path="username">User name</form:label>
      <form:input path="username" type="text" class="form-control" id="username"/>
      <h4>or...</h4><br>
      <form:label path="userEmail">User email</form:label>
      <form:input path="userEmail" type="text" class="form-control" id="userEmail"/>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <button id="submit" type="submit" class="btn btn-success">Submit</button>
      </div>
    </div>
  </form:form>
  </div>
  </div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
<script>
window.onload = function(){
	  var input = document.getElementById("username").focus();
}
</script>
</body>
</html>