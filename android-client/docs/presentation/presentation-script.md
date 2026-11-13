# SCASP Final Presentation Script (about 25 minutes)

## 1. Problem, architecture and roles – 2 minutes
Good morning. Our project is the Smart Campus Academic Services Platform, also known as SCASP. The purpose is to provide one platform where students can access courses, registration, waiting lists, academic service requests, announcements and campus services. Staff can manage courses, process requests and view reports. Our architecture separates clients, application services, the domain layer and database. Our group members are Joshua Lifumbo, Chipego Mukando and Mumba Bupe.

## 2. Collections, domain rules and persistence – 4 minutes
The core uses Map collections to find students and courses by unique identifiers. A Set prevents duplicate registrations. A Queue maintains a first-in-first-out waiting list. We use sorting and validation to protect the rules. When a course is full, another student joins the waiting list. When an enrolled student withdraws, the next eligible student is promoted. The relational database stores users, students, courses, registrations, waiting-list entries, service requests, announcements and audit events.

## 3. Concurrent processing and networking – 4 minutes
The processing module uses a bounded BlockingQueue and ExecutorService. A bounded queue prevents unlimited tasks from consuming memory. Workers process tasks and can be shut down safely. The TCP gateway accepts commands such as PING and COURSES. It uses a server socket, worker threads and timeouts. The protocol returns clear success or error messages.

## 4. Enterprise and web workflow – 4 minutes
The enterprise layer is designed around stateless business operations and a singleton configuration or metrics component. The web application follows MVC. Servlets receive HTTP requests, validate input and call business services. JSP pages display the data. HttpSession stores the authenticated demo user's session, while role checks protect staff functions.

## 5. RMI and CORBA – 3 minutes
RMI provides remote administration operations such as listing courses, counting registrations and viewing waiting lists. The client looks up the remote object in the RMI registry. Remote failures are handled with RemoteException. CORBA is covered through an IDL contract and migration design. We explain the ORB, naming service, stubs, skeleton/servant and how an adapter can allow old clients to coexist while new clients use HTTP.

## 6. Android – 3 minutes
The Android client is planned with screens for login, courses, registration, service requests and campus services. Explicit intents support navigation. An implicit intent opens a map application. Background work is used for synchronisation, and a small local cache supports limited offline use. Location permission is requested only when the user opens the location feature.

## 7. Questions and individual code explanation – 5 minutes
Each member should explain one design decision and one code section, then demonstrate a small change requested by the lecturer.

**Important:** Replace this script's planned statements with what the group has actually implemented and demonstrated.
