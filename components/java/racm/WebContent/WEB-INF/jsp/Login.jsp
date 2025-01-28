<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<spring:url value="/" var="context" htmlEscape="true"/>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <title>Welcome</title>

    <!-- Bootstrap -->
    <link href="<spring:url value="/static/css/bootstrap.min.css"/>" rel="stylesheet"> 
  </head>
  <body>

  <div class="container">
  	<h1>Welcome</h1>
    <form:form modelAttribute="loginInfo" style="max-width:330px;">
	  <div class="form-group">
	    <form:label path="username" for="username">User name</form:label>
	    <form:input path="username" type="text" class="form-control" id="username" />
	  </div>
	  <div class="form-group">
	    <form:label path="password" for="password" >Password</form:label>
	    <form:input path="password" type="password" class="form-control" id="password" />
	  </div>
	  <div class="row">
		  <div class="col-xs-4">
		  	<button type="submit" class="btn btn-success">Sign in</button>
		  </div>
		  <div class="col-xs-8 text-right">
		  	<a href="<spring:url value="/register"/>">Create a new account</a>
		  </div>
	  </div>
	</form:form>
	
  </div>
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="<spring:url value="/static/js/bootstrap.min.js"/>"></script>
  </body>
</html>