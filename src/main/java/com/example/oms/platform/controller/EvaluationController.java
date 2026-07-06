package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.EvaluationCandidateReviewRequest;
import com.example.oms.platform.dto.response.EvaluationCandidateListResponse;
import com.example.oms.platform.dto.response.EvaluationCandidateReviewResponse;
import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.AuthService;
import com.example.oms.platform.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "评测候选")
public class EvaluationController {
    private final EvaluationService evaluationService;
    private final AuthService authService;

    public EvaluationController(EvaluationService evaluationService, AuthService authService) {
        this.evaluationService = evaluationService;
        this.authService = authService;
    }

    @GetMapping
    @RequiresPermission("eval:review")
    @Operation(summary = "评测候选列表", description = "查询评测候选列表，支持分页")
    public EvaluationCandidateListResponse listCandidates(
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit) {
        return evaluationService.listCandidates(limit);
    }

    @PostMapping("/review")
    @RequiresPermission("eval:review")
    @Operation(summary = "提交评测审查", description = "提交评测候选的审查结果")
    public EvaluationCandidateReviewResponse saveReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) EvaluationCandidateReviewRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return evaluationService.saveReview(request, user.username());
    }

    @GetMapping(value = "/export", produces = "application/x-ndjson; charset=utf-8")
    @RequiresPermission("eval:review")
    @Operation(summary = "导出评测候选", description = "导出评测候选数据为NDJSON格式")
    public String exportCandidates(
            @Parameter(description = "每页数量") @RequestParam(value = "limit", defaultValue = "") String limit,
            @Parameter(description = "是否仅导出已审查项，1或0") @RequestParam(value = "reviewed_only", defaultValue = "") String reviewedOnly) {
        return evaluationService.exportCandidates(limit, reviewedOnly);
    }
}
