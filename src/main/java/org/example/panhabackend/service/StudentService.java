package org.example.panhabackend.service;

import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.student.StudentRequest;
import org.example.panhabackend.dto.student.StudentResponse;
import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.StudentStatus;
import org.springframework.data.domain.Pageable;

public interface StudentService {

    StudentResponse create(StudentRequest request);

    StudentResponse update(Long id, StudentRequest request);

    void delete(Long id);

    StudentResponse getById(Long id);

    /** The student profile of the logged-in student. */
    StudentResponse getMyProfile();

    PageResponse<StudentResponse> search(String keyword, Long classId, StudentStatus status, Gender gender,
                                         Pageable pageable);

    /** Moves a student into a class, closing the previous enrollment. */
    StudentResponse assignToClass(Long studentId, Long classId);

    /** Removes a student from their current class (enrollment history is kept). */
    StudentResponse removeFromClass(Long studentId);
}
