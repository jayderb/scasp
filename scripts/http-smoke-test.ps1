# HTTP test of the deployed SCASP web application (Stage 6/7) - Windows PowerShell 5.1 or PowerShell 7.
# Uses curl.exe, which ships with Windows 10/11. Same 64 checks as scripts/http-smoke-test.sh.
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File scripts\http-smoke-test.ps1 [-Base http://localhost:8080/scasp]
# Expects a freshly loaded sample database (database/schema.sql + sample-data.sql).
# Optional: $env:SCASP_API_TOKEN = "<token configured on the server>" to also test bearer-token access.
param([string]$Base = "http://localhost:8080/scasp")

$Curl = if ($env:OS -eq 'Windows_NT') { 'curl.exe' } else { 'curl' }
$JarS = [IO.Path]::GetTempFileName(); $JarT = [IO.Path]::GetTempFileName()
$JarX = [IO.Path]::GetTempFileName(); $Body = [IO.Path]::GetTempFileName()
$script:Pass = 0; $script:Fail = 0

# Deliberately a simple (non-advanced) function: curl options such as -c/-b/-d stay in $args untouched.
function Code {
    $out = & $Curl -s -o $Body -w '%{http_code}' @args
    return "$out".Trim()
}
function Check([string]$Name, [string]$Expected, [string]$Actual) {
    if ($Expected -eq $Actual) { $script:Pass++; Write-Host "PASS  $Name (HTTP $Actual)" }
    else { $script:Fail++; Write-Host "FAIL  $Name expected $Expected got $Actual" -ForegroundColor Red }
}
function PageText { return [IO.File]::ReadAllText($Body) }
function Has([string]$Name, [string]$Text) {
    if ((PageText).Contains($Text)) { $script:Pass++; Write-Host "PASS  $Name (page contains '$Text')" }
    else { $script:Fail++; Write-Host "FAIL  $Name (missing '$Text')" -ForegroundColor Red }
}
function Csrf { $m = [regex]::Match((PageText), 'name="csrf" value="([^"]*)"'); if ($m.Success) { $m.Groups[1].Value } else { '' } }

Write-Host "== Unauthenticated access"
Check "GET /courses without session redirects to login" 302 (Code "$Base/courses")
Check "POST /register without session is 401" 401 (Code -X POST -d courseCode=CSC301 "$Base/register")
Check "GET /reports without session redirects" 302 (Code "$Base/reports")
Check "API without token or session is 401" 401 (Code "$Base/api/announcements")

Write-Host "== Login (student)"
Check "GET /login" 200 (Code -c $JarS -b $JarS "$Base/login"); $T = Csrf
Check "POST /login without CSRF token is 403" 403 (Code -c $JarX -b $JarX -d 'username=STU001&password=student123' "$Base/login")
Check "POST /login wrong password is 401" 401 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'username=STU001&password=wrong' "$Base/login")
Has "error message shown" "Invalid user id or password"
Check "POST /login empty fields is 400" 400 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'username=&password=' "$Base/login")
Check "POST /login STU001 redirects (PRG)" 302 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'username=STU001&password=student123' "$Base/login")
Check "GET /dashboard as student" 200 (Code -c $JarS -b $JarS "$Base/dashboard"); $T = Csrf
Has "flash welcome message" "Welcome, Alex Banda."
Has "student sees announcement" "Registration reminder"

Write-Host "== Courses and registration (student)"
Check "GET /courses?q=java&sort=title" 200 (Code -c $JarS -b $JarS "$Base/courses?q=java&sort=title")
Has "search result" "Advanced Java"
Check "XSS attempt in search is escaped" 200 (Code -c $JarS -b $JarS --get --data-urlencode 'q=<script>alert(1)</script>' "$Base/courses")
Has "escaped output" "&lt;script&gt;alert(1)&lt;/script&gt;"
Check "POST /register CSC301 (EJB, PRG)" 302 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'action=register&courseCode=CSC301' "$Base/register")
Check "GET /register shows result" 200 (Code -c $JarS -b $JarS "$Base/register")
Has "registration flash" "STU001 is registered for CSC301."
Check "duplicate registration is 409" 409 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'action=register&courseCode=CSC301' "$Base/register")
Has "duplicate message" "Duplicate registration"
Check "unknown course is 404" 404 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'action=register&courseCode=NOPE99' "$Base/register")
Check "malformed course code is 400" 400 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" --data-urlencode 'courseCode=<bad>' -d 'action=register' "$Base/register")
Has "field-level error" "Use 2-20 letters"
Check "POST without CSRF token is 403" 403 (Code -c $JarS -b $JarS -d 'action=register&courseCode=DB302' "$Base/register")
Check "student cannot create course (403)" 403 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'action=create&courseCode=HACK1&title=Hack&capacity=5' "$Base/courses")
Check "student cannot open /reports (403)" 403 (Code -c $JarS -b $JarS "$Base/reports")
Check "student cannot open /waitlists (403)" 403 (Code -c $JarS -b $JarS "$Base/waitlists")

