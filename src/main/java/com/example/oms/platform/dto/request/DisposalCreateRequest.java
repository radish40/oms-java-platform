package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record DisposalCreateRequest(
        @Schema(description = "关联订单ID") @JsonProperty("order_id") String orderId,
        @Schema(description = "诊断运行ID") @JsonProperty("diagnosis_run_id") String diagnosisRunId,
        @Schema(description = "优先级") String priority,
        @Schema(description = "处置概要") String summary) {
}
