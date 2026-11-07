# REST API Reference

Base URL: `http://localhost:8080`. Interactive docs: `/swagger-ui.html`. OpenAPI JSON: `/v3/api-docs`.

Every endpoint except login, refresh, forgot-password and reset-password requires this header:

```
Authorization: Bearer <accessToken>
```

All dates use ISO-8601: `yyyy-MM-dd`, times `HH:mm:ss`, timestamps `yyyy-MM-ddTHH:mm:ss`.
The example values below were captured from a running instance.

## Conventions

### Pagination, sorting and filtering

List endpoints accept `page` (0-based), `size` (default 20, max 100) and `sort=field,asc|desc`, which can be
repeated. They return:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

### Error format

Every error uses the same body. `fieldErrors` and `details` appear only when they apply.

| Status | When | Example `message` |
|--------|------|-------------------|
| 400 | Validation or business-rule failure | `Validation failed` |
| 401 | Missing, invalid, expired or revoked token; bad login | `Token has been revoked` |
| 403 | Wrong role, or not assigned to that class or student | `You do not have permission to perform this action` |
| 404 | Unknown id | `Student not found with id: 99` |
| 409 | Duplicate (code, email, attendance) or referenced data | `Attendance is already recorded for 1 student(s) in this session...` |
| 500 | Unexpected error (details are logged, never returned) | `An unexpected error occurred` |

```json
{
  "timestamp": "2026-11-07T02:44:58.314504Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/students",
  "fieldErrors": {
    "studentCode": "must not be blank",
    "gender": "must not be null",
    "lastName": "must not be blank",
    "firstName": "must not be blank"
  }
}
```

```json
{
  "timestamp": "2026-11-07T02:44:58.232785100Z",
  "status": 409,
  "error": "Conflict",
  "message": "Attendance is already recorded for 1 student(s) in this session. Use PUT /api/attendance/{id} to change them.",
  "path": "/api/attendance/bulk",
  "details": ["S-0001 (id 1)"]
}
```

### Roles used below

`A` = ADMIN, `T` = TEACHER, `S` = STUDENT, `*` = any authenticated user, `public` = no token.
A teacher is restricted to the classes they are assigned to (through `teacher_classes`) or are homeroom
teacher of. A student is restricted to their own data.

---

## 1. Authentication: `/api/auth`

### POST `/api/auth/login` (public)

