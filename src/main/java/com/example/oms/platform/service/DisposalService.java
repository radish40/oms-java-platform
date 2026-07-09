package com.example.oms.platform.service;

import com.example.oms.platform.dto.request.DisposalActionDraftRequest;
import com.example.oms.platform.dto.request.DisposalCreateRequest;
import com.example.oms.platform.dto.request.DisposalRecordRequest;
import com.example.oms.platform.dto.request.DisposalWorkbenchRequest;
import com.example.oms.platform.dto.request.RollbackPlanRequest;
import com.example.oms.platform.dto.response.DisposalDetailResponse;
import com.example.oms.platform.dto.response.DisposalDetailResponse.ActionDraftItem;
import com.example.oms.platform.dto.response.DisposalDetailResponse.AuditEventItem;
import com.example.oms.platform.dto.response.DisposalDetailResponse.DisposalRecordItem;
import com.example.oms.platform.dto.response.DisposalDetailResponse.RollbackPlanItem;
import com.example.oms.platform.dto.response.DisposalDetailResponse.TicketItem;
import com.example.oms.platform.dto.response.DisposalWorkbenchResponse;
import com.example.oms.platform.dto.response.DisposalWorkflowResponse;
import com.example.oms.platform.dto.response.DisposalWorkflowResponse.DisposalWorkflowItem;
import com.example.oms.platform.entity.DisposalActionDraft;
import com.example.oms.platform.entity.DisposalAuditEntity;
import com.example.oms.platform.entity.DisposalRecord;
import com.example.oms.platform.entity.DisposalTicket;
import com.example.oms.platform.entity.DisposalWorkflow;
import com.example.oms.platform.entity.RollbackPlan;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.repository.DisposalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final DiagnosisService diagnosisService;

    public DisposalService(DisposalRepository repository, ObjectMapper objectMapper, DiagnosisService diagnosisService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.diagnosisService = diagnosisService;
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
        repository.recordAuditEvent(workflowId, "disposal.workflow.create", actor, "disposal:handle", "{}");
        return new DisposalWorkflowResponse(toWorkflowItem(wf));
    }

    public DisposalDetailResponse getWorkflow(String workflowId) {
        DisposalWorkflow wf = repository.findWorkflow(workflowId)
                .orElseThrow(() -> new BusinessException(404, "NOT_FOUND", "处置流程不存在",
                        payload("workflow_id", workflowId)));
        List<DisposalRecord> records = repository.findRecords(workflowId);
        List<DisposalActionDraft> drafts = repository.findActionDrafts(workflowId);
        List<RollbackPlan> plans = repository.findRollbackPlans(workflowId);
        List<DisposalTicket> tickets = repository.findTickets(workflowId);
        List<DisposalAuditEntity> audits = repository.findAuditEvents(workflowId, 100);
        return new DisposalDetailResponse(
                workflowId,
                toWorkflowItem(wf),
                records.stream().map(this::toRecordItem).toList(),
                drafts.stream().map(this::toActionDraftItem).toList(),
                plans.stream().map(this::toRollbackPlanItem).toList(),
                tickets.stream().map(this::toTicketItem).toList(),
                audits.stream().map(this::toAuditItem).toList());
    }

    public DisposalWorkbenchResponse getWorkbenchByDiagnosisRun(String runId) {
        if (runId == null || runId.isBlank()) {
            throw new BusinessException(400, "INVALID_RUN_ID", "缺少诊断记录ID");
        }
        DisposalWorkflow wf = repository.findWorkflowByDiagnosisRunId(runId)
                .orElseThrow(() -> new BusinessException(404, "NOT_FOUND", "该诊断尚未生成处置流程",
                        Map.of("run_id", runId)));
        DisposalDetailResponse detail = getWorkflow(wf.workflowId());
        return new DisposalWorkbenchResponse(
                runId,
                wf.workflowId(),
                false,
                toWorkflowItem(wf),
                detail,
                handlingNoteDraft(null, wf.summary()),
                detail.action_drafts().stream().map(this::actionDraftMap).toList(),
                rollbackGuidance(null),
                firstTicket(detail.tickets()));
    }

    public DisposalWorkbenchResponse createWorkbenchFromDiagnosis(DisposalWorkbenchRequest request, String actor, String authorization) {
        if (request == null || request.runId() == null || request.runId().isBlank()) {
            throw new BusinessException(400, "INVALID_RUN_ID", "缺少诊断记录ID");
        }
        JsonNode diagnosis = diagnosisDetail(request, authorization);
        JsonNode summary = diagnosis == null ? objectMapper.createObjectNode() : diagnosis.path("summary");
        String orderId = firstNonBlank(
                request.orderId(),
                textAt(summary, "/subject/oms_order_id"),
                textAt(summary, "/subject/platform_order_id"));
        String rootCause = firstNonBlank(textAt(summary, "/root_cause"), "诊断记录 " + request.runId());
        String priority = priority(summary);
        String workflowSummary = "诊断 " + request.runId() + "：" + rootCause;

        DisposalWorkflow wf = repository.findWorkflowByDiagnosisRunId(request.runId()).orElse(null);
        boolean created = false;
        if (wf == null) {
            String workflowId = "wf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            wf = repository.createWorkflow(workflowId, orderId, request.runId(), priority, workflowSummary, actor);
            created = true;
            repository.recordAuditEvent(workflowId, "disposal.workbench.create", actor, "disposal:handle",
                    serialize(payload("run_id", request.runId(), "order_id", orderId)));
        } else {
            repository.recordAuditEvent(wf.workflowId(), "disposal.workbench.reuse", actor, "disposal:handle",
                    serialize(Map.of("run_id", request.runId())));
        }

        List<DisposalActionDraft> drafts = repository.findActionDrafts(wf.workflowId());
        List<Map<String, Object>> suggestedActions = suggestedActions(summary);
        if (drafts.isEmpty()) {
            int index = 0;
            for (Map<String, Object> action : suggestedActions) {
                repository.createActionDraft(
                        wf.workflowId(),
                        text(action.get("action_type")),
                        text(action.get("description")),
                        text(action.getOrDefault("risk_level", "low")),
                        Boolean.TRUE.equals(action.get("requires_approval")),
                        index++);
            }
            repository.recordAuditEvent(wf.workflowId(), "disposal.suggestions.generate", actor, "disposal:handle",
                    serialize(Map.of("count", suggestedActions.size())));
        }

        if (repository.findRollbackPlans(wf.workflowId()).isEmpty()) {
            List<Map<String, Object>> rollback = rollbackGuidance(summary);
            repository.createRollbackPlan(
                    wf.workflowId(),
                    "处置回滚预案 " + request.runId(),
                    "如果处置步骤失败，将流程恢复到可复核状态。",
                    serialize(rollback),
                    serialize(List.of(Map.of("when", "所选动作失败，或业务负责人驳回处理结果"))));
            repository.recordAuditEvent(wf.workflowId(), "disposal.rollback.generate", actor, "disposal:handle", "{}");
        }

        DisposalTicket ticket = ensureTicket(wf, request, actor);
        DisposalDetailResponse detail = getWorkflow(wf.workflowId());
        return new DisposalWorkbenchResponse(
                request.runId(),
                wf.workflowId(),
                created,
                toWorkflowItem(wf),
                detail,
                handlingNoteDraft(summary, workflowSummary),
                suggestedActions,
                rollbackGuidance(summary),
                ticket == null ? firstTicket(detail.tickets()) : new DisposalWorkbenchResponse.TicketSummary(
                        ticket.ticketId(), ticket.ticketSource(), ticket.status(), ticket.title()));
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
        repository.recordAuditEvent(workflowId, "disposal.record.create", actor, "disposal:handle",
                serialize(payload("step_type", request.stepType(), "decision", request.decision())));
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
        repository.recordAuditEvent(workflowId, "disposal.draft.create", actor, "disposal:handle",
                serialize(payload("action_type", request.actionType())));
        return toActionDraftItem(draft);
    }

    public void deleteActionDraft(long id, String actor) {
        repository.findActionDraft(id).ifPresent(draft ->
                repository.recordAuditEvent(draft.workflowId(), "disposal.draft.delete", actor, "disposal:handle",
                        serialize(Map.of("draft_id", id, "action_type", draft.actionType()))));
        repository.deleteActionDraft(id);
    }

    public DisposalDetailResponse.RollbackPlanItem createRollbackPlan(RollbackPlanRequest request, String actor) {
        String workflowId = request.workflowId();
        ensureWorkflowExists(workflowId);
        String stepsJson = serialize(request.steps());
        String triggersJson = serialize(request.triggers());
        RollbackPlan plan = repository.createRollbackPlan(
                workflowId,
                request.planName() == null ? "未命名预案" : request.planName(),
                request.description() == null ? "" : request.description(),
                stepsJson,
                triggersJson);
        repository.recordAuditEvent(workflowId, "disposal.rollback.create", actor, "disposal:handle", "{}");
        return toRollbackPlanItem(plan);
    }

    public void updateWorkflowStatus(String workflowId, String status, String actor) {
        ensureWorkflowExists(workflowId);
        repository.updateWorkflowStatus(workflowId, status, actor);
        repository.recordAuditEvent(workflowId, "disposal.workflow.status_change", actor, "disposal:handle",
                serialize(payload("new_status", status)));
    }

    public void approveRollbackPlan(long id, String actor) {
        repository.findRollbackPlan(id).ifPresent(plan ->
                repository.recordAuditEvent(plan.workflowId(), "disposal.rollback.approve", actor, "disposal:handle",
                        serialize(Map.of("rollback_plan_id", id))));
        repository.updateRollbackPlanStatus(id, "approved");
    }

    private JsonNode diagnosisDetail(DisposalWorkbenchRequest request, String authorization) {
        if (request.diagnosisDetail() != null && !request.diagnosisDetail().isEmpty()) {
            return objectMapper.valueToTree(request.diagnosisDetail());
        }
        return diagnosisService.getRun(request.runId(), authorization);
    }

    private DisposalTicket ensureTicket(DisposalWorkflow wf, DisposalWorkbenchRequest request, String actor) {
        List<DisposalTicket> existing = repository.findTickets(wf.workflowId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        boolean shouldCreate = request.createTicket() == null || request.createTicket();
        if (!shouldCreate && (request.ticketId() == null || request.ticketId().isBlank())) {
            return null;
        }
        String ticketId = firstNonBlank(request.ticketId(), "OMS-AI-" + firstNonBlank(wf.orderId(), wf.diagnosisRunId()));
        DisposalTicket ticket = repository.createTicket(
                wf.workflowId(),
                ticketId,
                request.ticketId() == null || request.ticketId().isBlank() ? "placeholder" : "external",
                "linked",
                "处置跟进：" + firstNonBlank(wf.orderId(), wf.diagnosisRunId()),
                actor);
        repository.recordAuditEvent(wf.workflowId(), "disposal.ticket.link", actor, "disposal:handle",
                serialize(Map.of("ticket_id", ticketId, "ticket_source", ticket.ticketSource())));
        return ticket;
    }

    private String handlingNoteDraft(JsonNode summary, String fallback) {
        String rootCause = summary == null ? "" : textAt(summary, "/root_cause");
        String category = summary == null ? "" : textAt(summary, "/root_cause_category");
        String confidence = summary == null ? "" : textAt(summary, "/confidence/label");
        String subject = summary == null ? "" : firstNonBlank(textAt(summary, "/subject/oms_order_id"), textAt(summary, "/subject/platform_order_id"));
        StringBuilder note = new StringBuilder();
        note.append("诊断结论");
        if (!subject.isBlank()) {
            note.append("（").append(subject).append("）");
        }
        note.append("：").append(firstNonBlank(rootCause, fallback, "暂无根因摘要。"));
        if (!category.isBlank()) {
            note.append(" 分类：").append(categoryLabel(category)).append("。");
        }
        if (!confidence.isBlank()) {
            note.append(" 置信度：").append(confidenceLabel(confidence)).append("。");
        }
        note.append(" 执行任何业务操作前必须先完成人工确认。");
        return note.toString();
    }

    private List<Map<String, Object>> suggestedActions(JsonNode summary) {
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(action("manual_verify_evidence", "复核诊断证据，确认受影响的订单和客户上下文。", "low", true));
        if (summary != null && (summary.path("missing_data").size() > 0 || summary.path("next_questions").size() > 0
                || summary.path("next_questions_structured").size() > 0)) {
            actions.add(action("collect_missing_information", "处置前补齐缺失数据，或回答待澄清问题。", "medium", true));
        }
        if (summary != null && !textAt(summary, "/root_cause").isBlank()) {
            actions.add(action("contact_business_owner", "将处理说明草稿发送给相关业务负责人确认。", "medium", true));
        }
        if (summary != null && summary.path("tool_errors").size() > 0) {
            actions.add(action("rerun_diagnosis_after_tool_recovery", "先恢复失败工具并重新诊断，再关闭处置流程。", "medium", true));
        }
        return actions;
    }

    private Map<String, Object> action(String type, String description, String risk, boolean approval) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("action_type", type);
        action.put("description", description);
        action.put("risk_level", risk);
        action.put("requires_approval", approval);
        return action;
    }

    private List<Map<String, Object>> rollbackGuidance(JsonNode summary) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(Map.of("step", "pause_selected_action", "description", "停止当前人工动作，并保持流程在复核状态。"));
        steps.add(Map.of("step", "restore_workflow_state", "description", "将流程恢复为待处理，并保留所有确认记录。"));
        steps.add(Map.of("step", "notify_owner", "description", "将失败原因通知确认人和业务负责人。"));
        if (summary != null && summary.path("tool_errors").size() > 0) {
            steps.add(Map.of("step", "repair_diagnosis_inputs", "description", "修复工具错误，重新诊断后再生成处置建议。"));
        }
        return steps;
    }

    private String priority(JsonNode summary) {
        if (summary != null && (summary.path("tool_errors").size() > 0 || summary.path("missing_data").size() > 0)) {
            return "high";
        }
        String confidence = summary == null ? "" : textAt(summary, "/confidence/label");
        return "low".equals(confidence) ? "high" : "medium";
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
                    "禁止自动修改订单状态、取消订单、修改地址或通知客户。这里只允许记录经过人工复核的处置决定。");
        }
    }

    private String confidenceLabel(String confidence) {
        return switch (confidence) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> confidence;
        };
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "external_push_failed" -> "外部推送失败";
            case "not_entered_oms" -> "未进入 OMS";
            case "sourcing_failed" -> "寻源失败";
            case "warehouse_or_logistics_pending" -> "仓库或物流待处理";
            case "after_sales_or_fulfilled" -> "已履约或售后";
            case "source_code_hint_only" -> "仅源码线索";
            case "official_web_no_signal" -> "官网无信号";
            case "mapping_missing" -> "映射缺失";
            case "unknown" -> "未知";
            default -> category;
        };
    }

    private void ensureWorkflowExists(String workflowId) {
        if (!repository.findWorkflow(workflowId).isPresent()) {
            throw new BusinessException(404, "NOT_FOUND", "处置流程不存在",
                    payload("workflow_id", workflowId));
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

    private TicketItem toTicketItem(DisposalTicket ticket) {
        return new TicketItem(
                ticket.id(),
                ticket.workflowId(),
                ticket.ticketId(),
                ticket.ticketSource(),
                ticket.status(),
                ticket.title(),
                ticket.createdBy(),
                ts(ticket.createdAt()));
    }

    private AuditEventItem toAuditItem(DisposalAuditEntity event) {
        return new AuditEventItem(
                event.id(),
                event.workflowId(),
                event.eventType(),
                event.actor(),
                event.permission(),
                parseMap(event.detailJson()),
                ts(event.createdAt()));
    }

    private Map<String, Object> actionDraftMap(ActionDraftItem item) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("id", item.id());
        action.put("action_type", item.actionType());
        action.put("description", item.description());
        action.put("risk_level", item.riskLevel());
        action.put("requires_approval", item.requiresApproval());
        return action;
    }

    private DisposalWorkbenchResponse.TicketSummary firstTicket(List<TicketItem> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return null;
        }
        TicketItem ticket = tickets.get(0);
        return new DisposalWorkbenchResponse.TicketSummary(
                ticket.ticketId(), ticket.ticketSource(), ticket.status(), ticket.title());
    }

    private String textAt(JsonNode node, String pointer) {
        if (node == null) {
            return "";
        }
        JsonNode value = node.at(pointer);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (keyValues == null) {
            return payload;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            payload.put(String.valueOf(keyValues[i]), keyValues[i + 1] == null ? "" : keyValues[i + 1]);
        }
        return payload;
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
