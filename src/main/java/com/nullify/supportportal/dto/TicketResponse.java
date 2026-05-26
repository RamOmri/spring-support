package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.Ticket;
import com.nullify.supportportal.domain.TicketPriority;
import com.nullify.supportportal.domain.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        long customerId,
        String customerUsername,
        Long assignedAgentId,
        String assignedAgentUsername,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCustomer().getId(),
                ticket.getCustomer().getUsername(),
                ticket.getAssignedAgent() == null ? null : ticket.getAssignedAgent().getId(),
                ticket.getAssignedAgent() == null ? null : ticket.getAssignedAgent().getUsername(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
