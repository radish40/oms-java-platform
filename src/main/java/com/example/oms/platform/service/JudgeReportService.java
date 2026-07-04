package com.example.oms.platform.service;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.entity.DiagnosisAuditEvent;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.repository.DiagnosisRuntimeRepository;
import com.example.oms.platform.security.AuthUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JudgeReportService {
    private static final String EVAL_REVIEW = "eval:review";

    private final DiagnosisRuntimeRepository runtimeRepository;
    private final AuthService authService;
    private final RbacService rbacService;
    private final ObjectMapper objectMapper;

    public JudgeReportService(
            DiagnosisRuntimeRepository runtimeRepository,
            AuthService authService,
            RbacService rbacService,
            ObjectMapper objectMapper) {
        this.runtimeRepository = runtimeRepository;
        this.authService = authService;
        this.rbacService = rbacService;
        this.objectMapper = objectMapper;
    }

    public JsonNode listReports(String runId, String limit, String authorization) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("run_id", runId);
        query.put("limit", limit);
        return runtimeRepository.getJson("/diagnosis/judge-reports", query, authorization).body();
    }

    public JsonNode summary(String limit, String provider, String authorization) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("limit", limit);
        query.put("provider", provider);
        return runtimeRepository.getJson("/diagnosis/judge-reports/summary", query, authorization).body();
    }

    public JsonNode run(DiagnosisPayloadRequest payload, String authorization) {
        AuthUser actor = authService.currentUser(authorization);
        JsonNode body = runtimeRepository.postJson("/diagnosis/judge-reports/run", payload, authorization).body();
        record(new DiagnosisAuditEvent(
                "ai_judge.run",
                actor.username(),
                EVAL_REVIEW,
                "diagnosis_run",
                text(payload.field("run_id")),
                mapOf(
                        "run_id", payload.field("run_id"),
                        "provider", defaultProvider(payload.field("provider")),
                        "status", body.path("report").path("status").asText(""),
                        "path", "/diagnosis/judge-reports/run")));
        return body;
    }

    public JsonNode batchRun(DiagnosisPayloadRequest payload, String authorization) {
        AuthUser actor = authService.currentUser(authorization);
        JsonNode body = runtimeRepository.postJson("/diagnosis/judge-reports/batch-run", payload, authorization).body();
        record(new DiagnosisAuditEvent(
                "ai_judge.batch_run",
                actor.username(),
                EVAL_REVIEW,
                "ai_judge_report",
                defaultProvider(payload.field("provider")),
                mapOf(
                        "limit", payload.field("limit"),
                        "provider", defaultProvider(payload.field("provider")),
                        "completed", body.path("completed").isMissingNode() ? null : body.path("completed").asInt(),
                        "failed", body.path("failed").isMissingNode() ? null : body.path("failed").asInt(),
                        "path", "/diagnosis/judge-reports/batch-run")));
        return body;
    }

    private void record(DiagnosisAuditEvent event) {
        try {
            rbacService.recordAuditEvent(
                    event.eventType(),
                    event.actor(),
                    event.permission(),
                    event.resourceType(),
                    event.resourceId(),
                    objectMapper.writeValueAsString(event.payload()));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(500, "AUDIT_SERIALIZATION_FAILED", "Audit payload serialization failed");
        }
    }

    private static String defaultProvider(Object provider) {
        String value = text(provider);
        return value.isBlank() ? "longcat" : value;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
