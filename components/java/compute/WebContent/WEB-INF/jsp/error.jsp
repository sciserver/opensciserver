<%-- 
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0. 
  See LICENSE.txt in the project root for license information.
 --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../jspf/bootstrap-header.jspf" %>
<title>Error</title>
</head>
<body>
<main>
    <div class="container">
		<h1>An error has occurred...</h1>
		<div class="panel panel-primary">
			<div class="panel-body">
				<dl class="dl-horizontal">
					<dt>Time:</dt>
					<dd>${error.getTime()}</dd>
					<dt>Message:</dt>
					<dd>${error.getMessage()}</dd>
					<dt style="display: none">Stack trace:</dt>
					<dd style="display: none"><pre>${error.getStackTrace()}</pre></dd>
				</dl>
			</div>
		</div>
	</div>
</main>

<%@ include file="../jspf/footer.jspf" %>
</body>
</html>
