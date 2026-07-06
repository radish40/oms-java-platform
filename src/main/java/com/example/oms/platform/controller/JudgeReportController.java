package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.JudgeReportService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diagnosis/judge-reports")
@Tag(name = "评审报告")
public class JudgeReportController {
    private final JudgeReportService judgeReportService;

    public JudgeReportController(JudgeReportService judgeReportService) {
        this.judgeReportService = judgeReportService;
    }

    @GetMapping
    @Operation(summary = "评审报告列表", description = "查询评审报告列表，可按运行ID过滤")
    public JsonNode listReports(
            @Parameter(description = "诊断运行ID") @RequestParam(value = "run_id", defaultValue = "") String runId,
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.listReports(runId, limit, authorization);
    }

    @GetMapping("/summary")
    @RequiresPermission("eval:review")
    @Operation(summary = "评审摘要", description = "获取评审报告的汇总统计")
    public JsonNode summary(
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit,
            @Parameter(description = "供应商/模型名称") @RequestParam(value = "provider", defaultValue = "") String provider,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.summary(limit, provider, authorization);
    }

    @PostMapping("/run")
    @RequiresPermission("eval:review")
    @Operation(summary = "运行评审", description = "触发单条评审任务")
    public JsonNode run(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.run(DiagnosisPayloadRequest.from(payload), authorization);
    }

    @PostMapping("/batch-run")
    @RequiresPermission("eval:review")
    @Operation(summary = "批量运行评审", description = "触发批量评审任务")
    public JsonNode batchRun(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.batchRun(DiagnosisPayloadRequest.from(payload), authorization);
    }
}
