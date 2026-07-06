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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "处置流程")
public class DisposalController {
    private final DisposalService disposalService;
    private final AuthService authService;

    public DisposalController(DisposalService disposalService, AuthService authService) {
        this.disposalService = disposalService;
        this.authService = authService;
    }

    @GetMapping("/workflows")
    @RequiresPermission("disposal:review")
    @Operation(summary = "工作流列表", description = "分页查询处置工作流列表")
    public DisposalWorkflowResponse listWorkflows(
            @Parameter(description = "每页数量，默认50") @RequestParam(value = "limit", defaultValue = "50") int limit,
            @Parameter(description = "偏移量，默认0") @RequestParam(value = "offset", defaultValue = "0") int offset,
            @Parameter(description = "状态筛选") @RequestParam(value = "status", defaultValue = "") String status,
            @Parameter(description = "负责人筛选") @RequestParam(value = "assignee", defaultValue = "") String assignee) {
        return disposalService.listWorkflows(limit, offset, status, assignee);
    }

    @PostMapping("/workflows")
    @RequiresPermission("disposal:create")
    @Operation(summary = "创建工作流", description = "创建新的处置工作流")
    public DisposalWorkflowResponse createWorkflow(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DisposalCreateRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.createWorkflow(request, user.username());
    }

    @GetMapping("/workflows/{workflowId}")
    @RequiresPermission("disposal:review")
    @Operation(summary = "工作流详情", description = "获取指定工作流的详细信息")
    public DisposalDetailResponse getWorkflow(@Parameter(description = "工作流ID") @PathVariable String workflowId) {
        return disposalService.getWorkflow(workflowId);
    }

    @PutMapping("/workflows/{workflowId}/status")
    @RequiresPermission("disposal:update")
    @Operation(summary = "更新工作流状态", description = "更新指定工作流的状态")
    public Map<String, Object> updateWorkflowStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "工作流ID") @PathVariable String workflowId,
            @RequestBody Map<String, Object> body) {
        AuthUser user = authService.currentUser(authorization);
        String status = (String) body.getOrDefault("status", "");
        disposalService.updateWorkflowStatus(workflowId, status, user.username());
        return Map.of("workflow_id", workflowId, "status", status);
    }

    @PostMapping("/records")
    @RequiresPermission("disposal:review")
    @Operation(summary = "记录处置决策", description = "记录处置流程中的决策操作")
    public DisposalDetailResponse.DisposalRecordItem recordDecision(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DisposalRecordRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.recordDecision(request, user.username());
    }

    @PostMapping("/action-drafts")
    @RequiresPermission("disposal:draft")
    @Operation(summary = "创建处置草稿", description = "创建处置行动草稿")
    public DisposalDetailResponse.ActionDraftItem createActionDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DisposalActionDraftRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.createActionDraft(request, user.username());
    }

    @DeleteMapping("/action-drafts/{id}")
    @RequiresPermission("disposal:draft")
    @Operation(summary = "删除处置草稿", description = "删除指定的处置行动草稿")
    public Map<String, Object> deleteActionDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "草稿ID") @PathVariable long id) {
        AuthUser user = authService.currentUser(authorization);
        disposalService.deleteActionDraft(id, user.username());
        return Map.of("deleted", id);
    }

    @PostMapping("/rollback-plans")
    @RequiresPermission("disposal:rollback")
    @Operation(summary = "创建回滚计划", description = "创建处置回滚计划")
    public DisposalDetailResponse.RollbackPlanItem createRollbackPlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody RollbackPlanRequest request) {
        AuthUser user = authService.currentUser(authorization);
        return disposalService.createRollbackPlan(request, user.username());
    }

    @PutMapping("/rollback-plans/{id}/approve")
    @RequiresPermission("disposal:approve")
    @Operation(summary = "审批回滚计划", description = "审批指定的回滚计划")
    public Map<String, Object> approveRollbackPlan(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "回滚计划ID") @PathVariable long id) {
        AuthUser user = authService.currentUser(authorization);
        disposalService.approveRollbackPlan(id, user.username());
        return Map.of("id", id, "status", "approved");
    }
}
