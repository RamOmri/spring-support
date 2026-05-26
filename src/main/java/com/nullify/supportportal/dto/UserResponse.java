package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.Role;
import com.nullify.supportportal.domain.User;

import java.time.Instant;

public record UserResponse(
        long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}
