package com.example.oms.platform.service;

import com.example.oms.platform.repository.DiagnosisRuntimeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiagnosisService {
    private final DiagnosisRuntimeRepository runtimeRepository;

    public DiagnosisService(DiagnosisRuntimeRepository runtimeRepository) {
        this.runtimeRepository = runtimeRepository;
    }

    public JsonNode listRuns(String limit, String sessionId, String authorization) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("limit", limit);
        query.put("session_id", sessionId);
        return runtimeRepository.getJson("/diagnosis/runs", query, authorization).body();
    }

    public JsonNode getRun(String runId, String authorization) {
        return runtimeRepository.getJson("/diagnosis/runs/" + encode(runId), Map.of(), authorization).body();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