Request:
```json
{ "usernameOrEmail": "admin", "password": "Admin@123" }
```
Response `200`:
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@school.local",
    "enabled": true,
    "roles": ["ROLE_ADMIN"],
    "teacherId": null,
    "studentId": null,
    "lastLoginAt": "2026-11-07T09:44:57.1",
    "createdAt": "2026-11-07T09:44:12.3",
    "updatedAt": "2026-11-07T09:44:57.1"
  }
}
```
Errors: `401` with `Invalid username/email or password`, or `Account is disabled. Please contact the administrator`.

### POST `/api/auth/register` (A)

Creates an account. Linking it to an existing teacher or student profile is optional. The password must be
8-100 characters and contain an uppercase letter, a lowercase letter, a digit and a special character.

```json
{
  "username": "dara.sok",
  "email": "dara.sok@school.local",
  "password": "Teacher@123",
  "roles": ["ROLE_TEACHER"],
  "teacherId": 1
}
```
Response `201`: a `UserResponse` (the same shape as `user` above, with `"teacherId": 1`).
Errors: `409` if the username or email is taken or the profile already has an account; `400` if
`teacherId` is given without `ROLE_TEACHER` (or `studentId` without `ROLE_STUDENT`).

### POST `/api/auth/refresh` (public)

```json
{ "refreshToken": "eyJhbGciOiJIUzM4NCJ9..." }
```
Response `200`: the same shape as login (a new token pair). Errors: `401` with `Invalid or expired refresh token`
or `Refresh token has been revoked`.

### POST `/api/auth/logout` (*)

No body. Revokes **all** access and refresh tokens of the user.

```json
{ "message": "Logged out successfully" }
```

### POST `/api/auth/change-password` (*)

```json
{ "currentPassword": "Admin@123", "newPassword": "NewAdmin@456", "confirmPassword": "NewAdmin@456" }
```
```json
{ "message": "Password changed successfully. Please log in again." }
```
Errors: `400` with `Current password is incorrect`, or a password-policy or confirmation mismatch.

### POST `/api/auth/forgot-password` (public)

```json
{ "email": "admin@school.local" }
```
```json
{ "message": "If an account exists for that email, a password reset link has been sent." }
```
The response is the same whether or not the email exists. The link is valid for 30 minutes and is delivered by
`PasswordResetNotifier`; the development implementation writes it to the application log.

### POST `/api/auth/reset-password` (public)

```json
{ "token": "q3H0v8...from-link", "newPassword": "Reset@789", "confirmPassword": "Reset@789" }
```
```json
{ "message": "Password has been reset. You can now log in with the new password." }
```
Errors: `400` with `Invalid or expired password reset token`.

### GET `/api/auth/me` (*)

Response `200`: a `UserResponse`.

---

## 2. Users: `/api/users` (A)

| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/users?keyword=dara&enabled=true&page=0&size=20` | none | `PageResponse<UserResponse>` |
| GET | `/api/users/{id}` | none | `UserResponse` |
| PATCH | `/api/users/{id}/status` | `{ "enabled": false }` | `UserResponse` (disabling revokes the user's tokens) |
| PUT | `/api/users/{id}/roles` | `{ "roles": ["ROLE_TEACHER", "ROLE_ADMIN"] }` | `UserResponse` (the user must log in again) |
| POST | `/api/users/{id}/reset-password` | `{ "newPassword": "Teacher@123" }` | `{ "message": "Password has been reset" }` |
| DELETE | `/api/users/{id}` | none | `204` (linked teacher or student profiles are kept and unlinked) |

Admins cannot disable or delete their own account, or remove their own admin role (`400`).

---

## 3. Students: `/api/students`

`StudentRequest` (used by create and update):
```json
{
  "studentCode": "S-0001",
  "firstName": "Bopha",
  "lastName": "Chan",
  "gender": "FEMALE",
  "dateOfBirth": "2010-03-14",
  "phone": "+855 11 111 111",
  "email": "bopha.chan@student.local",
  "address": "Phnom Penh",
  "classId": 1,
  "status": "ACTIVE"
}
```
* `gender`: `MALE | FEMALE | OTHER`. `status`: `ACTIVE | INACTIVE | SUSPENDED | GRADUATED | TRANSFERRED` (default `ACTIVE`).
* `classId` is optional. Setting it enrolls the student; changing it moves the student; sending `null` on update removes the student from the class.

`StudentResponse`:
```json
{
  "id": 1,
  "studentCode": "S-0001",
  "firstName": "Bopha",
  "lastName": "Chan",
  "fullName": "Bopha Chan",
  "gender": "FEMALE",
  "dateOfBirth": "2010-03-14",
  "phone": "+855 11 111 111",
  "email": "bopha.chan@student.local",
  "address": "Phnom Penh",
  "classId": 1,
  "className": "Grade 10 - A",
  "status": "ACTIVE",
  "userId": 3,
  "createdAt": "2026-11-07T09:44:42.9",
  "updatedAt": "2026-11-07T09:44:43.1"
}
```

| Method | Path | Roles | Body | Response |
|--------|------|-------|------|----------|
| POST | `/api/students` | A | `StudentRequest` | `201 StudentResponse` |
| GET | `/api/students?keyword=chan&classId=1&status=ACTIVE&gender=FEMALE&sort=lastName,asc` | A, T | none | `PageResponse<StudentResponse>` (teachers only see students of their classes) |
| GET | `/api/students/me` | S | none | `StudentResponse` |
| GET | `/api/students/{id}` | * | none | `StudentResponse` (the admin, the student, or a teacher of the student's class) |
| PUT | `/api/students/{id}` | A | `StudentRequest` | `StudentResponse` |
| DELETE | `/api/students/{id}` | A | none | `204`; `409` if the student has attendance history |
| PUT | `/api/students/{id}/class` | A | `{ "classId": 2 }` | `StudentResponse` |
| DELETE | `/api/students/{id}/class` | A | none | `StudentResponse` with `classId: null` |

Errors: `409` for a duplicate `studentCode` or `email`, or `Class G10A-2026 is full (capacity 40)`.

---

## 4. Teachers: `/api/teachers`

`TeacherRequest`:
```json
{
  "teacherCode": "T-001",
  "firstName": "Dara",
  "lastName": "Sok",
  "gender": "MALE",
  "dateOfBirth": "1988-04-12",
  "phone": "+855 12 345 678",
  "email": "dara.sok@school.local",
  "address": "Phnom Penh",
  "specialization": "Mathematics",
  "hireDate": "2020-08-01",
  "status": "ACTIVE"
}
```
`TeacherResponse` has the same fields plus `id`, `fullName`, `userId`, `createdAt` and `updatedAt`.

| Method | Path | Roles | Body | Response |
|--------|------|-------|------|----------|
| POST | `/api/teachers` | A | `TeacherRequest` | `201 TeacherResponse` |
| GET | `/api/teachers?keyword=dara&status=ACTIVE&specialization=math` | A | none | `PageResponse<TeacherResponse>` |
| GET | `/api/teachers/me` | T | none | `TeacherResponse` |
| GET | `/api/teachers/{id}` | A, T | none | `TeacherResponse` |
| PUT | `/api/teachers/{id}` | A | `TeacherRequest` | `TeacherResponse` |
| DELETE | `/api/teachers/{id}` | A | none | `204`; `409` if the teacher has recorded attendance |
| GET | `/api/teachers/{id}/assignments` | A, T (own) | none | `TeacherAssignmentResponse[]` |
| POST | `/api/teachers/{id}/assignments` | A | `{ "classId": 1, "subjectId": 1 }` | `201 TeacherAssignmentResponse` |
| DELETE | `/api/teachers/{id}/assignments/{assignmentId}` | A | none | `204` |

`TeacherAssignmentResponse`:
```json
{
  "id": 1,
  "teacherId": 1,
  "teacherName": "Dara Sok",
  "classId": 1,
  "className": "Grade 10 - A",
  "subjectId": 1,
  "subjectName": "Mathematics",
  "createdAt": "2026-11-07T09:44:42.7"
}
```
`subjectId` is optional (a general class assignment). Assigning the same teacher, class and subject twice returns `409`.

---

## 5. Classes: `/api/classes`

`SchoolClassRequest`:
```json
{
  "code": "G10A-2026",
  "name": "Grade 10 - A",
  "gradeLevel": 10,
  "section": "A",
  "room": "B-201",
  "capacity": 40,
  "academicYearId": 1,
  "homeroomTeacherId": 1
}
```
`academicYearId` defaults to the current academic year. `gradeLevel` must be between 1 and 13.

`SchoolClassResponse`:
```json
{
  "id": 1,
  "code": "G10A-2026",
  "name": "Grade 10 - A",
  "gradeLevel": 10,
  "section": "A",
  "room": "B-305",
  "capacity": 42,
  "academicYearId": 1,
  "academicYearName": "2026-2027",
  "homeroomTeacherId": 1,
  "homeroomTeacherName": "Dara Sok",
  "studentCount": 2,
  "createdAt": "2026-11-07T09:44:42.268249",
  "updatedAt": "2026-11-07T09:44:42.604328"
}
```

| Method | Path | Roles | Body | Response |
|--------|------|-------|------|----------|
| POST | `/api/classes` | A | `SchoolClassRequest` | `201 SchoolClassResponse` |
| GET | `/api/classes?keyword=grade&gradeLevel=10&academicYearId=1` | A | none | `PageResponse<SchoolClassResponse>` |
| GET | `/api/classes/my` | T, S | none | `SchoolClassResponse[]` (a teacher's classes, or the student's class) |
| GET | `/api/classes/{id}` | * | none | `SchoolClassResponse` (access checked) |
| PUT | `/api/classes/{id}` | A | `SchoolClassRequest` | `SchoolClassResponse`; `400` if the capacity is below the current number of students |
| DELETE | `/api/classes/{id}` | A | none | `204`; `409` if the class has attendance sessions |
| GET | `/api/classes/{id}/students` | A, T | none | `PageResponse<StudentResponse>` |
| POST | `/api/classes/{id}/students` | A | `{ "studentIds": [1, 2, 3] }` | `StudentResponse[]` (moves students from their previous class) |
| DELETE | `/api/classes/{id}/students/{studentId}` | A | none | `204` |
| PUT | `/api/classes/{id}/homeroom-teacher` | A | `{ "teacherId": 1 }` | `SchoolClassResponse` |
| DELETE | `/api/classes/{id}/homeroom-teacher` | A | none | `SchoolClassResponse` |
| GET | `/api/classes/{id}/teachers` | A, T | none | `TeacherAssignmentResponse[]` |

---

## 6. Subjects: `/api/subjects`

Request:
```json
{ "code": "MATH", "name": "Mathematics", "description": "Algebra and geometry", "credits": 4 }
```
Response:
```json
{
  "id": 1,
  "code": "MATH",
  "name": "Mathematics",
  "description": "Algebra and geometry",
  "credits": 4,
  "createdAt": "2026-11-07T09:44:41.9",
  "updatedAt": "2026-11-07T09:44:41.9"
}
```

| Method | Path | Roles | Response |
|--------|------|-------|----------|
| POST | `/api/subjects` | A | `201 SubjectResponse` (`code` is stored in upper case) |
| GET | `/api/subjects?keyword=math` | * | `PageResponse<SubjectResponse>` |
| GET | `/api/subjects/{id}` | * | `SubjectResponse` |
| PUT | `/api/subjects/{id}` | A | `SubjectResponse` |
| DELETE | `/api/subjects/{id}` | A | `204`; `409` if the subject is used by assignments or sessions |

---

## 7. Academic years and semesters

### `/api/academic-years`

Request:
```json
{ "name": "2026-2027", "startDate": "2026-08-01", "endDate": "2027-06-30", "current": true }
```
Response:
```json
{
  "id": 1,
  "name": "2026-2027",
  "startDate": "2026-08-01",
  "endDate": "2027-06-30",
  "current": true,
  "createdAt": "...",
  "updatedAt": "..."
}
```

| Method | Path | Roles | Response |
|--------|------|-------|----------|
| POST | `/api/academic-years` | A | `201` (`current: true` clears the flag on every other year) |
| GET | `/api/academic-years` | * | `AcademicYearResponse[]`, newest first |
| GET | `/api/academic-years/{id}` | * | `AcademicYearResponse` |
| PUT | `/api/academic-years/{id}` | A | `AcademicYearResponse` |
| DELETE | `/api/academic-years/{id}` | A | `204`; `409` if the year has semesters or classes |

### `/api/semesters`

Request:
```json
{ "academicYearId": 1, "name": "Semester 1", "startDate": "2026-08-01", "endDate": "2027-01-15", "current": true }
```
Response:
```json
{
  "id": 1,
  "academicYearId": 1,
  "academicYearName": "2026-2027",
  "name": "Semester 1",
  "startDate": "2026-08-01",
  "endDate": "2027-01-15",
  "current": true,
  "createdAt": "...",
  "updatedAt": "..."
}
```

| Method | Path | Roles | Response |
|--------|------|-------|----------|
| POST | `/api/semesters` | A | `201`; `400` if the dates fall outside the academic year |
| GET | `/api/semesters?academicYearId=1` | * | `SemesterResponse[]` |
| GET | `/api/semesters/{id}` | * | `SemesterResponse` |
| PUT | `/api/semesters/{id}` | A | `SemesterResponse` |
| DELETE | `/api/semesters/{id}` | A | `204`; `409` if the semester has attendance sessions |

---

## 8. Attendance: `/api/attendance`

**Model.** A *session* is one roll call: a class on a date for a `period` (default `1`, the daily roll call;
use 2, 3 and so on for subject periods). A session is created automatically the first time attendance is
recorded for that class, date and period. The teacher and semester are filled in automatically. There is at
most one record per student per session (enforced by a database unique key).
Statuses: `PRESENT | ABSENT | LATE | EXCUSED`. For `ABSENT`, `checkInTime` is stored as `null`.

### POST `/api/attendance` (A, T of the class): single student

```json
{
  "classId": 1,
  "subjectId": 1,
  "sessionDate": "2026-11-07",
  "period": 2,
  "studentId": 1,
  "status": "LATE",
  "checkInTime": "09:12:00",
  "remarks": "Arrived after break"
}
```
Response `201` (`AttendanceResponse`):
```json
{
  "id": 3,
  "sessionId": 2,
  "sessionDate": "2026-11-07",
  "period": 2,
  "classId": 1,
  "className": "Grade 10 - A",
  "subjectId": 1,
  "subjectName": "Mathematics",
  "studentId": 1,
  "studentCode": "S-0001",
  "studentName": "Bopha Chan",
  "status": "LATE",
  "checkInTime": "09:12:00",
  "remarks": "Arrived after break",
  "recordedByTeacherId": 1,
  "recordedByTeacherName": "Dara Sok",
  "recordedByUsername": "dara.sok",
  "createdAt": "2026-11-07T09:44:45.9",
  "updatedAt": "2026-11-07T09:44:45.9"
}
```
Errors:
* `409`: the student already has a record in that session.
* `400`: the student is not an active member of the class, or the date is in the future.
* `403`: the teacher is not assigned to the class.

### POST `/api/attendance/bulk` (A, T of the class): whole class

The request is all or nothing: if any student fails validation, nothing is saved.

```json
{
  "classId": 1,
  "sessionDate": "2026-11-07",
  "period": 1,
  "notes": "Morning roll call",
  "records": [
    { "studentId": 1, "status": "PRESENT", "checkInTime": "07:25:00" },
    { "studentId": 2, "status": "ABSENT", "remarks": "No notice from parents" }
  ]
}
```
Response `201`:
```json
{
  "session": {
    "id": 1,
    "classId": 1,
    "className": "Grade 10 - A",
    "subjectId": null,
    "subjectName": null,
    "semesterId": 1,
    "semesterName": "Semester 1",
    "teacherId": 1,
    "teacherName": "Dara Sok",
    "sessionDate": "2026-11-07",
    "period": 1,
    "notes": "Morning roll call",
    "recordCount": 2,
    "createdAt": "2026-11-07T09:44:45.2"
  },
  "recordedCount": 2,
  "summary": { "totalRecords": 2, "present": 1, "absent": 1, "late": 0, "excused": 0, "attendancePercentage": 50.0 },
  "unmarkedStudentIds": [],
  "records": [ { "id": 1, "studentCode": "S-0001", "status": "PRESENT", "...": "..." }, { "id": 2, "...": "..." } ]
}
```
`unmarkedStudentIds` lists active students of the class who still have no record in this session.
Errors:
* `409`: the same student appears twice in the request, or some students are already recorded. `details` lists them.
* `404`: an unknown student id.
* `400`: a student is not in the class.

### PUT `/api/attendance/{id}` (A, T of the class)

```json
{ "status": "LATE", "checkInTime": "07:48:00", "remarks": "Traffic" }
```
Response `200`: an `AttendanceResponse`. Teachers may only edit sessions from the last
`app.attendance.teacher-edit-window-days` days (default 7); older edits return `403` and require an admin.
Every change is written to the audit log (`PRESENT -> LATE`).

### Queries

| Method | Path | Roles | Response |
|--------|------|-------|----------|
| GET | `/api/attendance?classId=&studentId=&subjectId=&status=&date=&fromDate=&toDate=&page=&size=&sort=` | * | `PageResponse<AttendanceResponse>`. Scoped automatically: admins see all, teachers see their classes, students see only themselves. Default sort: `session.sessionDate,desc`. |
| GET | `/api/attendance/me` | S | The student's own records (same filters) |
| GET | `/api/attendance/{id}` | * | `AttendanceResponse` (access checked) |
| DELETE | `/api/attendance/{id}` | A | `204` |
| GET | `/api/attendance/student/{studentId}?fromDate=2026-08-01` | * | Paged student history (admin, the student, or a teacher of the student's class) |
| GET | `/api/attendance/class/{classId}?date=2026-11-07&status=ABSENT` | A, T | Paged class attendance |
| GET | `/api/attendance/sessions?classId=1&fromDate=&toDate=` | A, T | `PageResponse<AttendanceSessionResponse>` |
| GET | `/api/attendance/sessions/{sessionId}/records` | A, T | `AttendanceResponse[]` for one roll call |
| GET | `/api/attendance/report?period=MONTHLY&date=2026-11-07&classId=1` | A, T | The same as `/api/reports/attendance` |

---

## 9. Reports: `/api/reports`

**Attendance percentage = (Present / Total recorded sessions) x 100**, rounded to 2 decimals. With one roll call
per day, the total equals the number of school days. `LATE` counts as present when
`app.attendance.count-late-as-present=true` (the default). `EXCUSED` counts toward the total but not as present.

Period parameters (`ReportQuery`):

| `period` | Window |
|----------|--------|
| `DAILY` (default) | `date` (default today) |
| `WEEKLY` | Monday to Sunday of the week containing `date` |
| `MONTHLY` | The calendar month of `date` |
| `SEMESTER` | `semesterId`, or the semester containing `date` |
| `CUSTOM` | `fromDate` to `toDate` (inclusive, at most 366 days); assumed when only the dates are given |

### GET `/api/reports/attendance?period=DAILY&classId=1` (A; T requires `classId` of their class)

```json
{
  "period": "DAILY",
  "fromDate": "2026-11-07",
  "toDate": "2026-11-07",
  "classId": 1,
  "className": "Grade 10 - A",
  "totalSchoolDays": 1,
  "totalSessions": 2,
  "overall": { "totalRecords": 3, "present": 0, "absent": 1, "late": 2, "excused": 0, "attendancePercentage": 66.67 },
  "students": [
    {
      "studentId": 1, "studentCode": "S-0001", "studentName": "Bopha Chan", "className": "Grade 10 - A",
      "stats": { "totalRecords": 2, "present": 0, "absent": 0, "late": 2, "excused": 0, "attendancePercentage": 100.0 }
    },
    {
      "studentId": 2, "studentCode": "S-0002", "studentName": "Rithy Kim", "className": "Grade 10 - A",
      "stats": { "totalRecords": 1, "present": 0, "absent": 1, "late": 0, "excused": 0, "attendancePercentage": 0.0 }
    }
  ],
  "generatedAt": "2026-11-07T09:44:57.7592231"
}
```
When `classId` is given, active students with no records yet are included with zero counts.

| Method | Path | Roles | Response |
|--------|------|-------|----------|
| GET | `/api/reports/attendance?period=WEEKLY\|MONTHLY\|SEMESTER\|CUSTOM...` | A, T | `AttendanceReportResponse` (above) |
| GET | `/api/reports/class/{classId}?fromDate=&toDate=` | A, T | Class summary (defaults to the current month) |
| GET | `/api/reports/student/{studentId}?fromDate=&toDate=` | * (access checked) | `StudentAttendanceReportResponse` (below) |
| GET | `/api/reports/absent?period=DAILY&date=&classId=` | * (scoped) | `AttendanceResponse[]` (the absent student list) |
| GET | `/api/reports/late?period=WEEKLY&date=` | * (scoped) | `AttendanceResponse[]` (the late student list) |
| GET | `/api/reports/export?type=SUMMARY\|ABSENT\|LATE\|STUDENT_HISTORY&format=EXCEL\|PDF&...` | * (scoped) | A file download (`Content-Disposition: attachment`) |

`StudentAttendanceReportResponse`:
```json
{
  "studentId": 1,
  "studentCode": "S-0001",
  "studentName": "Bopha Chan",
  "className": "Grade 10 - A",
  "fromDate": null,
  "toDate": null,
  "stats": { "totalRecords": 2, "present": 0, "absent": 0, "late": 2, "excused": 0, "attendancePercentage": 100.0 },
  "records": [ { "id": 3, "sessionDate": "2026-11-07", "period": 2, "status": "LATE", "...": "..." } ]
}
```

Export examples:
```
GET /api/reports/export?type=SUMMARY&format=EXCEL&period=MONTHLY&date=2026-11-07&classId=1
  -> 200, Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     Content-Disposition: attachment; filename="attendance-summary-2026-11-07.xlsx"

GET /api/reports/export?type=STUDENT_HISTORY&format=PDF&studentId=1
  -> 200, Content-Type: application/pdf
```
`STUDENT_HISTORY` requires `studentId` (otherwise `400`).

---

## 10. Dashboards: `/api/dashboard`

### GET `/api/dashboard/admin` (A)
```json
{
  "date": "2026-11-07",
  "totalStudents": 2,
  "totalTeachers": 1,
  "totalClasses": 1,
  "totalSubjects": 1,
  "todaySessions": 2,
  "todayPresent": 0,
  "todayAbsent": 1,
  "todayLate": 2,
  "todayExcused": 0,
  "todayAttendancePercentage": 66.67,
  "classesNotMarkedToday": 0,
  "today": { "totalRecords": 3, "present": 0, "absent": 1, "late": 2, "excused": 0, "attendancePercentage": 66.67 }
}
```
The totals count `ACTIVE` students and teachers. `classesNotMarkedToday` counts classes that have active students
but no roll call yet today.

### GET `/api/dashboard/teacher` (T)
```json
{
  "teacherId": 1,
  "teacherName": "Dara Sok",
  "date": "2026-11-07",
  "assignedClassCount": 1,
  "assignedClasses": [
    { "classId": 1, "code": "G10A-2026", "name": "Grade 10 - A", "gradeLevel": 10, "studentCount": 2, "attendanceTakenToday": true }
  ],
  "totalStudents": 2,
  "todaySummary": { "totalRecords": 3, "present": 0, "absent": 1, "late": 2, "excused": 0, "attendancePercentage": 66.67 },
  "recentRecords": [ { "id": 3, "studentName": "Bopha Chan", "status": "LATE", "...": "..." } ]
}
```

### GET `/api/dashboard/student` (S)
```json
{
  "studentId": 1,
  "studentCode": "S-0001",
  "studentName": "Bopha Chan",
  "className": "Grade 10 - A",
  "attendancePercentage": 100.0,
  "totalRecords": 2,
  "totalPresent": 0,
  "totalAbsent": 0,
  "totalLate": 2,
  "totalExcused": 0,
  "recentHistory": [ { "id": 3, "sessionDate": "2026-11-07", "status": "LATE", "...": "..." } ]
}
```
The figures cover all time; `recentHistory` holds the latest 10 records.

---

## 11. Audit logs: `/api/audit-logs` (A)

`GET /api/audit-logs?entityName=AttendanceRecord&entityId=1&username=dara.sok`
```json
{
  "content": [
    {
      "id": 57,
      "userId": 2,
      "username": "dara.sok",
      "action": "UPDATE",
      "entityName": "AttendanceRecord",
      "entityId": 1,
      "details": "Attendance of S-0001 on 2026-11-07 changed PRESENT -> LATE",
      "ipAddress": "127.0.0.1",
      "createdAt": "2026-11-07T09:44:45.8"
    }
  ],
  "page": 0, "size": 50, "totalElements": 1, "totalPages": 1, "first": true, "last": true
}
```
Actions: `CREATE, UPDATE, DELETE, LOGIN, LOGOUT, PASSWORD_CHANGE, PASSWORD_RESET_REQUEST, PASSWORD_RESET`.
