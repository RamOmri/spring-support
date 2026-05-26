package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.Attachment;

import java.time.Instant;

public record AttachmentResponse(
        long id,
        long ticketId,
        String filename,
        String contentType,
        long sizeBytes,
        long uploadedById,
        String uploadedByUsername,
        Instant createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getTicket().getId(),
                a.getFilename(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getUploadedBy().getId(),
                a.getUploadedBy().getUsername(),
                a.getCreatedAt());
    }
}
