package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record EvaluationCandidateEntity(
        long feedbackId,
        String runId,
        String sessionId,
        String rating,
        Boolean rootCauseCorrect,
        String comment,
        LocalDateTime feedbackCreatedAt,
        String question,
        String runStatus,
        int latencyMs,
        String reviewStatus,
        String reviewer,
        String reviewNote,
        LocalDateTime reviewedAt,
        String caseDraftJson,
        String caseJson) {
}
