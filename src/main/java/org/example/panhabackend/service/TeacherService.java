package org.example.panhabackend.service;

import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.teacher.TeacherAssignmentRequest;
import org.example.panhabackend.dto.teacher.TeacherAssignmentResponse;
import org.example.panhabackend.dto.teacher.TeacherRequest;
import org.example.panhabackend.dto.teacher.TeacherResponse;
import org.example.panhabackend.enums.TeacherStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeacherService {

    TeacherResponse create(TeacherRequest request);

    TeacherResponse update(Long id, TeacherRequest request);

    void delete(Long id);

    TeacherResponse getById(Long id);

    TeacherResponse getMyProfile();

    PageResponse<TeacherResponse> search(String keyword, TeacherStatus status, String specialization, Pageable pageable);

    List<TeacherAssignmentResponse> getAssignments(Long teacherId);

    TeacherAssignmentResponse addAssignment(Long teacherId, TeacherAssignmentRequest request);

    void removeAssignment(Long teacherId, Long assignmentId);
}
