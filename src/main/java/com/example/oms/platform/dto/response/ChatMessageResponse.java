package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageResponse(
        @Schema(description = "消息角色") String role,
        @Schema(description = "消息内容") String content,
        @Schema(description = "推理过程内容") @JsonProperty("reasoning_content") String reasoningContent,
        @Schema(description = "工具调用ID") @JsonProperty("tool_call_id") String toolCallId,
        @Schema(description = "显示名称") @JsonProperty("display_name") String displayName,
        @Schema(description = "名称") String name,
        @Schema(description = "参数") String params,
        @Schema(description = "描述") String description,
        @Schema(description = "摘要") String summary,
        @Schema(description = "耗时（毫秒）") @JsonProperty("elapsed_ms") Integer elapsedMs,
        @Schema(description = "解释数据") Map<String, Object> interp,
        @Schema(description = "工具调用列表") @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls) {
}
