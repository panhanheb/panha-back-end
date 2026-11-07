-- =====================================================================
-- School Attendance Management System - MySQL 8 schema
-- ---------------------------------------------------------------------
-- Column types mirror the JPA mappings exactly, so the application can
-- run with spring.jpa.hibernate.ddl-auto=validate against this schema.
--
-- Run in MySQL Workbench or:  mysql -u root -p < schema.sql
-- (In development the app can also create/update tables itself with
--  ddl-auto=update; this script is the reviewed, production version.)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS school_attendance_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE school_attendance_db;

-- ---------------------------------------------------------------------
-- 1. users - login accounts (passwords are BCrypt hashes)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    username                  VARCHAR(50)  NOT NULL,
    email                     VARCHAR(120) NOT NULL,
    password                  VARCHAR(100) NOT NULL,
    enabled                   BIT          NOT NULL,
    token_version             INTEGER      NOT NULL,      -- bumped on logout / password change to revoke JWTs
    last_login_at             DATETIME(6),
    password_reset_token_hash VARCHAR(64),                -- SHA-256 of the reset token, never the token itself
    password_reset_expires_at DATETIME(6),
    created_at                DATETIME(6)  NOT NULL,
    updated_at                DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    INDEX idx_users_enabled (enabled)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. roles
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT                                         NOT NULL AUTO_INCREMENT,
    name        ENUM ('ROLE_ADMIN','ROLE_STUDENT','ROLE_TEACHER') NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6)                                    NOT NULL,
    updated_at  DATETIME(6)                                    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. user_roles - many-to-many users <-> roles
