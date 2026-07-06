package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SessionListResponse(
        @Schema(description = "会话摘要列表") List<SessionSummaryResponse> sessions,
        @Schema(description = "总记录数") @JsonInclude(JsonInclude.Include.NON_DEFAULT) int total) {

    public SessionListResponse(List<SessionSummaryResponse> sessions) {
        this(sessions, 0);
    }
}
