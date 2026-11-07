package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.academic.SemesterRequest;
import org.example.panhabackend.dto.academic.SemesterResponse;
import org.example.panhabackend.service.SemesterService;
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

@Tag(name = "8. Semesters", description = "Semester management")
@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @Operation(summary = "Create a semester inside an academic year (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SemesterResponse create(@Valid @RequestBody SemesterRequest request) {
        return semesterService.create(request);
    }

    @Operation(summary = "List semesters, optionally for one academic year")
    @GetMapping
    public List<SemesterResponse> getAll(@RequestParam(required = false) Long academicYearId) {
        return semesterService.getAll(academicYearId);
    }

    @Operation(summary = "Get a semester")
    @GetMapping("/{id}")
    public SemesterResponse getById(@PathVariable Long id) {
        return semesterService.getById(id);
    }

    @Operation(summary = "Update a semester (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public SemesterResponse update(@PathVariable Long id, @Valid @RequestBody SemesterRequest request) {
        return semesterService.update(id, request);
    }

    @Operation(summary = "Delete a semester without attendance sessions (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        semesterService.delete(id);
    }
}
