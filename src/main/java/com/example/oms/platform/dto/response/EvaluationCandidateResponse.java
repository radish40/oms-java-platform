package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record EvaluationCandidateResponse(
        @JsonProperty("feedback_id") long feedbackId,
        @JsonProperty("run_id") String runId,
        @JsonProperty("session_id") String sessionId,
        String rating,
        @JsonProperty("root_cause_correct") Boolean rootCauseCorrect,
        String comment,
        @JsonProperty("feedback_created_at") String feedbackCreatedAt,
        String question,
        @JsonProperty("run_status") String runStatus,
        @JsonProperty("latency_ms") int latencyMs,
        @JsonProperty("review_status") String reviewStatus,
        String reviewer,
        @JsonProperty("review_note") String reviewNote,
        @JsonProperty("reviewed_at") String reviewedAt,
        @JsonProperty("case_draft") Map<String, Object> caseDraft,
        @JsonProperty("case_json") Map<String, Object> caseJson) {
}
