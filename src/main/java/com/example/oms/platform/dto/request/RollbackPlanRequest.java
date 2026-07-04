package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record RollbackPlanRequest(
        @JsonProperty("workflow_id") String workflowId,
        @JsonProperty("plan_name") String planName,
        String description,
        List<Map<String, Object>> steps,
        List<Map<String, Object>> triggers) {
}
