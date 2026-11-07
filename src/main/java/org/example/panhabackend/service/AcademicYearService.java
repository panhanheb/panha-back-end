package org.example.panhabackend.service;

import org.example.panhabackend.dto.academic.AcademicYearRequest;
import org.example.panhabackend.dto.academic.AcademicYearResponse;

import java.util.List;

public interface AcademicYearService {

    AcademicYearResponse create(AcademicYearRequest request);

    AcademicYearResponse update(Long id, AcademicYearRequest request);

    void delete(Long id);

    AcademicYearResponse getById(Long id);

    List<AcademicYearResponse> getAll();
}
