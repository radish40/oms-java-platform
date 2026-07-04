package com.example.oms.platform.service;

import com.example.oms.platform.dto.request.DisposalActionDraftRequest;
import com.example.oms.platform.dto.request.DisposalCreateRequest;
import com.example.oms.platform.dto.request.DisposalRecordRequest;
import com.example.oms.platform.dto.request.RollbackPlanRequest;
import com.example.oms.platform.dto.response.DisposalDetailResponse;
import com.example.oms.platform.dto.response.DisposalDetailResponse.ActionDraftItem;
import com.example.oms.platform.dto.response.DisposalDetailResponse.DisposalRecordItem;
import com.example.oms.platform.dto.response.DisposalDetailResponse.RollbackPlanItem;
import com.example.oms.platform.dto.response.DisposalWorkflowResponse;
import com.example.oms.platform.dto.response.DisposalWorkflowResponse.DisposalWorkflowItem;
import com.example.oms.platform.entity.DisposalActionDraft;
import com.example.oms.platform.entity.DisposalRecord;
import com.example.oms.platform.entity.DisposalWorkflow;
import com.example.oms.platform.entity.RollbackPlan;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.repository.DisposalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DisposalService {
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAP = new TypeReference<>() {};

    private static final List<String> FORBIDDEN_ACTION_TYPES = List.of(
            "auto_change_order_status",
            "auto_cancel_order",
            "auto_modify_address",
            "auto_notify_customer");

    private final DisposalRepository repository;
    private final ObjectMapper objectMapper;

    public DisposalService(DisposalRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public DisposalWorkflowResponse listWorkflows(int limit, int offset, String status, String assignee) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 200));
        int boundedOffset = Math.max(0, offset);
        List<DisposalWorkflow> workflows = repository.listWorkflows(boundedLimit, boundedOffset, status, assignee);
        int total = repository.countWorkflows(status, assignee);
        List<DisposalWorkflowItem> items = workflows.stream().map(this::toWorkflowItem).toList();
        return new DisposalWorkflowResponse(items, total);
    }

    public DisposalWorkflowResponse createWorkflow(DisposalCreateRequest request, String actor) {
        String workflowId = "wf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        DisposalWorkflow wf = repository.createWorkflow(
                workflowId,
                request.orderId() == null ? "" : request.orderId(),
                request.diagnosisRunId() == null ? "" : request.diagnosisRunId(),
                request.priority() == null ? "medium" : request.priority(),
                request.summary() == null ? "" : request.summary(),
                actor);
        repository.recordAuditEvent(workflowId, "disposal.workflow.create", actor, "disposal:create", "{}");
        return new DisposalWorkflowResponse(toWorkflowItem(wf));
    }

    public DisposalDetailResponse getWorkflow(String workflowId) {
        DisposalWorkflow wf = repository.findWorkflow(workflowId)
                .orElseThrow(() -> new BusinessException(404, "NOT_FOUND", "Workflow not found",
                        Map.of("workflow_id", workflowId)));
        List<DisposalRecord> records = repository.findRecords(workflowId);
        List<DisposalActionDraft> drafts = repository.findActionDrafts(workflowId);
        List<RollbackPlan> plans = repository.findRollbackPlans(workflowId);
        return new DisposalDetailResponse(
                workflowId,
                toWorkflowItem(wf),
                records.stream().map(this::toRecordItem).toList(),
                drafts.stream().map(this::toActionDraftItem).toList(),
                plans.stream().map(this::toRollbackPlanItem).toList());
    }

    public DisposalDetailResponse.DisposalRecordItem recordDecision(DisposalRecordRequest request, String actor) {
        validateForbiddenAction(request);
        String workflowId = request.workflowId();
        ensureWorkflowExists(workflowId);
        String draftActionJson = serialize(request.draftAction());
        DisposalRecord record = repository.createRecord(
                workflowId,
                request.stepType() == null ? "manual_confirmation" : request.stepType(),
                actor,
                request.decision() == null ? "" : request.decision(),
                request.note() == null ? "" : request.note(),
                draftActionJson);
        repository.recordAuditEvent(workflowId, "disposal.record.create", actor, "disposal:review",
                serialize(Map.of("step_type", request.stepType(), "decision", request.decision())));
        return toRecordItem(record);
    }

    public DisposalDetailResponse.ActionDraftItem createActionDraft(DisposalActionDraftRequest request, String actor) {
        validateForbiddenActionType(request.actionType());
        String workflowId = request.workflowId();
        ensureWorkflowExists(workflowId);
        DisposalActionDraft draft = repository.createActionDraft(
                workflowId,
                request.actionType(),
                request.description() == null ? "" : request.description(),
                request.riskLevel() == null ? "low" : request.riskLevel(),
                request.requiresApproval(),
                request.sortOrder());
        repository.recordAuditEvent(workflowId, "disposal.draft.create", actor, "disposal:draft",
                serialize(Map.of("action_type", request.actionType())));
        return toActionDraftItem(draft);
    }

    public void deleteActionDraft(long id, String actor) {
        repository.deleteActionDraft(id);
    }

    public DisposalDetailResponse.RollbackPlanItem createRollbackPlan(RollbackPlanRequest request, String actor) {
        String workflowId = request.workflowId();
        ensureWorkflowExists(workflowId);
        String stepsJson = serialize(request.steps());
        String triggersJson = serialize(request.triggers());
        RollbackPlan plan = repository.createRollbackPlan(
                workflowId,
                request.planName() == null ? "Untitled Plan" : request.planName(),
                request.description() == null ? "" : request.description(),
                stepsJson,
                triggersJson);
        repository.recordAuditEvent(workflowId, "disposal.rollback.create", actor, "disposal:rollback", "{}");
        return toRollbackPlanItem(plan);
    }

    public void updateWorkflowStatus(String workflowId, String status, String actor) {
        ensureWorkflowExists(workflowId);
        repository.updateWorkflowStatus(workflowId, status, actor);
        repository.recordAuditEvent(workflowId, "disposal.workflow.status_change", actor, "disposal:update",
                serialize(Map.of("new_status", status)));
    }

    public void approveRollbackPlan(long id, String actor) {
        repository.updateRollbackPlanStatus(id, "approved");
    }

    private void validateForbiddenAction(DisposalRecordRequest request) {
        if (request.draftAction() != null) {
            String actionType = (String) request.draftAction().getOrDefault("action_type", "");
            validateForbiddenActionType(actionType);
        }
    }

    private void validateForbiddenActionType(String actionType) {
        if (FORBIDDEN_ACTION_TYPES.contains(actionType)) {
            throw new BusinessException(403, "FORBIDDEN_ACTION",
                    "Automatic changes to order status, cancellations, address, " +
                    "or customer notifications are prohibited. Only human-reviewed decisions may be recorded.");
        }
    }

    private void ensureWorkflowExists(String workflowId) {
        if (!repository.findWorkflow(workflowId).isPresent()) {
            throw new BusinessException(404, "NOT_FOUND", "Workflow not found",
                    Map.of("workflow_id", workflowId));
        }
    }

    private DisposalWorkflowItem toWorkflowItem(DisposalWorkflow wf) {
        return new DisposalWorkflowItem(
                wf.workflowId(),
                wf.orderId(),
                wf.diagnosisRunId(),
                wf.status(),
                wf.assignee(),
                wf.priority(),
                wf.summary(),
                ts(wf.createdAt()),
                ts(wf.updatedAt()),
                ts(wf.resolvedAt()));
    }

    private DisposalRecordItem toRecordItem(DisposalRecord record) {
        return new DisposalRecordItem(
                record.id(),
                record.stepType(),
                record.actor(),
                record.decision(),
                record.note(),
                parseMap(record.draftActionJson()),
                ts(record.createdAt()));
    }

    private ActionDraftItem toActionDraftItem(DisposalActionDraft draft) {
        return new ActionDraftItem(
                draft.id(),
                draft.actionType(),
                draft.description(),
                draft.riskLevel(),
                draft.requiresApproval(),
                draft.sortOrder(),
                ts(draft.createdAt()));
    }

    private RollbackPlanItem toRollbackPlanItem(RollbackPlan plan) {
        return new RollbackPlanItem(
                plan.id(),
                plan.planName(),
                plan.description(),
                parseList(plan.stepsJson()),
                parseList(plan.triggersJson()),
                plan.status(),
                ts(plan.createdAt()),
                ts(plan.updatedAt()));
    }

    private String serialize(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_OF_MAP);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String ts(java.time.LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
