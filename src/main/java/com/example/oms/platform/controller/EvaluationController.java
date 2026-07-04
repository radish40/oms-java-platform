package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.EvaluationCandidateReviewRequest;
import com.example.oms.platform.dto.response.EvaluationCandidateListResponse;
import com.example.oms.platform.dto.response.EvaluationCandidateReviewResponse;
import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.AuthService;
import com.example.oms.platform.service.EvaluationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eval/candidates")
public class EvaluationController {
    private final EvaluationService evaluationService;
    private final AuthService authService;

    public EvaluationController(EvaluationService evaluationService, AuthService authService) {
        this.evaluationService = evaluationService;
        this.authService = authService;
    }

    @GetMapping
    @RequiresPermission("eval:review")
    public EvaluationCandidateListResponse listCandidates(
            @RequestParam(value = "limit", defaultValue = "") String limit) {
        return evaluationService.listCandidates(limit);
    }

    @PostMapping("/review")
    @RequiresPermission("eval:review")
    public EvaluationCandidateReviewResponse saveReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) EvaluationCandidateReviewRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return evaluationService.saveReview(request, user.username());
    }

    @GetMapping(value = "/export", produces = "application/x-ndjson; charset=utf-8")
    @RequiresPermission("eval:review")
    public String exportCandidates(
            @RequestParam(value = "limit", defaultValue = "") String limit,
            @RequestParam(value = "reviewed_only", defaultValue = "") String reviewedOnly) {
        return evaluationService.exportCandidates(limit, reviewedOnly);
    }
}
