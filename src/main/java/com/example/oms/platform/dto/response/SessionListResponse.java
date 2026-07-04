package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record SessionListResponse(
        List<SessionSummaryResponse> sessions,
        @JsonInclude(JsonInclude.Include.NON_DEFAULT) int total) {

    public SessionListResponse(List<SessionSummaryResponse> sessions) {
        this(sessions, 0);
    }
}
