<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>    
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<spring:url value="/" var="context" htmlEscape="true"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
<script type="text/javascript" src="https://cdn.datatables.net/1.10.12/js/jquery.dataTables.js"></script>
<link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.10.12/css/jquery.dataTables.min.css">
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
  	<h1>Context Class Registration</h1>
    <form:form id="form" modelAttribute="contextClassModel" style="max-width:350px;">
	  <div class="form-group">
	    <form:label path="name" for="name">Context class name</form:label>
	    <form:input path="name" type="text" class="form-control" id="name"/>
	  </div>
	  <div class="form-group">
	    <form:label path="description" for="description">Description</form:label>
	    <form:input path="description" type="text" class="form-control" id="descriptiontext"/>
	  </div>
	  <div class="form-group">
	    <form:label path="release" for="release">release</form:label>
	    <form:input path="release" type="text" class="form-control" id="release"/>
	  </div>
	  	
  	<!--% String message = (String)request.getAttribute("message");
  		if (message != null) 
  			out.write("<h1>" +message + "</h1>");
  	%-->	
  	<% String message1 = (String)request.getAttribute("message1");
  		if (message1 != null) {
  	%>
  		<div><p><%= message1 %>
  		</p></div>
  	<% 	} else {%>
   		<div><p></p></div>
    <%} %>   		  		
	  
	  <div class="row">
		  <div class="col-xs-12">
		  	<button id="submit" type="submit" class="btn btn-success">Save</button>
		  </div>
	  </div>
	</form:form>
  </div>
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="http://ajax.aspnetcdn.com/ajax/jquery.validate/1.15.0/jquery.validate.js"></script>
    <script src="<spring:url value="/static/js/bootstrap.min.js"/>"></script>
    <script src="js/jquery.validate.js"></script> 

    <script>
	    $.validator.addMethod( //this validates as user enters field
	            "regexp",
	            function(value, element, regexp) {
	                var re = new RegExp(regexp);
	                return re.test(value);
	            },
	            "Please use only [a-zA-Z0-9_]."
	    );
    
	    $('#form').validate( {  //this validation occures only after submit
			rules: {
				name: {
					required: true,
					regexp: "^[a-zA-Z0-9_]+$"
				},
				release: {
					required: true
				},
				description: "required"
			},
			messages: {
				description: "Add description"
			}
		});

		$('#submit').click(function() {
			$('#form').valid();
		});
    </script>
  </body>
</html>