package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.service.FeedbackService;
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
@RequestMapping("/diagnosis/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public JsonNode listFeedback(
            @RequestParam(value = "run_id", defaultValue = "") String runId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedbackService.listFeedback(runId, authorization);
    }

    @GetMapping("/summary")
    public JsonNode summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedbackService.summary(authorization);
    }

    @PostMapping
    public JsonNode save(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return feedbackService.save(DiagnosisPayloadRequest.from(payload), authorization);
    }
}
