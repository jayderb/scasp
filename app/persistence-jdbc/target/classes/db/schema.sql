-- SCASP relational schema for MySQL 8.0.16+ (InnoDB, utf8mb4). Also runs on MariaDB 10.6+.
-- Repeatable: drops and recreates every table. Run with:  mysql -u scasp -p scasp < database/schema.sql
-- Identifier strategy: natural keys for users/courses (stable codes); AUTO_INCREMENT surrogate keys
-- (read back with JDBC getGeneratedKeys) for registrations, waiting list, requests, announcements, audit.

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS audit_event;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS campus_service;
DROP TABLE IF EXISTS service_request;
DROP TABLE IF EXISTS waiting_list;
DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS app_user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE app_user (
  user_id       VARCHAR(30)  NOT NULL,
  full_name     VARCHAR(120) NOT NULL,
  role          VARCHAR(20)  NOT NULL,
  active        BOOLEAN      NOT NULL DEFAULT TRUE,
  password_hash VARCHAR(255) NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_app_user PRIMARY KEY (user_id),
  CONSTRAINT ck_user_role CHECK (role IN ('STUDENT','STAFF','ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE students (
  student_id VARCHAR(30)  NOT NULL,
  programme  VARCHAR(100) NOT NULL,
  study_year INT          NOT NULL,
  CONSTRAINT pk_students PRIMARY KEY (student_id),
  CONSTRAINT fk_student_user FOREIGN KEY (student_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
  CONSTRAINT ck_study_year CHECK (study_year BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE courses (
  course_code VARCHAR(20)  NOT NULL,
  title       VARCHAR(120) NOT NULL,
  capacity    INT          NOT NULL,
  active      BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_courses PRIMARY KEY (course_code),
  CONSTRAINT ck_course_capacity CHECK (capacity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE registrations (
  registration_id BIGINT      NOT NULL AUTO_INCREMENT,
  student_id      VARCHAR(30) NOT NULL,
  course_code     VARCHAR(20) NOT NULL,
  status          VARCHAR(20) NOT NULL,
  created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_registrations PRIMARY KEY (registration_id),
  CONSTRAINT uq_student_course UNIQUE (student_id, course_code),
  CONSTRAINT fk_reg_student FOREIGN KEY (student_id) REFERENCES students(student_id),
  CONSTRAINT fk_reg_course FOREIGN KEY (course_code) REFERENCES courses(course_code),
  CONSTRAINT ck_reg_status CHECK (status IN ('ACTIVE','WITHDRAWN')),
  INDEX idx_reg_course_status (course_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE waiting_list (
  queue_id    BIGINT       NOT NULL AUTO_INCREMENT,
  student_id  VARCHAR(30)  NOT NULL,
  course_code VARCHAR(20)  NOT NULL,
  queued_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_waiting_list PRIMARY KEY (queue_id),
  CONSTRAINT uq_wait_student_course UNIQUE (student_id, course_code),
  CONSTRAINT fk_wait_student FOREIGN KEY (student_id) REFERENCES students(student_id),
  CONSTRAINT fk_wait_course FOREIGN KEY (course_code) REFERENCES courses(course_code),
  INDEX idx_waiting_course_time (course_code, queued_at, queue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE service_request (
  request_id  BIGINT        NOT NULL AUTO_INCREMENT,
  student_id  VARCHAR(30)   NOT NULL,
  category    VARCHAR(60)   NOT NULL,
  description VARCHAR(1000) NOT NULL,
  priority    INT           NOT NULL,
  status      VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
  assigned_to VARCHAR(30)   NULL,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_service_request PRIMARY KEY (request_id),
  CONSTRAINT fk_req_student FOREIGN KEY (student_id) REFERENCES students(student_id),
  CONSTRAINT fk_req_assignee FOREIGN KEY (assigned_to) REFERENCES app_user(user_id),
  CONSTRAINT ck_req_priority CHECK (priority BETWEEN 1 AND 5),
  CONSTRAINT ck_req_status CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','REJECTED')),
  INDEX idx_request_status_priority (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE announcement (
  announcement_id BIGINT        NOT NULL AUTO_INCREMENT,
  title           VARCHAR(150)  NOT NULL,
  body            VARCHAR(2000) NOT NULL,
  audience        VARCHAR(20)   NOT NULL,
  published_by    VARCHAR(30)   NOT NULL,
  published_at    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_announcement PRIMARY KEY (announcement_id),
  CONSTRAINT fk_ann_user FOREIGN KEY (published_by) REFERENCES app_user(user_id),
  CONSTRAINT ck_ann_audience CHECK (audience IN ('ALL','STUDENTS','STAFF')),
  INDEX idx_ann_audience_time (audience, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campus_service (
  service_id  VARCHAR(30)   NOT NULL,
  name        VARCHAR(120)  NOT NULL,
  description VARCHAR(500)  NULL,
  latitude    DECIMAL(10,7) NOT NULL,
  longitude   DECIMAL(10,7) NOT NULL,
  CONSTRAINT pk_campus_service PRIMARY KEY (service_id),
  CONSTRAINT ck_latitude CHECK (latitude BETWEEN -90 AND 90),
  CONSTRAINT ck_longitude CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- actor is deliberately NOT a foreign key: the audit trail must survive user deletion and
-- also records non-user actors such as SYSTEM.
CREATE TABLE audit_event (
  event_id    BIGINT       NOT NULL AUTO_INCREMENT,
  actor       VARCHAR(30)  NOT NULL,
  action      VARCHAR(60)  NOT NULL,
  entity_type VARCHAR(40)  NOT NULL,
  entity_id   VARCHAR(80)  NOT NULL,
  details     VARCHAR(500) NOT NULL DEFAULT '',
  occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_audit_event PRIMARY KEY (event_id),
  INDEX idx_audit_entity (entity_type, entity_id),
  INDEX idx_audit_actor_time (actor, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
