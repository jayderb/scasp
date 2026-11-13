#!/usr/bin/env bash
# HTTP test of the deployed SCASP web application (Stage 6/7). Requires curl.
# Usage: scripts/http-smoke-test.sh [base-url]   (default http://localhost:8080/scasp)
# Expects a freshly loaded sample database (database/schema.sql + sample-data.sql).
set -u
BASE="${1:-http://localhost:8080/scasp}"
JAR_S=$(mktemp); JAR_T=$(mktemp); JAR_X=$(mktemp); BODY=$(mktemp)
PASS=0; FAIL=0
check() { # name expected actual
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "PASS  $1 (HTTP $3)"; else FAIL=$((FAIL+1)); echo "FAIL  $1 expected $2 got $3"; fi
}
code() { curl -s -o "$BODY" -w '%{http_code}' "$@"; }
csrf() { grep -o 'name="csrf" value="[^"]*"' "$BODY" | head -1 | sed 's/.*value="//;s/"$//'; }
has() { if grep -q "$2" "$BODY"; then PASS=$((PASS+1)); echo "PASS  $1 (page contains '$2')"; else FAIL=$((FAIL+1)); echo "FAIL  $1 (missing '$2')"; fi; }

echo "== Unauthenticated access"
check "GET /courses without session redirects to login" 302 "$(code "$BASE/courses")"
check "POST /register without session is 401" 401 "$(code -X POST -d courseCode=CSC301 "$BASE/register")"
check "GET /reports without session redirects" 302 "$(code "$BASE/reports")"
check "API without token or session is 401" 401 "$(code "$BASE/api/announcements")"

echo "== Login (student)"
check "GET /login" 200 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/login")"; T=$(csrf)
check "POST /login without CSRF token is 403" 403 "$(code -c "$JAR_X" -b "$JAR_X" -d 'username=STU001&password=student123' "$BASE/login")"
check "POST /login wrong password is 401" 401 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'username=STU001&password=wrong' "$BASE/login")"
has "error message shown" "Invalid user id or password"
check "POST /login empty fields is 400" 400 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'username=&password=' "$BASE/login")"
check "POST /login STU001 redirects (PRG)" 302 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'username=STU001&password=student123' "$BASE/login")"
check "GET /dashboard as student" 200 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/dashboard")"; T=$(csrf)
has "flash welcome message" "Welcome, Alex Banda."
has "student sees announcement" "Registration reminder"

echo "== Courses and registration (student)"
check "GET /courses?q=java&sort=title" 200 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/courses?q=java&sort=title")"
has "search result" "Advanced Java"
check "XSS attempt in search is escaped" 200 "$(code -c "$JAR_S" -b "$JAR_S" --get --data-urlencode 'q=<script>alert(1)</script>' "$BASE/courses")"
has "escaped output" "&lt;script&gt;alert(1)&lt;/script&gt;"
check "POST /register CSC301 (EJB, PRG)" 302 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'action=register&courseCode=CSC301' "$BASE/register")"
check "GET /register shows result" 200 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/register")"
has "registration flash" "STU001 is registered for CSC301."
check "duplicate registration is 409" 409 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'action=register&courseCode=CSC301' "$BASE/register")"
has "duplicate message" "Duplicate registration"
check "unknown course is 404" 404 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'action=register&courseCode=NOPE99' "$BASE/register")"
check "malformed course code is 400" 400 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" --data-urlencode 'courseCode=<bad>' -d 'action=register' "$BASE/register")"
has "field-level error" "Use 2-20 letters"
check "POST without CSRF token is 403" 403 "$(code -c "$JAR_S" -b "$JAR_S" -d 'action=register&courseCode=DB302' "$BASE/register")"
check "student cannot create course (403)" 403 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'action=create&courseCode=HACK1&title=Hack&capacity=5' "$BASE/courses")"
check "student cannot open /reports (403)" 403 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/reports")"
check "student cannot open /waitlists (403)" 403 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/waitlists")"

echo "== Service requests (student)"
check "submit invalid request is 400" 400 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'action=submit&category=TRANSCRIPT&description=x&priority=9' "$BASE/requests")"
check "submit request (PRG)" 302 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" -d 'action=submit&category=TRANSCRIPT&priority=4' --data-urlencode 'description=Official transcript for bursary' "$BASE/requests")"
check "GET /requests" 200 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/requests")"
has "request listed" "TRANSCRIPT"
check "API with session" 200 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/api/announcements")"

