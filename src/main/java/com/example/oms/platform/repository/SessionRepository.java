package com.example.oms.platform.repository;

import com.example.oms.platform.entity.SessionEntity;
import com.example.oms.platform.entity.SessionMessageEntity;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SessionRepository {
    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_sessions (
                    id VARCHAR(128) PRIMARY KEY,
                    preview VARCHAR(512) NOT NULL DEFAULT '',
                    turns INT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_session_messages (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id VARCHAR(128) NOT NULL,
                    seq INT NOT NULL DEFAULT 0,
                    role VARCHAR(32) NOT NULL,
                    content TEXT,
                    reasoning_content TEXT,
                    tool_call_id VARCHAR(128),
                    display_name VARCHAR(128),
                    name VARCHAR(128),
                    params TEXT,
                    description TEXT,
                    summary TEXT,
                    elapsed_ms INT,
                    interp_json TEXT,
                    tool_calls_json TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    public List<SessionEntity> findAll() {
        return findAll(50, 0, "");
    }

    public List<SessionEntity> findAll(int limit, int offset, String search) {
        if (search != null && !search.isBlank()) {
            return jdbcTemplate.query("""
                    SELECT id, preview, turns, created_at, updated_at
                    FROM oms_ai_sessions
                    WHERE preview LIKE ? OR id LIKE ?
                    ORDER BY updated_at DESC, id DESC
                    LIMIT ? OFFSET ?
                    """, this::sessionRow,
                    "%" + search + "%", "%" + search + "%", limit, offset);
        }
        return jdbcTemplate.query("""
                SELECT id, preview, turns, created_at, updated_at
                FROM oms_ai_sessions
                ORDER BY updated_at DESC, id DESC
                LIMIT ? OFFSET ?
                """, this::sessionRow, limit, offset);
    }

    public int countSessions(String search) {
        if (search != null && !search.isBlank()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM oms_ai_sessions WHERE preview LIKE ? OR id LIKE ?",
                    Integer.class, "%" + search + "%", "%" + search + "%");
            return count == null ? 0 : count;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_sessions", Integer.class);
        return count == null ? 0 : count;
    }

    public boolean exists(String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oms_ai_sessions WHERE id = ?",
                Integer.class,
                sessionId);
        return count != null && count > 0;
    }

    public List<SessionMessageEntity> findMessages(String sessionId) {
        return jdbcTemplate.query("""
                SELECT id, session_id, seq, role, content, reasoning_content, tool_call_id,
                       display_name, name, params, description, summary, elapsed_ms,
                       interp_json, tool_calls_json
                FROM oms_ai_session_messages
                WHERE session_id = ?
                ORDER BY seq, id
                """, this::messageRow, sessionId);
    }

    @Transactional
    public boolean delete(String sessionId) {
        if (!exists(sessionId)) {
            return false;
        }
        jdbcTemplate.update("DELETE FROM oms_ai_session_messages WHERE session_id = ?", sessionId);
        jdbcTemplate.update("DELETE FROM oms_ai_sessions WHERE id = ?", sessionId);
        return true;
    }

    private SessionEntity sessionRow(ResultSet rs, int index) throws SQLException {
        return new SessionEntity(
                rs.getString("id"),
                rs.getString("preview"),
                rs.getInt("turns"),
                timestamp(rs, "created_at"),
                timestamp(rs, "updated_at"));
    }

    private SessionMessageEntity messageRow(ResultSet rs, int index) throws SQLException {
        int elapsed = rs.getInt("elapsed_ms");
        return new SessionMessageEntity(
                rs.getLong("id"),
                rs.getString("session_id"),
                rs.getInt("seq"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("reasoning_content"),
                rs.getString("tool_call_id"),
                rs.getString("display_name"),
                rs.getString("name"),
                rs.getString("params"),
                rs.getString("description"),
                rs.getString("summary"),
                rs.wasNull() ? null : elapsed,
                rs.getString("interp_json"),
                rs.getString("tool_calls_json"));
    }

    private LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }
}
