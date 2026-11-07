package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.teacher.TeacherAssignmentRequest;
import org.example.panhabackend.dto.teacher.TeacherAssignmentResponse;
import org.example.panhabackend.dto.teacher.TeacherRequest;
import org.example.panhabackend.dto.teacher.TeacherResponse;
import org.example.panhabackend.enums.TeacherStatus;
import org.example.panhabackend.service.TeacherService;
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

@Tag(name = "4. Teachers", description = "Teacher management and class/subject assignments")
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "Create a teacher (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
        return teacherService.create(request);
    }

    @Operation(summary = "Search teachers (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<TeacherResponse> search(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) TeacherStatus status,
                                                @RequestParam(required = false) String specialization,
                                                @ParameterObject @PageableDefault(sort = "lastName") Pageable pageable) {
        return teacherService.search(keyword, status, specialization, pageable);
    }

    @Operation(summary = "Profile of the logged-in teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/me")
    public TeacherResponse me() {
        return teacherService.getMyProfile();
    }

    @Operation(summary = "Get a teacher")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/{id}")
    public TeacherResponse getById(@PathVariable Long id) {
        return teacherService.getById(id);
    }

    @Operation(summary = "Update a teacher (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return teacherService.update(id, request);
    }

    @Operation(summary = "Delete a teacher who has not recorded attendance (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teacherService.delete(id);
    }

    @Operation(summary = "List the class/subject assignments of a teacher (ADMIN or the teacher)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/{id}/assignments")
    public List<TeacherAssignmentResponse> assignments(@PathVariable Long id) {
        return teacherService.getAssignments(id);
    }

    @Operation(summary = "Assign a teacher to a class, optionally for one subject (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherAssignmentResponse addAssignment(@PathVariable Long id,
                                                   @Valid @RequestBody TeacherAssignmentRequest request) {
        return teacherService.addAssignment(id, request);
    }

    @Operation(summary = "Remove a class/subject assignment (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAssignment(@PathVariable Long id, @PathVariable Long assignmentId) {
        teacherService.removeAssignment(id, assignmentId);
    }
}
