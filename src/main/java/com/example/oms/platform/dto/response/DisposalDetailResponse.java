package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record DisposalDetailResponse(
        @JsonProperty("workflow_id") String workflowId,
        DisposalWorkflowResponse.DisposalWorkflowItem workflow,
        List<DisposalRecordItem> records,
        List<ActionDraftItem> action_drafts,
        List<RollbackPlanItem> rollback_plans) {

    public record DisposalRecordItem(
            long id,
            @JsonProperty("step_type") String stepType,
            String actor,
            String decision,
            String note,
            @JsonProperty("draft_action") Map<String, Object> draftAction,
            @JsonProperty("created_at") String createdAt) {
    }

    public record ActionDraftItem(
            long id,
            @JsonProperty("action_type") String actionType,
            String description,
            @JsonProperty("risk_level") String riskLevel,
            @JsonProperty("requires_approval") boolean requiresApproval,
            @JsonProperty("sort_order") int sortOrder,
            @JsonProperty("created_at") String createdAt) {
    }

    public record RollbackPlanItem(
            long id,
            @JsonProperty("plan_name") String planName,
            String description,
            List<Map<String, Object>> steps,
            List<Map<String, Object>> triggers,
            String status,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt) {
    }
}
