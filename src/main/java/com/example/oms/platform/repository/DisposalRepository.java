package com.example.oms.platform.repository;

import com.example.oms.platform.entity.DisposalActionDraft;
import com.example.oms.platform.entity.DisposalAuditEntity;
import com.example.oms.platform.entity.DisposalRecord;
import com.example.oms.platform.entity.DisposalTicket;
import com.example.oms.platform.entity.DisposalWorkflow;
import com.example.oms.platform.entity.RollbackPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DisposalRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DisposalRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_disposal_workflows (
                    workflow_id VARCHAR(64) PRIMARY KEY,
                    order_id VARCHAR(128) NOT NULL DEFAULT '',
                    diagnosis_run_id VARCHAR(128) NOT NULL DEFAULT '',
                    status VARCHAR(32) NOT NULL DEFAULT 'pending',
                    assignee VARCHAR(128) NOT NULL DEFAULT '',
                    priority VARCHAR(16) NOT NULL DEFAULT 'medium',
                    summary TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved_at TIMESTAMP NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_disposal_records (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    workflow_id VARCHAR(64) NOT NULL,
                    step_type VARCHAR(64) NOT NULL,
                    actor VARCHAR(128) NOT NULL DEFAULT '',
                    decision VARCHAR(32) NOT NULL DEFAULT '',
                    note TEXT,
                    draft_action_json TEXT,
                    before_snapshot_json TEXT,
                    after_snapshot_json TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_disposal_action_drafts (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    workflow_id VARCHAR(64) NOT NULL,
                    action_type VARCHAR(64) NOT NULL,
                    description TEXT,
                    risk_level VARCHAR(16) NOT NULL DEFAULT 'low',
                    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
                    sort_order INT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_disposal_audit_events (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    workflow_id VARCHAR(64) NOT NULL,
                    event_type VARCHAR(128) NOT NULL,
                    actor VARCHAR(128) NOT NULL DEFAULT '',
                    permission VARCHAR(128) DEFAULT '',
                    detail_json TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_disposal_rollback_plans (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    workflow_id VARCHAR(64) NOT NULL,
                    plan_name VARCHAR(256) NOT NULL,
                    description TEXT,
                    steps_json TEXT,
                    triggers_json TEXT,
                    status VARCHAR(32) NOT NULL DEFAULT 'draft',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_disposal_tickets (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    workflow_id VARCHAR(64) NOT NULL,
                    ticket_id VARCHAR(128) NOT NULL,
                    ticket_source VARCHAR(64) NOT NULL DEFAULT 'placeholder',
                    status VARCHAR(32) NOT NULL DEFAULT 'linked',
                    title VARCHAR(512) NOT NULL DEFAULT '',
                    created_by VARCHAR(128) NOT NULL DEFAULT '',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    @Transactional
    public DisposalWorkflow createWorkflow(String workflowId, String orderId, String diagnosisRunId,
                                            String priority, String summary, String assignee) {
        jdbcTemplate.update("""
                INSERT INTO oms_disposal_workflows
                    (workflow_id, order_id, diagnosis_run_id, status, assignee, priority, summary)
                VALUES (?, ?, ?, 'pending', ?, ?, ?)
                """, workflowId, orderId, diagnosisRunId, assignee, priority, summary);
        return findWorkflow(workflowId).orElseThrow();
    }

    public Optional<DisposalWorkflow> findWorkflow(String workflowId) {
        List<DisposalWorkflow> rows = jdbcTemplate.query("""
                SELECT workflow_id, order_id, diagnosis_run_id, status, assignee,
                       priority, summary, created_at, updated_at, resolved_at
                FROM oms_disposal_workflows WHERE workflow_id = ?
                """, this::workflowRow, workflowId);
        return rows.stream().findFirst();
    }

    public Optional<DisposalWorkflow> findWorkflowByDiagnosisRunId(String diagnosisRunId) {
        List<DisposalWorkflow> rows = jdbcTemplate.query("""
                SELECT workflow_id, order_id, diagnosis_run_id, status, assignee,
                       priority, summary, created_at, updated_at, resolved_at
                FROM oms_disposal_workflows WHERE diagnosis_run_id = ?
                ORDER BY created_at DESC LIMIT 1
                """, this::workflowRow, diagnosisRunId);
        return rows.stream().findFirst();
    }

    public List<DisposalWorkflow> listWorkflows(int limit, int offset, String status, String assignee) {
        StringBuilder sql = new StringBuilder("""
                SELECT workflow_id, order_id, diagnosis_run_id, status, assignee,
                       priority, summary, created_at, updated_at, resolved_at
                FROM oms_disposal_workflows WHERE 1=1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (assignee != null && !assignee.isBlank()) {
            sql.append(" AND assignee = ?");
            params.add(assignee);
        }
        sql.append(" ORDER BY updated_at DESC, workflow_id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), this::workflowRow, params.toArray());
    }

    public int countWorkflows(String status, String assignee) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM oms_disposal_workflows WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (assignee != null && !assignee.isBlank()) {
            sql.append(" AND assignee = ?");
            params.add(assignee);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count == null ? 0 : count;
    }

    @Transactional
    public void updateWorkflowStatus(String workflowId, String status, String assignee) {
        jdbcTemplate.update("""
                UPDATE oms_disposal_workflows
                SET status = ?, assignee = ?, updated_at = CURRENT_TIMESTAMP
                WHERE workflow_id = ?
                """, status, assignee, workflowId);
        if ("resolved".equals(status) || "closed".equals(status)) {
            jdbcTemplate.update("UPDATE oms_disposal_workflows SET resolved_at = CURRENT_TIMESTAMP WHERE workflow_id = ?", workflowId);
        }
    }

    @Transactional
    public DisposalRecord createRecord(String workflowId, String stepType, String actor, String decision,
                                        String note, String draftActionJson) {
        jdbcTemplate.update("""
                INSERT INTO oms_disposal_records
                    (workflow_id, step_type, actor, decision, note, draft_action_json)
                VALUES (?, ?, ?, ?, ?, ?)
                """, workflowId, stepType, actor, decision, note, draftActionJson);
        List<DisposalRecord> rows = jdbcTemplate.query("""
                SELECT id, workflow_id, step_type, actor, decision, note,
                       draft_action_json, before_snapshot_json, after_snapshot_json, created_at
                FROM oms_disposal_records WHERE workflow_id = ? ORDER BY id DESC LIMIT 1
                """, this::recordRow, workflowId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<DisposalRecord> findRecords(String workflowId) {
        return jdbcTemplate.query("""
                SELECT id, workflow_id, step_type, actor, decision, note,
                       draft_action_json, before_snapshot_json, after_snapshot_json, created_at
                FROM oms_disposal_records WHERE workflow_id = ? ORDER BY id
                """, this::recordRow, workflowId);
    }

    @Transactional
    public DisposalActionDraft createActionDraft(String workflowId, String actionType, String description,
                                                  String riskLevel, boolean requiresApproval, int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO oms_disposal_action_drafts
                    (workflow_id, action_type, description, risk_level, requires_approval, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """, workflowId, actionType, description, riskLevel, requiresApproval, sortOrder);
        List<DisposalActionDraft> rows = jdbcTemplate.query("""
                SELECT id, workflow_id, action_type, description, risk_level,
                       requires_approval, sort_order, created_at
                FROM oms_disposal_action_drafts WHERE workflow_id = ? ORDER BY id DESC LIMIT 1
                """, this::actionDraftRow, workflowId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<DisposalActionDraft> findActionDrafts(String workflowId) {
        return jdbcTemplate.query("""
                SELECT id, workflow_id, action_type, description, risk_level,
                       requires_approval, sort_order, created_at
                FROM oms_disposal_action_drafts WHERE workflow_id = ? ORDER BY sort_order, id
                """, this::actionDraftRow, workflowId);
    }

    public Optional<DisposalActionDraft> findActionDraft(long id) {
        List<DisposalActionDraft> rows = jdbcTemplate.query("""
                SELECT id, workflow_id, action_type, description, risk_level,
                       requires_approval, sort_order, created_at
                FROM oms_disposal_action_drafts WHERE id = ?
                """, this::actionDraftRow, id);
        return rows.stream().findFirst();
    }

    @Transactional
    public void deleteActionDraft(long id) {
        jdbcTemplate.update("DELETE FROM oms_disposal_action_drafts WHERE id = ?", id);
    }

    public void recordAuditEvent(String workflowId, String eventType, String actor, String permission, String detailJson) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO oms_disposal_audit_events
                        (workflow_id, event_type, actor, permission, detail_json)
                    VALUES (?, ?, ?, ?, ?)
                    """, workflowId, eventType, actor, permission,
                    detailJson == null ? "{}" : detailJson);
        } catch (RuntimeException ignored) {
        }
    }

    public List<DisposalAuditEntity> findAuditEvents(String workflowId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, workflow_id, event_type, actor, permission, detail_json, created_at
                FROM oms_disposal_audit_events WHERE workflow_id = ?
                ORDER BY id DESC LIMIT ?
                """, this::auditRow, workflowId, limit);
    }

    @Transactional
    public DisposalTicket createTicket(String workflowId, String ticketId, String ticketSource,
                                       String status, String title, String createdBy) {
        jdbcTemplate.update("""
                INSERT INTO oms_disposal_tickets
                    (workflow_id, ticket_id, ticket_source, status, title, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """, workflowId, ticketId, ticketSource, status, title, createdBy);
        List<DisposalTicket> rows = jdbcTemplate.query("""
                SELECT id, workflow_id, ticket_id, ticket_source, status, title, created_by, created_at
                FROM oms_disposal_tickets WHERE workflow_id = ? ORDER BY id DESC LIMIT 1
                """, this::ticketRow, workflowId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<DisposalTicket> findTickets(String workflowId) {
        return jdbcTemplate.query("""
                SELECT id, workflow_id, ticket_id, ticket_source, status, title, created_by, created_at
                FROM oms_disposal_tickets WHERE workflow_id = ? ORDER BY id
                """, this::ticketRow, workflowId);
    }

    @Transactional
    public RollbackPlan createRollbackPlan(String workflowId, String planName, String description,
                                            String stepsJson, String triggersJson) {
        jdbcTemplate.update("""
                INSERT INTO oms_disposal_rollback_plans
                    (workflow_id, plan_name, description, steps_json, triggers_json, status)
                VALUES (?, ?, ?, ?, ?, 'draft')
                """, workflowId, planName, description, stepsJson, triggersJson);
        List<RollbackPlan> rows = jdbcTemplate.query("""
                SELECT id, workflow_id, plan_name, description, steps_json,
                       triggers_json, status, created_at, updated_at
                FROM oms_disposal_rollback_plans WHERE workflow_id = ? ORDER BY id DESC LIMIT 1
                """, this::rollbackPlanRow, workflowId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<RollbackPlan> findRollbackPlans(String workflowId) {
        return jdbcTemplate.query("""
                SELECT id, workflow_id, plan_name, description, steps_json,
                       triggers_json, status, created_at, updated_at
                FROM oms_disposal_rollback_plans WHERE workflow_id = ? ORDER BY id
                """, this::rollbackPlanRow, workflowId);
    }

    public Optional<RollbackPlan> findRollbackPlan(long id) {
        List<RollbackPlan> rows = jdbcTemplate.query("""
                SELECT id, workflow_id, plan_name, description, steps_json,
                       triggers_json, status, created_at, updated_at
                FROM oms_disposal_rollback_plans WHERE id = ?
                """, this::rollbackPlanRow, id);
        return rows.stream().findFirst();
    }

    @Transactional
    public void updateRollbackPlanStatus(long id, String status) {
        jdbcTemplate.update("""
                UPDATE oms_disposal_rollback_plans
                SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, status, id);
    }

    private DisposalWorkflow workflowRow(ResultSet rs, int index) throws SQLException {
        return new DisposalWorkflow(
                rs.getString("workflow_id"),
                rs.getString("order_id"),
                rs.getString("diagnosis_run_id"),
                rs.getString("status"),
                rs.getString("assignee"),
                rs.getString("priority"),
                rs.getString("summary"),
                timestamp(rs, "created_at"),
                timestamp(rs, "updated_at"),
                timestamp(rs, "resolved_at"));
    }

    private DisposalRecord recordRow(ResultSet rs, int index) throws SQLException {
        return new DisposalRecord(
                rs.getLong("id"),
                rs.getString("workflow_id"),
                rs.getString("step_type"),
                rs.getString("actor"),
                rs.getString("decision"),
                rs.getString("note"),
                rs.getString("draft_action_json"),
                rs.getString("before_snapshot_json"),
                rs.getString("after_snapshot_json"),
                timestamp(rs, "created_at"));
    }

    private DisposalActionDraft actionDraftRow(ResultSet rs, int index) throws SQLException {
        return new DisposalActionDraft(
                rs.getLong("id"),
                rs.getString("workflow_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("risk_level"),
                rs.getBoolean("requires_approval"),
                rs.getInt("sort_order"),
                timestamp(rs, "created_at"));
    }

    private DisposalAuditEntity auditRow(ResultSet rs, int index) throws SQLException {
        return new DisposalAuditEntity(
                rs.getLong("id"),
                rs.getString("workflow_id"),
                rs.getString("event_type"),
                rs.getString("actor"),
                rs.getString("permission"),
                rs.getString("detail_json"),
                timestamp(rs, "created_at"));
    }

    private RollbackPlan rollbackPlanRow(ResultSet rs, int index) throws SQLException {
        return new RollbackPlan(
                rs.getLong("id"),
                rs.getString("workflow_id"),
                rs.getString("plan_name"),
                rs.getString("description"),
                rs.getString("steps_json"),
                rs.getString("triggers_json"),
                rs.getString("status"),
                timestamp(rs, "created_at"),
                timestamp(rs, "updated_at"));
    }

    private DisposalTicket ticketRow(ResultSet rs, int index) throws SQLException {
        return new DisposalTicket(
                rs.getLong("id"),
                rs.getString("workflow_id"),
                rs.getString("ticket_id"),
                rs.getString("ticket_source"),
                rs.getString("status"),
                rs.getString("title"),
                rs.getString("created_by"),
                timestamp(rs, "created_at"));
    }

    private LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }
}
