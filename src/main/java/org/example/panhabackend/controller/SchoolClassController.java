package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.schoolclass.EnrollStudentsRequest;
import org.example.panhabackend.dto.schoolclass.HomeroomTeacherRequest;
import org.example.panhabackend.dto.schoolclass.SchoolClassRequest;
import org.example.panhabackend.dto.schoolclass.SchoolClassResponse;
import org.example.panhabackend.dto.student.StudentResponse;
import org.example.panhabackend.dto.teacher.TeacherAssignmentResponse;
import org.example.panhabackend.service.SchoolClassService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "5. Classes", description = "Class management, enrollment and homeroom teachers")
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService classService;

    @Operation(summary = "Create a class (ADMIN). academicYearId defaults to the current academic year")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolClassResponse create(@Valid @RequestBody SchoolClassRequest request) {
        return classService.create(request);
    }

    @Operation(summary = "Search classes (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<SchoolClassResponse> search(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer gradeLevel,
                                                    @RequestParam(required = false) Long academicYearId,
                                                    @ParameterObject @PageableDefault(sort = {"gradeLevel", "name"})
                                                    Pageable pageable) {
        return classService.search(keyword, gradeLevel, academicYearId, pageable);
    }

    @Operation(summary = "Classes of the logged-in teacher, or the class of the logged-in student")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/my")
    public List<SchoolClassResponse> myClasses() {
        return classService.getMyClasses();
    }

    @Operation(summary = "Get class details")
    @GetMapping("/{id}")
    public SchoolClassResponse getById(@PathVariable Long id) {
        return classService.getById(id);
    }

    @Operation(summary = "Update a class (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public SchoolClassResponse update(@PathVariable Long id, @Valid @RequestBody SchoolClassRequest request) {
        return classService.update(id, request);
    }

    @Operation(summary = "Delete a class without attendance sessions (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        classService.delete(id);
    }

    @Operation(summary = "Students currently in the class")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/{id}/students")
    public PageResponse<StudentResponse> students(@PathVariable Long id,
                                                  @ParameterObject @PageableDefault(size = 50, sort = "lastName")
                                                  Pageable pageable) {
        return classService.getStudents(id, pageable);
    }

    @Operation(summary = "Enroll students into the class (moves them from their previous class) (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/students")
    public List<StudentResponse> enroll(@PathVariable Long id, @Valid @RequestBody EnrollStudentsRequest request) {
        return classService.enrollStudents(id, request.studentIds());
    }

    @Operation(summary = "Remove a student from the class (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        classService.removeStudent(id, studentId);
    }

    @Operation(summary = "Set the homeroom teacher (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/homeroom-teacher")
    public SchoolClassResponse assignHomeroom(@PathVariable Long id, @Valid @RequestBody HomeroomTeacherRequest request) {
        return classService.assignHomeroomTeacher(id, request.teacherId());
    }

    @Operation(summary = "Remove the homeroom teacher (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/homeroom-teacher")
    public SchoolClassResponse removeHomeroom(@PathVariable Long id) {
        return classService.removeHomeroomTeacher(id);
    }

    @Operation(summary = "Teachers assigned to the class and their subjects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/{id}/teachers")
    public List<TeacherAssignmentResponse> teachers(@PathVariable Long id) {
        return classService.getTeachers(id);
    }
}
