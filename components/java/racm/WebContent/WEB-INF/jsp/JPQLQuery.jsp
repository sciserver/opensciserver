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
<title>JPQL Query</title>
  <script src="https://code.jquery.com/jquery-1.12.4.js"></script>
  <script src="<spring:url value="/static/js/bootstrap.min.js"/>"></script>
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
  	<h1>JPQL Query</h1>
  	Utility page for testing JPQL queries.<hr/>
    <form:form id="form" modelAttribute="jpqlQueryModel">
			<div class="form-group">
				<form:label path="jpql" for="jpql">JPQL:</form:label>
				<form:textarea path="jpql" class="form-control" id="jpql" rows="15" cols="80"/>
			</div>
			<div class="row">
				<div class="col-xs-12">
					<button id="submit" type="submit" class="btn btn-success">Submit</button>
				</div>
			</div>
<hr/>
<h3>Last result</h3>

		<c:if test="${not empty jpqlQueryModel.error}">
		<h4>Error</h4>
					${jpqlQueryModel.error}<br/><hr/>
		</c:if>
		<c:if test="${not empty jpqlQueryModel.rows}">
		<h4>DOM output</h4>
					<c:forEach var="o" items="${jpqlQueryModel.rows}">
					${o}<br/><hr/>
					</c:forEach>
		</c:if>
		<c:if test="${not empty jpqlQueryModel.xmlString}">
		<h4>State of TOM in XML</h4>
		<textarea rows="40" cols="200">
${jpqlQueryModel.xmlString}
</textarea>
					</c:if>

		</form:form>
  </div>
  

  </body>
</html>