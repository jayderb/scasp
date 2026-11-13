# SCASP Test Plan and Evidence Record

## Test cases
| ID | Test | Expected result | Actual result |
|---|---|---|---|
| T01 | Add a valid student | Student stored | To be recorded during execution |
| T02 | Add duplicate course code | Duplicate rejected | To be recorded |
| T03 | Register student in available course | REGISTERED | To be recorded |
| T04 | Register same student twice | Duplicate rejected | To be recorded |
| T05 | Fill course and register another student | WAITLISTED | To be recorded |
| T06 | Withdraw enrolled student | Next eligible student promoted | To be recorded |
| T07 | Submit invalid priority request | Validation error | To be recorded |
| T08 | Submit PING to TCP gateway | OK PONG | To be recorded |
| T09 | Send unknown TCP command | ERROR response | To be recorded |
| T10 | Database registration violates FK | Statement fails and transaction rolls back | To be recorded |
| T11 | Unauthenticated web request to protected page | Redirect/401/403 | To be recorded |
| T12 | RMI server unavailable | Meaningful RemoteException message | To be recorded |
| T13 | Android location permission denied | App remains usable and explains limitation | To be recorded |
| T14 | Android offline mode | Cached/mock data or safe error shown | To be recorded |

## Evidence to capture
- Terminal output for Maven build and tests.
- Screenshots of course catalogue and registration/waitlist demonstration.
- TCP client/server request and response.
- SQL query output for reports.
- Web login and course page screenshots.
- RMI server and client terminal output.
- Android emulator screenshots for normal, denied-permission and offline states.

**Do not fill the Actual result column until the group has executed each test.**
