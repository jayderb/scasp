-- SCASP join reports (MySQL 8). The same queries are used by JdbcReportRepository.
-- Report 1: enrolment, waiting list and remaining seats by course (courses LEFT JOIN registrations / waiting_list).
SELECT c.course_code, c.title, c.capacity,
       COUNT(DISTINCT r.registration_id) AS enrolled,
       COUNT(DISTINCT w.queue_id)        AS waiting,
       c.capacity - COUNT(DISTINCT r.registration_id) AS seats_remaining
FROM courses c
LEFT JOIN registrations r ON r.course_code = c.course_code AND r.status = 'ACTIVE'
LEFT JOIN waiting_list  w ON w.course_code = c.course_code
GROUP BY c.course_code, c.title, c.capacity
ORDER BY c.course_code;

-- Report 2: students and their active registrations (app_user + students + registrations + courses).
SELECT s.student_id, u.full_name, s.programme, c.course_code, c.title, r.created_at
FROM students s
JOIN app_user u      ON u.user_id = s.student_id
JOIN registrations r ON r.student_id = s.student_id AND r.status = 'ACTIVE'
JOIN courses c       ON c.course_code = r.course_code
ORDER BY s.student_id, c.course_code;

-- Report 3: waiting lists in queue order with position (waiting_list + courses + app_user).
SELECT w.course_code, c.title,
       ROW_NUMBER() OVER (PARTITION BY w.course_code ORDER BY w.queued_at, w.queue_id) AS position,
       w.student_id, u.full_name, w.queued_at
FROM waiting_list w
JOIN courses c  ON c.course_code = w.course_code
JOIN app_user u ON u.user_id = w.student_id
ORDER BY w.course_code, position;

-- Report 4: request workload by assignee and status (service_request LEFT JOIN app_user).
SELECT COALESCE(sr.assigned_to, '(unassigned)') AS assignee, COALESCE(u.full_name, '-') AS assignee_name,
       sr.status, COUNT(*) AS requests, AVG(sr.priority) AS avg_priority
FROM service_request sr
LEFT JOIN app_user u ON u.user_id = sr.assigned_to
GROUP BY sr.assigned_to, u.full_name, sr.status
ORDER BY assignee, sr.status;

-- Report 5: audit activity by actor (audit_event LEFT JOIN app_user).
SELECT a.actor, COALESCE(u.full_name, a.actor) AS actor_name, COALESCE(u.role, 'SYSTEM') AS role,
       COUNT(*) AS events, MAX(a.occurred_at) AS last_event_at
FROM audit_event a
LEFT JOIN app_user u ON u.user_id = a.actor
GROUP BY a.actor, u.full_name, u.role
ORDER BY events DESC, a.actor;
