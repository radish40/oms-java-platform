package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionSummaryResponse(
        String id,
        String preview,
        int turns,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {
}
