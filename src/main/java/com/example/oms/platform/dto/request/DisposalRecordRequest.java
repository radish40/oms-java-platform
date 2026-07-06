package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record DisposalRecordRequest(
        @Schema(description = "工作流ID") @JsonProperty("workflow_id") String workflowId,
        @Schema(description = "处置步骤类型") @JsonProperty("step_type") String stepType,
        @Schema(description = "决策内容") String decision,
        @Schema(description = "备注说明") String note,
        @Schema(description = "关联的处置草稿") @JsonProperty("draft_action") Map<String, Object> draftAction) {
}
