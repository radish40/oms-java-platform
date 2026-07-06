package com.example.oms.platform.service;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.repository.DiagnosisRuntimeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EvaluationRuntimeProxyService {
    private final DiagnosisRuntimeRepository runtimeRepository;

    public EvaluationRuntimeProxyService(DiagnosisRuntimeRepository runtimeRepository) {
        this.runtimeRepository = runtimeRepository;
    }

    public JsonNode listCaseBank(String status, String scenarioType, String limit, String authorization) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("scenario_type", scenarioType);
        query.put("limit", limit);
        return runtimeRepository.getJson("/eval/case-bank", query, authorization).body();
    }

    public JsonNode getCase(String caseId, String authorization) {
        return runtimeRepository.getJson("/eval/case-bank/" + encode(caseId), Map.of(), authorization).body();
    }

    public String exportCaseBank(String status, String limit, String authorization) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("limit", limit);
        return runtimeRepository.getText("/eval/case-bank/export", query, authorization);
    }

    public JsonNode saveCase(Map<String, Object> payload, String authorization) {
        return runtimeRepository.postJson("/eval/case-bank", DiagnosisPayloadRequest.from(payload), authorization).body();
    }

    public JsonNode listKnowledgeEntries(String status, String category, String limit, String authorization) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("category", category);
        query.put("limit", limit);
        return runtimeRepository.getJson("/eval/knowledge-entry", query, authorization).body();
    }

    public JsonNode getKnowledgeEntry(String entryId, String authorization) {
        return runtimeRepository.getJson("/eval/knowledge-entry/" + encode(entryId), Map.of(), authorization).body();
    }

    public JsonNode saveKnowledgeEntry(Map<String, Object> payload, String authorization) {
        return runtimeRepository.postJson("/eval/knowledge-entry", DiagnosisPayloadRequest.from(payload), authorization).body();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
