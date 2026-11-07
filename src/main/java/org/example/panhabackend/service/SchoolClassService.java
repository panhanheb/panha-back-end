package org.example.panhabackend.service;

import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.schoolclass.SchoolClassRequest;
import org.example.panhabackend.dto.schoolclass.SchoolClassResponse;
import org.example.panhabackend.dto.student.StudentResponse;
import org.example.panhabackend.dto.teacher.TeacherAssignmentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SchoolClassService {

    SchoolClassResponse create(SchoolClassRequest request);

    SchoolClassResponse update(Long id, SchoolClassRequest request);

    void delete(Long id);

    SchoolClassResponse getById(Long id);

    PageResponse<SchoolClassResponse> search(String keyword, Integer gradeLevel, Long academicYearId, Pageable pageable);

    /** Classes of the logged-in teacher (assigned + homeroom) or the class of the logged-in student. */
    List<SchoolClassResponse> getMyClasses();

    PageResponse<StudentResponse> getStudents(Long classId, Pageable pageable);

    List<StudentResponse> enrollStudents(Long classId, List<Long> studentIds);

    void removeStudent(Long classId, Long studentId);

    SchoolClassResponse assignHomeroomTeacher(Long classId, Long teacherId);

    SchoolClassResponse removeHomeroomTeacher(Long classId);

    List<TeacherAssignmentResponse> getTeachers(Long classId);
}
