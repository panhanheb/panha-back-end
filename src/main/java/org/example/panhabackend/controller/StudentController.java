package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.student.AssignClassRequest;
import org.example.panhabackend.dto.student.StudentRequest;
import org.example.panhabackend.dto.student.StudentResponse;
import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.service.StudentService;
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

@Tag(name = "3. Students", description = "Student management")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Create a student (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return studentService.create(request);
    }

    @Operation(summary = "Search students (teachers only see students of their classes)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping
    public PageResponse<StudentResponse> search(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long classId,
                                                @RequestParam(required = false) StudentStatus status,
                                                @RequestParam(required = false) Gender gender,
                                                @ParameterObject @PageableDefault(sort = "lastName") Pageable pageable) {
        return studentService.search(keyword, classId, status, gender, pageable);
    }

    @Operation(summary = "Profile of the logged-in student")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public StudentResponse me() {
        return studentService.getMyProfile();
    }

    @Operation(summary = "Get a student (admin, the student, or a teacher of the student's class)")
    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        return studentService.getById(id);
    }

    @Operation(summary = "Update a student (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    @Operation(summary = "Delete a student without attendance history (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    @Operation(summary = "Assign / move a student to a class (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/class")
    public StudentResponse assignClass(@PathVariable Long id, @Valid @RequestBody AssignClassRequest request) {
        return studentService.assignToClass(id, request.classId());
    }

    @Operation(summary = "Remove a student from their current class (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/class")
    public StudentResponse removeFromClass(@PathVariable Long id) {
        return studentService.removeFromClass(id);
    }
}
