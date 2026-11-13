# SCASP mobile app – user guide (Android)

The SCASP app lets a student check courses, register or join a waiting list, see their registrations, submit
service requests, and find campus services on a map. Staff can browse courses and the open request queue.
All data is fictional classroom sample data.

## 1. Signing in
1. Open **SCASP**. The sign-in screen shows three fields:
   * **Server address**: on the Android emulator keep `http://10.0.2.2:8080/scasp`. That is the PC running the SCASP server.
   * **User id** and **Password**: demo accounts: `STU001`–`STU004` / `student123`, `STAFF01` / `staff123`.
2. **Use built-in demo data when the server cannot be reached**: if this is on and the server is down, you can
   still sign in with a demo account and explore. A yellow *Demo data* note is shown and nothing is sent to the server.
3. Tap **Sign in**. You stay signed in until you sign out or the server session expires (20 minutes without use).
   You then return to this screen with *"Your session has expired"*.

## 2. Home
Shows who you are and which server you use, with buttons for every screen.
* **Background sync**: when switched on, the app checks the server every 15 minutes (only with a connection).
  If a waiting-list place turns into a registration, you get a notification *"You got a seat"*. On Android 13
  and newer the app asks for permission to send notifications at this point, and only then. Sync still updates the saved data if you say no.
* **Sign out** ends the session and removes your personal saved data from the phone.

## 3. Course catalogue
* Type in **Search** (code or title) and tap **Search**. Pull down to refresh.
* Each course shows *enrolled/capacity · seats left · waiting*.
* **Register** takes a seat. When the course is full the button reads **Join waiting list**.
  The result appears as a short message (for example *"HCI308 is full: STU003 was added to the waiting list."*).
* Registering twice shows *"Duplicate registration"*.

## 4. My registrations (students)
Lists your registered courses and waiting-list places (with position). **Withdraw** asks for confirmation. If you
held a seat, the first student on the waiting list is promoted automatically.

## 5. Service requests
* Students: choose a **Category** and **Priority**, write a **Description** (5–1000 characters) and tap
  **Submit request**. Your requests and their status (OPEN, ASSIGNED, IN_PROGRESS, RESOLVED) are listed below.
* Staff: the open queue, highest priority first. Assigning and resolving is done in the web application.

## 6. Campus services and map
* Three services are listed (Main Library, Registry, Campus Clinic) with their coordinates.
  **The coordinates are illustrative sample values** until the group enters the real campus locations.
* **Open in map** opens the service in your map app (Google Maps or another). If no map app is installed, it opens in the browser.
* **Show my position** shows where you are and which service is nearest, with the distance.
  The first time, Android asks for location permission (see below). If you said no before, SCASP first explains
  why it asks (*Why SCASP asks for your location*) before Android asks again.
  On the emulator, set a location first: *Extended controls (⋯) › Location*, enter a point, **Set location**.

## 7. Offline use
Banner text at the top of each list tells you where the data came from:
| Banner | Meaning |
|---|---|
| *Live from server · updated 14:03* | Fresh data from the server |
| *Offline – showing data saved at 13:55. Pull down to retry.* | No connection; this is the last saved copy |
| *Demo data – the SCASP server could not be reached* | No connection and nothing saved yet; built-in sample data |

Registering, withdrawing and submitting requests need a connection. Offline you see *"You are offline … nothing was saved"*.
Try again when connected.

## 8. Permissions: what the app asks for and why
| Permission | When it is asked | Why | If you say no |
|---|---|---|---|
| Internet / network state | Never asked (normal permission) | Talk to the SCASP server | – |
| **Location** (approximate or precise) | Only when you tap **Show my position** | Show your position and the nearest campus service. Used once, on that screen; **not stored and not sent to the server** | Everything else works, including *Open in map*. You see *"Location permission was denied…"*. After a permanent denial, **Open app settings** lets you change it |
| **Notifications** (Android 13+) | Only when you switch on **Background sync** | Tell you when a waiting-list place becomes a registration | Sync still runs and updates saved data; you just get no notification |

The app never asks for permissions at start-up.
