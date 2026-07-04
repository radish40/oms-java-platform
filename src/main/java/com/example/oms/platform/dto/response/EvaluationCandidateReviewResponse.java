package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record EvaluationCandidateReviewResponse(Review review) {
    public record Review(
            @JsonProperty("feedback_id") long feedbackId,
            @JsonProperty("run_id") String runId,
            String status,
            String reviewer,
            @JsonProperty("review_note") String reviewNote,
            @JsonProperty("case_json") Map<String, Object> caseJson) {
    }
}
