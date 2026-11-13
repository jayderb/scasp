<%@ include file="fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Dashboard" scope="request"/>
<c:set var="activeNav" value="dashboard" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<h1>Welcome, <c:out value="${me.name}"/></h1>
<%@ include file="fragments/messages.jspf" %>
<c:choose>
  <c:when test="${me.staff}">
    <section aria-labelledby="cap-h">
      <h2 id="cap-h">Course capacity</h2>
      <table>
        <caption class="sr-only">Enrolment against capacity</caption>
        <thead><tr><th scope="col">Course</th><th scope="col">Enrolled</th><th scope="col">Waiting</th><th scope="col">Utilisation</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${capacity}">
          <tr><td><c:out value="${r.courseCode}"/> <c:out value="${r.title}"/></td><td><c:out value="${r.enrolled}"/>/<c:out value="${r.capacity}"/></td>
            <td><c:out value="${r.waiting}"/></td><td><fmt:formatNumber value="${r.utilisationPercent()}" maxFractionDigits="0"/>%</td></tr>
        </c:forEach>
        </tbody>
      </table>
      <p><a href="${ctx}/requests"><c:out value="${openRequests}"/> open service request(s)</a></p>
    </section>
    <section aria-labelledby="pub-h">
      <h2 id="pub-h">Publish an announcement</h2>
      <form method="post" action="${ctx}/announcements" class="stack" novalidate>
        <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
        <label for="title">Title</label>
        <input id="title" name="title" value="${fn:escapeXml(param.title)}" maxlength="150" aria-describedby="title-err">
        <c:if test="${not empty fieldErrors.title}"><span id="title-err" class="field-error"><c:out value="${fieldErrors.title}"/></span></c:if>
        <label for="body">Message</label>
        <textarea id="body" name="body" rows="3" maxlength="2000" aria-describedby="body-err"><c:out value="${param.body}"/></textarea>
        <c:if test="${not empty fieldErrors.body}"><span id="body-err" class="field-error"><c:out value="${fieldErrors.body}"/></span></c:if>
        <label for="audience">Audience</label>
        <select id="audience" name="audience">
          <c:forEach var="a" items="${audiences}"><option value="${a}" ${param.audience == a.name() ? 'selected' : ''}><c:out value="${a}"/></option></c:forEach>
        </select>
        <c:if test="${not empty fieldErrors.audience}"><span class="field-error"><c:out value="${fieldErrors.audience}"/></span></c:if>
        <button type="submit">Publish</button>
      </form>
    </section>
  </c:when>
  <c:otherwise>
    <section aria-labelledby="reg-h">
      <h2 id="reg-h">My registrations</h2>
      <c:choose>
        <c:when test="${empty registrations}"><p class="empty">You are not registered for any course yet. <a href="${ctx}/courses">Browse courses</a>.</p></c:when>
        <c:otherwise>
          <ul><c:forEach var="r" items="${registrations}"><li><c:out value="${r.courseCode}"/> &ndash; <c:out value="${r.status}"/></li></c:forEach></ul>
        </c:otherwise>
      </c:choose>
    </section>
    <section aria-labelledby="req-h">
      <h2 id="req-h">My service requests</h2>
      <c:choose>
        <c:when test="${empty myRequests}"><p class="empty">No requests. <a href="${ctx}/requests">Raise a request</a>.</p></c:when>
        <c:otherwise>
          <ul><c:forEach var="q" items="${myRequests}"><li>#<c:out value="${q.requestId}"/> <c:out value="${q.category}"/> &ndash; <c:out value="${q.status}"/></li></c:forEach></ul>
        </c:otherwise>
      </c:choose>
    </section>
  </c:otherwise>
</c:choose>
<section aria-labelledby="ann-h">
  <h2 id="ann-h">Announcements</h2>
  <c:choose>
    <c:when test="${empty announcements}"><p class="empty">No announcements.</p></c:when>
    <c:otherwise>
      <c:forEach var="a" items="${announcements}">
        <article class="card"><h3><c:out value="${a.title}"/></h3><p><c:out value="${a.body}"/></p>
          <small><c:out value="${a.audience}"/> &middot; <c:out value="${a.publishedAt}"/></small></article>
      </c:forEach>
    </c:otherwise>
  </c:choose>
</section>
<%@ include file="fragments/footer.jspf" %>
