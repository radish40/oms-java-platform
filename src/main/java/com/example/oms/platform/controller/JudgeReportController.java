package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.JudgeReportService;
import com.fasterxml.jackson.databind.JsonNode;
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
public class JudgeReportController {
    private final JudgeReportService judgeReportService;

    public JudgeReportController(JudgeReportService judgeReportService) {
        this.judgeReportService = judgeReportService;
    }

    @GetMapping
    public JsonNode listReports(
            @RequestParam(value = "run_id", defaultValue = "") String runId,
            @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.listReports(runId, limit, authorization);
    }

    @GetMapping("/summary")
    @RequiresPermission("eval:review")
    public JsonNode summary(
            @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestParam(value = "provider", defaultValue = "") String provider,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.summary(limit, provider, authorization);
    }

    @PostMapping("/run")
    @RequiresPermission("eval:review")
    public JsonNode run(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.run(DiagnosisPayloadRequest.from(payload), authorization);
    }

    @PostMapping("/batch-run")
    @RequiresPermission("eval:review")
    public JsonNode batchRun(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return judgeReportService.batchRun(DiagnosisPayloadRequest.from(payload), authorization);
    }
}
