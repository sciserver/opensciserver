<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under a modified Apache 2.0 license. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%! AppSettings appSettings = AppConfig.getInstance().getAppSettings(); %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/banner.jspf" %>
<title>Info</title>
</head>
<body>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<div class="container">
    <div class="center-block">
    <div class="row">
        <p>Your registration has been submitted, but your account needs manual approval.
        Please provide us with information to help us with the approval process by filling in this form.<br/>
        Note, we will not use this information anywhere else but in our approval process and discard it afterwards.</p>
    </div>
    <div class="row">
    <form method="POST" action="extra-submit">
        <div class="form-group">
            <label for="extraFullName">Full Name</label>
            <p>Please provide your full name.</p>
            <input type="text" class="form-control" id="extraFullName" name="extraFullName">
        </div>
        <div class="form-group">
            <label for="extraAffiliation">Affiliation</label>
            <p>Please provide the name of an organization you are affiliated with, if possible an educational or research institution.</p>
            <input type="text" class="form-control" id="extraAffiliation" name="extraAffiliation">
        </div>
        <div class="form-group">
            <label for="extraComments">Comments</label>
            <p>Please provide some explanation why you want to use SciServer, taking into account our
            <a href="<%=appSettings.getPoliciesUrl()%>" target="_blank"><%= appSettings.getApplicationName() %> Data Storage and Non-Commercial Use Policy</a>.</p>
            <textarea class="form-control" id="extraComments" name="extraComments" rows="10"></textarea>
        </div>
        <button type="submit" class="btn btn-primary">Submit</button>
        <input type="hidden" name="userId" value="${userId}">
    </form>
    </div>
    </div>
</div>
<%@ include file="../jspf/bootstrap-footer.jspf" %>

</body>
</html>
