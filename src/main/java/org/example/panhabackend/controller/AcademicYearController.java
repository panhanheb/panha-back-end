package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.academic.AcademicYearRequest;
import org.example.panhabackend.dto.academic.AcademicYearResponse;
import org.example.panhabackend.service.AcademicYearService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "7. Academic years", description = "Academic year management")
@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @Operation(summary = "Create an academic year (ADMIN). Setting current=true clears the flag on others")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicYearResponse create(@Valid @RequestBody AcademicYearRequest request) {
        return academicYearService.create(request);
    }

    @Operation(summary = "List academic years, newest first")
    @GetMapping
    public List<AcademicYearResponse> getAll() {
        return academicYearService.getAll();
    }

    @Operation(summary = "Get an academic year")
    @GetMapping("/{id}")
    public AcademicYearResponse getById(@PathVariable Long id) {
        return academicYearService.getById(id);
    }

    @Operation(summary = "Update an academic year (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public AcademicYearResponse update(@PathVariable Long id, @Valid @RequestBody AcademicYearRequest request) {
        return academicYearService.update(id, request);
    }

    @Operation(summary = "Delete an academic year without semesters or classes (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        academicYearService.delete(id);
    }
}
