package org.example.panhabackend.mapper;

import org.example.panhabackend.dto.user.UserResponse;
import org.example.panhabackend.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user, Long teacherId, Long studentId) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                roles,
                teacherId,
                studentId,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
