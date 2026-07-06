package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "评测候选")
public record EvaluationCandidateResponse(
        @Schema(description = "反馈ID") @JsonProperty("feedback_id") long feedbackId,
        @Schema(description = "诊断运行ID") @JsonProperty("run_id") String runId,
        @Schema(description = "会话ID") @JsonProperty("session_id") String sessionId,
        @Schema(description = "评分等级") String rating,
        @Schema(description = "根因是否正确") @JsonProperty("root_cause_correct") Boolean rootCauseCorrect,
        @Schema(description = "反馈评论") String comment,
        @Schema(description = "反馈创建时间") @JsonProperty("feedback_created_at") String feedbackCreatedAt,
        @Schema(description = "问题描述") String question,
        @Schema(description = "运行状态") @JsonProperty("run_status") String runStatus,
        @Schema(description = "延迟（毫秒）") @JsonProperty("latency_ms") int latencyMs,
        @Schema(description = "审查状态") @JsonProperty("review_status") String reviewStatus,
        @Schema(description = "审查人") String reviewer,
        @Schema(description = "审查备注") @JsonProperty("review_note") String reviewNote,
        @Schema(description = "审查时间") @JsonProperty("reviewed_at") String reviewedAt,
        @Schema(description = "案例草稿") @JsonProperty("case_draft") Map<String, Object> caseDraft,
        @Schema(description = "案例JSON") @JsonProperty("case_json") Map<String, Object> caseJson) {
}
