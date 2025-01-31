<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/banner.jspf" %>
<title>Info</title>
</head>
<body>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<div class="container">
  <div class="row">
  <div class="center-block">
    <c:if test="${info_message != null}">
      <div class="alert alert-info" role="alert">${info_message}</div>
    </c:if>
    <h1>Welcome ${username}</h1>
    <a class="btn btn-success" href="<spring:url value="/logout"/>">Sign out</a>  
    <a class="btn btn-default" href="<spring:url value="/change-password"/>">Change password</a>  
  </div>
  </div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
</body>
</html>
