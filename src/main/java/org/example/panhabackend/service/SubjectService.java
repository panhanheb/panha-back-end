package org.example.panhabackend.service;

import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.subject.SubjectRequest;
import org.example.panhabackend.dto.subject.SubjectResponse;
import org.springframework.data.domain.Pageable;

public interface SubjectService {

    SubjectResponse create(SubjectRequest request);

    SubjectResponse update(Long id, SubjectRequest request);

    void delete(Long id);

    SubjectResponse getById(Long id);

    PageResponse<SubjectResponse> search(String keyword, Pageable pageable);
}
