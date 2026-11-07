package org.example.panhabackend.service;

import org.example.panhabackend.dto.academic.SemesterRequest;
import org.example.panhabackend.dto.academic.SemesterResponse;

import java.util.List;

public interface SemesterService {

    SemesterResponse create(SemesterRequest request);

    SemesterResponse update(Long id, SemesterRequest request);

    void delete(Long id);

    SemesterResponse getById(Long id);

    /** All semesters, or only those of one academic year when {@code academicYearId} is given. */
    List<SemesterResponse> getAll(Long academicYearId);
}
