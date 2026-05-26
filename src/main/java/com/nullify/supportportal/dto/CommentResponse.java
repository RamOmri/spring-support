package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.Comment;

import java.time.Instant;

public record CommentResponse(
        long id,
        long ticketId,
        long authorId,
        String authorUsername,
        String body,
        Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getBody(),
                comment.getCreatedAt());
    }
}
