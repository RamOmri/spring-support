package com.nullify.supportportal.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailIngestRequest(
        @NotBlank String rawMessage
) {}
