<%@ include file="fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Waiting lists" scope="request"/>
<c:set var="activeNav" value="waitlists" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<h1>Waiting lists</h1>
<%@ include file="fragments/messages.jspf" %>
<form method="get" action="${ctx}/waitlists" class="inline">
  <label for="course">Course</label>
  <select id="course" name="course">
    <option value="">All courses</option>
    <c:forEach var="c" items="${courses}"><option value="${fn:escapeXml(c.courseCode)}" ${course == c.courseCode ? 'selected' : ''}><c:out value="${c.courseCode}"/></option></c:forEach>
  </select>
  <button type="submit">Show</button>
</form>
<c:choose>
  <c:when test="${not empty course}">
    <h2><c:out value="${course}"/></h2>
    <c:choose>
      <c:when test="${empty entries}"><p class="empty">Nobody is waiting for this course.</p></c:when>
      <c:otherwise><ol><c:forEach var="e" items="${entries}"><li><c:out value="${e.studentId}"/> <small>since <c:out value="${e.createdAt}"/></small></li></c:forEach></ol></c:otherwise>
    </c:choose>
  </c:when>
  <c:otherwise>
    <c:choose>
      <c:when test="${empty rows}"><p class="empty">All waiting lists are empty.</p></c:when>
      <c:otherwise>
        <table><thead><tr><th scope="col">Course</th><th scope="col">Position</th><th scope="col">Student</th><th scope="col">Queued at</th></tr></thead>
          <tbody><c:forEach var="w" items="${rows}"><tr><td><c:out value="${w.courseCode}"/></td><td><c:out value="${w.position}"/></td>
            <td><c:out value="${w.studentId}"/> <c:out value="${w.fullName}"/></td><td><c:out value="${w.queuedAt}"/></td></tr></c:forEach></tbody></table>
      </c:otherwise>
    </c:choose>
  </c:otherwise>
</c:choose>
<%@ include file="fragments/footer.jspf" %>
