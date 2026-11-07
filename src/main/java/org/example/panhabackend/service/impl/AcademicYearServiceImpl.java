package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.academic.AcademicYearRequest;
import org.example.panhabackend.dto.academic.AcademicYearResponse;
import org.example.panhabackend.entity.AcademicYear;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.AcademicMapper;
import org.example.panhabackend.repository.AcademicYearRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.SemesterRepository;
import org.example.panhabackend.service.AcademicYearService;
import org.example.panhabackend.service.AuditService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private static final String ENTITY = "AcademicYear";

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final SchoolClassRepository classRepository;
    private final AcademicMapper academicMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public AcademicYearResponse create(AcademicYearRequest request) {
        String name = request.name().trim();
        if (academicYearRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Academic year '" + name + "' already exists");
        }
        AcademicYear year = new AcademicYear();
        apply(year, request);
        academicYearRepository.save(year);
        enforceSingleCurrent(year);
        auditService.log(AuditAction.CREATE, ENTITY, year.getId(), "Created academic year " + name);
        return academicMapper.toResponse(year);
    }

    @Override
    @Transactional
    public AcademicYearResponse update(Long id, AcademicYearRequest request) {
        AcademicYear year = findYear(id);
        String name = request.name().trim();
        if (academicYearRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Academic year '" + name + "' already exists");
        }
        apply(year, request);
        enforceSingleCurrent(year);
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated academic year " + name);
        return academicMapper.toResponse(year);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AcademicYear year = findYear(id);
        if (semesterRepository.existsByAcademicYearId(id) || classRepository.existsByAcademicYearId(id)) {
            throw new ConflictException("Academic year " + year.getName()
                    + " has semesters or classes and cannot be deleted");
        }
        academicYearRepository.delete(year);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted academic year " + year.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getById(Long id) {
        return academicMapper.toResponse(findYear(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYearResponse> getAll() {
        return academicYearRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate")).stream()
                .map(academicMapper::toResponse)
                .toList();
    }

    private void apply(AcademicYear year, AcademicYearRequest request) {
        year.setName(request.name().trim());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setCurrent(Boolean.TRUE.equals(request.current()));
    }

    /** Only one academic year can be flagged as current. */
    private void enforceSingleCurrent(AcademicYear year) {
        if (year.isCurrent()) {
            academicYearRepository.flush();
            academicYearRepository.clearCurrentExcept(year.getId());
        }
    }

    private AcademicYear findYear(Long id) {
        return academicYearRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Academic year", id));
    }
}
