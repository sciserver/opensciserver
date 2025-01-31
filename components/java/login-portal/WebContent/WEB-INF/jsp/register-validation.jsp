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

<!DOCTYPE html>
<html lang="en">
<head>
  <%@ include file="../jspf/banner.jspf" %>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
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
  <p>By registering you agree to the <a href="<%=AppConfig.getInstance().getAppSettings().getPoliciesUrl()%>" target="_blank"><%= AppConfig.getInstance().getAppSettings().getApplicationName() %> Data Storage and Non-Commercial Use Policy</a>.</p>
  <form:form id="form" modelAttribute="registrationInfo" style="max-width:350px;">
    <div class="form-group">
      <form:label path="validationCode">Validation code</form:label>
      <form:input path="validationCode" type="text" class="form-control" id="validationCode"/>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <button id="submitbtn" type="submit" class="btn btn-success" onclick="hideSubmitButton();">
        	<span id="submitText">
        		Complete account creation
        	</span>
        </button>
      </div>
    </div>
  </form:form>
  </div>
  </div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
<script>
window.onload = function(){
    var input = document.getElementById("validationCode").focus();
}

function hideSubmitButton(){
    document.getElementById('form').submit()
    document.getElementById('submitbtn').disabled = true
    document.getElementById('submitText').innerHTML= '<strong>Please wait...</strong> <i id="spinner" class="fa fa-spinner fa-spin" style="font-size:20px;"></i>'
}
</script>
<%@ include file="../jspf/frame-buster.jspf" %>
</body>
</html>
