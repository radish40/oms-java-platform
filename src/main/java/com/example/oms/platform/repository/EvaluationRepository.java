package com.example.oms.platform.repository;

import com.example.oms.platform.entity.EvaluationCandidateEntity;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EvaluationRepository {
    private final JdbcTemplate jdbcTemplate;

    public EvaluationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_evaluation_candidates (
                    feedback_id BIGINT PRIMARY KEY,
                    run_id VARCHAR(128) NOT NULL,
                    session_id VARCHAR(128) NOT NULL,
                    rating VARCHAR(32) NOT NULL,
                    root_cause_correct BOOLEAN,
                    feedback_comment TEXT,
                    feedback_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    question TEXT,
                    run_status VARCHAR(32) NOT NULL DEFAULT '',
                    latency_ms INT NOT NULL DEFAULT 0,
                    review_status VARCHAR(32) NOT NULL DEFAULT 'candidate',
                    reviewer VARCHAR(128) NOT NULL DEFAULT '',
                    review_note TEXT,
                    reviewed_at TIMESTAMP NULL,
                    case_draft_json TEXT,
                    case_json TEXT
                )
                """);
    }

    public List<EvaluationCandidateEntity> findCandidates(int limit) {
        return jdbcTemplate.query("""
                SELECT feedback_id, run_id, session_id, rating, root_cause_correct,
                       feedback_comment, feedback_created_at, question, run_status,
                       latency_ms, review_status, reviewer, review_note, reviewed_at,
                       case_draft_json, case_json
                FROM oms_ai_evaluation_candidates
                ORDER BY feedback_created_at DESC, feedback_id DESC
                LIMIT ?
                """, this::candidateRow, limit);
    }

    public List<EvaluationCandidateEntity> findExportCandidates(int limit, boolean reviewedOnly) {
        if (reviewedOnly) {
            return jdbcTemplate.query("""
                    SELECT feedback_id, run_id, session_id, rating, root_cause_correct,
                           feedback_comment, feedback_created_at, question, run_status,
                           latency_ms, review_status, reviewer, review_note, reviewed_at,
                           case_draft_json, case_json
                    FROM oms_ai_evaluation_candidates
                    WHERE review_status = 'reviewed'
                    ORDER BY feedback_created_at DESC, feedback_id DESC
                    LIMIT ?
                    """, this::candidateRow, limit);
        }
        return findCandidates(limit);
    }

    public Optional<EvaluationCandidateEntity> findByFeedbackId(long feedbackId) {
        List<EvaluationCandidateEntity> rows = jdbcTemplate.query("""
                SELECT feedback_id, run_id, session_id, rating, root_cause_correct,
                       feedback_comment, feedback_created_at, question, run_status,
                       latency_ms, review_status, reviewer, review_note, reviewed_at,
                       case_draft_json, case_json
                FROM oms_ai_evaluation_candidates
                WHERE feedback_id = ?
                """, this::candidateRow, feedbackId);
        return rows.stream().findFirst();
    }

    public void saveReview(long feedbackId, String status, String reviewer, String reviewNote, String caseJson) {
        jdbcTemplate.update("""
                UPDATE oms_ai_evaluation_candidates
                SET review_status = ?, reviewer = ?, review_note = ?, reviewed_at = CURRENT_TIMESTAMP, case_json = ?
                WHERE feedback_id = ?
                """, status, reviewer, reviewNote, caseJson, feedbackId);
    }

    private EvaluationCandidateEntity candidateRow(ResultSet rs, int index) throws SQLException {
        Boolean rootCauseCorrect = rs.getObject("root_cause_correct") == null
                ? null
                : rs.getBoolean("root_cause_correct");
        return new EvaluationCandidateEntity(
                rs.getLong("feedback_id"),
                rs.getString("run_id"),
                rs.getString("session_id"),
                rs.getString("rating"),
                rootCauseCorrect,
                text(rs.getString("feedback_comment")),
                timestamp(rs, "feedback_created_at"),
                text(rs.getString("question")),
                text(rs.getString("run_status")),
                rs.getInt("latency_ms"),
                text(rs.getString("review_status")),
                text(rs.getString("reviewer")),
                text(rs.getString("review_note")),
                timestamp(rs, "reviewed_at"),
                text(rs.getString("case_draft_json")),
                text(rs.getString("case_json")));
    }

    private LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
