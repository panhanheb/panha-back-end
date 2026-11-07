package org.example.panhabackend.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flow through the real HTTP layer, security filter chain and H2 database:
 * admin setup -> teacher records attendance -> duplicates rejected -> reports, exports and dashboards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void protectedEndpointWithoutTokenReturns401Json() throws Exception {
        mvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void openApiDocumentIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("School Attendance Management System API"))
                .andExpect(jsonPath("$.paths['/api/attendance/bulk']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        String admin = login("admin", "Admin@123");
        mvc.perform(auth(post("/api/students"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.studentCode").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.gender").exists());
    }

    @Test
    void fullAttendanceFlow() throws Exception {
        LocalDate today = LocalDate.now();
        String admin = login("admin", "Admin@123");

        // ---- academic structure
        long yearId = idOf(mvc.perform(auth(post("/api/academic-years"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"IT-%d","startDate":"%s","endDate":"%s","current":true}
                                """.formatted(today.getYear(), today.minusMonths(3), today.plusMonths(6))))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(auth(post("/api/semesters"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"academicYearId":%d,"name":"Semester 1","startDate":"%s","endDate":"%s","current":true}
                                """.formatted(yearId, today.minusMonths(1), today.plusMonths(2))))
                .andExpect(status().isCreated());

        long teacherId = idOf(mvc.perform(auth(post("/api/teachers"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherCode":"T-IT1","firstName":"Sok","lastName":"Dara","gender":"MALE",
                                 "email":"dara.it@school.local","specialization":"Mathematics"}
                                """))
                .andExpect(status().isCreated()).andReturn());

        long classId = idOf(mvc.perform(auth(post("/api/classes"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"IT-G10A","name":"Grade 10 - A","gradeLevel":10,"section":"A","capacity":30,
                                 "academicYearId":%d,"homeroomTeacherId":%d}
                                """.formatted(yearId, teacherId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.homeroomTeacherName").value("Sok Dara"))
                .andReturn());

        long student1 = createStudent(admin, "IT-S001", "Chan", "Bopha", "FEMALE", classId);
        long student2 = createStudent(admin, "IT-S002", "Kim", "Rithy", "MALE", classId);

        // ---- accounts (admin-controlled registration)
        mvc.perform(auth(post("/api/auth/register"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"it.teacher","email":"it.teacher@school.local","password":"Teacher@123",
                                 "roles":["ROLE_TEACHER"],"teacherId":%d}
                                """.formatted(teacherId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teacherId").value(teacherId))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(auth(post("/api/auth/register"), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"it.student","email":"it.student@school.local","password":"Student@123",
                                 "roles":["ROLE_STUDENT"],"studentId":%d}
                                """.formatted(student1)))
                .andExpect(status().isCreated());

        String teacher = login("it.teacher", "Teacher@123");

        // ---- bulk roll call by the homeroom teacher
        String bulk = """
                {"classId":%d,"sessionDate":"%s","period":1,"notes":"Morning roll call",
                 "records":[{"studentId":%d,"status":"PRESENT","checkInTime":"07:25:00"},
                            {"studentId":%d,"status":"ABSENT","remarks":"No notice"}]}
                """.formatted(classId, today, student1, student2);

        MvcResult bulkResult = mvc.perform(auth(post("/api/attendance/bulk"), teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(bulk))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordedCount").value(2))
                .andExpect(jsonPath("$.summary.attendancePercentage").value(50.0))
                .andExpect(jsonPath("$.session.teacherName").value("Sok Dara"))
                .andExpect(jsonPath("$.session.semesterName").value("Semester 1"))
                .andExpect(jsonPath("$.unmarkedStudentIds").isEmpty())
                .andReturn();
        Number absentRecordId = JsonPath.read(bulkResult.getResponse().getContentAsString(), "$.records[1].id");

        // ---- duplicate roll call is rejected
        mvc.perform(auth(post("/api/attendance/bulk"), teacher).contentType(MediaType.APPLICATION_JSON).content(bulk))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.length()").value(2));

        // ---- single duplicate is rejected too
        mvc.perform(auth(post("/api/attendance"), teacher).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classId":%d,"sessionDate":"%s","studentId":%d,"status":"LATE"}
                                """.formatted(classId, today, student1)))
                .andExpect(status().isConflict());

        // ---- teacher corrects a record within the edit window
        mvc.perform(auth(put("/api/attendance/" + absentRecordId), teacher).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LATE\",\"checkInTime\":\"07:50:00\",\"remarks\":\"Bus delay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LATE"));

        // ---- RBAC
        mvc.perform(auth(get("/api/users"), teacher)).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/students"), teacher).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentCode\":\"X\",\"firstName\":\"a\",\"lastName\":\"b\",\"gender\":\"MALE\"}"))
                .andExpect(status().isForbidden());

        // ---- teacher dashboard
        mvc.perform(auth(get("/api/dashboard/teacher"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedClassCount").value(1))
                .andExpect(jsonPath("$.totalStudents").value(2))
                .andExpect(jsonPath("$.todaySummary.present").value(1))
                .andExpect(jsonPath("$.todaySummary.late").value(1));

        // ---- student sees only their own data
        String studentToken = login("it.student", "Student@123");
        mvc.perform(auth(get("/api/dashboard/student"), studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPresent").value(1))
                .andExpect(jsonPath("$.attendancePercentage").value(100.0));
        mvc.perform(auth(get("/api/students/" + student2), studentToken)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/attendance"), studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentId").value(student1));

        // ---- reports (LATE counts as present -> 100%)
        mvc.perform(auth(get("/api/reports/attendance"), admin)
                        .param("period", "DAILY").param("classId", String.valueOf(classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSchoolDays").value(1))
                .andExpect(jsonPath("$.overall.present").value(1))
                .andExpect(jsonPath("$.overall.late").value(1))
                .andExpect(jsonPath("$.overall.attendancePercentage").value(100.0))
                .andExpect(jsonPath("$.students.length()").value(2));

        mvc.perform(auth(get("/api/reports/late"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentCode").value("IT-S002"));

        mvc.perform(auth(get("/api/reports/export"), admin)
                        .param("type", "SUMMARY").param("format", "EXCEL").param("classId", String.valueOf(classId)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(".xlsx")));

        MvcResult pdf = mvc.perform(auth(get("/api/reports/export"), admin)
                        .param("type", "STUDENT_HISTORY").param("format", "PDF").param("studentId", String.valueOf(student2)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");

        // ---- admin dashboard
        mvc.perform(auth(get("/api/dashboard/admin"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayPresent").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.todayLate").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // ---- deleting a student with history is refused
        mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/students/" + student1), admin))
                .andExpect(status().isConflict());

        // ---- logout revokes the teacher's tokens
        mvc.perform(auth(post("/api/auth/logout"), teacher)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/auth/me"), teacher))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has been revoked"));
    }

    // ------------------------------------------------------------------ helpers

    private long createStudent(String token, String code, String first, String last, String gender, long classId)
            throws Exception {
        return idOf(mvc.perform(auth(post("/api/students"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentCode":"%s","firstName":"%s","lastName":"%s","gender":"%s",
                                 "dateOfBirth":"2010-05-14","classId":%d}
                                """.formatted(code, first, last, gender, classId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classId").value(classId))
                .andReturn());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
