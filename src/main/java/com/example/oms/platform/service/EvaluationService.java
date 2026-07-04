package com.example.oms.platform.service;

import com.example.oms.platform.dto.request.EvaluationCandidateReviewRequest;
import com.example.oms.platform.dto.response.EvaluationCandidateListResponse;
import com.example.oms.platform.dto.response.EvaluationCandidateResponse;
import com.example.oms.platform.dto.response.EvaluationCandidateReviewResponse;
import com.example.oms.platform.entity.EvaluationCandidateEntity;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.repository.EvaluationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final List<String> REVIEW_STATUSES = List.of("candidate", "reviewed", "rejected");

    private final EvaluationRepository repository;
    private final ObjectMapper objectMapper;
    private final RbacService rbacService;

    public EvaluationService(EvaluationRepository repository, ObjectMapper objectMapper, RbacService rbacService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.rbacService = rbacService;
    }

    public EvaluationCandidateListResponse listCandidates(String limit) {
        List<EvaluationCandidateResponse> candidates = repository.findCandidates(limit(limit)).stream()
                .map(this::toCandidate)
                .toList();
        return new EvaluationCandidateListResponse(candidates);
    }

    public EvaluationCandidateReviewResponse saveReview(EvaluationCandidateReviewRequest request, String reviewer) {
        EvaluationCandidateReviewRequest payload = request == null
                ? new EvaluationCandidateReviewRequest(null, null, null, null, null)
                : request;
        long feedbackId = requireFeedbackId(payload.feedbackId());
        String status = normalizeStatus(payload.status());
        EvaluationCandidateEntity existing = repository.findByFeedbackId(feedbackId)
                .orElseThrow(() -> new BusinessException(404, "NOT_FOUND", "Evaluation candidate not found",
                        Map.of("feedback_id", feedbackId)));
        if (payload.runId() != null && !payload.runId().isBlank() && !existing.runId().equals(payload.runId())) {
            throw new BusinessException(400, "BAD_REQUEST", "Review run_id does not match candidate",
                    Map.of("feedback_id", feedbackId, "run_id", payload.runId()));
        }

        Map<String, Object> caseJson = payload.caseJson() == null ? parseMap(existing.caseJson()) : payload.caseJson();
        String caseJsonText = writeJson(caseJson);
        String note = payload.reviewNote() == null ? "" : payload.reviewNote();
        repository.saveReview(feedbackId, status, reviewer, note, caseJsonText);
        recordAudit(payload, feedbackId, existing.runId(), status, reviewer);
        return new EvaluationCandidateReviewResponse(new EvaluationCandidateReviewResponse.Review(
                feedbackId,
                existing.runId(),
                status,
                reviewer,
                note,
                caseJson));
    }

    public String exportCandidates(String limit, String reviewedOnly) {
        boolean onlyReviewed = "true".equalsIgnoreCase(reviewedOnly) || "1".equals(reviewedOnly);
        StringBuilder builder = new StringBuilder();
        for (EvaluationCandidateEntity candidate : repository.findExportCandidates(limit(limit), onlyReviewed)) {
            Map<String, Object> exported = parseMap(candidate.caseJson());
            if (exported.isEmpty()) {
                exported = new LinkedHashMap<>();
                exported.put("feedback_id", candidate.feedbackId());
                exported.put("run_id", candidate.runId());
                exported.put("session_id", candidate.sessionId());
            }
            builder.append(writeJson(exported)).append('\n');
        }
        return builder.toString();
    }

    private EvaluationCandidateResponse toCandidate(EvaluationCandidateEntity candidate) {
        return new EvaluationCandidateResponse(
                candidate.feedbackId(),
                candidate.runId(),
                candidate.sessionId(),
                candidate.rating(),
                candidate.rootCauseCorrect(),
                candidate.comment(),
                timestamp(candidate.feedbackCreatedAt()),
                candidate.question(),
                candidate.runStatus(),
                candidate.latencyMs(),
                candidate.reviewStatus(),
                candidate.reviewer(),
                candidate.reviewNote(),
                timestamp(candidate.reviewedAt()),
                parseMap(candidate.caseDraftJson()),
                parseMap(candidate.caseJson()));
    }

    private int limit(String value) {
        if (value == null || value.isBlank()) {
            return 50;
        }
        try {
            return Math.max(1, Math.min(Integer.parseInt(value), 200));
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, "BAD_REQUEST", "limit must be a number");
        }
    }

    private long requireFeedbackId(Long feedbackId) {
        if (feedbackId == null || feedbackId <= 0) {
            throw new BusinessException(400, "BAD_REQUEST", "feedback_id is required");
        }
        return feedbackId;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank() ? "reviewed" : status;
        if (!REVIEW_STATUSES.contains(normalized)) {
            throw new BusinessException(400, "BAD_REQUEST", "Unsupported review status",
                    Map.of("status", normalized));
        }
        return normalized;
    }

    private void recordAudit(EvaluationCandidateReviewRequest payload, long feedbackId, String runId, String status, String reviewer) {
        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("feedback_id", feedbackId);
        auditPayload.put("run_id", runId);
        auditPayload.put("status", status);
        auditPayload.put("path", "/eval/candidates/review");
        rbacService.recordAuditEvent(
                "eval.review.save",
                reviewer,
                "eval:review",
                "eval_candidate",
                String.valueOf(feedbackId),
                writeJson(auditPayload));
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new BusinessException(500, "JSON_WRITE_FAILED", "Failed to serialize JSON");
        }
    }

    private String timestamp(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }
}
