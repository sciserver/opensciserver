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
    
    <style>
    	label.error {
    		font-weight: normal;
    		color: red;
    	}
    </style>
  </head>
  <body>
  <div class="container">
  	<h1>Registration</h1>
    <form:form id="form" modelAttribute="registrationInfo" style="max-width:350px;">
	  <div class="form-group">
	    <form:label path="username" for="username">User name</form:label>
	    <form:input path="username" type="text" class="form-control" id="username"/>
	  </div>
	  <div class="form-group">
	    <form:label path="email" for="email">Email</form:label>
	    <form:input path="email" type="email" class="form-control" id="email"/>
	  </div>
	  <div class="form-group">
	    <form:label path="password" for="password">Password</form:label>
	    <form:input path="password" type="password" class="form-control" id="password"/>
	  </div>
	  <div class="form-group">
	    <form:label path="confirmPassword" for="confirmPassword">Confirm password</form:label>
	    <form:input path="confirmPassword" type="password" class="form-control" id="confirmPassword"/>
	  </div>
	  <div class="row">
		  <div class="col-xs-12">
		  	<button id="submit" type="submit" class="btn btn-success">Create account</button>
		  </div>
	  </div>
	</form:form>
  </div>
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/jquery.validate.js"></script>
    <script src="<spring:url value="/static/js/bootstrap.min.js"/>"></script>

    <script>
	    $.validator.addMethod(
	            "regexp",
	            function(value, element, regexp) {
	                var re = new RegExp(regexp);
	                return re.test(value);
	            },
	            "Please use only [a-zA-Z0-9_]."
	    );
    
	    $('#form').validate( {
			rules: {
				username: {
					required: true,
					regexp: "^[a-zA-Z0-9_]+$"
				},
				email: {
					required: true
				},
				password: {
					required: true,
					minlength: 5
				},
				confirmPassword: {
					required: true,
					minlength: 5,
					equalTo: "#password"
				}
			}
		});

		$('#submit').click(function() {
			$('#form').valid();
		});
    </script>
  </body>
</html>