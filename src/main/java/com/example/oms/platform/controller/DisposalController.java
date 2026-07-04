package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.DisposalActionDraftRequest;
import com.example.oms.platform.dto.request.DisposalCreateRequest;
import com.example.oms.platform.dto.request.DisposalRecordRequest;
import com.example.oms.platform.dto.request.RollbackPlanRequest;
import com.example.oms.platform.dto.response.DisposalDetailResponse;
import com.example.oms.platform.dto.response.DisposalWorkflowResponse;
import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.AuthService;
import com.example.oms.platform.service.DisposalService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/disposal")
public class DisposalController {
    private final DisposalService disposalService;
    private final AuthService authService;

    public DisposalController(DisposalService disposalService, AuthService authService) {
        this.disposalService = disposalService;
        this.authService = authService;
    }

    @GetMapping("/workflows")
    @RequiresPermission("disposal:review")
    public DisposalWorkflowResponse listWorkflows(
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "assignee", defaultValue = "") String assignee) {
        return disposalService.listWorkflows(limit, offset, status, assignee);
    }

    @PostMapping("/workflows")
    @RequiresPermission("disposal:create")
    public DisposalWorkflowResponse createWorkflow(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DisposalCreateRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.createWorkflow(request, user.username());
    }

    @GetMapping("/workflows/{workflowId}")
    @RequiresPermission("disposal:review")
    public DisposalDetailResponse getWorkflow(@PathVariable String workflowId) {
        return disposalService.getWorkflow(workflowId);
    }

    @PutMapping("/workflows/{workflowId}/status")
    @RequiresPermission("disposal:update")
    public Map<String, Object> updateWorkflowStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> body) {
        AuthUser user = authService.currentUser(authorization);
        String status = (String) body.getOrDefault("status", "");
        disposalService.updateWorkflowStatus(workflowId, status, user.username());
        return Map.of("workflow_id", workflowId, "status", status);
    }

    @PostMapping("/records")
    @RequiresPermission("disposal:review")
    public DisposalDetailResponse.DisposalRecordItem recordDecision(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DisposalRecordRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.recordDecision(request, user.username());
    }

    @PostMapping("/action-drafts")
    @RequiresPermission("disposal:draft")
    public DisposalDetailResponse.ActionDraftItem createActionDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DisposalActionDraftRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.createActionDraft(request, user.username());
    }

    @DeleteMapping("/action-drafts/{id}")
    @RequiresPermission("disposal:draft")
    public Map<String, Object> deleteActionDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable long id) {
        AuthUser user = authService.currentUser(authorization);
        disposalService.deleteActionDraft(id, user.username());
        return Map.of("deleted", id);
    }

    @PostMapping("/rollback-plans")
    @RequiresPermission("disposal:rollback")
    public DisposalDetailResponse.RollbackPlanItem createRollbackPlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody RollbackPlanRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.createRollbackPlan(request, user.username());
    }

    @PutMapping("/rollback-plans/{id}/approve")
    @RequiresPermission("disposal:approve")
    public Map<String, Object> approveRollbackPlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable long id) {
        AuthUser user = authService.currentUser(authorization);
        disposalService.approveRollbackPlan(id, user.username());
        return Map.of("id", id, "status", "approved");
    }
}
