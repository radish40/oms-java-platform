package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageResponse(
        String role,
        String content,
        @JsonProperty("reasoning_content") String reasoningContent,
        @JsonProperty("tool_call_id") String toolCallId,
        @JsonProperty("display_name") String displayName,
        String name,
        String params,
        String description,
        String summary,
        @JsonProperty("elapsed_ms") Integer elapsedMs,
        Map<String, Object> interp,
        @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls) {
}
