// Generates the Postman v2.1 collection + environment for the School Attendance API.
// Usage: node gen-postman.js <outputDir>
const fs = require('fs');
const path = require('path');

const out = process.argv[2];
if (!out) { console.error('output dir required'); process.exit(1); }
fs.mkdirSync(out, { recursive: true });

const saveId = (varName) => [
  `if (pm.response.code === 200 || pm.response.code === 201) {`,
  `  const body = pm.response.json();`,
  `  if (body.id) pm.collectionVariables.set('${varName}', body.id);`,
  `}`,
];
const expectStatus = (code) => [
  `pm.test('Status is ${code}', () => pm.response.to.have.status(${code}));`,
];

function req(name, method, urlPath, { body, query, token = 'accessToken', tests = [], auth = true, desc } = {}) {
  const [rawPath, rawQuery] = urlPath.split('?');
  const segments = rawPath.replace(/^\//, '').split('/');
  const queryParams = [];
  if (rawQuery) {
    for (const kv of rawQuery.split('&')) {
      const [key, value] = kv.split('=');
      queryParams.push({ key, value });
    }
  }
  if (query) queryParams.push(...query);
  const item = {
    name,
    request: {
      method,
      header: [{ key: 'Content-Type', value: 'application/json' }],
      url: {
        raw: '{{baseUrl}}' + urlPath,
        host: ['{{baseUrl}}'],
        path: segments,
        ...(queryParams.length ? { query: queryParams } : {}),
      },
      ...(desc ? { description: desc } : {}),
    },
  };
  if (auth) {
    item.request.auth = { type: 'bearer', bearer: [{ key: 'token', value: `{{${token}}}`, type: 'string' }] };
  } else {
    item.request.auth = { type: 'noauth' };
  }
  if (body !== undefined) {
    // Id placeholders are emitted unquoted so the substituted values are JSON numbers.
    const raw = JSON.stringify(body, null, 2).replace(/"\{\{(\w+Id\d?)\}\}"/g, '{{$1}}');
    item.request.body = { mode: 'raw', raw, options: { raw: { language: 'json' } } };
  }
  if (tests.length) {
    item.event = [{ listen: 'test', script: { type: 'text/javascript', exec: tests } }];
  }
  return item;
}

const loginTests = (accessVar, refreshVar) => [
  ...expectStatus(200),
  `const body = pm.response.json();`,
  `pm.collectionVariables.set('${accessVar}', body.accessToken);`,
  ...(refreshVar ? [`pm.collectionVariables.set('${refreshVar}', body.refreshToken);`] : []),
];

const folders = [
  {
    name: '1. Authentication',
    item: [
      req('Login (admin)', 'POST', '/api/auth/login', {
        auth: false, body: { usernameOrEmail: '{{adminUsername}}', password: '{{adminPassword}}' },
        tests: loginTests('accessToken', 'refreshToken'),
      }),
      req('Register teacher account (ADMIN)', 'POST', '/api/auth/register', {
        body: { username: 'dara.sok', email: 'dara.sok@school.local', password: 'Teacher@123', roles: ['ROLE_TEACHER'], teacherId: '{{teacherId}}' },
        tests: [...expectStatus(201), ...saveId('teacherUserId')],
      }),
      req('Register student account (ADMIN)', 'POST', '/api/auth/register', {
        body: { username: 'bopha.chan', email: 'bopha.chan@student.local', password: 'Student@123', roles: ['ROLE_STUDENT'], studentId: '{{studentId}}' },
        tests: [...expectStatus(201), ...saveId('studentUserId')],
      }),
      req('Login (teacher)', 'POST', '/api/auth/login', {
        auth: false, body: { usernameOrEmail: 'dara.sok', password: 'Teacher@123' },
        tests: loginTests('teacherToken'),
      }),
      req('Login (student)', 'POST', '/api/auth/login', {
        auth: false, body: { usernameOrEmail: 'bopha.chan', password: 'Student@123' },
        tests: loginTests('studentToken'),
      }),
      req('Refresh token', 'POST', '/api/auth/refresh', {
        auth: false, body: { refreshToken: '{{refreshToken}}' }, tests: loginTests('accessToken', 'refreshToken'),
      }),
      req('Current user', 'GET', '/api/auth/me', { tests: expectStatus(200) }),
      req('Change password', 'POST', '/api/auth/change-password', {
        body: { currentPassword: 'Admin@123', newPassword: 'NewAdmin@456', confirmPassword: 'NewAdmin@456' },
        desc: 'Revokes all tokens of the user. Log in again afterwards.',
      }),
      req('Forgot password', 'POST', '/api/auth/forgot-password', { auth: false, body: { email: 'admin@school.local' } }),
      req('Reset password', 'POST', '/api/auth/reset-password', {
        auth: false, body: { token: '<token-from-reset-link>', newPassword: 'Reset@789', confirmPassword: 'Reset@789' },
      }),
      req('Logout', 'POST', '/api/auth/logout', { tests: expectStatus(200) }),
    ],
  },
  {
    name: '2. Users (ADMIN)',
    item: [
      req('Search users', 'GET', '/api/users?keyword=&page=0&size=20', { tests: expectStatus(200) }),
      req('Get user', 'GET', '/api/users/{{teacherUserId}}'),
      req('Disable / enable user', 'PATCH', '/api/users/{{teacherUserId}}/status', { body: { enabled: true } }),
      req('Replace roles', 'PUT', '/api/users/{{teacherUserId}}/roles', { body: { roles: ['ROLE_TEACHER'] } }),
      req('Admin reset password', 'POST', '/api/users/{{teacherUserId}}/reset-password', { body: { newPassword: 'Teacher@123' } }),
      req('Delete user', 'DELETE', '/api/users/{{studentUserId}}'),
    ],
  },
  {
    name: '3. Academic years & semesters',
    item: [
      req('Create academic year', 'POST', '/api/academic-years', {
        body: { name: '2026-2027', startDate: '2026-08-01', endDate: '2027-06-30', current: true },
        tests: [...expectStatus(201), ...saveId('academicYearId')],
      }),
      req('List academic years', 'GET', '/api/academic-years'),
      req('Get academic year', 'GET', '/api/academic-years/{{academicYearId}}'),
      req('Update academic year', 'PUT', '/api/academic-years/{{academicYearId}}', {
        body: { name: '2026-2027', startDate: '2026-08-01', endDate: '2027-06-30', current: true },
      }),
      req('Create semester', 'POST', '/api/semesters', {
        body: { academicYearId: '{{academicYearId}}', name: 'Semester 1', startDate: '2026-08-01', endDate: '2027-01-15', current: true },
        tests: [...expectStatus(201), ...saveId('semesterId')],
      }),
      req('List semesters', 'GET', '/api/semesters?academicYearId={{academicYearId}}'),
      req('Get semester', 'GET', '/api/semesters/{{semesterId}}'),
      req('Update semester', 'PUT', '/api/semesters/{{semesterId}}', {
        body: { academicYearId: '{{academicYearId}}', name: 'Semester 1', startDate: '2026-08-01', endDate: '2027-01-15', current: true },
      }),
      req('Delete semester', 'DELETE', '/api/semesters/{{semesterId}}'),
      req('Delete academic year', 'DELETE', '/api/academic-years/{{academicYearId}}'),
    ],
  },
  {
    name: '4. Subjects',
    item: [
      req('Create subject', 'POST', '/api/subjects', {
        body: { code: 'MATH', name: 'Mathematics', description: 'Algebra and geometry', credits: 4 },
        tests: [...expectStatus(201), ...saveId('subjectId')],
      }),
      req('Search subjects', 'GET', '/api/subjects?keyword=math'),
      req('Get subject', 'GET', '/api/subjects/{{subjectId}}'),
      req('Update subject', 'PUT', '/api/subjects/{{subjectId}}', {
        body: { code: 'MATH', name: 'Mathematics', description: 'Algebra, geometry and statistics', credits: 4 },
      }),
      req('Delete subject', 'DELETE', '/api/subjects/{{subjectId}}'),
    ],
  },
  {
    name: '5. Teachers',
    item: [
      req('Create teacher', 'POST', '/api/teachers', {
        body: {
          teacherCode: 'T-001', firstName: 'Dara', lastName: 'Sok', gender: 'MALE', dateOfBirth: '1988-04-12',
          phone: '+855 12 345 678', email: 'dara.sok@school.local', address: 'Phnom Penh',
          specialization: 'Mathematics', hireDate: '2020-08-01', status: 'ACTIVE',
        },
        tests: [...expectStatus(201), ...saveId('teacherId')],
      }),
      req('Search teachers', 'GET', '/api/teachers?keyword=dara&status=ACTIVE'),
      req('Get teacher', 'GET', '/api/teachers/{{teacherId}}'),
      req('My teacher profile (TEACHER)', 'GET', '/api/teachers/me', { token: 'teacherToken' }),
      req('Update teacher', 'PUT', '/api/teachers/{{teacherId}}', {
        body: {
          teacherCode: 'T-001', firstName: 'Dara', lastName: 'Sok', gender: 'MALE', dateOfBirth: '1988-04-12',
          phone: '+855 12 999 888', email: 'dara.sok@school.local', address: 'Phnom Penh',
          specialization: 'Mathematics', hireDate: '2020-08-01', status: 'ACTIVE',
        },
      }),
      req('Assign teacher to class/subject', 'POST', '/api/teachers/{{teacherId}}/assignments', {
        body: { classId: '{{classId}}', subjectId: '{{subjectId}}' },
        tests: [...expectStatus(201), ...saveId('assignmentId')],
      }),
      req('List teacher assignments', 'GET', '/api/teachers/{{teacherId}}/assignments'),
      req('Remove assignment', 'DELETE', '/api/teachers/{{teacherId}}/assignments/{{assignmentId}}'),
      req('Delete teacher', 'DELETE', '/api/teachers/{{teacherId}}'),
    ],
  },
  {
    name: '6. Classes',
    item: [
      req('Create class', 'POST', '/api/classes', {
        body: {
          code: 'G10A-2026', name: 'Grade 10 - A', gradeLevel: 10, section: 'A', room: 'B-201', capacity: 40,
          academicYearId: '{{academicYearId}}', homeroomTeacherId: '{{teacherId}}',
        },
        tests: [...expectStatus(201), ...saveId('classId')],
      }),
      req('Search classes', 'GET', '/api/classes?gradeLevel=10'),
      req('My classes (TEACHER/STUDENT)', 'GET', '/api/classes/my', { token: 'teacherToken' }),
      req('Get class', 'GET', '/api/classes/{{classId}}'),
      req('Update class', 'PUT', '/api/classes/{{classId}}', {
        body: {
          code: 'G10A-2026', name: 'Grade 10 - A', gradeLevel: 10, section: 'A', room: 'B-305', capacity: 42,
          academicYearId: '{{academicYearId}}', homeroomTeacherId: '{{teacherId}}',
        },
      }),
      req('Class students', 'GET', '/api/classes/{{classId}}/students'),
      req('Enroll students', 'POST', '/api/classes/{{classId}}/students', { body: { studentIds: ['{{studentId}}'] } }),
      req('Remove student from class', 'DELETE', '/api/classes/{{classId}}/students/{{studentId}}'),
      req('Set homeroom teacher', 'PUT', '/api/classes/{{classId}}/homeroom-teacher', { body: { teacherId: '{{teacherId}}' } }),
      req('Remove homeroom teacher', 'DELETE', '/api/classes/{{classId}}/homeroom-teacher'),
      req('Class teachers', 'GET', '/api/classes/{{classId}}/teachers'),
      req('Delete class', 'DELETE', '/api/classes/{{classId}}'),
    ],
  },
  {
    name: '7. Students',
    item: [
      req('Create student', 'POST', '/api/students', {
        body: {
          studentCode: 'S-0001', firstName: 'Bopha', lastName: 'Chan', gender: 'FEMALE', dateOfBirth: '2010-03-14',
          phone: '+855 11 111 111', email: 'bopha.chan@student.local', address: 'Phnom Penh',
          classId: '{{classId}}', status: 'ACTIVE',
        },
        tests: [...expectStatus(201), ...saveId('studentId')],
      }),
      req('Create second student', 'POST', '/api/students', {
        body: { studentCode: 'S-0002', firstName: 'Rithy', lastName: 'Kim', gender: 'MALE', classId: '{{classId}}' },
        tests: [...expectStatus(201), ...saveId('studentId2')],
      }),
      req('Search students', 'GET', '/api/students?keyword=chan&classId={{classId}}&status=ACTIVE&page=0&size=20&sort=lastName,asc'),
      req('Get student', 'GET', '/api/students/{{studentId}}'),
      req('My student profile (STUDENT)', 'GET', '/api/students/me', { token: 'studentToken' }),
      req('Update student', 'PUT', '/api/students/{{studentId}}', {
        body: {
          studentCode: 'S-0001', firstName: 'Bopha', lastName: 'Chan', gender: 'FEMALE', dateOfBirth: '2010-03-14',
          phone: '+855 11 222 333', email: 'bopha.chan@student.local', address: 'Phnom Penh',
          classId: '{{classId}}', status: 'ACTIVE',
        },
      }),
      req('Assign student to class', 'PUT', '/api/students/{{studentId}}/class', { body: { classId: '{{classId}}' } }),
      req('Remove student from class', 'DELETE', '/api/students/{{studentId}}/class'),
      req('Delete student', 'DELETE', '/api/students/{{studentId2}}'),
    ],
  },
  {
    name: '8. Attendance',
    item: [
      req('Bulk roll call (TEACHER)', 'POST', '/api/attendance/bulk', {
        token: 'teacherToken',
        body: {
          classId: '{{classId}}', sessionDate: '{{today}}', period: 1, notes: 'Morning roll call',
          records: [
            { studentId: '{{studentId}}', status: 'PRESENT', checkInTime: '07:25:00' },
            { studentId: '{{studentId2}}', status: 'ABSENT', remarks: 'No notice from parents' },
          ],
        },
        tests: [
          ...expectStatus(201),
          `const body = pm.response.json();`,
          `pm.collectionVariables.set('attendanceId', body.records[0].id);`,
          `pm.collectionVariables.set('sessionId', body.session.id);`,
        ],
      }),
      req('Bulk roll call again -> 409 duplicate', 'POST', '/api/attendance/bulk', {
        token: 'teacherToken',
        body: {
          classId: '{{classId}}', sessionDate: '{{today}}', period: 1,
          records: [{ studentId: '{{studentId}}', status: 'PRESENT' }],
        },
        tests: expectStatus(409),
      }),
      req('Record single attendance (period 2)', 'POST', '/api/attendance', {
        token: 'teacherToken',
        body: {
          classId: '{{classId}}', subjectId: '{{subjectId}}', sessionDate: '{{today}}', period: 2,
          studentId: '{{studentId}}', status: 'LATE', checkInTime: '09:12:00', remarks: 'Arrived after break',
        },
        tests: expectStatus(201),
      }),
      req('Search attendance', 'GET', '/api/attendance?classId={{classId}}&fromDate={{today}}&toDate={{today}}&status=ABSENT'),
      req('Get attendance record', 'GET', '/api/attendance/{{attendanceId}}'),
      req('Update attendance (TEACHER)', 'PUT', '/api/attendance/{{attendanceId}}', {
        token: 'teacherToken', body: { status: 'LATE', checkInTime: '07:48:00', remarks: 'Traffic' },
      }),
      req('Attendance by student', 'GET', '/api/attendance/student/{{studentId}}?fromDate=2026-08-01'),
      req('Attendance by class', 'GET', '/api/attendance/class/{{classId}}?date={{today}}'),
      req('My attendance (STUDENT)', 'GET', '/api/attendance/me', { token: 'studentToken' }),
      req('List sessions', 'GET', '/api/attendance/sessions?classId={{classId}}'),
      req('Session records', 'GET', '/api/attendance/sessions/{{sessionId}}/records'),
      req('Attendance report (summary)', 'GET', '/api/attendance/report?period=MONTHLY&date={{today}}&classId={{classId}}'),
      req('Delete attendance (ADMIN)', 'DELETE', '/api/attendance/{{attendanceId}}'),
    ],
  },
  {
    name: '9. Reports',
    item: [
      req('Daily summary', 'GET', '/api/reports/attendance?period=DAILY&date={{today}}'),
      req('Weekly summary for class', 'GET', '/api/reports/attendance?period=WEEKLY&date={{today}}&classId={{classId}}'),
      req('Monthly summary', 'GET', '/api/reports/attendance?period=MONTHLY&date={{today}}'),
      req('Semester summary', 'GET', '/api/reports/attendance?period=SEMESTER&semesterId={{semesterId}}'),
      req('Custom range summary', 'GET', '/api/reports/attendance?period=CUSTOM&fromDate=2026-09-01&toDate=2026-09-30'),
      req('Class summary', 'GET', '/api/reports/class/{{classId}}?fromDate=2026-09-01&toDate={{today}}'),
      req('Student history', 'GET', '/api/reports/student/{{studentId}}'),
      req('Absent list', 'GET', '/api/reports/absent?period=DAILY&date={{today}}'),
      req('Late list', 'GET', '/api/reports/late?period=WEEKLY&date={{today}}'),
      req('Export summary (Excel)', 'GET', '/api/reports/export?type=SUMMARY&format=EXCEL&period=MONTHLY&date={{today}}',
        { desc: 'Use "Send and Download" to save the .xlsx file.' }),
      req('Export absent list (PDF)', 'GET', '/api/reports/export?type=ABSENT&format=PDF&period=WEEKLY&date={{today}}'),
      req('Export student history (PDF)', 'GET', '/api/reports/export?type=STUDENT_HISTORY&format=PDF&studentId={{studentId}}'),
    ],
  },
  {
    name: '10. Dashboards',
    item: [
      req('Admin dashboard', 'GET', '/api/dashboard/admin', { tests: expectStatus(200) }),
      req('Teacher dashboard', 'GET', '/api/dashboard/teacher', { token: 'teacherToken', tests: expectStatus(200) }),
      req('Student dashboard', 'GET', '/api/dashboard/student', { token: 'studentToken', tests: expectStatus(200) }),
    ],
  },
  {
    name: '11. Audit logs (ADMIN)',
    item: [req('Search audit logs', 'GET', '/api/audit-logs?entityName=AttendanceRecord')],
  },
];

// ---- Reassemble into a run-safe order: setup first, destructive calls last.
const lookup = new Map();
for (const f of folders) {
  const prefix = f.name.replace(/^\d+\.\s*/, '').split(' ')[0]; // e.g. "Authentication", "Classes"
  for (const it of f.item) lookup.set(`${prefix}|${it.name}`, it);
}
const pick = (...keys) => keys.map((k) => {
  const it = lookup.get(k);
  if (!it) throw new Error('Unknown request ' + k);
  lookup.delete(k);
  return it;
});
const ordered = [
  { name: '01. Login', item: pick('Authentication|Login (admin)', 'Authentication|Current user', 'Authentication|Refresh token') },
  { name: '02. Academic years & semesters', item: pick('Academic|Create academic year', 'Academic|List academic years',
      'Academic|Get academic year', 'Academic|Update academic year', 'Academic|Create semester', 'Academic|List semesters',
      'Academic|Get semester', 'Academic|Update semester') },
  { name: '03. Subjects', item: pick('Subjects|Create subject', 'Subjects|Search subjects', 'Subjects|Get subject',
      'Subjects|Update subject') },
  { name: '04. Teachers', item: pick('Teachers|Create teacher', 'Teachers|Search teachers', 'Teachers|Get teacher',
      'Teachers|Update teacher') },
  { name: '05. Classes', item: pick('Classes|Create class', 'Classes|Search classes', 'Classes|Get class',
      'Classes|Update class', 'Classes|Set homeroom teacher', 'Teachers|Assign teacher to class/subject',
      'Teachers|List teacher assignments', 'Classes|Class teachers') },
  { name: '06. Students', item: pick('Students|Create student', 'Students|Create second student', 'Students|Search students',
      'Students|Get student', 'Students|Update student', 'Students|Assign student to class', 'Classes|Enroll students',
      'Classes|Class students') },
  // Role changes revoke tokens, so user administration runs before the teacher/student log in.
  { name: '07. Accounts & users', item: pick('Authentication|Register teacher account (ADMIN)',
      'Authentication|Register student account (ADMIN)', 'Users|Search users', 'Users|Get user',
      'Users|Disable / enable user', 'Users|Replace roles', 'Authentication|Login (teacher)', 'Authentication|Login (student)',
      'Teachers|My teacher profile (TEACHER)', 'Students|My student profile (STUDENT)', 'Classes|My classes (TEACHER/STUDENT)') },
  { name: '08. Attendance', item: pick('Attendance|Bulk roll call (TEACHER)', 'Attendance|Bulk roll call again -> 409 duplicate',
      'Attendance|Record single attendance (period 2)', 'Attendance|Search attendance', 'Attendance|Get attendance record',
      'Attendance|Update attendance (TEACHER)', 'Attendance|Attendance by student', 'Attendance|Attendance by class',
      'Attendance|My attendance (STUDENT)', 'Attendance|List sessions', 'Attendance|Session records',
      'Attendance|Attendance report (summary)') },
  { name: '09. Reports', item: pick(...[...lookup.keys()].filter((k) => k.startsWith('Reports|'))) },
  { name: '10. Dashboards', item: pick(...[...lookup.keys()].filter((k) => k.startsWith('Dashboards|'))) },
  { name: '11. Audit logs', item: pick('Audit|Search audit logs') },
  { name: '12. Cleanup & password flows (destructive - run last)', item: pick('Authentication|Forgot password',
      'Authentication|Reset password', 'Users|Admin reset password', 'Teachers|Remove assignment',
      'Classes|Remove homeroom teacher', 'Classes|Remove student from class', 'Students|Remove student from class',
      'Attendance|Delete attendance (ADMIN)', 'Students|Delete student', 'Users|Delete user',
      'Subjects|Delete subject', 'Academic|Delete semester', 'Teachers|Delete teacher',
      'Classes|Delete class', 'Academic|Delete academic year', 'Authentication|Change password', 'Authentication|Logout') },
];
if (lookup.size) throw new Error('Requests not placed: ' + [...lookup.keys()].join(', '));
folders.length = 0;
folders.push(...ordered);

const variables = ['accessToken', 'refreshToken', 'teacherToken', 'studentToken', 'teacherUserId', 'studentUserId',
  'academicYearId', 'semesterId', 'subjectId', 'teacherId', 'assignmentId', 'classId', 'studentId', 'studentId2',
  'attendanceId', 'sessionId'].map((key) => ({ key, value: '' }));

const collection = {
  info: {
    name: 'School Attendance Management System API',
    description: 'Select the "School Attendance - Local" environment, then run folders 01 to 11 in order against a fresh '
      + 'database (or use Run collection and deselect folder 12). Tokens and created ids are stored in collection '
      + 'variables automatically. Folder 12 deletes data and changes passwords - run it last, on purpose.',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  event: [{
    listen: 'prerequest',
    script: {
      type: 'text/javascript',
      exec: [
        `// Keeps {{today}} current (ISO yyyy-MM-dd)`,
        `pm.collectionVariables.set('today', new Date().toISOString().slice(0, 10));`,
      ],
    },
  }],
  variable: [...variables, { key: 'today', value: '' }],
  item: folders,
};

const environment = {
  name: 'School Attendance - Local',
  values: [
    { key: 'baseUrl', value: 'http://localhost:8080', type: 'default', enabled: true },
    { key: 'adminUsername', value: 'admin', type: 'default', enabled: true },
    { key: 'adminPassword', value: 'Admin@123', type: 'secret', enabled: true },
  ],
  _postman_variable_scope: 'environment',
};

fs.writeFileSync(path.join(out, 'School-Attendance-API.postman_collection.json'), JSON.stringify(collection, null, 2));
fs.writeFileSync(path.join(out, 'School-Attendance-Local.postman_environment.json'), JSON.stringify(environment, null, 2));
const count = folders.reduce((n, f) => n + f.item.length, 0);
console.log(`Wrote ${count} requests in ${folders.length} folders to ${out}`);
