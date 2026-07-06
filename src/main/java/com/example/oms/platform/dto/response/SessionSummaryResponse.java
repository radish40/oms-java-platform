package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record SessionSummaryResponse(
        @Schema(description = "会话ID") String id,
        @Schema(description = "会话预览内容") String preview,
        @Schema(description = "对话轮次数") int turns,
        @Schema(description = "创建时间") @JsonProperty("created_at") String createdAt,
        @Schema(description = "更新时间") @JsonProperty("updated_at") String updatedAt) {
}
