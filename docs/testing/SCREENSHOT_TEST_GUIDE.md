# SCASP: Run-and-Screenshot Test Guide (Windows first)

This guide is for a **fresh Windows 10/11 laptop** using **PowerShell**. A short Linux appendix is at the end.
Work through the numbered steps **in order**. Each step gives the exact command, the expected output,
and **Screenshot N**: what must be visible and the file name to save under `docs\evidence\`.

**Rules for screenshots**
* Use `Win + Shift + S` (Snipping Tool) and save as PNG with the exact file name given.
* The screenshot must show **the command you typed and its output**. Do not crop the command out.
* Record only what really happened. If a step fails, screenshot the failure and write it in
  `docs\testing\LOCAL_SMOKE_TEST_RESULTS.md`. Do not mark a failed step as PASS.

**Conventions**
* Open **PowerShell** (Start → type *PowerShell*). Unless a step says otherwise, run every command from the
  project root folder, for example `cd C:\Users\<you>\Desktop\SCASP_Advanced_Java_Project`.
* `C:\tools` is used for Maven and Payara. Create it once: `mkdir C:\tools`
* After any step that changes `Path` or `JAVA_HOME`, **close every PowerShell window and open a new one**.
* When Windows Firewall asks about `java.exe`, click **Allow** (Private networks).

Time needed: about 60–90 minutes on the first run (mostly downloads).

---

## Part A: Install the tools (once)

### 1. JDK 21 (Eclipse Temurin MSI)
1. Download **Temurin 21 (LTS), Windows x64, JDK, `.msi`** from https://adoptium.net/temurin/releases/?version=21
   (or run: `winget install EclipseAdoptium.Temurin.21.JDK`).
2. In the installer, on *Custom Setup*, set **“Set JAVA_HOME variable”** and **“Add to PATH”** to
   *Will be installed on local hard drive* (JAVA_HOME is **off** by default). Finish.
3. Open a **new** PowerShell:
   ```powershell
   java -version
   echo $env:JAVA_HOME
   ```
   Expected: `openjdk version "21.0.x" ... Temurin-21...` and a folder such as
   `C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot\`.
   If `JAVA_HOME` is empty, set it yourself (use the folder name you actually have):
   ```powershell
   dir "C:\Program Files\Eclipse Adoptium"
   [Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot", "User")
   ```

### 2. Maven 3.9 (zip + Path)
1. Download **`apache-maven-3.9.x-bin.zip`** (Binary zip archive) from https://maven.apache.org/download.cgi
2. Extract it so that you have `C:\tools\apache-maven-3.9.x\bin\mvn.cmd`:
   ```powershell
   Expand-Archive "$HOME\Downloads\apache-maven-3.9.11-bin.zip" -DestinationPath C:\tools
   ```
   (Use the version number you downloaded.)
3. Add Maven to your user `Path`:
   ```powershell
   [Environment]::SetEnvironmentVariable("Path", [Environment]::GetEnvironmentVariable("Path", "User") + ";C:\tools\apache-maven-3.9.11\bin", "User")
   ```
   (Or do it in the GUI: Start → *Edit environment variables for your account* → `Path` → *New*.)
4. Open a **new** PowerShell:
   ```powershell
   mvn -v
   ```
   Expected: `Apache Maven 3.9.x`, `Java version: 21.0.x, vendor: Eclipse Adoptium`, `runtime: C:\Program Files\Eclipse Adoptium\...`.
   If you see `JAVA_HOME environment variable is not defined correctly`, go back to step 1.

> **Screenshot 1**: `java -version`, `echo $env:JAVA_HOME` and `mvn -v` in one window → `docs\evidence\01-tools-versions.png`

### 3. MySQL 8 Server (MySQL Installer) and Workbench
1. Download **MySQL Installer for Windows** from https://dev.mysql.com/downloads/installer/
   (the larger *mysql-installer-community* `.msi`; “No thanks, just start my download”).
2. Choose **Custom** and add **MySQL Server 8.0.x** (or 8.4) and **MySQL Workbench**. Install.
3. Server configuration: *Development Computer*, port **3306**, **Use Strong Password Encryption**,
   set a **root password** (write it down), keep *Configure as a Windows Service* (name `MySQL80` or `MySQL84`).
4. Put the MySQL command-line client on `Path` (check the folder name in `C:\Program Files\MySQL`):
   ```powershell
   [Environment]::SetEnvironmentVariable("Path", [Environment]::GetEnvironmentVariable("Path", "User") + ";C:\Program Files\MySQL\MySQL Server 8.0\bin", "User")
   ```
5. In a **new** PowerShell:
   ```powershell
   mysql --version
   Get-Service MySQL*
   ```
   Expected: `mysql  Ver 8.x.x for Win64` and the service `Running`.

> **Screenshot 2**: `mysql --version` and `Get-Service MySQL*` showing *Running* → `docs\evidence\02-mysql-installed.png`

---

## Part B: Database

### 4. Create the databases and the `scasp` user
Open `database\create-database.sql` in Notepad and replace **both** `change_me_local_only` with a password you
choose (local use only; never commit or share it). Then:
```powershell
mysql -u root -p -e "source database/create-database.sql"
```
Type the **root** password. Expected: no output, no error.

### 5. Load the schema and the fictional sample data
PowerShell has no `<` redirection, so the scripts are run with `source`. Each command asks for the **scasp** password.
```powershell
mysql -u scasp -p scasp -e "source database/schema.sql"
mysql -u scasp -p scasp -e "source database/sample-data.sql"
mysql -u scasp -p scasp -e "SHOW TABLES; SELECT user_id, full_name, role FROM app_user;"
```
Expected: 9 tables (`announcement, app_user, audit_event, campus_service, courses, registrations,
service_request, students, waiting_list`) and 6 users: STU001–STU004 (STUDENT), STAFF01 (STAFF), ADMIN01 (ADMIN).

*Workbench alternative:* connect as `scasp` → *File → Open SQL Script* → choose the file → ⚡ (Execute).

> **Screenshot 3**: the `SHOW TABLES` and `app_user` output → `docs\evidence\03-mysql-schema-loaded.png`

### 6. Run the join reports
```powershell
mysql -u scasp -p scasp -e "source database/reports.sql"
```
Expected: five result tables (course capacity, student registrations, waiting list, request workload, audit by actor).
On a fresh database most counts are 0; that is correct.
> **Screenshot 4** → `docs\evidence\04-mysql-reports.png`

---

## Part C: Build and automated tests

### 7. Point the JDBC tests at the test database (same PowerShell window as step 8)
The JDBC tests **drop and recreate the tables in `scasp_test`** only (never `scasp`).
```powershell
$env:SCASP_TEST_JDBC_URL = "jdbc:mysql://127.0.0.1:3306/scasp_test?allowPublicKeyRetrieval=true&useSSL=false"
$env:SCASP_TEST_DB_USER = "scasp"
$env:SCASP_TEST_DB_PASSWORD = "your-scasp-password"
```
These variables last only for this window.

### 8. Build everything and run all tests
```powershell
mvn clean package
```
The first run downloads dependencies (needs internet, a few minutes). Expected near the end:
```
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0      (scasp-core)
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0      (scasp-persistence-jdbc)
[INFO] Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0      (scasp-web-application)
[INFO] Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0      (scasp-rmi-administration)
[INFO] Reactor Summary ... SUCCESS (6 lines)
[INFO] BUILD SUCCESS
```
In the persistence section you must see `[TestDatabase] MySQL 8.x.x`, which proves the JDBC tests used your MySQL.
`Skipped: 12` on `JdbcRepositoryIntegrationTest` means step 7 was not done in this window.
(The RMI test starts a second Java process; if Windows Firewall asks, click Allow.)

> **Screenshot 5**: the Reactor Summary and `BUILD SUCCESS` → `docs\evidence\05-mvn-build-success.png`
> **Screenshot 6**: scroll up to `[TestDatabase] MySQL ...` and `JdbcRepositoryIntegrationTest` with
> `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` → `docs\evidence\06-jdbc-tests-mysql.png`

---

## Part D: Console demonstrations
Open a second PowerShell window in the project root when a step says *Window 2*.

### 9. Stage 1: core collections demo
```powershell
java -cp core\target\classes com.scasp.app.Main
```
Expected: courses sorted by code and by title; STU001 and STU002 `REGISTERED`; STU003 and STU004 `WAITLISTED`;
after STU001 withdraws, `enrolled=[STU002, STU003]`; the request queue with priority 5 first; reports; and an
audit trail ending with lines like `19 | 2026-...T... | STAFF01 | PUBLISH_ANNOUNCEMENT | ANNOUNCEMENT:ANN001 | STUDENTS`.
> **Screenshot 7** (top part) and **8** (audit trail at the bottom) → `docs\evidence\07-core-demo.png`, `docs\evidence\08-core-audit.png`

### 10. Stage 2: race condition and fix
```powershell
java -cp core\target\classes com.scasp.concurrent.RaceConditionDemo
```
Expected: `UNSAFE` lines with `successfulTakes` above 1000 and `invariantHeld=false` (numbers change every run;
that is the race). Every `SAFE` line: `successfulTakes=1000 finalCount=1000 capacity=1000 invariantHeld=true`.
If all UNSAFE runs happen to show `true`, run the command again.
> **Screenshot 9** → `docs\evidence\09-race-condition.png`

### 11. Stage 2: platform vs virtual threads
```powershell
java -cp core\target\classes com.scasp.concurrent.ThreadModelComparison 2000
```
Expected: `PLATFORM workers=16 elapsed=` about 2500 ms, `VIRTUAL workers=per-task elapsed=` much smaller;
both `succeeded=2000 failed=0`.
> **Screenshot 10** → `docs\evidence\10-platform-vs-virtual.png`

### 12. Stage 2: thread dump
Window 1:
```powershell
java -cp core\target\classes com.scasp.concurrent.ThreadDumpDemo 120
```
It prints `READY pid=12345 stats=submitted=4 ...`. Within two minutes, in **Window 2** (use *your* pid):
```powershell
jcmd 12345 Thread.print | Out-File -Encoding utf8 docs\evidence\thread-dump-local.txt
Select-String -Path docs\evidence\thread-dump-local.txt -Pattern '"scasp-worker' -Context 0,3
```
Expected: `scasp-worker-1`, `-2`, `-3` each `WAITING (parking)` inside `CountDownLatch.await`.
Window 1 prints `Stopped. enrolled CSC301=[STU001]` after 120 s (the queued task ran after the workers were released).
> **Screenshot 11**: both windows, with the `Select-String` output visible → `docs\evidence\11-thread-dump.png`

### 13. Stage 3: TCP server and client
Window 1:
```powershell
java -cp core\target\classes com.scasp.network.CampusTcpServer 5050
```
Window 2 (one line):
```powershell
java -cp core\target\classes com.scasp.network.CampusTcpClient HELP PING COURSES "REGISTER|STU001|CSC301" "REGISTER|STU002|CSC301" "REGISTER|STU003|CSC301" "SEATS|CSC301" "REGISTER|STU001|CSC301" "REGISTER|STU001" FLY
```
(Keep the double quotes: `|` is special in PowerShell.)
Expected in Window 2: `OK PONG`, `OK REGISTERED` twice, `OK WAITLISTED`, `OK CSC301 enrolled=2 capacity=2 waiting=1`,
`ERR 409 CONFLICT Duplicate registration`, `ERR 400 MALFORMED ...`, `ERR 400 UNKNOWN_COMMAND ...`.
Window 1 shows the same exchange as `C:` (client) and `S:` (server) lines; this is the message trace.
Now press **Ctrl+C** in Window 1, and run the client again in Window 2 with just `PING`:
```powershell
java -cp core\target\classes com.scasp.network.CampusTcpClient PING
```
Expected: `SCASP gateway not reachable at localhost:5050 (...)`, a safe failure with no stack trace.
> **Screenshot 12**: both windows side by side → `docs\evidence\12-tcp-client-server.png`
> **Screenshot 13**: client after the server was stopped → `docs\evidence\13-tcp-server-down.png`

### 14. Stage 3: UDP announcement and InetAddress
Window 1: `java -cp core\target\classes com.scasp.network.AnnouncementUdpServer 9091`
Window 2: `java -cp core\target\classes com.scasp.network.AnnouncementUdpClient 127.0.0.1 9091 "Library closes at 22:00"`
Expected: Window 2 `Datagram sent to 127.0.0.1:9091`; Window 1 `127.0.0.1:<port> -> Library closes at 22:00`.
Stop Window 1 with Ctrl+C. Then:
```powershell
java -cp core\target\classes com.scasp.network.InetAddressDemo localhost no-such-host.invalid
```
Expected: `Local host: name=<your PC> ...`, `Loopback: ... isLoopback=true`, `Resolve localhost: 127.0.0.1 ...`,
`Resolve no-such-host.invalid: unknown host (handled safely)`, then your network interfaces. Some campus or hotel
networks answer every DNS name; in that case an address is shown instead, which is a network setting and not a bug.
> **Screenshot 14**: UDP, both windows → `docs\evidence\14-udp-announcement.png`
> **Screenshot 15**: InetAddress output → `docs\evidence\15-inetaddress.png`

### 15. Stage 8: RMI server and client (two separate JVMs)
On Windows the classpath separator is `;`, so keep the quotes.
Window 1:
```powershell
java -cp "core\target\classes;rmi-administration\target\classes" com.scasp.rmi.RmiServer 1099
```
Expected: `SCASP RMI server ready on port 1099 as SCASPAdmin`.
Window 2:
```powershell
java -cp "core\target\classes;rmi-administration\target\classes" com.scasp.rmi.RmiClient localhost 1099 STAFF01
```
Expected: course list, `CSC301 registrations: 2`, waiting list `[STU003]`, `CourseSummaryDTO[...]` lines, workload,
a new announcement id, recent audit DTOs, and finally
`As STU001: Request refused: Not authorised: Staff role required` (the role check works over RMI).
Press Ctrl+C in Window 1 and run the client again → `Administration server unavailable (registry not reachable): ...`.
(Optional, stronger evidence: run the server on a classmate's laptop with
`java -Djava.rmi.server.hostname=<their LAN IP> -cp "..." com.scasp.rmi.RmiServer 1099` and the client with that IP.)
> **Screenshot 16**: server and client windows → `docs\evidence\16-rmi-success.png`
> **Screenshot 17**: client after the server was stopped → `docs\evidence\17-rmi-server-down.png`

---

## Part E: Web application on Payara Server 6 (Stages 4, 6, 7)

### 16. Install Payara Server 6 and the MySQL driver
```powershell
curl.exe -L -o C:\tools\payara-6.2025.10.zip https://repo1.maven.org/maven2/fish/payara/distributions/payara/6.2025.10/payara-6.2025.10.zip
Expand-Archive C:\tools\payara-6.2025.10.zip -DestinationPath C:\tools
mvn dependency:copy "-Dartifact=com.mysql:mysql-connector-j:8.4.0" "-DoutputDirectory=C:\tools\payara6\glassfish\domains\domain1\lib"
C:\tools\payara6\bin\asadmin.bat version --local
```
(About 170 MB. The same zip is on https://www.payara.fish/downloads/ as *Payara Server Community 6.2025.10 Full*.)
Expected: `C:\tools\payara6\bin\asadmin.bat` exists, `mysql-connector-j-8.4.0.jar` is in `...\domain1\lib`, and the
version command prints `Payara Server 6.2025.10`.

### 17. Reload a clean database, start the domain, create the DataSource, deploy the WAR
Start from a clean sample database so the screens match this guide:
```powershell
mysql -u scasp -p scasp -e "source database/schema.sql"
mysql -u scasp -p scasp -e "source database/sample-data.sql"
```
Set the variables **in the same window** that starts the domain. Payara reads the DB password from the environment
through `${ENV=SCASP_DB_PASSWORD}`, so it is never written in a file:
```powershell
$env:SCASP_DB_PASSWORD = "your-scasp-password"
$env:SCASP_API_TOKEN   = "demo-token-for-local-testing"
C:\tools\payara6\bin\asadmin.bat start-domain
C:\tools\payara6\bin\asadmin.bat multimode --file deploy\payara\payara-server-setup.asadmin
```
The first asadmin command may take 20–60 s. If it asks for a user name or password, press Enter (default `admin`, empty password).
Expected:
```
Command start-domain executed successfully.
JDBC connection pool scasp-pool created successfully.
JDBC resource jdbc/scasp created successfully.
Command ping-connection-pool executed successfully.      <- proves Payara can reach MySQL
Application deployed with name scasp.
scasp  <ejb, web>
Command multimode executed successfully.
```
Show the server log lines for the EJBs and the web app:
```powershell
Select-String -Path C:\tools\payara6\glassfish\domains\domain1\logs\server.log -Pattern "Portable JNDI names for EJB","metrics singleton","SCASP web application started","successfully deployed"
```
Expected: `Portable JNDI names for EJB RegistrationBean`, `...PlatformMetricsBean`, `...AnnouncementTimerBean`,
`SCASP metrics singleton started`, `SCASP web application started ...`, `scasp was successfully deployed`.
Every 5 minutes the log also gets `SCASP heartbeat` (EJB timer).
> **Screenshot 18**: the asadmin output (create pool, ping, deploy) → `docs\evidence\18-payara-asadmin-deploy.png`
> **Screenshot 19**: the `Select-String` log lines → `docs\evidence\19-payara-server-log.png`
> **Screenshot 20**: Admin Console http://localhost:4848 → *Applications* showing `scasp` (and *Resources → JDBC → JDBC Resources* showing `jdbc/scasp`) → `docs\evidence\20-payara-admin-console.png`

*Trouble?*
* `ping-connection-pool` fails with *Access denied*: the domain was started without the right `SCASP_DB_PASSWORD`.
  Run `asadmin.bat stop-domain`, set the variable again, `start-domain`, then
  `asadmin.bat ping-connection-pool scasp-pool`.
* `create-jdbc-connection-pool ... already exists` on a second run: harmless; the deploy line still runs.
* Port 8080 already used: `netstat -ano | findstr :8080` shows the process to close.
* After changing code: `mvn -DskipTests package` then
  `C:\tools\payara6\bin\asadmin.bat deploy --force=true --contextroot=scasp --name=scasp web-application\target\scasp.war`

### 18. Browser tests (Edge/Chrome InPrivate/Incognito window)
1. Open http://localhost:8080/scasp/courses while logged out. You are redirected to the **login page**.
2. Log in as `STU001` with password `wrong`. Expected: red *Invalid user id or password*.
   > **Screenshot 21** → `docs\evidence\21-login-failed.png`
3. Log in as `STU001` / `student123`. Expected: Dashboard with *Welcome, Alex Banda.*, your registrations, and the announcement *Registration reminder*.
   > **Screenshot 22** → `docs\evidence\22-student-dashboard.png`
4. **Courses**: search `java`, sort by *title*, click **Register** on CSC301. Expected: green
   *STU001 is registered for CSC301.* Go back to **Courses** and click **Register** on CSC301 again. Expected: *Duplicate registration*.
   > **Screenshot 23**: success message → `docs\evidence\23-student-registered.png`
   > **Screenshot 24**: duplicate error → `docs\evidence\24-duplicate-registration.png`
5. **Requests**: submit with a description of `x`. Expected: a field error under *Description*. Then submit a
   TRANSCRIPT request with a real description. Expected: *Request #1 submitted.*
   > **Screenshot 25**: field-level validation error → `docs\evidence\25-validation-error.png`
6. **403 check**: type http://localhost:8080/scasp/reports in the address bar. Expected: *Access denied* page with
   *This page is for staff only.* (Error code 403).
   > **Screenshot 26** → `docs\evidence\26-student-403-reports.png`
7. **Log out**. Expected: *You have been signed out.* Press the browser **Back** button and reload. You are sent back to login.
   > **Screenshot 27** → `docs\evidence\27-logged-out.png`
8. Log in as `STAFF01` / `staff123`. Expected: staff dashboard with the capacity table and *Publish an announcement*.
   Publish one for *ALL* (for example title *Library hours*).
   > **Screenshot 28** → `docs\evidence\28-staff-dashboard.png`
9. **Registration**: choose STU002 and register CSC301; choose STU003 and register CSC301. Expected:
   *CSC301 is full: STU003 was added to the waiting list.* Open **Waiting lists** and pick CSC301: STU003 is listed.
   Back in **Registration**, choose STU001 and **Withdraw** CSC301. Expected:
   *STU001 withdrawn from CSC301. STU003 was promoted from the waiting list.*
   > **Screenshot 29**: waiting list page with STU003 → `docs\evidence\29-staff-waitlist.png`
   > **Screenshot 30**: the promotion message → `docs\evidence\30-waitlist-promotion.png`
10. **Requests**: click *Assign to me* on request #1, then set its status to IN_PROGRESS, then RESOLVED.
    > **Screenshot 31** → `docs\evidence\31-staff-requests.png`
11. **Reports**: the five reports (1. Capacity … 5. Audit activity by actor) and the recent audit trail with the actions you just did.
    > **Screenshot 32** (top) and **33** (audit section) → `docs\evidence\32-reports-top.png`, `docs\evidence\33-reports-audit.png`
12. Press F12 → device toolbar (or make the window narrow) to show the responsive layout.
    > **Screenshot 34** → `docs\evidence\34-responsive.png`

### 19. Automated HTTP test (91 checks, PowerShell port of the Linux script)
Needs a **clean** database: reload it (the two `mysql ... source` lines in step 17), then in the Payara window:
```powershell
C:\tools\payara6\bin\asadmin.bat restart-domain
powershell -ExecutionPolicy Bypass -File scripts\http-smoke-test.ps1
```
(`scripts\http-smoke-test.bat` does the same if you double-click it.) It uses `curl.exe`, which is built into Windows 10/11.
To include the API-token checks, run `$env:SCASP_API_TOKEN = "demo-token-for-local-testing"` first, the same value the domain was started with.
Expected: a list of `PASS ...` lines and finally `== Result: 91 passed, 0 failed` (89 if the token is not set). The section *Mobile JSON API* checks the endpoints the Android app uses.
> **Screenshot 35**: the end of the output → `docs\evidence\35-http-test-powershell.png`

### 20. Stop everything
```powershell
C:\tools\payara6\bin\asadmin.bat stop-domain
```
Close the demo windows with Ctrl+C. MySQL can keep running as a Windows service.

---

## Part G: Android app in Android Studio (Stage 10)
The app is in `android-client\`. It talks to the Payara server from Part E, so start it again after step 20
(`C:\tools\payara6\bin\asadmin.bat start-domain`) with a **freshly reloaded** database (step 17). Detailed manual scenarios: `docs\testing\android-test-scenarios.md`.

### 21. Install Android Studio (once)
1. Install the current stable **Android Studio** from https://developer.android.com/studio (Narwhal 3 Feature Drop
   2025.1.3 or newer; the project uses Android Gradle Plugin 8.13.2). Accept the defaults: it installs the
   Android SDK and its own JDK. No separate JDK setting is needed for Android.
2. If the setup wizard offers **Android Virtual Device** and **Hypervisor Driver / Windows Hypervisor Platform**, keep them ticked.
   If the emulator later says acceleration is unavailable, turn on *Windows features › Windows Hypervisor Platform* and restart.

### 22. Open, sync and build
1. **File › Open…** and pick the folder `android-client` (not the project root). Choose **Trust Project**.
2. Android Studio runs **Gradle Sync** by itself (bottom status bar; the first time it downloads Gradle 8.14.3 and libraries, 5–10 minutes).
   It creates `local.properties` with your SDK path. That file is personal and is ignored by git; do not commit it.
   Expected: *Sync finished* without errors. If it asks to upgrade the Android Gradle Plugin, choose **Don't ask for this project**.
3. Run the automated tests from the **Terminal** tab in Android Studio (it opens in `android-client`):
   ```powershell
   .\gradlew.bat assembleDebug testDebugUnitTest
   ```
   Expected: `BUILD SUCCESSFUL`. The HTML report is `android-client\app\build\reports\tests\testDebugUnitTest\index.html`
   (expected 48 tests, 0 failures, 1 skipped: `LiveServerTest` only runs when `SCASP_LIVE_URL` is set).
   Optional, with Payara running and a fresh database:
   `$env:SCASP_LIVE_URL = "http://localhost:8080/scasp"; .\gradlew.bat testDebugUnitTest --tests "*LiveServerTest*"` then
   `Remove-Item Env:SCASP_LIVE_URL`.
   > **Screenshot 36**: `BUILD SUCCESSFUL` in the terminal → `docs\evidence\36-android-gradle-build.png`
   > **Screenshot 37**: the test report summary page in a browser → `docs\evidence\37-android-unit-tests.png`

### 23. Create the emulator
1. **Tools › Device Manager › + › Create Virtual Device** → **Pixel 7** → system image **API 35, Google Play** (x86_64; click the
   download arrow) → Finish. Google Play images include Google Maps, which the *Open in map* step uses.
2. Press ▶ next to the device. Wait for the home screen.

### 24. Run the app
1. In the toolbar, the run configuration **app** is selected automatically (Android Studio creates it for the `app` module).
   Select the Pixel 7 emulator and click **Run 'app'** (green ▶, or Shift+F10).
2. The SCASP sign-in screen appears. Keep **Server address** `http://10.0.2.2:8080/scasp`.
   `10.0.2.2` is how the emulator reaches the Windows PC itself, so this is your local Payara.
   (A real phone needs the PC's Wi-Fi address, and the app only allows unencrypted HTTP to 10.0.2.2/localhost,
   so use the emulator for the demonstration.)

### 25. Screenshots on the emulator
Use the emulator's camera button (*Take screenshot*, it saves a PNG) or Win+Shift+S. Rotate with the rotate buttons in the emulator toolbar.

| # | Do this (details in android-test-scenarios.md) | Expected | File |
|---|---|---|---|
| 38 | Sign in STU001 / student123 (A1) | Home: *Welcome, Alex Banda (STU001, STUDENT)* | `38-android-home.png` |
| 39 | Course catalogue (A2) | 3 courses, banner *Live from server · updated …* | `39-android-courses-live.png` |
| 40 | Register CSC301 for STU001 and STU002, then STU003 (A3) | *…was added to the waiting list* | `40-android-waitlist.png` |
| 41 | STU003 › My registrations | CSC301 *Waiting list, position 1* | `41-android-registrations.png` |
| 42 | Service requests: submit with description `x`, then a valid one (A5) | Validation message, then the request in the list | `42-android-request.png` |
| 43 | Campus services › **Open in map** for Main Library (A6) | Map app with the pin (implicit intent) | `43-android-map-intent.png` |
| 44 | Extended controls (⋯) › Location: -15.4181, 28.2819 › Set location. **Show my position** › *While using the app* (A7) | Position and *Nearest service: Campus Clinic* | `44-android-location-granted.png` |
| 45 | Uninstall/reinstall (or Settings › Apps › SCASP › Permissions › Location › Don't allow), **Show my position** › **Don't allow** (B1) | *Location permission was denied…* | `45-android-permission-denied.png` |
| 46 | Course catalogue, then `C:\tools\payara6\bin\asadmin.bat stop-domain`, pull down (C1) | Banner *Offline – showing data saved at …* | `46-android-offline-cache.png` |
| 47 | Still offline, tap Register (C2) | *You are offline … nothing was saved* | `47-android-offline-change.png` |
| 48 | Start Payara again (`start-domain`), sign out, sign in STU004, My registrations (D1) | *You have no registrations yet…* | `48-android-empty.png` |
| 49 | Course catalogue, search `java`, rotate to landscape (E1) | Same filtered list and search text in landscape | `49-android-rotation.png` |

Write PASS/FAIL for each scenario in `docs\testing\LOCAL_SMOKE_TEST_RESULTS.md` (section *Android*).
Note: after scenario A3/A4 the database has changed; reload it (step 17) before running the HTTP test again.

---

## Part F: Still to do by the group (cannot be automated)
* **Usability test (Stage 7):** two people outside the group do steps 18.3–18.11. Write who (first names only),
  what confused them, and what you changed in `docs\testing\usability-test.md`.
* **Contribution log:** fill `docs\contribution-log-template.csv` (task, owner, reviewer, evidence file).
* **Git history:** commit this version and tag it, for example `git tag v11.0-final`.
* **Android (Stage 10):** follow Part G and record the results of `docs\testing\android-test-scenarios.md`.
* **Real campus coordinates:** replace the illustrative coordinates in `database\sample-data.sql` (table `campus_service`) and in `android-client\app\src\main\java\com\scasp\mobile\data\remote\MockBackend.kt` with verified NIPA Lusaka locations.
* **Results:** update `docs\testing\LOCAL_SMOKE_TEST_RESULTS.md` with *your* date, laptop, and PASS/FAIL for every step above.

---

## Appendix: Linux (Ubuntu/Debian) differences
| Step | Linux command |
|---|---|
| 1–3 | `sudo apt update && sudo apt install -y openjdk-21-jdk maven mysql-server && sudo systemctl start mysql` |
| 4 | `sudo mysql -e "source database/create-database.sql"` |
| 7 | `export SCASP_TEST_JDBC_URL="jdbc:mysql://127.0.0.1:3306/scasp_test?allowPublicKeyRetrieval=true&useSSL=false"`, `export SCASP_TEST_DB_USER=scasp`, `export SCASP_TEST_DB_PASSWORD='...'` |
| 9–15 | Use `/` instead of `\`. The RMI classpath separator is `:`, e.g. `java -cp core/target/classes:rmi-administration/target/classes com.scasp.rmi.RmiServer 1099` |
| 12 | `jcmd <pid> Thread.print > docs/evidence/thread-dump-local.txt` |
| 16 | `curl -L -o payara-6.2025.10.zip <same URL>`, `unzip payara-6.2025.10.zip -d ~/tools`, copy the driver to `~/tools/payara6/glassfish/domains/domain1/lib` |
| 17 | `export SCASP_DB_PASSWORD='...'`, `export SCASP_API_TOKEN=...`, `~/tools/payara6/bin/asadmin start-domain`, `~/tools/payara6/bin/asadmin multimode --file deploy/payara/payara-server-setup.asadmin` |
| 19 | `bash scripts/http-smoke-test.sh http://localhost:8080/scasp` |

**Payara Micro alternative (both OS, no install):** `mvn dependency:copy "-Dartifact=fish.payara.extras:payara-micro:6.2025.10" "-DoutputDirectory=deploy/lib"`, copy the MySQL driver to `deploy/lib`, then
`java -jar deploy/lib/payara-micro-6.2025.10.jar --addLibs deploy/lib/mysql-connector-j-8.4.0.jar --postbootcommandfile deploy/payara/post-boot-commands.asadmin --deploy web-application/target/scasp.war --contextroot scasp --port 8080 --nocluster`
(`post-boot-commands.asadmin` uses the same `SCASP_DB_PASSWORD` variable).
