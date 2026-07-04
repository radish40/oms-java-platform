package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DisposalActionDraftRequest(
        @JsonProperty("workflow_id") String workflowId,
        @JsonProperty("action_type") String actionType,
        String description,
        @JsonProperty("risk_level") String riskLevel,
        @JsonProperty("requires_approval") boolean requiresApproval,
        @JsonProperty("sort_order") int sortOrder) {
}
