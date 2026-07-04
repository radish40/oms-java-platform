package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DisposalCreateRequest(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("diagnosis_run_id") String diagnosisRunId,
        String priority,
        String summary) {
}
