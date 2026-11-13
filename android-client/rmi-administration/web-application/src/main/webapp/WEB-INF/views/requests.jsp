<%@ include file="fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Service requests" scope="request"/>
<c:set var="activeNav" value="requests" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<h1>Academic service requests</h1>
<%@ include file="fragments/messages.jspf" %>
<c:choose>
  <c:when test="${me.staff}">
    <h2>Open requests (highest priority first)</h2>
    <c:choose>
      <c:when test="${empty queue}"><p class="empty">No open requests.</p></c:when>
      <c:otherwise>
        <table>
          <thead><tr><th scope="col">#</th><th scope="col">Student</th><th scope="col">Category</th><th scope="col">Priority</th><th scope="col">Status</th><th scope="col">Assigned</th><th scope="col">Action</th></tr></thead>
          <tbody>
          <c:forEach var="q" items="${queue}">
            <tr><td><c:out value="${q.requestId}"/></td><td><c:out value="${q.studentId}"/></td><td><c:out value="${q.category}"/><br><small><c:out value="${q.description}"/></small></td>
              <td><c:out value="${q.priority}"/></td><td><c:out value="${q.status}"/></td><td><c:out value="${q.assignedTo}"/></td>
              <td>
                <c:if test="${q.status == 'OPEN'}">
                  <form method="post" action="${ctx}/requests" class="inline">
                    <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
                    <input type="hidden" name="requestId" value="${fn:escapeXml(q.requestId)}">
                    <button type="submit" name="action" value="assign">Assign to me</button>
                  </form>
                </c:if>
                <form method="post" action="${ctx}/requests" class="inline">
                  <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
                  <input type="hidden" name="requestId" value="${fn:escapeXml(q.requestId)}">
                  <input type="hidden" name="action" value="status">
                  <label class="sr-only" for="st${q.requestId}">New status</label>
                  <select id="st${q.requestId}" name="status"><c:forEach var="s" items="${statuses}"><option value="${s}"><c:out value="${s}"/></option></c:forEach></select>
                  <button class="secondary" type="submit">Update</button>
                </form>
              </td></tr>
          </c:forEach>
          </tbody>
        </table>
      </c:otherwise>
    </c:choose>
  </c:when>
  <c:otherwise>
    <h2>Raise a request</h2>
    <form method="post" action="${ctx}/requests" class="stack" novalidate>
      <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
      <input type="hidden" name="action" value="submit">
      <label for="category">Category</label>
      <select id="category" name="category"><c:forEach var="cat" items="${categories}"><option value="${cat}" ${param.category == cat ? 'selected' : ''}><c:out value="${cat}"/></option></c:forEach></select>
      <c:if test="${not empty fieldErrors.category}"><span class="field-error"><c:out value="${fieldErrors.category}"/></span></c:if>
      <label for="description">Description</label>
      <textarea id="description" name="description" rows="3" maxlength="1000"><c:out value="${param.description}"/></textarea>
      <c:if test="${not empty fieldErrors.description}"><span class="field-error"><c:out value="${fieldErrors.description}"/></span></c:if>
      <label for="priority">Priority (1 low - 5 urgent)</label>
      <input id="priority" name="priority" type="number" min="1" max="5" value="${empty param.priority ? '3' : fn:escapeXml(param.priority)}">
      <c:if test="${not empty fieldErrors.priority}"><span class="field-error"><c:out value="${fieldErrors.priority}"/></span></c:if>
      <button type="submit">Submit request</button>
    </form>
    <h2>My requests</h2>
    <c:choose>
      <c:when test="${empty myRequests}"><p class="empty">You have not raised any requests.</p></c:when>
      <c:otherwise>
        <table><thead><tr><th scope="col">#</th><th scope="col">Category</th><th scope="col">Priority</th><th scope="col">Status</th><th scope="col">Assigned to</th></tr></thead>
          <tbody><c:forEach var="q" items="${myRequests}"><tr><td><c:out value="${q.requestId}"/></td><td><c:out value="${q.category}"/></td><td><c:out value="${q.priority}"/></td>
            <td><c:out value="${q.status}"/></td><td><c:out value="${empty q.assignedTo ? '-' : q.assignedTo}"/></td></tr></c:forEach></tbody></table>
      </c:otherwise>
    </c:choose>
  </c:otherwise>
</c:choose>
<%@ include file="fragments/footer.jspf" %>