-- ---------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, user_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 12. academic_years
-- ---------------------------------------------------------------------
CREATE TABLE academic_years (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(20) NOT NULL,                       -- e.g. 2026-2027
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    is_current BIT         NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_years_name UNIQUE (name)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 13. semesters - belong to an academic year
-- ---------------------------------------------------------------------
CREATE TABLE semesters (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    academic_year_id BIGINT      NOT NULL,
    name             VARCHAR(50) NOT NULL,
    start_date       DATE        NOT NULL,
    end_date         DATE        NOT NULL,
    is_current       BIT         NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_semester_year_name UNIQUE (academic_year_id, name),
    INDEX idx_semester_dates (start_date, end_date),
    CONSTRAINT fk_semesters_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 5. teachers - optional 1:1 link to a login account
-- ---------------------------------------------------------------------
CREATE TABLE teachers (
    id             BIGINT                               NOT NULL AUTO_INCREMENT,
    teacher_code   VARCHAR(30)                          NOT NULL,
    first_name     VARCHAR(60)                          NOT NULL,
    last_name      VARCHAR(60)                          NOT NULL,
    gender         ENUM ('FEMALE','MALE','OTHER')       NOT NULL,
    date_of_birth  DATE,
    phone          VARCHAR(20),
    email          VARCHAR(120)                         NOT NULL,
    address        VARCHAR(255),
    specialization VARCHAR(100),
    hire_date      DATE,
    status         ENUM ('ACTIVE','INACTIVE','ON_LEAVE') NOT NULL,
    user_id        BIGINT,
    created_at     DATETIME(6)                          NOT NULL,
    updated_at     DATETIME(6)                          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teachers_code UNIQUE (teacher_code),
    CONSTRAINT uk_teachers_email UNIQUE (email),
    CONSTRAINT uk_teachers_user UNIQUE (user_id),
    INDEX idx_teacher_name (last_name, first_name),
    INDEX idx_teacher_status (status),
    CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 6. classes - one homeroom group per academic year
-- ---------------------------------------------------------------------
CREATE TABLE classes (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    code                VARCHAR(30)  NOT NULL,
    name                VARCHAR(100) NOT NULL,
    grade_level         INTEGER      NOT NULL,
    section             VARCHAR(10),
    room                VARCHAR(30),
    capacity            INTEGER,
    academic_year_id    BIGINT,
    homeroom_teacher_id BIGINT,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_classes_code UNIQUE (code),
    CONSTRAINT uk_class_name_year UNIQUE (name, academic_year_id),
    INDEX idx_class_grade (grade_level),
    CONSTRAINT fk_classes_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id),
    CONSTRAINT fk_classes_homeroom_teacher FOREIGN KEY (homeroom_teacher_id) REFERENCES teachers (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4. students - class_id is the CURRENT class (history in class_students)
-- ---------------------------------------------------------------------
CREATE TABLE students (
    id            BIGINT                                                         NOT NULL AUTO_INCREMENT,
    student_code  VARCHAR(30)                                                    NOT NULL,
    first_name    VARCHAR(60)                                                    NOT NULL,
    last_name     VARCHAR(60)                                                    NOT NULL,
    gender        ENUM ('FEMALE','MALE','OTHER')                                 NOT NULL,
    date_of_birth DATE,
    phone         VARCHAR(20),
    email         VARCHAR(120),
    address       VARCHAR(255),
    class_id      BIGINT,
    status        ENUM ('ACTIVE','GRADUATED','INACTIVE','SUSPENDED','TRANSFERRED') NOT NULL,
    user_id       BIGINT,
    created_at    DATETIME(6)                                                    NOT NULL,
    updated_at    DATETIME(6)                                                    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_code UNIQUE (student_code),
    CONSTRAINT uk_students_email UNIQUE (email),
    CONSTRAINT uk_students_user UNIQUE (user_id),
    INDEX idx_student_name (last_name, first_name),
    INDEX idx_student_status (status),
    CONSTRAINT fk_students_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 7. subjects
-- ---------------------------------------------------------------------
CREATE TABLE subjects (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    credits     INTEGER,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_subjects_code UNIQUE (code),
    INDEX idx_subject_name (name)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 8. class_students - enrollment history (one row per student per class)
-- ---------------------------------------------------------------------
CREATE TABLE class_students (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    class_id      BIGINT      NOT NULL,
    student_id    BIGINT      NOT NULL,
    enrolled_date DATE        NOT NULL,
    left_date     DATE,
    active        BIT         NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_student UNIQUE (class_id, student_id),
    INDEX idx_class_student_active (class_id, active),
    CONSTRAINT fk_class_students_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_class_students_student FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 9. teacher_classes - teacher teaches (optionally a subject) in a class
-- ---------------------------------------------------------------------
CREATE TABLE teacher_classes (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT      NOT NULL,
    class_id   BIGINT      NOT NULL,
    subject_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teacher_class_subject UNIQUE (teacher_id, class_id, subject_id),
    INDEX idx_teacher_classes_class (class_id),
    CONSTRAINT fk_teacher_classes_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_teacher_classes_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_teacher_classes_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 10. attendance_sessions - one roll call: class + date + period
-- ---------------------------------------------------------------------
CREATE TABLE attendance_sessions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    class_id     BIGINT       NOT NULL,
    subject_id   BIGINT,
    semester_id  BIGINT,
    teacher_id   BIGINT,
    session_date DATE         NOT NULL,
    period       INTEGER      NOT NULL,                     -- 1 = daily roll call
    notes        VARCHAR(500),
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_session_class_date_period UNIQUE (class_id, session_date, period),
    INDEX idx_session_date (session_date),
    INDEX idx_session_teacher (teacher_id),
    CONSTRAINT fk_sessions_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_sessions_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_sessions_semester FOREIGN KEY (semester_id) REFERENCES semesters (id),
    CONSTRAINT fk_sessions_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 11. attendance_records - one student in one session
--     uk_record_session_student prevents duplicate attendance
-- ---------------------------------------------------------------------
CREATE TABLE attendance_records (
    id                     BIGINT                                     NOT NULL AUTO_INCREMENT,
    session_id             BIGINT                                     NOT NULL,
    student_id             BIGINT                                     NOT NULL,
    status                 ENUM ('ABSENT','EXCUSED','LATE','PRESENT') NOT NULL,
    check_in_time          TIME(0),
    remarks                VARCHAR(500),
    recorded_by_teacher_id BIGINT,
    recorded_by_user_id    BIGINT                                     NOT NULL,
    created_at             DATETIME(6)                                NOT NULL,
    updated_at             DATETIME(6)                                NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_record_session_student UNIQUE (session_id, student_id),
    INDEX idx_record_student_status (student_id, status),
    INDEX idx_record_status (status),
    CONSTRAINT fk_records_session FOREIGN KEY (session_id) REFERENCES attendance_sessions (id),
    CONSTRAINT fk_records_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_records_teacher FOREIGN KEY (recorded_by_teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_records_user FOREIGN KEY (recorded_by_user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 14. audit_logs - append-only trail (no FKs so it survives deletions)
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT,
    username    VARCHAR(50),
    action      ENUM ('CREATE','DELETE','LOGIN','LOGOUT','PASSWORD_CHANGE','PASSWORD_RESET','PASSWORD_RESET_REQUEST','UPDATE') NOT NULL,
    entity_name VARCHAR(50)   NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(2000),
    ip_address  VARCHAR(45),
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_entity (entity_name, entity_id),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_created (created_at)
) ENGINE = InnoDB;

-- Roles and the bootstrap admin are seeded by the application on startup
-- (DataInitializer). See db/sample-data.sql for optional demo data.
