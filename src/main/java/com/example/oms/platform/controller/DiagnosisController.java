package com.example.oms.platform.controller;

import com.example.oms.platform.service.DiagnosisService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diagnosis/runs")
public class DiagnosisController {
    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @GetMapping
    public JsonNode listRuns(
            @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestParam(value = "session_id", defaultValue = "") String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return diagnosisService.listRuns(limit, sessionId, authorization);
    }

    @GetMapping("/{runId}")
    public JsonNode getRun(
            @PathVariable String runId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return diagnosisService.getRun(runId, authorization);
    }
}
