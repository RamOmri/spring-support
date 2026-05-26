package com.nullify.supportportal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignAgentRequest(
        @NotNull @Positive Long agentId
) {}
