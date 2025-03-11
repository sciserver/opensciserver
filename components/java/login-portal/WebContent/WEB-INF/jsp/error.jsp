<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.sciserver.sso.model.ErrorContent" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/banner.jspf" %>
<title>Error</title>
</head>
<body>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<div class="container">
  <div class="row">
  <div class="center-block">
    <h1>An error has occurred...</h1>
    <div class="panel panel-default">
      <div class="panel-body">
        <dl class="dl-horizontal">
          <dt>Time:</dt>
          <dd>${error.getTime()}</dd>
          <dt>Message:</dt>
          <dd><%= StringEscapeUtils.escapeHtml4(ErrorContent.class.cast(request.getAttribute("error")).getMessage()) %></dd>
          <dt style="display: none">Stack trace:</dt>
          <dd style="display: none"><pre><%= StringEscapeUtils.escapeHtml4(ErrorContent.class.cast(request.getAttribute("error")).getStackTrace()) %></pre></dd>
        </dl>
      </div>
    </div>
  </div>
  </div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>
</body>
</html>
