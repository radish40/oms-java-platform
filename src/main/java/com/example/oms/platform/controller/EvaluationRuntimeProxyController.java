package com.example.oms.platform.controller;

import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.EvaluationRuntimeProxyService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationRuntimeProxyController {
    private final EvaluationRuntimeProxyService proxyService;

    public EvaluationRuntimeProxyController(EvaluationRuntimeProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/eval/case-bank")
    @RequiresPermission("eval:review")
    public JsonNode listCaseBank(
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "scenario_type", defaultValue = "") String scenarioType,
            @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.listCaseBank(status, scenarioType, limit, authorization);
    }

    @GetMapping(value = "/eval/case-bank/export", produces = "application/x-ndjson; charset=utf-8")
    @RequiresPermission("eval:review")
    public String exportCaseBank(
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.exportCaseBank(status, limit, authorization);
    }

    @GetMapping("/eval/case-bank/{caseId}")
    @RequiresPermission("eval:review")
    public JsonNode getCase(
            @PathVariable String caseId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.getCase(caseId, authorization);
    }

    @PostMapping("/eval/case-bank")
    @RequiresPermission("eval:review")
    public JsonNode saveCase(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.saveCase(payload, authorization);
    }

    @GetMapping("/knowledge/entries")
    @RequiresPermission("eval:review")
    public JsonNode listKnowledgeEntries(
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "category", defaultValue = "") String category,
            @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.listKnowledgeEntries(status, category, limit, authorization);
    }

    @GetMapping("/knowledge/entries/{entryId}")
    @RequiresPermission("eval:review")
    public JsonNode getKnowledgeEntry(
            @PathVariable String entryId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.getKnowledgeEntry(entryId, authorization);
    }

    @PostMapping("/knowledge/entries")
    @RequiresPermission("eval:review")
    public JsonNode saveKnowledgeEntry(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.saveKnowledgeEntry(payload, authorization);
    }
}
