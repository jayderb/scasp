# SCASP Test Record

## A. Developer build environment, 13 November 2026 (Linux, JDK 21.0.13, Maven 3.9.9, MySQL 8.4.6)
These results come from the development environment. They are **not** a substitute for the group's own run:
see section B.

| Check | Result | Evidence |
|---|---|---|
| `mvn clean package`, all 6 reactor modules | PASS (BUILD SUCCESS) | `docs/evidence/mvn-build-and-test-log.txt` |
| core JUnit (domain rules, collections, concurrency, TCP/UDP/HTTP) | PASS: 72 run, 0 failed, 0 skipped | same log |
| persistence-jdbc against real MySQL 8.4.6 (`scasp_test`) | PASS: 15 run, 0 failed, 0 skipped | same log (`[TestDatabase] MySQL 8.4.6`) |
| web-application unit tests (validation, record EL resolver, JSON writer) | PASS: 9 run | same log |
| RMI server and client in separate JVMs | PASS: 5 run | same log, `docs/evidence/rmi-evidence.txt` |
| Core demo, race condition, platform vs virtual threads, thread dump | PASS | `stage1-core-demo.txt`, `race-condition-evidence.txt`, `thread-model-comparison.txt`, `thread-dump.txt` |
| TCP message trace, UDP, InetAddress | PASS | `network-trace.txt`, `inetaddress-demo.txt` |
| WAR on **Payara Server 6.2025.10** (asadmin, `jdbc/scasp` pool, EJBs + timer) | PASS: 91/91 HTTP checks incl. 27 mobile JSON API checks (bash) | `http-test-log-payara-server.txt`, `payara-server-deployment-log.txt` |
| WAR on **Payara Micro 6.2025.10** (earlier run, before the mobile API) | PASS: 64/64 HTTP checks | `http-test-log.txt`, `payara-deployment-log.txt` |
| `scripts/http-smoke-test.ps1` (PowerShell port) | PASS: 91/91 (PowerShell 7.4.6 on Linux) | `http-test-log-powershell.txt`; not yet run on Windows PowerShell 5.1, please confirm on your laptop |
| Android `gradlew assembleDebug testDebugUnitTest` (Gradle 8.14.3, AGP 8.13.2, SDK 35) | PASS: BUILD SUCCESSFUL, 48 tests: 47 passed (JVM unit + Robolectric), 0 failed, 1 skipped (`LiveServerTest` without `SCASP_LIVE_URL`) | `android-gradle-build-and-test-log.txt`; APK `release/scasp-android-10.0-debug.apk` |
| Android HTTP client against the live server (`LiveServerTest`, JVM: login, catalogue, search, register, 409 duplicate, request 201, campus services, withdraw, logout → 401) | PASS | `android-live-server-check.txt` |
| Android on an emulator or phone (scenarios A1–F2) | **NOT RUN** | No emulator in the development environment; the group runs `docs/testing/android-test-scenarios.md` |
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
| 22 Android build + tests | | | | 36, 37 | |
| 25 Android scenarios A1–A7 (normal, map intent) | | | | 38–44 | |
| 25 B1 denied permission | | | | 45 | |
| 25 C1–C2 offline | | | | 46, 47 | |
| 25 D1 empty data | | | | 48 | |
| 25 E1 rotation | | | | 49 | |
