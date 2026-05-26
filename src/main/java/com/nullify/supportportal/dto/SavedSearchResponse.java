package com.nullify.supportportal.dto;

import com.nullify.supportportal.domain.SavedSearch;

import java.time.Instant;

public record SavedSearchResponse(
        long id,
        String name,
        String query,
        Instant createdAt
) {
    public static SavedSearchResponse from(SavedSearch s) {
        return new SavedSearchResponse(s.getId(), s.getName(), s.getQuery(), s.getCreatedAt());
    }
}
