package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

public record RollbackPlanRequest(
        @Schema(description = "工作流ID") @JsonProperty("workflow_id") String workflowId,
        @Schema(description = "回滚计划名称") @JsonProperty("plan_name") String planName,
        @Schema(description = "计划描述") String description,
        @Schema(description = "回滚步骤列表") List<Map<String, Object>> steps,
        @Schema(description = "触发条件列表") List<Map<String, Object>> triggers) {
}
