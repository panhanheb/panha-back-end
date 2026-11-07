package org.example.panhabackend.dto.subject;

import java.time.LocalDateTime;

public record SubjectResponse(Long id,
                              String code,
                              String name,
                              String description,
                              Integer credits,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
}
