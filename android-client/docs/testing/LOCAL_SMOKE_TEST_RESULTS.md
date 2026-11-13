# SCASP Test Record

## A. Developer build environment, 13 November 2026 (Linux, JDK 21.0.13, Maven 3.9.9, MySQL 8.4.6)
These results come from the development environment. They are **not** a substitute for the group's own run:
see section B.

| Check | Result | Evidence |
|---|---|---|
| `mvn clean package`, all 6 reactor modules | PASS (BUILD SUCCESS) | `docs/evidence/mvn-build-and-test-log.txt` |
| core JUnit (domain rules, collections, concurrency, TCP/UDP/HTTP) | PASS: 72 run, 0 failed, 0 skipped | same log |
| persistence-jdbc against real MySQL 8.4.6 (`scasp_test`) | PASS: 14 run, 0 failed, 0 skipped | same log (`[TestDatabase] MySQL 8.4.6`) |
| web-application unit tests (validation, record EL resolver) | PASS: 5 run | same log |
| RMI server and client in separate JVMs | PASS: 5 run | same log, `docs/evidence/rmi-evidence.txt` |
| Core demo, race condition, platform vs virtual threads, thread dump | PASS | `stage1-core-demo.txt`, `race-condition-evidence.txt`, `thread-model-comparison.txt`, `thread-dump.txt` |
| TCP message trace, UDP, InetAddress | PASS | `network-trace.txt`, `inetaddress-demo.txt` |
| WAR on **Payara Server 6.2025.10** (asadmin, `jdbc/scasp` pool, EJBs + timer) | PASS: 64/64 HTTP checks | `http-test-log-payara-server.txt` |
| WAR on **Payara Micro 6.2025.10** | PASS: 64/64 HTTP checks | `http-test-log.txt`, `payara-deployment-log.txt` |
| `scripts/http-smoke-test.ps1` (PowerShell port) | PASS: 64/64 (PowerShell 7 on Linux) | not yet run on Windows PowerShell 5.1; please confirm on your laptop |
| Android build/emulator | NOT RUN | Needs Android Studio |
| Usability test with two students | NOT RUN | Must be done by the group |

## B. Group member's own run (fill in)
Follow `docs/testing/SCREENSHOT_TEST_GUIDE.md` and record your results here.

| Step | Date | Laptop / OS | Result (PASS/FAIL) | Screenshot file | Notes |
|---|---|---|---|---|---|
| 1–3 tools installed | | | | 01, 02 | |
| 4–6 database | | | | 03, 04 | |
| 8 `mvn clean package` | | | | 05, 06 | |
| 9 core demo | | | | 07, 08 | |
| 10–12 concurrency | | | | 09–11 | |
| 13–14 TCP / UDP | | | | 12–15 | |
| 15 RMI | | | | 16, 17 | |
| 16–17 Payara deploy | | | | 18–20 | |
| 18 browser tests | | | | 21–34 | |
| 19 HTTP test script | | | | 35 | |
