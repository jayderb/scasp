<%@ include file="fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Courses" scope="request"/>
<c:set var="activeNav" value="courses" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<h1>Course catalogue</h1>
<%@ include file="fragments/messages.jspf" %>
<form method="get" action="${ctx}/courses" class="inline" role="search">
  <label for="q">Search</label>
  <input id="q" name="q" value="${fn:escapeXml(q)}" maxlength="50">
  <label for="sort">Sort by</label>
  <select id="sort" name="sort">
    <c:forEach var="s" items="${['code','title','capacity','status']}"><option value="${s}" ${sort == s ? 'selected' : ''}><c:out value="${s}"/></option></c:forEach>
  </select>
  <button type="submit">Apply</button>
</form>
<c:choose>
  <c:when test="${empty courses}"><p class="empty">No courses match &ldquo;<c:out value="${q}"/>&rdquo;.</p></c:when>
  <c:otherwise>
    <table>
      <thead><tr><th scope="col">Code</th><th scope="col">Course</th><th scope="col">Enrolled / capacity</th><th scope="col">Status</th><th scope="col">Action</th></tr></thead>
      <tbody>
      <c:forEach var="course" items="${courses}">
        <tr>
          <td><c:out value="${course.courseCode}"/></td>
          <td><c:out value="${course.title}"/></td>
          <td><c:out value="${enrolled[course.courseCode]}"/> / <c:out value="${course.capacity}"/></td>
          <td><c:out value="${course.status}"/></td>
          <td>
            <c:choose>
              <c:when test="${me.staff}">
                <form method="post" action="${ctx}/courses" class="inline">
                  <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
                  <input type="hidden" name="courseCode" value="${fn:escapeXml(course.courseCode)}">
                  <input type="hidden" name="action" value="${course.active ? 'deactivate' : 'activate'}">
                  <button class="secondary" type="submit">${course.active ? 'Deactivate' : 'Activate'}</button>
                </form>
              </c:when>
              <c:otherwise>
                <form method="post" action="${ctx}/register" class="inline">
                  <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
                  <input type="hidden" name="courseCode" value="${fn:escapeXml(course.courseCode)}">
                  <input type="hidden" name="action" value="register">
                  <button type="submit">Register</button>
                </form>
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
      </c:forEach>
      </tbody>
    </table>
  </c:otherwise>
</c:choose>
<c:if test="${me.staff}">
  <section aria-labelledby="new-h">
    <h2 id="new-h">Create or update a course</h2>
    <form method="post" action="${ctx}/courses" class="stack" novalidate>
      <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
      <label for="courseCode">Course code</label>
      <input id="courseCode" name="courseCode" value="${fn:escapeXml(param.courseCode)}" maxlength="20">
      <c:if test="${not empty fieldErrors.courseCode}"><span class="field-error"><c:out value="${fieldErrors.courseCode}"/></span></c:if>
      <label for="title">Title</label>
      <input id="title" name="title" value="${fn:escapeXml(param.title)}" maxlength="120">
      <c:if test="${not empty fieldErrors.title}"><span class="field-error"><c:out value="${fieldErrors.title}"/></span></c:if>
      <label for="capacity">Capacity</label>
      <input id="capacity" name="capacity" type="number" min="1" max="1000" value="${fn:escapeXml(param.capacity)}">
      <c:if test="${not empty fieldErrors.capacity}"><span class="field-error"><c:out value="${fieldErrors.capacity}"/></span></c:if>
      <div class="inline">
        <button type="submit" name="action" value="create">Create</button>
        <button type="submit" name="action" value="update" class="secondary">Update</button>
      </div>
    </form>
  </section>
</c:if>
<%@ include file="fragments/footer.jspf" %>
