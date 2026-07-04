package com.example.oms.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.oms.platform.client.PythonRuntimeClient;
import com.example.oms.platform.client.RuntimePing;
import com.example.oms.platform.dto.response.DiagnosisRuntimeResponse;
import com.example.oms.platform.repository.DiagnosisRuntimeRepository;
import com.example.oms.platform.service.ModelRuntimeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = com.example.oms.platform.PlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformContractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PythonRuntimeClient pythonRuntimeClient;

    @MockBean
    private DiagnosisRuntimeRepository diagnosisRuntimeRepository;

    @MockBean
    private ModelRuntimeClient modelRuntimeClient;

    @BeforeEach
    void setUp() throws Exception {
        when(pythonRuntimeClient.ping()).thenReturn(RuntimePing.up());
        when(pythonRuntimeClient.get(eq("/ops/debug"), any())).thenReturn(Map.of(
                "status", "ok",
                "summary", Map.of("total", 1, "ok", 1, "warn", 0, "fail", 0),
                "checks", List.of()));
        when(modelRuntimeClient.get(eq("/model-options/chat"), any())).thenReturn(Map.of(
                "options", List.of(Map.of("provider", "longcat", "model", "model-a"))));
        when(diagnosisRuntimeRepository.getJson(eq("/diagnosis/runs"), any(), any()))
                .thenReturn(json("{\"runs\":[{\"run_id\":\"run-1\",\"session_id\":\"s_1\",\"status\":\"done\"}]}"));
        when(diagnosisRuntimeRepository.getJson(eq("/diagnosis/feedback/summary"), any(), any()))
                .thenReturn(json("{\"total\":1,\"negative_count\":0,\"impact_mode\":\"record_only\"}"));

        jdbcTemplate.update("DELETE FROM oms_ai_evaluation_candidates");
        jdbcTemplate.update("DELETE FROM oms_ai_session_messages");
        jdbcTemplate.update("DELETE FROM oms_ai_sessions");
        jdbcTemplate.update("DELETE FROM oms_disposal_audit_events");
        jdbcTemplate.update("DELETE FROM oms_disposal_records");
        jdbcTemplate.update("DELETE FROM oms_disposal_action_drafts");
        jdbcTemplate.update("DELETE FROM oms_disposal_rollback_plans");
        jdbcTemplate.update("DELETE FROM oms_disposal_workflows");
        jdbcTemplate.update("""
                INSERT INTO oms_ai_sessions (id, preview, turns, created_at, updated_at)
                VALUES (?, ?, ?, TIMESTAMP '2026-07-04 09:00:00', TIMESTAMP '2026-07-04 09:01:00')
                """, "contract-session", "contract preview", 1);
        jdbcTemplate.update("""
                INSERT INTO oms_ai_session_messages
                    (session_id, seq, role, content, elapsed_ms, interp_json, tool_calls_json)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                "contract-session",
                1,
                "assistant",
                "contract response",
                9,
                "{\"mode\":\"contract\"}",
                "[]");
        jdbcTemplate.update("""
                INSERT INTO oms_ai_evaluation_candidates
                    (feedback_id, run_id, session_id, rating, root_cause_correct,
                     feedback_comment, feedback_created_at, question, run_status,
                     latency_ms, review_status, reviewer, review_note, reviewed_at,
                     case_draft_json, case_json)
                VALUES (?, ?, ?, ?, ?, ?, TIMESTAMP '2026-07-04 09:00:00', ?, ?, ?, ?, ?, ?, NULL, ?, ?)
                """,
                9001L,
                "run-1",
                "contract-session",
                "negative",
                false,
                "needs review",
                "Why did the order fail?",
                "done",
                99,
                "candidate",
                "",
                "",
                "{\"case_id\":\"draft-contract\"}",
                "{}");
    }

    @Test
    void finalContractSmokeCoversPrimaryApiSurface() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");
        String supportToken = loginToken("support", "support-pass");

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.permissions[?(@ == 'admin:models')]").exists());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.runtime.status").value("UP"))
                .andExpect(jsonPath("$.mysql.status").value("UP"));

        mockMvc.perform(get("/admin/rbac").header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.permission").value("admin:rbac"));

        mockMvc.perform(get("/diagnosis/runs?limit=1&session_id=contract-session")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runs[0].run_id").value("run-1"));

        mockMvc.perform(get("/diagnosis/feedback/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impact_mode").value("record_only"));

        mockMvc.perform(get("/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions[0].id").value("contract-session"))
                .andExpect(jsonPath("$.sessions[0].preview").value("contract preview"));

        mockMvc.perform(get("/sessions/contract-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id").value("contract-session"))
                .andExpect(jsonPath("$.messages[0].interp.mode").value("contract"));

        mockMvc.perform(get("/eval/candidates?limit=1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].feedback_id").value(9001))
                .andExpect(jsonPath("$.candidates[0].case_draft.case_id").value("draft-contract"));

        mockMvc.perform(post("/eval/candidates/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feedback_id": 9001,
                                  "run_id": "run-1",
                                  "status": "reviewed",
                                  "review_note": "accepted",
                                  "case_json": {"case_id": "contract-reviewed"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.status").value("reviewed"))
                .andExpect(jsonPath("$.review.case_json.case_id").value("contract-reviewed"));

        mockMvc.perform(get("/eval/candidates/export?reviewed_only=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/x-ndjson"))
                .andExpect(content().string("{\"case_id\":\"contract-reviewed\"}\n"));

        mockMvc.perform(get("/model-options/chat").header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].provider").value("longcat"));

        mockMvc.perform(get("/ops/debug").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.total").value(1));

        mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-SMOKE\",\"diagnosis_run_id\":\"run-1\",\"priority\":\"high\",\"summary\":\"Smoke test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflow.status").value("pending"));

        mockMvc.perform(get("/disposal/workflows").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/admin/menus").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus").isArray());

        mockMvc.perform(get("/admin/roles").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());

        mockMvc.perform(get("/admin/permissions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").isArray());
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private DiagnosisRuntimeResponse json(String body) throws Exception {
        return new DiagnosisRuntimeResponse(objectMapper.readTree(body));
    }
}
