package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Diagnosis disposal workbench")
public record DisposalWorkbenchResponse(
        @JsonProperty("run_id") String runId,
        @JsonProperty("workflow_id") String workflowId,
        @JsonProperty("created") boolean created,
        DisposalWorkflowResponse.DisposalWorkflowItem workflow,
        DisposalDetailResponse detail,
        @JsonProperty("handling_note_draft") String handlingNoteDraft,
        @JsonProperty("suggested_actions") List<Map<String, Object>> suggestedActions,
        @JsonProperty("rollback_guidance") List<Map<String, Object>> rollbackGuidance,
        TicketSummary ticket) {

    public record TicketSummary(
            @JsonProperty("ticket_id") String ticketId,
            @JsonProperty("ticket_source") String ticketSource,
            String status,
            String title) {
    }
}
