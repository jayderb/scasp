<%@ include file="fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Reports" scope="request"/>
<c:set var="activeNav" value="reports" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<h1>Reports</h1>
<%@ include file="fragments/messages.jspf" %>
<h2>1. Capacity</h2>
<table><thead><tr><th scope="col">Course</th><th scope="col">Enrolled</th><th scope="col">Capacity</th><th scope="col">Waiting</th><th scope="col">Seats left</th><th scope="col">Utilisation</th></tr></thead>
  <tbody><c:forEach var="r" items="${capacity}"><tr><td><c:out value="${r.courseCode}"/> <c:out value="${r.title}"/></td><td><c:out value="${r.enrolled}"/></td>
    <td><c:out value="${r.capacity}"/></td><td><c:out value="${r.waiting}"/></td><td><c:out value="${r.seatsRemaining()}"/></td>
    <td><fmt:formatNumber value="${r.utilisationPercent()}" maxFractionDigits="1"/>%</td></tr></c:forEach></tbody></table>
<h2>2. Student registrations</h2>
<c:choose><c:when test="${empty studentRegistrations}"><p class="empty">No active registrations.</p></c:when><c:otherwise>
<table><thead><tr><th scope="col">Student</th><th scope="col">Programme</th><th scope="col">Course</th><th scope="col">Registered</th></tr></thead>
  <tbody><c:forEach var="r" items="${studentRegistrations}"><tr><td><c:out value="${r.studentId}"/> <c:out value="${r.fullName}"/></td><td><c:out value="${r.programme}"/></td>
    <td><c:out value="${r.courseCode}"/> <c:out value="${r.courseTitle}"/></td><td><c:out value="${r.registeredAt}"/></td></tr></c:forEach></tbody></table>
</c:otherwise></c:choose>
<h2>3. Waiting lists</h2>
<c:choose><c:when test="${empty waiting}"><p class="empty">All waiting lists are empty.</p></c:when><c:otherwise>
<table><thead><tr><th scope="col">Course</th><th scope="col">Position</th><th scope="col">Student</th></tr></thead>
  <tbody><c:forEach var="w" items="${waiting}"><tr><td><c:out value="${w.courseCode}"/></td><td><c:out value="${w.position}"/></td><td><c:out value="${w.studentId}"/> <c:out value="${w.fullName}"/></td></tr></c:forEach></tbody></table>
</c:otherwise></c:choose>
<h2>4. Request workload</h2>
<c:choose><c:when test="${empty workload}"><p class="empty">No service requests.</p></c:when><c:otherwise>
<table><thead><tr><th scope="col">Assignee</th><th scope="col">Status</th><th scope="col">Requests</th><th scope="col">Avg priority</th></tr></thead>
  <tbody><c:forEach var="w" items="${workload}"><tr><td><c:out value="${w.assignee}"/> <c:out value="${w.assigneeName}"/></td><td><c:out value="${w.status}"/></td>
    <td><c:out value="${w.requests}"/></td><td><fmt:formatNumber value="${w.averagePriority}" maxFractionDigits="1"/></td></tr></c:forEach></tbody></table>
</c:otherwise></c:choose>
<h2>5. Audit activity by actor</h2>
<table><thead><tr><th scope="col">Actor</th><th scope="col">Role</th><th scope="col">Events</th><th scope="col">Last event</th></tr></thead>
  <tbody><c:forEach var="a" items="${auditByActor}"><tr><td><c:out value="${a.actor}"/> <c:out value="${a.actorName}"/></td><td><c:out value="${a.role}"/></td>
    <td><c:out value="${a.events}"/></td><td><c:out value="${a.lastEventAt}"/></td></tr></c:forEach></tbody></table>
<h2>Recent audit trail</h2>
<table><thead><tr><th scope="col">#</th><th scope="col">When</th><th scope="col">Actor</th><th scope="col">Action</th><th scope="col">Entity</th></tr></thead>
  <tbody><c:forEach var="e" items="${recentAudit}"><tr><td><c:out value="${e.eventId}"/></td><td><c:out value="${e.occurredAt}"/></td><td><c:out value="${e.actor}"/></td>
    <td><c:out value="${e.action}"/></td><td><c:out value="${e.entityType}"/>:<c:out value="${e.entityId}"/></td></tr></c:forEach></tbody></table>
<%@ include file="fragments/footer.jspf" %>
