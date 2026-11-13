-- Atomic registration transaction (what JdbcRegistrationRepository.register does with PreparedStatements).
-- Run in the mysql client to see commit/rollback behaviour. Values are fictional.
START TRANSACTION;
-- 1. Lock the course row so concurrent registrations for the same course are serialised.
SELECT capacity, active FROM courses WHERE course_code = 'DB302' FOR UPDATE;
-- 2. Check duplicates and current enrolment.
SELECT status FROM registrations WHERE student_id = 'STU001' AND course_code = 'DB302';
SELECT COUNT(*) FROM registrations WHERE course_code = 'DB302' AND status = 'ACTIVE';
-- 3a. Seat available: insert registration ...
INSERT INTO registrations (student_id, course_code, status) VALUES ('STU001', 'DB302', 'ACTIVE');
-- 3b. ... otherwise: INSERT INTO waiting_list (student_id, course_code) VALUES ('STU001', 'DB302');
-- 4. Audit in the same transaction.
INSERT INTO audit_event (actor, action, entity_type, entity_id) VALUES ('STU001', 'REGISTER', 'REGISTRATION', 'STU001:DB302');
COMMIT;

-- Rollback demonstration: the audit insert violates NOT NULL (actor), so the whole unit is undone.
START TRANSACTION;
INSERT INTO registrations (student_id, course_code, status) VALUES ('STU002', 'DB302', 'ACTIVE');
INSERT INTO audit_event (actor, action, entity_type, entity_id) VALUES (NULL, 'REGISTER', 'REGISTRATION', 'STU002:DB302');
-- The statement above fails (ERROR 1048 Column 'actor' cannot be null) -> issue:
ROLLBACK;
-- STU002 is NOT registered:
SELECT COUNT(*) AS should_be_zero FROM registrations WHERE student_id = 'STU002' AND course_code = 'DB302';
