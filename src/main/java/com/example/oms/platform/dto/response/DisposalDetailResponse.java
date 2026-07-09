package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Disposal workflow detail")
public record DisposalDetailResponse(
        @Schema(description = "Workflow ID") @JsonProperty("workflow_id") String workflowId,
        @Schema(description = "Workflow") DisposalWorkflowResponse.DisposalWorkflowItem workflow,
        @Schema(description = "Human confirmation and decision records") List<DisposalRecordItem> records,
        @Schema(description = "Suggested action drafts") List<ActionDraftItem> action_drafts,
        @Schema(description = "Rollback plans") List<RollbackPlanItem> rollback_plans,
        @Schema(description = "Linked ticket placeholders") List<TicketItem> tickets,
        @Schema(description = "Disposal audit events") List<AuditEventItem> audit_events) {

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

    public record TicketItem(
            long id,
            @JsonProperty("workflow_id") String workflowId,
            @JsonProperty("ticket_id") String ticketId,
            @JsonProperty("ticket_source") String ticketSource,
            String status,
            String title,
            @JsonProperty("created_by") String createdBy,
            @JsonProperty("created_at") String createdAt) {
    }

    public record AuditEventItem(
            long id,
            @JsonProperty("workflow_id") String workflowId,
            @JsonProperty("event_type") String eventType,
            String actor,
            String permission,
            @JsonProperty("detail_json") Map<String, Object> detailJson,
            @JsonProperty("created_at") String createdAt) {
    }
}
