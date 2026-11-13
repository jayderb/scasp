<%@ include file="fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Sign in" scope="request"/>
<%@ include file="fragments/header.jspf" %>
<h1>Smart Campus Academic Services</h1>
<c:if test="${param.loggedOut == '1'}"><p class="notice" role="status">You have been signed out.</p></c:if>
<%@ include file="fragments/messages.jspf" %>
<form method="post" action="${ctx}/login" class="stack" novalidate>
  <input type="hidden" name="csrf" value="${fn:escapeXml(sessionScope.csrf)}">
  <label for="username">User id</label>
  <input id="username" name="username" value="${fn:escapeXml(username)}" required autocomplete="username" maxlength="30">
  <label for="password">Password</label>
  <input id="password" name="password" type="password" required autocomplete="current-password" maxlength="128">
  <button type="submit">Sign in</button>
</form>
<p class="hint">Classroom demo accounts (fictional): STU001 / student123 &middot; STAFF01 / staff123</p>
<%@ include file="fragments/footer.jspf" %>
