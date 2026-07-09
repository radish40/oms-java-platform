package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record DisposalWorkbenchRequest(
        @Schema(description = "Diagnosis run ID") @JsonProperty("run_id") String runId,
        @Schema(description = "Optional linked order ID") @JsonProperty("order_id") String orderId,
        @Schema(description = "Existing ticket ID to link") @JsonProperty("ticket_id") String ticketId,
        @Schema(description = "Create placeholder ticket when ticket_id is empty") @JsonProperty("create_ticket") Boolean createTicket,
        @Schema(description = "Fixture diagnosis detail fallback") @JsonProperty("diagnosis_detail") Map<String, Object> diagnosisDetail) {
}