Write-Host "== Service requests (student)"
Check "submit invalid request is 400" 400 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'action=submit&category=TRANSCRIPT&description=x&priority=9' "$Base/requests")
Check "submit request (PRG)" 302 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" -d 'action=submit&category=TRANSCRIPT&priority=4' --data-urlencode 'description=Official transcript for bursary' "$Base/requests")
Check "GET /requests" 200 (Code -c $JarS -b $JarS "$Base/requests")
Has "request listed" "TRANSCRIPT"
Check "API with session" 200 (Code -c $JarS -b $JarS "$Base/api/announcements")

Write-Host "== Logout / session termination"
Check "POST /logout" 302 (Code -c $JarS -b $JarS --data-urlencode "csrf=$T" "$Base/logout")
Check "old session no longer works" 302 (Code -c $JarS -b $JarS "$Base/dashboard")

Write-Host "== Staff"
$null = Code -c $JarT -b $JarT "$Base/login"; $T = Csrf
Check "POST /login STAFF01" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'username=STAFF01&password=staff123' "$Base/login")
Check "GET /reports as staff" 200 (Code -c $JarT -b $JarT "$Base/reports"); $T = Csrf
Has "capacity report" "1. Capacity"
Has "audit report shows student action" "REGISTER"
Check "create course invalid capacity is 400" 400 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'action=create&courseCode=NET303&title=Network Programming&capacity=0' "$Base/courses")
Has "field error capacity" "Must be between 1 and 1000"
Check "create course (PRG)" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'action=create&courseCode=NET303&capacity=3' --data-urlencode 'title=Network Programming' "$Base/courses")
Check "duplicate course is 409" 409 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'action=create&courseCode=NET303&capacity=3' --data-urlencode 'title=Network Programming' "$Base/courses")
Check "staff registers STU002 for CSC301" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'action=register&studentId=STU002&courseCode=CSC301' "$Base/register")
Check "staff registers STU003 -> waiting list" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'action=register&studentId=STU003&courseCode=CSC301' "$Base/register")
Check "GET /register?studentId=STU003" 200 (Code -c $JarT -b $JarT "$Base/register?studentId=STU003")
Has "waitlist flash" "added to the waiting list"
Check "GET /waitlists?course=CSC301" 200 (Code -c $JarT -b $JarT "$Base/waitlists?course=CSC301")
Has "STU003 waiting" "STU003"
Check "GET /waitlists?course=NOPE99 is 404" 404 (Code -c $JarT -b $JarT "$Base/waitlists?course=NOPE99")
Check "staff withdraws STU001 -> promotes STU003" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'action=withdraw&studentId=STU001&courseCode=CSC301' "$Base/register")
Check "GET /register?studentId=STU001" 200 (Code -c $JarT -b $JarT "$Base/register?studentId=STU001")
Has "promotion flash" "STU003 was promoted from the waiting list"
Check "GET /requests (staff queue)" 200 (Code -c $JarT -b $JarT "$Base/requests")
$m = [regex]::Match((PageText), 'name="requestId" value="([0-9]+)"'); $Rid = if ($m.Success) { $m.Groups[1].Value } else { '0' }
Check "invalid transition OPEN->RESOLVED is 409" 409 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d "action=status&requestId=$Rid&status=RESOLVED" "$Base/requests")
Check "assign request to me" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d "action=assign&requestId=$Rid" "$Base/requests")
Check "status -> IN_PROGRESS" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d "action=status&requestId=$Rid&status=IN_PROGRESS" "$Base/requests")
Check "unknown request is 404" 404 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d "action=assign&requestId=999999" "$Base/requests")
Check "publish announcement" 302 (Code -c $JarT -b $JarT --data-urlencode "csrf=$T" -d 'audience=ALL' --data-urlencode 'title=Library hours' --data-urlencode 'body=Open until 22:00 during exams' "$Base/announcements")
Check "missing page is 404" 404 (Code -c $JarT -b $JarT "$Base/no-such-page")