echo "== Logout / session termination"
check "POST /logout" 302 "$(code -c "$JAR_S" -b "$JAR_S" --data-urlencode "csrf=$T" "$BASE/logout")"
check "old session no longer works" 302 "$(code -c "$JAR_S" -b "$JAR_S" "$BASE/dashboard")"

echo "== Staff"
code -c "$JAR_T" -b "$JAR_T" "$BASE/login" >/dev/null; T=$(csrf)
check "POST /login STAFF01" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'username=STAFF01&password=staff123' "$BASE/login")"
check "GET /reports as staff" 200 "$(code -c "$JAR_T" -b "$JAR_T" "$BASE/reports")"; T=$(csrf)
has "capacity report" "1. Capacity"
has "audit report shows student action" "REGISTER"
check "create course invalid capacity is 400" 400 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'action=create&courseCode=NET303&title=Network Programming&capacity=0' "$BASE/courses")"
has "field error capacity" "Must be between 1 and 1000"
check "create course (PRG)" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'action=create&courseCode=NET303&capacity=3' --data-urlencode 'title=Network Programming' "$BASE/courses")"
check "duplicate course is 409" 409 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'action=create&courseCode=NET303&capacity=3' --data-urlencode 'title=Network Programming' "$BASE/courses")"
check "staff registers STU002 for CSC301" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'action=register&studentId=STU002&courseCode=CSC301' "$BASE/register")"
check "staff registers STU003 -> waiting list" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'action=register&studentId=STU003&courseCode=CSC301' "$BASE/register")"
check "GET /register?studentId=STU003" 200 "$(code -c "$JAR_T" -b "$JAR_T" "$BASE/register?studentId=STU003")"
has "waitlist flash" "added to the waiting list"
check "GET /waitlists?course=CSC301" 200 "$(code -c "$JAR_T" -b "$JAR_T" "$BASE/waitlists?course=CSC301")"
has "STU003 waiting" "STU003"
check "GET /waitlists?course=NOPE99 is 404" 404 "$(code -c "$JAR_T" -b "$JAR_T" "$BASE/waitlists?course=NOPE99")"
check "staff withdraws STU001 -> promotes STU003" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'action=withdraw&studentId=STU001&courseCode=CSC301' "$BASE/register")"
check "GET /register?studentId=STU001" 200 "$(code -c "$JAR_T" -b "$JAR_T" "$BASE/register?studentId=STU001")"
has "promotion flash" "STU003 was promoted from the waiting list"
check "GET /requests (staff queue)" 200 "$(code -c "$JAR_T" -b "$JAR_T" "$BASE/requests")"
RID=$(grep -o 'name="requestId" value="[0-9]*"' "$BODY" | head -1 | grep -o '[0-9]*')
check "invalid transition OPEN->RESOLVED is 409" 409 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d "action=status&requestId=$RID&status=RESOLVED" "$BASE/requests")"
check "assign request to me" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d "action=assign&requestId=$RID" "$BASE/requests")"
check "status -> IN_PROGRESS" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d "action=status&requestId=$RID&status=IN_PROGRESS" "$BASE/requests")"
check "unknown request is 404" 404 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d "action=assign&requestId=999999" "$BASE/requests")"
check "publish announcement" 302 "$(code -c "$JAR_T" -b "$JAR_T" --data-urlencode "csrf=$T" -d 'audience=ALL' --data-urlencode 'title=Library hours' --data-urlencode 'body=Open until 22:00 during exams' "$BASE/announcements")"
check "missing page is 404" 404 "$(code -c "$JAR_T" -b "$JAR_T" "$BASE/no-such-page")"
echo "== API token (Stage 3 HttpResourceFetcher target)"
check "API with wrong token is 401" 401 "$(code -H 'Authorization: Bearer wrong' "$BASE/api/announcements")"
if [ -n "${SCASP_API_TOKEN:-}" ]; then
  check "API with configured token" 200 "$(code -H "Authorization: Bearer $SCASP_API_TOKEN" "$BASE/api/announcements")"
  has "API lists the new announcement" "Library hours"
fi
rm -f "$JAR_S" "$JAR_T" "$JAR_X" "$BODY"
echo "== Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
