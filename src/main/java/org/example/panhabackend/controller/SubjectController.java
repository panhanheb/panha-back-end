package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.subject.SubjectRequest;
import org.example.panhabackend.dto.subject.SubjectResponse;
import org.example.panhabackend.service.SubjectService;
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

@Tag(name = "6. Subjects", description = "Subject management")
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @Operation(summary = "Create a subject (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody SubjectRequest request) {
        return subjectService.create(request);
    }

    @Operation(summary = "Search subjects")
    @GetMapping
    public PageResponse<SubjectResponse> search(@RequestParam(required = false) String keyword,
                                                @ParameterObject @PageableDefault(sort = "name") Pageable pageable) {
        return subjectService.search(keyword, pageable);
    }

    @Operation(summary = "Get a subject")
    @GetMapping("/{id}")
    public SubjectResponse getById(@PathVariable Long id) {
        return subjectService.getById(id);
    }

    @Operation(summary = "Update a subject (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return subjectService.update(id, request);
    }

    @Operation(summary = "Delete an unused subject (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        subjectService.delete(id);
    }
}
