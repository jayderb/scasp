# SCASP Android client (Stage 10, version 10.0)

Kotlin Android app for students: sign in, course catalogue and registration, my registrations and waiting-list
places, service requests, and campus services on a map. It talks to the SCASP web application (Payara) through
the JSON API in `web-application/.../MobileApiServlet.java`.

* Open in Android Studio: **File › Open › `android-client`** (this folder, not the repository root). Gradle syncs,
  then the **app** run configuration appears. Android Studio creates it automatically for the application module.
* Command line: `gradlew.bat assembleDebug testDebugUnitTest` (Windows) or `./gradlew assembleDebug testDebugUnitTest`.
* User guide: [`docs/user-guide/mobile-user-guide.md`](../docs/user-guide/mobile-user-guide.md).
  Test scenarios: [`docs/testing/android-test-scenarios.md`](../docs/testing/android-test-scenarios.md).

## Build setup
| Item | Value | Why |
|---|---|---|
| Gradle | 8.14.3 (wrapper: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`) | Pinned so every machine builds the same way |
| Android Gradle Plugin | 8.13.2, Kotlin 2.2.21 (`gradle/libs.versions.toml`) | Stable pair that current Android Studio opens without migration |
| compileSdk / targetSdk | 35 (Android 15) | Current Play requirement |
| **minSdk** | **24 (Android 7.0)** | Runs on almost all active Android phones; has runtime permissions (API 23+) and network security config (API 24+); supported by every AndroidX library used. Lower versions would need extra compatibility code for no real benefit to students |
| JDK | 17+ (Android Studio's bundled JDK 21 is fine) | Required by AGP 8.x |

No `local.properties` or machine paths are committed. Android Studio writes `local.properties` (SDK path) on the first sync.

## Package structure (`com.scasp.mobile`)
```
ScaspApp                    Application object: creates settings, session, cache, repository (simple service locator)
data/
  CampusRepository          interface used by all screens (faked in tests)
  ScaspRepository           network-first, then cache, then demo data (offline policy below)
  model/Models.kt           Course, RegistrationItem, ServiceRequestItem, CampusServiceItem, UserSession, Origin
  remote/ScaspApi           API contract; HttpScaspApi = HttpURLConnection client on Dispatchers.IO
  remote/JsonParser         org.json parsing of the server responses
  remote/MockBackend        controlled mock (same data as database/sample-data.sql), used only offline
  local/LocalCache          SharedPreferences cache of the last JSON per screen + time saved
  local/SessionStore        user, session cookie, CSRF token (never the password)
  local/Settings            server address, demo fallback switch, background sync switch
ui/login, ui/home, ui/courses, ui/registrations, ui/requests, ui/campus   Activity + ViewModel per screen
ui/common                   BaseActivity, ListViewModel (StateFlow), RowAdapter, UiText
work/SyncWorker             WorkManager periodic sync + "you got a seat" notification
util/Geo, util/Promotions   distance, geo: URI, waiting-list promotion detection
```

## Stage 10 requirements, where they are met
| # | Requirement | Implementation |
|---|---|---|
| 1 | Project, package structure, minSdk | This folder; minSdk 24 justified above |
| 2 | Login, catalogue, registrations, requests, campus services | `LoginActivity`, `CoursesActivity`, `RegistrationsActivity`, `RequestsActivity`, `CampusServicesActivity` (+ `HomeActivity`) |
| 3 | Explicit intents + one justified implicit intent | All navigation uses `Intent(this, X::class.java)`. The implicit intent is **ACTION_VIEW `geo:lat,lon?q=lat,lon(Name)`** on *Open in map*: the user's own map app shows the pin and directions, so the app needs no map SDK, no API key, and no location permission for it. If no map app is installed it falls back to a browser URL |
| 4 | Lifecycle and UI state | ViewModels keep lists, banners and location text across rotation; the course search text is in `SavedStateHandle` (survives process death); typed text is kept by view state; location updates are cancelled in `onStop` |
| 5 | HTTP endpoint or controlled mock | `/api/login`, `/api/me`, `/api/logout`, `/api/courses`, `/api/registrations`, `/api/requests`, `/api/campus-services` (session cookie + `X-CSRF-Token`). `MockBackend` is used only when the server is unreachable and the user allowed demo data. The screen then says **"Demo data"** |
| 6 | Background work, main thread never blocked | Coroutines (`viewModelScope`, `Dispatchers.IO` in `HttpScaspApi`), 5 s connect / 10 s read timeouts. `SyncWorker` (WorkManager, every 15 min, network required) refreshes the cache and notifies when a waiting-list place becomes a registration |
| 7 | Local cache + offline/refresh behaviour | See below |
| 8 | Runtime location permission only when needed | Requested only when *Show my position* is tapped (with a rationale dialog when Android asks for one). Denial keeps everything else working and offers *Open app settings* after a permanent denial |
| 9 | Position / map for ≥ 3 campus services | Three services (Main Library, Registry, Campus Clinic) each have *Open in map*; *Show my position* shows your coordinates and the nearest service with its distance |
| 10 | Tests: normal, denied permission, offline, empty data, configuration change | JVM + Robolectric tests in `app/src/test` (below); manual emulator scenarios in `docs/testing/android-test-scenarios.md` |

## Offline and refresh behaviour
1. **Network first.** Each screen loads from the server when opened and on pull-to-refresh. A successful answer is
   shown with the banner *"Live from server · updated 14:03"* and saved in the cache.
2. **Server unreachable:** the last saved copy is shown with *"Offline – showing data saved at 13:55. Pull down to retry."*
   An offline course search filters the saved catalogue.
3. **Unreachable and nothing saved:** the built-in demo data is shown with a *"Demo data"* banner, but only if
   *"Use built-in demo data when the server cannot be reached"* is on (login screen). Otherwise an error is shown.
4. **Changes** (register, withdraw, new request) always need the server. Offline they fail with
   *"You are offline … nothing was saved"*. Nothing is queued, so nothing is sent later without the user knowing.
5. **Session expiry** (HTTP 401) clears the saved session and returns to the login screen.
6. **Logout** clears the session and the user's personal cache (registrations, requests). The shared course catalogue
   and campus services stay cached.

## Server address and cleartext HTTP
* Default: `http://10.0.2.2:8080/scasp`. `10.0.2.2` is how the emulator reaches the PC running Payara.
  You can change it on the login screen.
* `res/xml/network_security_config.xml` allows plain HTTP **only** to `10.0.2.2`, `localhost` and `127.0.0.1`.
  Every other host must use HTTPS.
* Real phone on USB: run `adb reverse tcp:8080 tcp:8080` and use `http://127.0.0.1:8080/scasp`.

## Campus coordinates are illustrative
`database/sample-data.sql` (table `campus_service`) and `MockBackend.campusServices` contain **illustrative**
coordinates near Lusaka. Before the demonstration the group should replace them with verified NIPA Lusaka campus
locations, for example by right-clicking a building in Google Maps and copying the latitude and longitude. Update both places,
reload the database, and keep the description honest.

## Tests (`app/src/test`, run with `gradlew testDebugUnitTest`)
* `JsonParserTest`: contract tests using JSON captured from the real deployed server (`src/test/resources/fixtures`).
* `HttpScaspApiTest`: MockWebServer: form encoding, `X-SCASP-Client`, session cookie, `X-CSRF-Token`, 401/409/302 mapping, unreachable server.
* `ScaspRepositoryTest`: offline policy: live→cache, cache age, offline search, demo fallback on/off, no queued changes, 401 clears session, logout.
* `LocalStoreTest`, `GeoAndPromotionsTest`: cache, session, settings URL validation, haversine, geo: URI, promotion detection.
* `ViewModelTest`: loading/empty/error/session-expired states, load-once on rotation, `SavedStateHandle`, validation before network.
* `ActivityRobolectricTest`: real Activities on the JVM: explicit intents, rotation (`recreate()`), offline banner,
  offline error, empty state, **permission requested only on tap**, **denied permission message**, implicit geo: intent.
* `SyncWorkerTest`: worker refreshes when signed in, skips when signed out, retries when offline.
* `LiveServerTest` (optional): the real `HttpScaspApi` against a running SCASP server with a fresh database. Skipped
  unless `SCASP_LIVE_URL` is set (`$env:SCASP_LIVE_URL = "http://localhost:8080/scasp"` on Windows).

Result in the development environment (13 Nov 2026, 14:33 SAST): 48 tests, 47 passed, 1 skipped (LiveServerTest), 0 failures;
`LiveServerTest` run separately against Payara Server 6: passed (`docs/evidence/android-live-server-check.txt`).
No emulator or physical device was used there: the emulator scenarios are for the group.
