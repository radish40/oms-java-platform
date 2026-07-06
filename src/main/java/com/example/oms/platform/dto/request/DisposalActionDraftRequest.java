package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record DisposalActionDraftRequest(
        @Schema(description = "工作流ID") @JsonProperty("workflow_id") String workflowId,
        @Schema(description = "行动类型") @JsonProperty("action_type") String actionType,
        @Schema(description = "行动描述") String description,
        @Schema(description = "风险等级") @JsonProperty("risk_level") String riskLevel,
        @Schema(description = "是否需要审批") @JsonProperty("requires_approval") boolean requiresApproval,
        @Schema(description = "排序序号") @JsonProperty("sort_order") int sortOrder) {
}
