package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.model.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.isEnabled(),
                roles,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
