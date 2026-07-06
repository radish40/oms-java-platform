package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SessionDetailResponse(
        @Schema(description = "会话ID") @JsonProperty("session_id") String sessionId,
        @Schema(description = "聊天消息列表") List<ChatMessageResponse> messages) {
}
