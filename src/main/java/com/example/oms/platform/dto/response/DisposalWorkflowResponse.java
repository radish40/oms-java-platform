package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "处置工作流响应")
public record DisposalWorkflowResponse(
        @Schema(description = "工作流列表") @JsonInclude(JsonInclude.Include.NON_NULL) List<DisposalWorkflowItem> workflows,
        @Schema(description = "总记录数") @JsonInclude(JsonInclude.Include.NON_DEFAULT) int total,
        @Schema(description = "单个工作流详情") @JsonInclude(JsonInclude.Include.NON_NULL) DisposalWorkflowItem workflow) {

    public DisposalWorkflowResponse(List<DisposalWorkflowItem> workflows, int total) {
        this(workflows, total, null);
    }

    public DisposalWorkflowResponse(DisposalWorkflowItem workflow) {
        this(null, 0, workflow);
    }

    @Schema(description = "处置工作流条目")
    public record DisposalWorkflowItem(
            @Schema(description = "工作流ID") @JsonProperty("workflow_id") String workflowId,
            @Schema(description = "订单ID") @JsonProperty("order_id") String orderId,
            @Schema(description = "诊断运行ID") @JsonProperty("diagnosis_run_id") String diagnosisRunId,
            @Schema(description = "工作流状态") String status,
            @Schema(description = "负责人") String assignee,
            @Schema(description = "优先级") String priority,
            @Schema(description = "概要") String summary,
            @Schema(description = "创建时间") @JsonProperty("created_at") String createdAt,
            @Schema(description = "更新时间") @JsonProperty("updated_at") String updatedAt,
            @Schema(description = "解决时间") @JsonProperty("resolved_at") @JsonInclude(JsonInclude.Include.NON_NULL) String resolvedAt) {
    }
}
