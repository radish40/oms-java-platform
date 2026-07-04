package com.example.oms.platform.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record EvaluationCandidateReviewRequest(
        @JsonProperty("feedback_id") Long feedbackId,
        @JsonProperty("run_id") String runId,
        String status,
        @JsonProperty("review_note") String reviewNote,
        @JsonProperty("case_json") Map<String, Object> caseJson) {
}
