package com.example.oms.platform.service;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.repository.DiagnosisRuntimeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
    private final DiagnosisRuntimeRepository runtimeRepository;

    public FeedbackService(DiagnosisRuntimeRepository runtimeRepository) {
        this.runtimeRepository = runtimeRepository;
    }

    public JsonNode listFeedback(String runId, String authorization) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("run_id", runId);
        return runtimeRepository.getJson("/diagnosis/feedback", query, authorization).body();
    }

    public JsonNode summary(String authorization) {
        return runtimeRepository.getJson("/diagnosis/feedback/summary", Map.of(), authorization).body();
    }

    public JsonNode save(DiagnosisPayloadRequest payload, String authorization) {
        return runtimeRepository.postJson("/diagnosis/feedback", payload, authorization).body();
    }
}
