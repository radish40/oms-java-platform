package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DisposalWorkflowResponse(
        @JsonInclude(JsonInclude.Include.NON_NULL) List<DisposalWorkflowItem> workflows,
        @JsonInclude(JsonInclude.Include.NON_DEFAULT) int total,
        @JsonInclude(JsonInclude.Include.NON_NULL) DisposalWorkflowItem workflow) {

    public DisposalWorkflowResponse(List<DisposalWorkflowItem> workflows, int total) {
        this(workflows, total, null);
    }

    public DisposalWorkflowResponse(DisposalWorkflowItem workflow) {
        this(null, 0, workflow);
    }

    public record DisposalWorkflowItem(
            @JsonProperty("workflow_id") String workflowId,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("diagnosis_run_id") String diagnosisRunId,
            String status,
            String assignee,
            String priority,
            String summary,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("resolved_at") @JsonInclude(JsonInclude.Include.NON_NULL) String resolvedAt) {
    }
}
