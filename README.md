# Smart Campus Academic Services Platform (SCASP)
**Course:** BoCS3100 – Advanced Java  
**Group:** Joshua Lifumbo (2024001619), Chipego Mukando (2023002262), Mumba Bupe (2023002500)

## 1. Project purpose
SCASP is an educational prototype for student and staff academic services. It demonstrates a reusable Java domain layer, collection-based business rules, concurrent request processing (bounded queue, platform and virtual threads), TCP/UDP networking, Jakarta Enterprise Beans, JDBC persistence, a Servlet/JSP web application, Java RMI administration, a CORBA migration dossier and an Android client.

## 2. Repository map (Maven multi-module build, JDK 21)
- `core/`: domain model, `AcademicService` business rules, concurrency (`RequestProcessor`, race demo, thread dump), TCP/UDP/HTTP networking, repository interfaces, PBKDF2 password hashing. JUnit 5 tests.
- `persistence-jdbc/`: JDBC repositories for MySQL 8 (prepared statements, transactions, `SELECT ... FOR UPDATE` seat locking, wait-list promotion, join reports). Integration tests run against a real MySQL test database.
- `enterprise-services/`: Jakarta EE 10 beans: stateless `RegistrationBean` (container-managed transactions), singleton `PlatformMetricsBean`, `AnnouncementTimerBean` timers.
- `web-application/`: Servlet/JSP WAR (`scasp.war`): login/logout, `AuthFilter` (session, roles, CSRF), courses, registration, waiting lists, service requests, reports, token-protected JSON API, and the session-based mobile JSON API (`/api/login`, `/api/courses`, `/api/registrations`, `/api/requests`, `/api/campus-services`) used by the Android app.
- `rmi-administration/`: remote admin interface, serializable DTOs, server and client (tested in separate JVMs).
- `database/`: `create-database.sql`, `schema.sql`, `sample-data.sql` (fictional data), `reports.sql`, `transaction-example.sql`.
- `deploy/payara/`: asadmin command files that create the `jdbc/scasp` connection pool and deploy the WAR (Payara Server 6 or Payara Micro 6).
- `scripts/`: smoke and HTTP tests as `.ps1` (Windows), `.bat` wrappers and `.sh` (Linux).
- `android-client/`: Android app (Stage 10): Kotlin, Gradle Kotlin DSL with version catalog and wrapper; see [`android-client/README.md`](android-client/README.md).
- `release/`: the debug APK built from `android-client` (`scasp-android-10.0-debug.apk`) and its SHA-256.
- `docs/`: architecture, protocol, testing, CORBA migration dossier, user guide, presentation, contribution log, evidence.

## 3. Build and test
Requirements: JDK 21, Maven 3.9+, MySQL 8.0/8.4. **Step-by-step instructions with expected output and the screenshots to take are in [`docs/testing/SCREENSHOT_TEST_GUIDE.md`](docs/testing/SCREENSHOT_TEST_GUIDE.md)** (Windows PowerShell first, with a Linux appendix).

```powershell
mysql -u root -p -e "source database/create-database.sql"     # once; edit the password in the file first
mysql -u scasp -p scasp -e "source database/schema.sql"
mysql -u scasp -p scasp -e "source database/sample-data.sql"
$env:SCASP_TEST_JDBC_URL = "jdbc:mysql://127.0.0.1:3306/scasp_test?allowPublicKeyRetrieval=true&useSSL=false"
$env:SCASP_TEST_DB_USER = "scasp"; $env:SCASP_TEST_DB_PASSWORD = "<your local password>"
mvn clean package
java -cp core\target\classes com.scasp.app.Main
```
Without `SCASP_TEST_JDBC_URL` the MySQL integration tests are reported as **skipped**, not passed.

## 4. Persistence
Repositories receive a pooled `javax.sql.DataSource` (in the server: JNDI `jdbc/scasp`). Every user value goes through `PreparedStatement`. Registration locks the course row, checks capacity, and either inserts the registration or appends to the waiting list in one transaction. Withdrawal promotes the first waiting student in the same transaction. Passwords are stored as PBKDF2-HMAC-SHA256 hashes.

## 5. Enterprise services and web application (Payara 6, Jakarta EE 10)
`RegistrationBean` runs the JDBC registration logic inside a container-managed `REQUIRED` transaction, so the JTA transaction commits or rolls back (`RegistrationRuleException` is `@ApplicationException(rollback = true)`). Deploy with Payara Server 6:

```powershell
$env:SCASP_DB_PASSWORD = "<your local password>"     # read by Payara as ${ENV=SCASP_DB_PASSWORD}
C:\tools\payara6\bin\asadmin.bat start-domain
C:\tools\payara6\bin\asadmin.bat multimode --file deploy\payara\payara-server-setup.asadmin
```
Then open http://localhost:8080/scasp. The demo logins are fictional sample-data accounts for classroom use only: `STU001`–`STU004` / `student123`, `STAFF01` / `staff123`, `ADMIN01` / `admin123`. `scripts\http-smoke-test.ps1` runs 91 HTTP checks (including 27 for the mobile JSON API) against a freshly loaded database.

## 6. RMI and networking
TCP gateway: `CampusTcpServer 5050` + `CampusTcpClient` (line protocol in `docs/architecture`). UDP: `AnnouncementUdpServer 9091` + `AnnouncementUdpClient`. RMI: `RmiServer 1099` and `RmiClient localhost 1099 STAFF01` in separate JVMs (classpath `core\target\classes;rmi-administration\target\classes`). Exact commands are in the screenshot guide.

## 7. Android
Android Studio: **File › Open › `android-client`**, let Gradle sync, create a Pixel 7 / API 35 emulator in Device Manager, then **Run 'app'**.
The app (minSdk 24, targetSdk 35) signs in to the SCASP web application at `http://10.0.2.2:8080/scasp` (the PC as seen from the emulator; changeable on the sign-in screen)
and shows the course catalogue, registrations, service requests and campus services. It uses explicit intents between screens, one implicit
`geo:` intent for maps, ViewModels/SavedStateHandle for rotation, coroutines and WorkManager for background sync, a local cache with a documented
offline policy, and asks for location only when the user taps *Show my position*. A clearly labelled built-in demo mode is used only if the
server cannot be reached.

```powershell
cd android-client
.\gradlew.bat assembleDebug testDebugUnitTest     # 47 JVM unit + Robolectric tests (+1 live-server test, skipped unless SCASP_LIVE_URL is set)
```
Guides: [`docs/user-guide/mobile-user-guide.md`](docs/user-guide/mobile-user-guide.md) (including the permission explanation),
[`docs/testing/android-test-scenarios.md`](docs/testing/android-test-scenarios.md), and Part G of the screenshot guide.

## 8. CORBA
The CORBA section is an IDL and migration dossier. The brief makes an ORB demonstration optional unless the lecturer provides an environment, because CORBA was removed from the standard JDK.

## 9. Test and evidence policy
`docs/evidence/` holds console logs captured during development. Each group member must re-run the guide on their own machine and add their own screenshots. Never report deployments, usability tests or screenshots that were not actually performed. The team must complete `docs/contribution-log-template.csv`.

## 10. Known limitations
Classroom prototype: fictional data, demo passwords, no TLS, and an in-memory `AcademicService` used by the TCP/RMI demos (the web application uses MySQL). Campus map coordinates are illustrative until the group enters verified locations. The Android app uses cleartext HTTP only for emulator/local development hosts; the APK in `release/` is a debug build.
