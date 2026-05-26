package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.TicketPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 16000) String description,
        TicketPriority priority
) {}
