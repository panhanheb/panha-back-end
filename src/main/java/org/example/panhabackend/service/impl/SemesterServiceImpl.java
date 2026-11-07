package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.academic.SemesterRequest;
import org.example.panhabackend.dto.academic.SemesterResponse;
import org.example.panhabackend.entity.AcademicYear;
import org.example.panhabackend.entity.Semester;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.AcademicMapper;
import org.example.panhabackend.repository.AcademicYearRepository;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.SemesterRepository;
import org.example.panhabackend.service.AuditService;
import org.example.panhabackend.service.SemesterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {

    private static final String ENTITY = "Semester";

    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AcademicMapper academicMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public SemesterResponse create(SemesterRequest request) {
        AcademicYear year = findYear(request.academicYearId());
        String name = request.name().trim();
        if (semesterRepository.existsByAcademicYearIdAndNameIgnoreCase(year.getId(), name)) {
            throw new ConflictException("Semester '" + name + "' already exists in " + year.getName());
        }
        validateWithinYear(request, year);

        Semester semester = new Semester();
        apply(semester, request, year);
        semesterRepository.save(semester);
        enforceSingleCurrent(semester);
        auditService.log(AuditAction.CREATE, ENTITY, semester.getId(), "Created semester " + name + " of " + year.getName());
        return academicMapper.toResponse(semester);
    }

    @Override
    @Transactional
    public SemesterResponse update(Long id, SemesterRequest request) {
        Semester semester = findSemester(id);
        AcademicYear year = findYear(request.academicYearId());
        String name = request.name().trim();
        if (semesterRepository.existsByAcademicYearIdAndNameIgnoreCaseAndIdNot(year.getId(), name, id)) {
            throw new ConflictException("Semester '" + name + "' already exists in " + year.getName());
        }
        validateWithinYear(request, year);
        apply(semester, request, year);
        enforceSingleCurrent(semester);
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated semester " + name);
        return academicMapper.toResponse(semester);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Semester semester = findSemester(id);
        if (sessionRepository.existsBySemesterId(id)) {
            throw new ConflictException("Semester " + semester.getName() + " has attendance sessions and cannot be deleted");
        }
        semesterRepository.delete(semester);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted semester " + semester.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public SemesterResponse getById(Long id) {
        return academicMapper.toResponse(semesterRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SemesterResponse> getAll(Long academicYearId) {
        List<Semester> semesters = academicYearId != null
                ? semesterRepository.findByAcademicYearIdOrderByStartDateAsc(academicYearId)
                : semesterRepository.findAllByOrderByStartDateDesc();
        return semesters.stream().map(academicMapper::toResponse).toList();
    }

    private void validateWithinYear(SemesterRequest request, AcademicYear year) {
        if (request.startDate().isBefore(year.getStartDate()) || request.endDate().isAfter(year.getEndDate())) {
            throw new BadRequestException("Semester dates must fall within academic year " + year.getName()
                    + " (" + year.getStartDate() + " to " + year.getEndDate() + ")");
        }
    }

    private void apply(Semester semester, SemesterRequest request, AcademicYear year) {
        semester.setAcademicYear(year);
        semester.setName(request.name().trim());
        semester.setStartDate(request.startDate());
        semester.setEndDate(request.endDate());
        semester.setCurrent(Boolean.TRUE.equals(request.current()));
    }

    private void enforceSingleCurrent(Semester semester) {
        if (semester.isCurrent()) {
            semesterRepository.flush();
            semesterRepository.clearCurrentExcept(semester.getId());
        }
    }

    private AcademicYear findYear(Long id) {
        return academicYearRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Academic year", id));
    }

    private Semester findSemester(Long id) {
        return semesterRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }
}
