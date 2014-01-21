<%@ page language="java" pageEncoding="UTF-8"%>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ page session="false"%>
<html>
<head>
<title><spring:message code="app.title" /></title>
<link href="<c:url value="/resources/styles/styles.css" />"
	rel="stylesheet">
<script type="text/JavaScript">
	function refreshPage() {
		setTimeout("location.reload(true);", 3000);
	}
</script>
</head>
<body onload="JavaScript:refreshPage();">
	<p>
		<spring:message code="line.state" />
		<c:choose>
			<c:when test="${lineState == 'running'}">
				<spring:message code="state.running" />
			</c:when>
			<c:otherwise>
				<spring:message code="state.stopped" />
			</c:otherwise>
		</c:choose>
	</p>
	<p>
		<spring:message code="number.of.parcels" />
		${numParcels}
	</p>
	<form action="./ctrl" method="post">
		<table>
			<tr>
				<td><button class="startButton" type="submit" name="action"
						value="start" ${(lineState == 'running') ? 'disabled' : ''}>
						<spring:message code="button.start" />
					</button></td>
				<td><button class="stopButton" type="submit" name="action"
						value="stop" ${(lineState == 'running') ? '' : 'disabled'}>
						<spring:message code="button.stop" />
					</button></td>
			</tr>
		</table>
	</form>
</body>
</html>
