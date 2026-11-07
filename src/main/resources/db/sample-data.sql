-- =====================================================================
-- Optional demo data (run AFTER the application has started once, so
-- roles and the admin account already exist).
--
-- Accounts are not inserted here: create them through
-- POST /api/auth/register so passwords are BCrypt-hashed by the app.
-- =====================================================================
USE school_attendance_db;

INSERT INTO academic_years (name, start_date, end_date, is_current, created_at, updated_at)
VALUES ('2026-2027', '2026-08-01', '2027-06-30', b'1', NOW(6), NOW(6));
SET @year_id = LAST_INSERT_ID();

INSERT INTO semesters (academic_year_id, name, start_date, end_date, is_current, created_at, updated_at)
VALUES (@year_id, 'Semester 1', '2026-08-01', '2027-01-15', b'1', NOW(6), NOW(6)),
       (@year_id, 'Semester 2', '2027-01-16', '2027-06-30', b'0', NOW(6), NOW(6));

INSERT INTO subjects (code, name, description, credits, created_at, updated_at)
VALUES ('MATH', 'Mathematics', 'Algebra, geometry and statistics', 4, NOW(6), NOW(6)),
       ('KHM', 'Khmer Literature', 'Reading, writing and literature', 3, NOW(6), NOW(6)),
       ('ENG', 'English', 'English as a foreign language', 3, NOW(6), NOW(6)),
       ('PHY', 'Physics', 'Mechanics, electricity and waves', 3, NOW(6), NOW(6));

INSERT INTO teachers (teacher_code, first_name, last_name, gender, phone, email, specialization, hire_date, status,
                      created_at, updated_at)
VALUES ('T-001', 'Dara', 'Sok', 'MALE', '+855 12 345 678', 'dara.sok@school.local', 'Mathematics', '2020-08-01',
        'ACTIVE', NOW(6), NOW(6)),
       ('T-002', 'Sreymom', 'Chea', 'FEMALE', '+855 17 222 333', 'sreymom.chea@school.local', 'English', '2021-08-01',
        'ACTIVE', NOW(6), NOW(6));
SET @t1 = (SELECT id FROM teachers WHERE teacher_code = 'T-001');
SET @t2 = (SELECT id FROM teachers WHERE teacher_code = 'T-002');

INSERT INTO classes (code, name, grade_level, section, room, capacity, academic_year_id, homeroom_teacher_id,
                     created_at, updated_at)
VALUES ('G10A-2026', 'Grade 10 - A', 10, 'A', 'B-201', 40, @year_id, @t1, NOW(6), NOW(6)),
       ('G10B-2026', 'Grade 10 - B', 10, 'B', 'B-202', 40, @year_id, @t2, NOW(6), NOW(6));
SET @c1 = (SELECT id FROM classes WHERE code = 'G10A-2026');
SET @c2 = (SELECT id FROM classes WHERE code = 'G10B-2026');

INSERT INTO teacher_classes (teacher_id, class_id, subject_id, created_at, updated_at)
VALUES (@t1, @c1, (SELECT id FROM subjects WHERE code = 'MATH'), NOW(6), NOW(6)),
       (@t1, @c2, (SELECT id FROM subjects WHERE code = 'MATH'), NOW(6), NOW(6)),
       (@t2, @c1, (SELECT id FROM subjects WHERE code = 'ENG'), NOW(6), NOW(6));

INSERT INTO students (student_code, first_name, last_name, gender, date_of_birth, phone, email, address, class_id,
                      status, created_at, updated_at)
VALUES ('S-0001', 'Bopha', 'Chan', 'FEMALE', '2010-03-14', '+855 11 111 111', 'bopha.chan@student.local',
        'Phnom Penh', @c1, 'ACTIVE', NOW(6), NOW(6)),
       ('S-0002', 'Rithy', 'Kim', 'MALE', '2010-07-02', NULL, 'rithy.kim@student.local', 'Phnom Penh', @c1,
        'ACTIVE', NOW(6), NOW(6)),
       ('S-0003', 'Sophea', 'Long', 'FEMALE', '2010-11-21', NULL, NULL, 'Kandal', @c1, 'ACTIVE', NOW(6), NOW(6)),
       ('S-0004', 'Vuthy', 'Meas', 'MALE', '2010-01-30', NULL, NULL, 'Takeo', @c2, 'ACTIVE', NOW(6), NOW(6));

INSERT INTO class_students (class_id, student_id, enrolled_date, active, created_at, updated_at)
SELECT class_id, id, '2026-08-01', b'1', NOW(6), NOW(6)
FROM students
WHERE student_code IN ('S-0001', 'S-0002', 'S-0003', 'S-0004');
