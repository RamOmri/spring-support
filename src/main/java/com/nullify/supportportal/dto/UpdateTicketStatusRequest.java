package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.TicketStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull TicketStatus status
) {}
