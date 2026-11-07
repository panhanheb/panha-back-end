package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.subject.SubjectRequest;
import org.example.panhabackend.dto.subject.SubjectResponse;
import org.example.panhabackend.entity.Subject;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.SubjectMapper;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.SubjectRepository;
import org.example.panhabackend.repository.TeacherClassRepository;
import org.example.panhabackend.service.AuditService;
import org.example.panhabackend.service.SubjectService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private static final String ENTITY = "Subject";

    private final SubjectRepository subjectRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final SubjectMapper subjectMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        validateCode(request.code(), null);
        Subject subject = new Subject();
        subjectMapper.updateEntity(subject, request);
        subjectRepository.save(subject);
        auditService.log(AuditAction.CREATE, ENTITY, subject.getId(), "Created subject " + subject.getCode());
        return subjectMapper.toResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = findSubject(id);
        validateCode(request.code(), id);
        subjectMapper.updateEntity(subject, request);
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated subject " + subject.getCode());
        return subjectMapper.toResponse(subject);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subject subject = findSubject(id);
        if (teacherClassRepository.existsBySubjectId(id) || sessionRepository.existsBySubjectId(id)) {
            throw new ConflictException("Subject " + subject.getCode()
                    + " is assigned to teachers or used in attendance sessions and cannot be deleted");
        }
        subjectRepository.delete(subject);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted subject " + subject.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getById(Long id) {
        return subjectMapper.toResponse(findSubject(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> search(String keyword, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return PageResponse.from(subjectRepository.search(kw, pageable), subjectMapper::toResponse);
    }

    private void validateCode(String code, Long id) {
        String normalized = code.trim();
        boolean taken = id == null
                ? subjectRepository.existsByCodeIgnoreCase(normalized)
                : subjectRepository.existsByCodeIgnoreCaseAndIdNot(normalized, id);
        if (taken) {
            throw new ConflictException("Subject code '" + normalized + "' already exists");
        }
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }
}
