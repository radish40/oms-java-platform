package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SessionDetailResponse(
        @JsonProperty("session_id") String sessionId,
        List<ChatMessageResponse> messages) {
}