Write-Host "== API token (Stage 3 HttpResourceFetcher target)"
Check "API with wrong token is 401" 401 (Code -H 'Authorization: Bearer wrong' "$Base/api/announcements")
if ($env:SCASP_API_TOKEN) {
    Check "API with configured token" 200 (Code -H "Authorization: Bearer $($env:SCASP_API_TOKEN)" "$Base/api/announcements")
    Has "API lists the new announcement" "Library hours"
}
Write-Host "== Mobile JSON API (Stage 10 Android client)"
$JarA = [IO.Path]::GetTempFileName(); $JarB = [IO.Path]::GetTempFileName(); $H = 'X-SCASP-Client: test'
function JsonToken { $m = [regex]::Match((PageText), '"csrfToken":"([^"]*)"'); if ($m.Success) { $m.Groups[1].Value } else { '' } }
Check "API login without client header is 403" 403 (Code -c $JarA -b $JarA -d 'userId=STU001&password=student123' "$Base/api/login")
Check "API login wrong password is 401" 401 (Code -c $JarA -b $JarA -H $H -d 'userId=STU001&password=wrong' "$Base/api/login")
Check "API courses without session is 401" 401 (Code "$Base/api/courses")
Check "API login STU001" 200 (Code -c $JarA -b $JarA -H $H -d 'userId=STU001&password=student123' "$Base/api/login")
Has "API login returns JSON user" '"userId":"STU001"'
$AT = JsonToken
Check "API me" 200 (Code -c $JarA -b $JarA "$Base/api/me")
Check "API courses?q=java" 200 (Code -c $JarA -b $JarA "$Base/api/courses?q=java")
Has "API course JSON" '"courseCode":"CSC301"'
Check "API campus services" 200 (Code -c $JarA -b $JarA "$Base/api/campus-services")
Has "API campus service coordinates" '"name":"Main Library"'
Check "API register without CSRF header is 403" 403 (Code -c $JarA -b $JarA -d 'action=register&courseCode=DB302' "$Base/api/registrations")
Check "API register DB302" 200 (Code -c $JarA -b $JarA -H "X-CSRF-Token: $AT" -d 'action=register&courseCode=DB302' "$Base/api/registrations")
Has "API register result" '"result":"REGISTERED"'
Check "API duplicate registration is 409" 409 (Code -c $JarA -b $JarA -H "X-CSRF-Token: $AT" -d 'action=register&courseCode=DB302' "$Base/api/registrations")
Check "API student cannot read another student (gets own)" 200 (Code -c $JarA -b $JarA "$Base/api/registrations?studentId=STU002")
Has "API registrations are the caller's" '"studentId":"STU001"'
Check "API invalid request is 400" 400 (Code -c $JarA -b $JarA -H "X-CSRF-Token: $AT" -d 'category=CLEARANCE&description=x&priority=9' "$Base/api/requests")
Check "API submit request is 201" 201 (Code -c $JarA -b $JarA -H "X-CSRF-Token: $AT" -d 'category=CLEARANCE&priority=2' --data-urlencode 'description=Library clearance for graduation' "$Base/api/requests")
Check "API my requests" 200 (Code -c $JarA -b $JarA "$Base/api/requests")
Has "API request listed" '"category":"CLEARANCE"'
Check "API logout" 200 (Code -c $JarA -b $JarA -H "X-CSRF-Token: $AT" -X POST "$Base/api/logout")
Check "API session ended" 401 (Code -c $JarA -b $JarA "$Base/api/me")
Check "API login STAFF01" 200 (Code -c $JarB -b $JarB -H $H -d 'userId=STAFF01&password=staff123' "$Base/api/login")
$BT = JsonToken
Check "API staff registrations need studentId (400)" 400 (Code -c $JarB -b $JarB "$Base/api/registrations")
Check "API staff reads STU001 registrations" 200 (Code -c $JarB -b $JarB "$Base/api/registrations?studentId=STU001")
Has "API staff sees DB302" '"courseCode":"DB302"'
Check "API staff cannot submit a student request (403)" 403 (Code -c $JarB -b $JarB -H "X-CSRF-Token: $BT" -d 'category=OTHER&priority=1&description=Staff test' "$Base/api/requests")
Remove-Item -ErrorAction SilentlyContinue $JarA, $JarB
Remove-Item -ErrorAction SilentlyContinue $JarS, $JarT, $JarX, $Body
Write-Host "== Result: $($script:Pass) passed, $($script:Fail) failed"
if ($script:Fail -ne 0) { exit 1 } else { exit 0 }
