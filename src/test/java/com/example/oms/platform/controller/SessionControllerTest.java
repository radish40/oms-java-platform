package com.example.oms.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SessionEvaluationTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM oms_ai_session_messages");
        jdbcTemplate.update("DELETE FROM oms_ai_sessions");
        jdbcTemplate.update("""
                INSERT INTO oms_ai_sessions (id, preview, turns, created_at, updated_at)
                VALUES (?, ?, ?, TIMESTAMP '2026-07-04 08:00:00', TIMESTAMP '2026-07-04 08:05:00')
                """, "s_1", "hello", 2);
        jdbcTemplate.update("""
                INSERT INTO oms_ai_session_messages
                    (session_id, seq, role, content, reasoning_content, tool_call_id,
                     display_name, name, params, description, summary, elapsed_ms,
                     interp_json, tool_calls_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "s_1",
                1,
                "assistant",
                "hello",
                "thinking",
                "tool-1",
                "Assistant",
                "query_order",
                "{}",
                "Lookup order",
                "done",
                12,
                "{\"tone\":\"neutral\",\"text\":\"ok\"}",
                "[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"query_order\",\"arguments\":\"{}\"}}]");
    }

    @Test
    void listSessionsReturnsTypeScriptCompatibleFields() throws Exception {
        mockMvc.perform(get("/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions[0].id").value("s_1"))
                .andExpect(jsonPath("$.sessions[0].preview").value("hello"))
                .andExpect(jsonPath("$.sessions[0].turns").value(2))
                .andExpect(jsonPath("$.sessions[0].created_at").value("2026-07-04T08:00"))
                .andExpect(jsonPath("$.sessions[0].updated_at").value("2026-07-04T08:05"));
    }

    @Test
    void getSessionReturnsMessagesWithOptionalChatFields() throws Exception {
        mockMvc.perform(get("/sessions/s_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value("s_1"))
                .andExpect(jsonPath("$.messages[0].role").value("assistant"))
                .andExpect(jsonPath("$.messages[0].content").value("hello"))
                .andExpect(jsonPath("$.messages[0].reasoning_content").value("thinking"))
                .andExpect(jsonPath("$.messages[0].tool_call_id").value("tool-1"))
                .andExpect(jsonPath("$.messages[0].elapsed_ms").value(12))
                .andExpect(jsonPath("$.messages[0].interp.tone").value("neutral"))
                .andExpect(jsonPath("$.messages[0].tool_calls[0].function.name").value("query_order"));
    }

    @Test
    void deleteSessionDeletesMessagesAndReturnsDeletedId() throws Exception {
        mockMvc.perform(delete("/sessions/s_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value("s_1"));

        mockMvc.perform(get("/sessions/s_1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.session_id").value("s_1"));
    }

    @Test
    void getSessionReturnsStructuredNotFound() throws Exception {
        mockMvc.perform(get("/sessions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
