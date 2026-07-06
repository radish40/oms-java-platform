package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "评测候选审查结果响应")
public record EvaluationCandidateReviewResponse(
        @Schema(description = "审查结果") Review review) {
    @Schema(description = "审查记录")
    public record Review(
            @Schema(description = "反馈ID") @JsonProperty("feedback_id") long feedbackId,
            @Schema(description = "诊断运行ID") @JsonProperty("run_id") String runId,
            @Schema(description = "审查状态") String status,
            @Schema(description = "审查人") String reviewer,
            @Schema(description = "审查备注") @JsonProperty("review_note") String reviewNote,
            @Schema(description = "案例JSON") @JsonProperty("case_json") Map<String, Object> caseJson) {
    }
}
