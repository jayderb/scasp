<%@ include file="fragments/taglibs.jspf" %>
<%@ page isErrorPage="true" %>
<c:set var="pageTitle" value="Error" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<h1>
  <c:choose>
    <c:when test="${status == 400}">Invalid request</c:when>
    <c:when test="${status == 401}">Please sign in</c:when>
    <c:when test="${status == 403}">Access denied</c:when>
    <c:when test="${status == 404}">Not found</c:when>
    <c:otherwise>Something went wrong</c:otherwise>
  </c:choose>
</h1>
<p class="error" role="alert"><c:out value="${requestScope['jakarta.servlet.error.message']}"/></p>
<p>Error code <c:out value="${status}"/>. <a href="${ctx}/dashboard">Back to the dashboard</a> or <a href="${ctx}/login">sign in</a>.</p>
<%@ include file="fragments/footer.jspf" %>
