package com.scasp.service;

import com.scasp.model.*;
import com.scasp.report.CapacityRow;
import com.scasp.report.RequestWorkload;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * In-memory academic services core (Stage 1) made thread-safe for Stage 2.
 *
 * <p>Collection choices (see docs/architecture/collection-selection.md):
 * <ul>
 *   <li>{@code HashMap} users/students/courses keyed by unique id (O(1) lookup, ids are unique);</li>
 *   <li>{@code LinkedHashSet<Registration>} per course: a Set so duplicates are impossible, linked to keep registration order;</li>
 *   <li>{@code ArrayDeque} per course waiting list: FIFO queue, promotion takes the head;</li>
 *   <li>{@code PriorityBlockingQueue} for unassigned service requests: highest priority first, FIFO for equal priority;</li>
 *   <li>{@code ArrayList<AuditEvent>} append-only audit trail.</li>
 * </ul>
 * Invariants: a student is registered at most once per course; active registrations never exceed capacity;
 * a student is never both registered and wait-listed for the same course; request status changes follow
 * the ServiceRequest state machine; staff-only operations require a STAFF/ADMIN actor.
 * All mutators are synchronized on this service so each invariant check-and-update is atomic.
 * Public methods return unmodifiable copies or immutable values.
 */
public class AcademicService {
    public static final String SYSTEM_ACTOR = "SYSTEM";

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Course> courses = new HashMap<>();
    private final Map<String, Set<Registration>> registrations = new HashMap<>();
    private final Map<String, Deque<String>> waitingLists = new HashMap<>();
    private final Map<String, LocalDateTime> waitlistedAt = new HashMap<>();
    private final PriorityBlockingQueue<ServiceRequest> requestQueue = new PriorityBlockingQueue<>(16,
            Comparator.comparingInt(ServiceRequest::getPriority).reversed().thenComparingLong(ServiceRequest::getSequence));
    private final Map<String, ServiceRequest> requests = new LinkedHashMap<>();
    private final List<Announcement> announcements = new ArrayList<>();
    private final List<AuditEvent> audit = new ArrayList<>();
    private final List<Consumer<AuditEvent>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong eventCounter = new AtomicLong();
    private final AtomicLong announcementCounter = new AtomicLong();
    private final Clock clock;

    public AcademicService() { this(Clock.systemDefaultZone()); }
    public AcademicService(Clock clock) { this.clock = Objects.requireNonNull(clock); }

    // ------------------------------------------------------------------ users
    /** Bootstrap/sample-data loading of any user (system actor). */
    public synchronized void addUser(User u) {
        Objects.requireNonNull(u);
        if (users.putIfAbsent(u.getUserId(), u) != null) throw new IllegalArgumentException("User already exists: " + u.getUserId());
        if (u instanceof Student s) students.put(s.getUserId(), s);
        audit(SYSTEM_ACTOR, "ADD_USER", "USER", u.getUserId(), u.getRole().name());
    }
    public synchronized void addStudent(Student s) { addUser(s); }
    public synchronized Optional<User> findUser(String userId) { return Optional.ofNullable(users.get(userId)); }
    public synchronized Optional<Student> findStudent(String id) { return Optional.ofNullable(students.get(id)); }
    /** Students in a programme sorted by name then id (Comparator chain). */
    public synchronized List<Student> findStudentsByProgramme(String programme) {
        return students.values().stream().filter(s -> s.getProgramme().equalsIgnoreCase(programme))
                .sorted(Comparator.comparing(Student::getName).thenComparing(Student::getUserId)).toList();
    }

