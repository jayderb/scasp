-- Fictional sample data only (no real student records). Run after schema.sql.
-- Demo logins (classroom only): STU001..STU004 / student123, STAFF01 / staff123, ADMIN01 / admin123.
-- Passwords are stored as PBKDF2-HMAC-SHA256 hashes generated with com.scasp.security.PasswordHasher.
INSERT INTO app_user (user_id, full_name, role, password_hash) VALUES
  ('STU001', 'Alex Banda', 'STUDENT', 'pbkdf2-sha256$120000$yQ7lpL0d7IUYf0SZTbafkg==$1Qi2yZGg598dGzzPjyyU4C87uErpE9tlYB88ukCjgxg='),
  ('STU002', 'Mary Phiri', 'STUDENT', 'pbkdf2-sha256$120000$yQ7lpL0d7IUYf0SZTbafkg==$1Qi2yZGg598dGzzPjyyU4C87uErpE9tlYB88ukCjgxg='),
  ('STU003', 'John Zulu', 'STUDENT', 'pbkdf2-sha256$120000$yQ7lpL0d7IUYf0SZTbafkg==$1Qi2yZGg598dGzzPjyyU4C87uErpE9tlYB88ukCjgxg='),
  ('STU004', 'Ruth Mwale', 'STUDENT', 'pbkdf2-sha256$120000$yQ7lpL0d7IUYf0SZTbafkg==$1Qi2yZGg598dGzzPjyyU4C87uErpE9tlYB88ukCjgxg='),
  ('STAFF01', 'Grace Tembo', 'STAFF', 'pbkdf2-sha256$120000$vHoMk6zGDUO9OrXF425Wxw==$01HpuCn5clJYALrzJ4ZmlMd1ApRC3AiPzshZrG5Swls='),
  ('ADMIN01', 'Peter Lungu', 'ADMIN', 'pbkdf2-sha256$120000$RdJnRU72Q1kb4+W6Dsh7cQ==$dnMZlDulwt+C9rWjzBFAeo8m1D9/AcIutWNKRt25vhA=');
INSERT INTO students (student_id, programme, study_year) VALUES
  ('STU001', 'Computer Science', 2), ('STU002', 'Computer Science', 2),
  ('STU003', 'Information Technology', 1), ('STU004', 'Information Technology', 3);
INSERT INTO courses (course_code, title, capacity) VALUES
  ('CSC301', 'Advanced Java', 2), ('DB302', 'Advanced Database', 3), ('HCI308', 'Human Computer Interface', 2);
INSERT INTO campus_service (service_id, name, description, latitude, longitude) VALUES
  ('LIB', 'Main Library', 'Books and study space (illustrative coordinates)', -15.4167000, 28.2833000),
  ('REG', 'Registry', 'Academic records and registration (illustrative coordinates)', -15.4170000, 28.2840000),
  ('CLINIC', 'Campus Clinic', 'Basic health services (illustrative coordinates)', -15.4180000, 28.2820000);
INSERT INTO announcement (title, body, audience, published_by) VALUES
  ('Registration reminder', 'Check your course registration status before Friday.', 'STUDENTS', 'STAFF01');
INSERT INTO audit_event (actor, action, entity_type, entity_id, details) VALUES
  ('SYSTEM', 'LOAD_SAMPLE_DATA', 'DATABASE', 'scasp', 'sample-data.sql');
