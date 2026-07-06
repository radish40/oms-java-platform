package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record EvaluationCandidateReviewRequest(
        @Schema(description = "反馈ID") @JsonProperty("feedback_id") Long feedbackId,
        @Schema(description = "诊断运行ID") @JsonProperty("run_id") String runId,
        @Schema(description = "审查状态") String status,
        @Schema(description = "审查备注") @JsonProperty("review_note") String reviewNote,
        @Schema(description = "案例JSON内容") @JsonProperty("case_json") Map<String, Object> caseJson) {
}
