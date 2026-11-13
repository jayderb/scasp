# Android test scenarios (Stage 10)

Two levels of evidence:
* **Automated (JVM, ran in the build):** `gradlew testDebugUnitTest`. Unit and Robolectric tests, no emulator needed.
* **Manual / instrumented on an emulator:** the scenarios below. The group runs them in Android Studio and takes the
  screenshots named in `SCREENSHOT_TEST_GUIDE.md` Part G. Record PASS/FAIL in `LOCAL_SMOKE_TEST_RESULTS.md`.
  None of these emulator scenarios has been run by the development environment.

Setup for all scenarios: Payara running with a freshly loaded sample database, emulator *Pixel 7 (API 35)*,
app installed from Android Studio (**Run 'app'**), server address `http://10.0.2.2:8080/scasp`.

| ID | Scenario | Steps | Expected result | Automated test that covers the logic |
|---|---|---|---|---|
| A1 | Normal: sign in | Sign in as STU001 / student123 | Home shows *Welcome, Alex Banda (STU001, STUDENT)* and the server address | `ActivityRobolectricTest.loginWithBadInputShowsErrorThenNavigatesHomeWithAnExplicitIntent`, `ScaspRepositoryTest.loginOnline…` |
| A2 | Normal: live catalogue | Course catalogue | Three courses, banner *Live from server · updated hh:mm* | `JsonParserTest.parsesCoursesWithSeatMaths` |
| A3 | Normal: register / waitlist | Register CSC301. Then sign in as STU002 and STU003 in turn and register CSC301 | STU001/STU002 registered; STU003 *added to the waiting list* | `HttpScaspApiTest.changesSendCookieAndCsrfHeader`, server `http-smoke-test` API checks |
| A4 | Normal: withdraw + promotion | As STU001, My registrations › Withdraw CSC301; as STU003 open My registrations | STU003 now *Registered* | `GeoAndPromotionsTest.promotionDetection` |
| A5 | Normal: service request | Submit TRANSCRIPT with description "x", then a valid one | First: *Description must be 5-1000 characters* (no network call); second: a confirmation message and the request appears in the list as OPEN | `ViewModelTest.requestValidationHappensBeforeAnyNetworkCall` |
| A6 | Normal: map intent | Campus services › Open in map (Main Library) | Chooser/map app opens with a pin at -15.4167, 28.2833 labelled *Main Library* | `ActivityRobolectricTest.openInMapFiresTheImplicitGeoIntent` |
| A7 | Normal: position | Emulator Extended controls › Location: set -15.4181, 28.2819. Show my position › Allow | *You are at -15.41810, 28.28190 … Nearest service: Campus Clinic, about 15 m away* (the exact metres depend on the emulator fix) | `ViewModelTest.locationTextNamesTheNearestService` |
| B1 | **Denied permission** | Fresh install, Campus services › Show my position › **Don't allow** | Message *Location permission was denied…*; *Open in map* still works | `ActivityRobolectricTest.locationPermissionIsRequestedOnlyWhenTheUserTaps` |
| B2 | Second request, then permanent denial | After B1 tap Show my position again | Dialog *Why SCASP asks for your location* (rationale) with Continue / Not now. Continue › system dialog › **Don't allow**: Android now treats it as permanent, and the *Open app settings to allow location* button appears and opens the app's settings page | `CampusViewModel.onPermissionDenied(permanently = true)` (manual only; Robolectric covers the first request) |
| B3 | Permission not asked early | Fresh install, open every screen except tapping Show my position | No permission dialog at any time | `ActivityRobolectricTest.locationPermissionIsRequestedOnlyWhenTheUserTaps` |
| C1 | **Offline with saved data** | Open Course catalogue online. Stop Payara (`asadmin.bat stop-domain`) or turn on Airplane mode. Pull down to refresh | Banner *Offline – showing data saved at hh:mm*; same courses | `ScaspRepositoryTest.offlineShowsTheSavedCopyWithItsAge`, `ActivityRobolectricTest.offlineCachedDataShowsTheOfflineBanner` |
| C2 | Offline change | While offline tap Register | *You are offline … nothing was saved* | `ScaspRepositoryTest.changesNeedTheServerAndAreNotQueued`, `ActivityRobolectricTest.offlineWithoutDataShowsTheErrorNotAnEmptyList` |
| C3 | Offline, nothing saved, demo on | Clear app data (Settings › Apps › SCASP › Storage › Clear), server stopped, sign in STU001 | Home shows the yellow *Offline demo mode* note; lists show *Demo data* | `ScaspRepositoryTest.loginOnlineStoresUserOfflineFallsBackToDemoAccount` |
| C4 | Offline, nothing saved, demo off | Same as C3 with the demo switch off | Error *Cannot reach the SCASP server at …* | `ScaspRepositoryTest.offlineWithoutCacheUsesDemoDataOnlyWhenAllowed` |
| D1 | **Empty data** | Sign in as STU004 (no registrations) › My registrations | *You have no registrations yet…* | `ViewModelTest.emptyDataShowsEmptyState`, `ActivityRobolectricTest.emptyDataShowsTheEmptyMessage` |
| D2 | Empty search | Course catalogue › search `zzz` | *No courses match…* | `ScaspRepositoryTest.offlineSearchFiltersTheCachedCatalogue`, `ViewModelTest.emptyDataShowsEmptyState` |
| E1 | **Rotation** | Course catalogue, type `java`, Search, then rotate (Ctrl+← / rotate button) | Same list and search text; no reload spinner | `ActivityRobolectricTest.rotationKeepsListAndSearchWithoutReloading`, `ViewModelTest.coursesLoadOnceAndKeepStateForRotation`, `ViewModelTest.searchTextIsSavedInSavedStateHandle` |
| E2 | Rotation with location | After A7, rotate | Position text still shown | `CampusViewModel` keeps `location` |
| E3 | Rotation mid-form | Type a request description, rotate | Text still there | Android view state (EditText with id) |
| F1 | Session expiry | Stay idle 20+ min (or restart Payara), then refresh | Back to sign-in with *Your session has expired* | `ScaspRepositoryTest.http401ForgetsTheSession`, `ViewModelTest.offlineErrorIsShownAndSessionExpiryIsFlagged` |
| F2 | Background sync | Switch on Background sync, allow notifications; as STAFF in the web app withdraw a registered student so the waitlisted student is promoted; in Android Studio *App Inspection › Background Task Inspector* run the worker (or wait 15 min) | Notification *You got a seat* | `SyncWorkerTest`, `GeoAndPromotionsTest.promotionDetection` |
