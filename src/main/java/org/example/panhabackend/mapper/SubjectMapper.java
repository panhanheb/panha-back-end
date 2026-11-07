package org.example.panhabackend.mapper;

import org.example.panhabackend.dto.subject.SubjectRequest;
import org.example.panhabackend.dto.subject.SubjectResponse;
import org.example.panhabackend.entity.Subject;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {

    public void updateEntity(Subject subject, SubjectRequest request) {
        subject.setCode(request.code().trim().toUpperCase());
        subject.setName(request.name().trim());
        subject.setDescription(request.description());
        subject.setCredits(request.credits());
    }

    public SubjectResponse toResponse(Subject s) {
        return new SubjectResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(), s.getCredits(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
