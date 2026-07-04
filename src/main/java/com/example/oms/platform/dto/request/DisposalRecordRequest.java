package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record DisposalRecordRequest(
        @JsonProperty("workflow_id") String workflowId,
        @JsonProperty("step_type") String stepType,
        String decision,
        String note,
        @JsonProperty("draft_action") Map<String, Object> draftAction) {
}
