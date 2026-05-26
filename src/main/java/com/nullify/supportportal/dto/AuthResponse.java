package com.nullify.supportportal.dto;

public record AuthResponse(
        String accessToken,
        long userId,
        String username,
        String email,
        String role
) {}
