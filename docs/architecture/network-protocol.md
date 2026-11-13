# TCP gateway protocol
Transport: TCP, default port 5050. Encoding: UTF-8, one newline-terminated command per connection.

| Command | Meaning | Example response |
|---|---|---|
| PING | Health check | OK PONG |
| COURSES | Return demo course codes | OK CSC301,DB302,NET303 |
| HELP | List commands | OK PING\|COURSES\|HELP |
| ANNOUNCEMENT | Simulate announcement event | OK Announcement service received |

Malformed or unknown commands receive an ERROR response. The server uses a worker pool, socket timeout and try-with-resources cleanup. Production deployment should add authentication, TLS and structured JSON messages.

# Mobile JSON API (HTTP, used by the Android app)
Base URL: `http://<host>:8080/scasp` (emulator: `http://10.0.2.2:8080/scasp`). Implemented by `MobileApiServlet`.
Responses are `application/json; charset=UTF-8`. Errors are always `{"error": "...", "fieldErrors": {...}?}`.
Authentication uses the normal servlet session cookie (`JSESSIONID`). Every POST except login needs the header
`X-CSRF-Token` with the token returned by login. Requests are form-encoded (`application/x-www-form-urlencoded`).

| Method + path | Who | Parameters | Success | Errors |
|---|---|---|---|---|
| POST `/api/login` | anyone, header `X-SCASP-Client` required | `userId`, `password` | 200 `{userId,name,role,staff,csrfToken}`; session id is changed (fixation protection) | 400 missing fields, 401 wrong password, 403 header missing |
| POST `/api/logout` | signed in | – | 200 `{status}`, session invalidated | 401, 403 (CSRF) |
| GET `/api/me` | signed in | – | 200 the user | 401 |
| GET `/api/courses` | signed in | `q` (optional, ≤ 50 chars) | 200 `{courses:[{courseCode,title,capacity,enrolled,waiting,active}]}` | 400, 401 |
| GET `/api/registrations` | student: own; staff: `studentId` required | `studentId` (staff) | 200 `{studentId,registrations:[{courseCode,status,since}],waiting:[{courseCode,position}]}` | 400 (staff without studentId), 401 |
| POST `/api/registrations` | student: self; staff: any `studentId` | `action=register\|withdraw`, `courseCode`, `studentId` (staff) | 200 `{result: REGISTERED\|WAITLISTED\|WITHDRAWN, message, promoted?}` | 400 validation, 403, 404 unknown course, 409 duplicate |
| GET `/api/requests` | student: own; staff: open queue | – | 200 `{requests:[{requestId,studentId,category,description,priority,status,assignedTo,createdAt}]}` | 401 |
| POST `/api/requests` | students only | `category`, `description` (5–1000), `priority` (1–5) | 201 `{requestId,message}` | 400, 403 (staff or CSRF) |
| GET `/api/campus-services` | signed in | – | 200 `{services:[{serviceId,name,description,latitude,longitude}]}` | 401 |

Without a session every endpoint returns 401 JSON (no redirect to the HTML login page). These rules are
checked by the *Mobile JSON API* section of `scripts/http-smoke-test.sh` / `.ps1` (27 checks). The Android client
treats a connection failure as *offline* (cache or demo data, see `android-client/README.md`), and a 401 as *session expired*.
