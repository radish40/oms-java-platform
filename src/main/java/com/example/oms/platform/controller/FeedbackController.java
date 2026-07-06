package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.service.FeedbackService;
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
@RequestMapping("/diagnosis/feedback")
@Tag(name = "诊断反馈")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    @Operation(summary = "反馈列表", description = "查询诊断反馈列表，可按运行ID过滤")
    public JsonNode listFeedback(
            @Parameter(description = "诊断运行ID，按运行过滤") @RequestParam(value = "run_id", defaultValue = "") String runId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedbackService.listFeedback(runId, authorization);
    }

    @GetMapping("/summary")
    @Operation(summary = "反馈摘要", description = "获取诊断反馈的汇总统计数据")
    public JsonNode summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedbackService.summary(authorization);
    }

    @PostMapping
    @Operation(summary = "提交反馈", description = "提交诊断运行反馈")
    public JsonNode save(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedbackService.save(DiagnosisPayloadRequest.from(payload), authorization);
    }
}
