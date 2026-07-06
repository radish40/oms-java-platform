package com.example.oms.platform.controller;

import com.example.oms.platform.service.DiagnosisService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diagnosis/runs")
@Tag(name = "诊断记录")
public class DiagnosisController {
    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @GetMapping
    @Operation(summary = "诊断记录列表", description = "分页查询诊断运行记录列表")
    public JsonNode listRuns(
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit,
            @Parameter(description = "会话ID，按会话过滤") @RequestParam(value = "session_id", defaultValue = "") String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return diagnosisService.listRuns(limit, sessionId, authorization);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "诊断记录详情", description = "获取指定诊断运行的详细信息")
    public JsonNode getRun(
            @Parameter(description = "诊断运行ID") @PathVariable String runId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return diagnosisService.getRun(runId, authorization);
    }
}
