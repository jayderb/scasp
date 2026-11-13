<%@ include file="fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Registration" scope="request"/>
<c:set var="activeNav" value="register" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<h1>Course registration</h1>
<%@ include file="fragments/messages.jspf" %>
<c:if test="${me.staff}">
  <form method="get" action="${ctx}/register" class="inline">
    <label for="sid">Student</label>
    <select id="sid" name="studentId">
      <c:forEach var="s" items="${students}"><option value="${fn:escapeXml(s.userId)}" ${studentId == s.userId ? 'selected' : ''}><c:out value="${s.userId}"/> &ndash; <c:out value="${s.name}"/></option></c:forEach>
    </select>
    <button type="submit">Show</button>
  </form>
</c:if>
<c:if test="${not empty studentId}">
  <h2>Registrations for <c:out value="${studentId}"/></h2>
  <c:choose>
    <c:when test="${empty registrations}"><p class="empty">No registrations yet.</p></c:when>
    <c:otherwise>
      <table>
        <thead><tr><th scope="col">Course</th><th scope="col">Status</th><th scope="col">Since</th><th scope="col">Action</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${registrations}">
          <tr><td><c:out value="${r.courseCode}"/></td><td><c:out value="${r.status}"/></td><td><c:out value="${r.createdAt}"/></td>
            <td><c:if test="${r.status == 'ACTIVE'}">
              <form method="post" action="${ctx}/register" class="inline">
                <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
                <input type="hidden" name="studentId" value="${fn:escapeXml(studentId)}">
                <input type="hidden" name="courseCode" value="${fn:escapeXml(r.courseCode)}">
                <button class="secondary" type="submit" name="action" value="withdraw">Withdraw</button>
              </form></c:if></td></tr>
        </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
  <h2>Register or leave a waiting list</h2>
  <form method="post" action="${ctx}/register" class="stack" novalidate>
    <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
    <input type="hidden" name="studentId" value="${fn:escapeXml(studentId)}">
    <label for="courseCode">Course</label>
    <select id="courseCode" name="courseCode">
      <c:forEach var="c" items="${courses}"><option value="${fn:escapeXml(c.courseCode)}"><c:out value="${c.courseCode}"/> &ndash; <c:out value="${c.title}"/></option></c:forEach>
    </select>
    <c:if test="${not empty fieldErrors.courseCode}"><span class="field-error"><c:out value="${fieldErrors.courseCode}"/></span></c:if>
    <div class="inline">
      <button type="submit" name="action" value="register">Register</button>
      <button type="submit" name="action" value="withdraw" class="secondary">Withdraw / leave waiting list</button>
    </div>
  </form>
</c:if>
<%@ include file="fragments/footer.jspf" %>