    // ---------------------------------------------------------------- courses
    /** Bootstrap/sample-data loading (system actor). */
    public synchronized void addCourse(Course c) { putNewCourse(SYSTEM_ACTOR, c); }
    /** Staff-only course creation. */
    public synchronized void createCourse(String actorId, Course c) { requireStaff(actorId); putNewCourse(actorId, c); }
    private void putNewCourse(String actor, Course c) {
        Objects.requireNonNull(c);
        if (courses.putIfAbsent(c.getCourseCode(), c) != null) throw new IllegalArgumentException("Course already exists");
        registrations.put(c.getCourseCode(), new LinkedHashSet<>());
        waitingLists.put(c.getCourseCode(), new ArrayDeque<>());
        audit(actor, "ADD_COURSE", "COURSE", c.getCourseCode(), c.getTitle());
    }
    /** Staff-only update. Capacity may not be reduced below the current enrolment. */
    public synchronized Course updateCourse(String actorId, String code, String title, int capacity) {
        requireStaff(actorId);
        Course existing = requireCourse(code);
        int enrolled = registrations.get(existing.getCourseCode()).size();
        if (capacity < enrolled) throw new IllegalStateException("Capacity " + capacity + " is below current enrolment " + enrolled);
        Course updated = existing.withDetails(title, capacity);
        courses.put(updated.getCourseCode(), updated);
        audit(actorId, "UPDATE_COURSE", "COURSE", code, "title=" + title + ",capacity=" + capacity);
        promoteWhileSeats(actorId, updated);
        return updated.copy();
    }
    public synchronized void setCourseActive(String actorId, String code, boolean active) {
        requireStaff(actorId);
        requireCourse(code).setActive(active);
        audit(actorId, active ? "ACTIVATE_COURSE" : "DEACTIVATE_COURSE", "COURSE", code, "");
    }
    public synchronized List<Course> listCourses() { return listCourses(CourseComparators.BY_CODE); }
    public synchronized List<Course> listCourses(Comparator<Course> order) {
        return courses.values().stream().map(Course::copy).sorted(order).toList();
    }
    /** Case-insensitive search on code or title. */
    public synchronized List<Course> searchCourses(String keyword, boolean activeOnly, Comparator<Course> order) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        return courses.values().stream()
                .filter(c -> !activeOnly || c.isActive())
                .filter(c -> k.isEmpty() || c.getCourseCode().toLowerCase().contains(k) || c.getTitle().toLowerCase().contains(k))
                .map(Course::copy).sorted(order).toList();
    }
    /** Binary search over the code-sorted catalogue (Collections.binarySearch with the natural ordering). */
    public synchronized Optional<Course> findCourse(String code) {
        if (code == null) return Optional.empty();
        String normal = code.trim().toUpperCase();
        if (!normal.matches("[A-Z0-9_-]{2,20}")) return Optional.empty();
        List<Course> sorted = listCourses(CourseComparators.BY_CODE);
        int idx = Collections.binarySearch(sorted, new Course(normal, "probe", 1));
        return idx >= 0 ? Optional.of(sorted.get(idx)) : Optional.empty();
    }

    // ----------------------------------------------------------- registration
    /** Self-service registration: the student is the actor. */
    public String register(String studentId, String courseCode) { return register(studentId, studentId, courseCode); }
    /**
     * Registers if a seat is free, otherwise appends to the FIFO waiting list.
     * Students may only register themselves; staff may register anyone.
     * @return "REGISTERED" or "WAITLISTED"
     */
    public synchronized String register(String actorId, String studentId, String courseCode) {
        requireSelfOrStaff(actorId, studentId);
        requireStudent(studentId); Course c = requireCourse(courseCode);
        String code = c.getCourseCode();
        if (!c.isActive()) throw new IllegalStateException("Course is inactive");
        Set<Registration> enrolled = registrations.get(code);
        if (enrolled.contains(new Registration(studentId, code, Registration.Status.ACTIVE))) throw new IllegalStateException("Duplicate registration");
        Deque<String> queue = waitingLists.get(code);
        if (queue.contains(studentId)) throw new IllegalStateException("Already on waiting list");
        if (enrolled.size() < c.getCapacity()) {
            enrolled.add(new Registration(studentId, code, Registration.Status.ACTIVE, now()));
            audit(actorId, "REGISTER", "REGISTRATION", studentId + ":" + code, "");
            return "REGISTERED";
        }
        queue.addLast(studentId);
        waitlistedAt.put(studentId + "|" + code, now());
        audit(actorId, "WAITLIST", "WAITLIST", studentId + ":" + code, "position=" + queue.size());
        return "WAITLISTED";
    }
    public void withdraw(String studentId, String courseCode) { withdraw(studentId, studentId, courseCode); }
    /**
     * Withdraws an active registration (promoting the head of the waiting list) or removes the student
     * from the waiting list. Withdrawing when neither registered nor waiting is an invalid operation.
     * @return the promoted student id, or empty
     */
    public synchronized Optional<String> withdraw(String actorId, String studentId, String courseCode) {
        requireSelfOrStaff(actorId, studentId);
        requireStudent(studentId); Course c = requireCourse(courseCode);
        String code = c.getCourseCode();
        Set<Registration> enrolled = registrations.get(code);
        if (!enrolled.remove(new Registration(studentId, code, Registration.Status.ACTIVE))) {
            if (!waitingLists.get(code).remove(studentId)) throw new IllegalStateException("Student is not registered or waiting for " + code);
            waitlistedAt.remove(studentId + "|" + code);
            audit(actorId, "REMOVE_WAITLIST", "WAITLIST", studentId + ":" + code, "");
            return Optional.empty();
        }
        audit(actorId, "WITHDRAW", "REGISTRATION", studentId + ":" + code, "");
        return promoteWhileSeats(actorId, c).stream().findFirst();
    }
    private List<String> promoteWhileSeats(String actor, Course c) {
        String code = c.getCourseCode();
        Set<Registration> enrolled = registrations.get(code);
        Deque<String> q = waitingLists.get(code);
        List<String> promoted = new ArrayList<>();
        while (!q.isEmpty() && enrolled.size() < c.getCapacity()) {
            String next = q.removeFirst();
            waitlistedAt.remove(next + "|" + code);
            enrolled.add(new Registration(next, code, Registration.Status.ACTIVE, now()));
            audit(actor, "PROMOTE", "REGISTRATION", next + ":" + code, "from waiting list");
            promoted.add(next);
        }
        return promoted;
    }
    public synchronized int enrolledCount(String code) { return registrations.get(requireCourse(code).getCourseCode()).size(); }
    public synchronized List<String> enrolledStudents(String code) {
        return registrations.get(requireCourse(code).getCourseCode()).stream().map(Registration::studentId).toList();
    }
    public synchronized List<String> waitingList(String code) { return List.copyOf(waitingLists.get(requireCourse(code).getCourseCode())); }
    public synchronized List<WaitlistEntry> waitingListEntries(String code) {
        String cc = requireCourse(code).getCourseCode();
        List<WaitlistEntry> out = new ArrayList<>();
        int pos = 1;
        for (String s : waitingLists.get(cc)) out.add(new WaitlistEntry(s, cc, pos++, waitlistedAt.get(s + "|" + cc)));
        return List.copyOf(out);
    }
    public synchronized List<Registration> registrationsFor(String studentId) {
        requireStudent(studentId);
        return registrations.values().stream().flatMap(Set::stream).filter(r -> r.studentId().equals(studentId))
                .sorted(Comparator.comparing(Registration::courseCode)).toList();
    }

    // ------------------------------------------------------- service requests
    /** Student submits their own request (actor = request.studentId). */
    public ServiceRequestSummary submitRequest(ServiceRequest r) { return submitRequest(r.getStudentId(), r); }
    public synchronized ServiceRequestSummary submitRequest(String actorId, ServiceRequest r) {
        Objects.requireNonNull(r);
        requireSelfOrStaff(actorId, r.getStudentId());
        requireStudent(r.getStudentId());
        if (requests.putIfAbsent(r.getRequestId(), r) != null) throw new IllegalArgumentException("Duplicate request id");
        requestQueue.add(r);
        audit(actorId, "SUBMIT_REQUEST", "SERVICE_REQUEST", r.getRequestId(), r.getCategory() + ",p" + r.getPriority());
        return ServiceRequestSummary.of(r);
    }
    /** Staff takes the highest-priority unassigned request (FIFO within a priority) and assigns it to themself. */
    public synchronized Optional<ServiceRequestSummary> assignNextRequest(String staffId) {
        requireStaff(staffId);
        ServiceRequest next = requestQueue.poll();
        if (next == null) return Optional.empty();
        next.assign(staffId);
        audit(staffId, "ASSIGN_REQUEST", "SERVICE_REQUEST", next.getRequestId(), "to=" + staffId);
        return Optional.of(ServiceRequestSummary.of(next));
    }
    /** Staff assigns a specific open request to a staff member. */
    public synchronized ServiceRequestSummary assignRequest(String actorId, String requestId, String staffId) {
        requireStaff(actorId);
        User assignee = users.get(staffId);
        if (assignee == null || !assignee.isStaff()) throw new IllegalArgumentException("Assignee must be a staff member");
        ServiceRequest r = requireRequest(requestId);
        r.assign(staffId);               // throws on invalid transition
        requestQueue.remove(r);
        audit(actorId, "ASSIGN_REQUEST", "SERVICE_REQUEST", requestId, "to=" + staffId);
        return ServiceRequestSummary.of(r);
    }
    public synchronized ServiceRequestSummary updateRequestStatus(String actorId, String requestId, ServiceRequest.Status next) {
        requireStaff(actorId);
        ServiceRequest r = requireRequest(requestId);
        ServiceRequest.Status before = r.getStatus();
        r.updateStatus(next);            // throws IllegalStateException on invalid transition
        if (before == ServiceRequest.Status.OPEN) requestQueue.remove(r);
        audit(actorId, "REQUEST_STATUS", "SERVICE_REQUEST", requestId, before + "->" + next);
        return ServiceRequestSummary.of(r);
    }
    public synchronized List<ServiceRequestSummary> requestsFor(String actorId, String studentId) {
        requireSelfOrStaff(actorId, studentId);
        return requests.values().stream().filter(r -> r.getStudentId().equals(studentId)).map(ServiceRequestSummary::of).toList();
    }
    /** Staff view of unassigned requests in processing order (copy of the priority queue, sorted). */
    public synchronized List<ServiceRequestSummary> openRequestQueue(String actorId) {
        requireStaff(actorId);
        List<ServiceRequest> copy = new ArrayList<>(requestQueue);
        copy.sort(requestQueue.comparator());
        return copy.stream().map(ServiceRequestSummary::of).toList();
    }
    public synchronized List<ServiceRequestSummary> allRequests(String actorId) {
        requireStaff(actorId);
        return requests.values().stream().map(ServiceRequestSummary::of).toList();
    }

    // ---------------------------------------------------------- announcements
    public synchronized Announcement publishAnnouncement(String actorId, String title, String body, Announcement.Audience audience) {
        requireStaff(actorId);
        Announcement a = new Announcement("ANN" + String.format("%03d", announcementCounter.incrementAndGet()), title, body, audience, now());
        announcements.add(a);
        audit(actorId, "PUBLISH_ANNOUNCEMENT", "ANNOUNCEMENT", a.announcementId(), audience.name());
        return a;
    }
    /** Announcements visible to a role, newest first. */
    public synchronized List<Announcement> announcementsFor(Role role) {
        return announcements.stream().filter(a -> a.audience().includes(role))
                .sorted(Comparator.comparing(Announcement::publishedAt).thenComparing(Announcement::announcementId).reversed()).toList();
    }
    public synchronized List<Announcement> announcementsFor(Announcement.Audience audience) {
        return announcements.stream().filter(a -> a.audience() == audience)
                .sorted(Comparator.comparing(Announcement::publishedAt).thenComparing(Announcement::announcementId).reversed()).toList();
    }

    // ---------------------------------------------------------------- reports
    public synchronized List<CapacityRow> capacityReport(String actorId) {
        requireStaff(actorId);
        return courses.values().stream().sorted()
                .map(c -> new CapacityRow(c.getCourseCode(), c.getTitle(), c.getCapacity(),
                        registrations.get(c.getCourseCode()).size(), waitingLists.get(c.getCourseCode()).size(), c.isActive()))
                .toList();
    }
    /** Waiting list per course (only courses with someone waiting), ordered by course code. */
    public synchronized Map<String, List<String>> waitingListReport(String actorId) {
        requireStaff(actorId);
        return Collections.unmodifiableMap(waitingLists.entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue()), (a, b) -> a, TreeMap::new)));
    }
    public synchronized RequestWorkload requestWorkloadReport(String actorId) {
        requireStaff(actorId);
        Map<ServiceRequest.Status, Long> byStatus = requests.values().stream()
                .collect(Collectors.groupingBy(ServiceRequest::getStatus, () -> new EnumMap<>(ServiceRequest.Status.class), Collectors.counting()));
        Map<String, Long> byAssignee = requests.values().stream().filter(r -> !r.isClosed() && !r.getAssignedTo().isEmpty())
                .collect(Collectors.groupingBy(ServiceRequest::getAssignedTo, TreeMap::new, Collectors.counting()));
        Map<String, Long> byCategory = requests.values().stream()
                .collect(Collectors.groupingBy(ServiceRequest::getCategory, TreeMap::new, Collectors.counting()));
        double avg = requests.values().stream().filter(r -> !r.isClosed()).mapToInt(ServiceRequest::getPriority).average().orElse(0);
        return new RequestWorkload(byStatus, byAssignee, byCategory, avg);
    }
    /** Student id -> sorted course codes of active registrations. */
    public synchronized Map<String, List<String>> studentRegistrationReport(String actorId) {
        requireStaff(actorId);
        return Collections.unmodifiableMap(registrations.values().stream().flatMap(Set::stream)
                .collect(Collectors.groupingBy(Registration::studentId, TreeMap::new,
                        Collectors.collectingAndThen(Collectors.mapping(Registration::courseCode, Collectors.toList()),
                                l -> l.stream().sorted().toList()))));
    }
    /** Audit counts by action (sorted). */
    public synchronized Map<String, Long> auditReport(String actorId) {
        requireStaff(actorId);
        return Collections.unmodifiableMap(audit.stream().collect(Collectors.groupingBy(AuditEvent::action, TreeMap::new, Collectors.counting())));
    }
    public synchronized List<AuditEvent> auditTrail() { return List.copyOf(audit); }
    public synchronized List<AuditEvent> auditTrailFor(String entityType, String entityId) {
        return audit.stream().filter(e -> e.entityType().equals(entityType) && e.entityId().equals(entityId)).toList();
    }

    // ----------------------------------------------------------- notifications
    /** Listeners receive every audit event (e.g. UDP notifications). They run inside the service lock, so keep them fast. */
    public void addAuditListener(Consumer<AuditEvent> listener) { listeners.add(Objects.requireNonNull(listener)); }

    // ---------------------------------------------------------------- helpers
    private void audit(String actor, String action, String entityType, String entityId, String details) {
        AuditEvent e = new AuditEvent(eventCounter.incrementAndGet(), actor, action, entityType, entityId, details, now());
        audit.add(e);
        for (Consumer<AuditEvent> l : listeners) {
            try { l.accept(e); } catch (RuntimeException ignored) { /* a failing listener must not break the business operation */ }
        }
    }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private User requireUser(String actorId) {
        User u = actorId == null ? null : users.get(actorId);
        if (u == null) throw new AuthorisationException("Unknown actor");
        if (!u.isActive()) throw new AuthorisationException("Account is not active");
        return u;
    }
    private void requireStaff(String actorId) {
        if (!requireUser(actorId).isStaff()) throw new AuthorisationException("Staff role required");
    }
    private void requireSelfOrStaff(String actorId, String studentId) {
        User u = requireUser(actorId);
        if (!u.isStaff() && !u.getUserId().equals(studentId)) throw new AuthorisationException("Students may only act for themselves");
    }
    private Student requireStudent(String id) { Student s = id == null ? null : students.get(id); if (s == null) throw new IllegalArgumentException("Unknown student"); return s; }
    private Course requireCourse(String id) {
        Course c = id == null ? null : courses.get(id.trim().toUpperCase());
        if (c == null) throw new IllegalArgumentException("Unknown course");
        return c;
    }
    private ServiceRequest requireRequest(String id) { ServiceRequest r = id == null ? null : requests.get(id); if (r == null) throw new IllegalArgumentException("Unknown request"); return r; }
}
