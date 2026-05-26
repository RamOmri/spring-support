package com.nullify.supportportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveSearchRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 1024) String query
) {}
