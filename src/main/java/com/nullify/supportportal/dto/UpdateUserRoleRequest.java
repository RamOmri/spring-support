package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role
) {}
