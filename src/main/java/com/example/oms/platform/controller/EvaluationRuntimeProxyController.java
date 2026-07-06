package com.example.oms.platform.controller;

import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.EvaluationRuntimeProxyService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "案例与知识库代理")
public class EvaluationRuntimeProxyController {
    private final EvaluationRuntimeProxyService proxyService;

    public EvaluationRuntimeProxyController(EvaluationRuntimeProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/eval/case-bank")
    @RequiresPermission("eval:review")
    @Operation(summary = "案例库列表", description = "查询案例库条目列表")
    public JsonNode listCaseBank(
            @Parameter(description = "状态筛选") @RequestParam(value = "status", defaultValue = "") String status,
            @Parameter(description = "场景类型筛选") @RequestParam(value = "scenario_type", defaultValue = "") String scenarioType,
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.listCaseBank(status, scenarioType, limit, authorization);
    }

    @GetMapping(value = "/eval/case-bank/export", produces = "application/x-ndjson; charset=utf-8")
    @RequiresPermission("eval:review")
    @Operation(summary = "导出案例库", description = "导出案例库数据为NDJSON格式")
    public String exportCaseBank(
            @Parameter(description = "状态筛选") @RequestParam(value = "status", defaultValue = "") String status,
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.exportCaseBank(status, limit, authorization);
    }

    @GetMapping("/eval/case-bank/{caseId}")
    @RequiresPermission("eval:review")
    @Operation(summary = "案例详情", description = "获取指定案例的详细信息")
    public JsonNode getCase(
            @Parameter(description = "案例ID") @PathVariable String caseId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.getCase(caseId, authorization);
    }

    @PostMapping("/eval/case-bank")
    @RequiresPermission("eval:review")
    @Operation(summary = "保存案例", description = "创建或更新案例")
    public JsonNode saveCase(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.saveCase(payload, authorization);
    }

    @GetMapping("/knowledge/entries")
    @RequiresPermission("eval:review")
    @Operation(summary = "知识库条目列表", description = "查询知识库条目列表")
    public JsonNode listKnowledgeEntries(
            @Parameter(description = "状态筛选") @RequestParam(value = "status", defaultValue = "") String status,
            @Parameter(description = "分类筛选") @RequestParam(value = "category", defaultValue = "") String category,
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.listKnowledgeEntries(status, category, limit, authorization);
    }

    @GetMapping("/knowledge/entries/{entryId}")
    @RequiresPermission("eval:review")
    @Operation(summary = "知识库条目详情", description = "获取指定知识库条目详情")
    public JsonNode getKnowledgeEntry(
            @Parameter(description = "知识库条目ID") @PathVariable String entryId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.getKnowledgeEntry(entryId, authorization);
    }

    @PostMapping("/knowledge/entries")
    @RequiresPermission("eval:review")
    @Operation(summary = "保存知识库条目", description = "创建或更新知识库条目")
    public JsonNode saveKnowledgeEntry(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxyService.saveKnowledgeEntry(payload, authorization);
    }
}
