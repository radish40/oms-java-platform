package com.example.oms.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SessionEvaluationTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM oms_ai_evaluation_candidates");
        jdbcTemplate.update("""
                INSERT INTO oms_ai_evaluation_candidates
                    (feedback_id, run_id, session_id, rating, root_cause_correct,
                     feedback_comment, feedback_created_at, question, run_status,
                     latency_ms, review_status, reviewer, review_note, reviewed_at,
                     case_draft_json, case_json)
                VALUES (?, ?, ?, ?, ?, ?, TIMESTAMP '2026-07-04 08:00:00', ?, ?, ?, ?, ?, ?, NULL, ?, ?)
                """,
                1L,
                "run-1",
                "s_1",
                "negative",
                false,
                "wrong root cause",
                "Why is order stuck?",
                "done",
                123,
                "candidate",
                "",
                "",
                "{\"case_id\":\"draft_1\",\"question\":\"Why is order stuck?\"}",
                "{}");
        jdbcTemplate.update("""
                INSERT INTO oms_ai_evaluation_candidates
                    (feedback_id, run_id, session_id, rating, root_cause_correct,
                     feedback_comment, feedback_created_at, question, run_status,
                     latency_ms, review_status, reviewer, review_note, reviewed_at,
                     case_draft_json, case_json)
                VALUES (?, ?, ?, ?, ?, ?, TIMESTAMP '2026-07-04 07:00:00', ?, ?, ?, ?, ?, ?, TIMESTAMP '2026-07-04 07:10:00', ?, ?)
                """,
                2L,
                "run-2",
                "s_2",
                "positive",
                true,
                "good",
                "Export me",
                "done",
                50,
                "reviewed",
                "admin",
                "ok",
                "{}",
                "{\"case_id\":\"feedback_2\"}");
    }

    @Test
    void listCandidatesRequiresEvalReviewPermissionAndReturnsCompatibleFields() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(get("/eval/candidates?limit=1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].feedback_id").value(1))
                .andExpect(jsonPath("$.candidates[0].run_id").value("run-1"))
                .andExpect(jsonPath("$.candidates[0].session_id").value("s_1"))
                .andExpect(jsonPath("$.candidates[0].rating").value("negative"))
                .andExpect(jsonPath("$.candidates[0].root_cause_correct").value(false))
                .andExpect(jsonPath("$.candidates[0].comment").value("wrong root cause"))
                .andExpect(jsonPath("$.candidates[0].feedback_created_at").value("2026-07-04T08:00"))
                .andExpect(jsonPath("$.candidates[0].run_status").value("done"))
                .andExpect(jsonPath("$.candidates[0].latency_ms").value(123))
                .andExpect(jsonPath("$.candidates[0].review_status").value("candidate"))
                .andExpect(jsonPath("$.candidates[0].case_draft.case_id").value("draft_1"))
                .andExpect(jsonPath("$.candidates[0].case_json").isMap());
    }

    @Test
    void listCandidatesRejectsSupportUser() throws Exception {
        String supportToken = loginToken("support", "support-pass");

        mockMvc.perform(get("/eval/candidates").header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.permission").value("eval:review"));
    }

    @Test
    void listCandidatesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/eval/candidates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void saveReviewUpdatesCandidateAndRecordsAudit() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(post("/eval/candidates/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feedback_id": 1,
                                  "run_id": "run-1",
                                  "status": "reviewed",
                                  "review_note": "accepted",
                                  "case_json": {"case_id": "feedback_1"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.feedback_id").value(1))
                .andExpect(jsonPath("$.review.run_id").value("run-1"))
                .andExpect(jsonPath("$.review.status").value("reviewed"))
                .andExpect(jsonPath("$.review.reviewer").value("admin"))
                .andExpect(jsonPath("$.review.review_note").value("accepted"))
                .andExpect(jsonPath("$.review.case_json.case_id").value("feedback_1"));

        String status = jdbcTemplate.queryForObject(
                "SELECT review_status FROM oms_ai_evaluation_candidates WHERE feedback_id = 1",
                String.class);
        assertThat(status).isEqualTo("reviewed");
        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM oms_ai_audit_events
                WHERE event_type = 'eval.review.save'
                  AND actor = 'admin'
                  AND permission = 'eval:review'
                  AND resource_type = 'eval_candidate'
                  AND resource_id = '1'
                """, Integer.class);
        assertThat(auditCount).isNotNull().isGreaterThan(0);
    }

    @Test
    void saveReviewRejectsMissingCandidate() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(post("/eval/candidates/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback_id\":99,\"run_id\":\"run-99\",\"status\":\"reviewed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.details.feedback_id").value(99));
    }

    @Test
    void saveReviewRejectsInvalidStatus() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(post("/eval/candidates/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback_id\":1,\"status\":\"done\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.details.status").value("done"));
    }

    @Test
    void exportCandidatesReturnsNdjsonAndReviewedOnlyFilter() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        String response = mockMvc.perform(get("/eval/candidates/export?reviewed_only=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/x-ndjson"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).isEqualTo("{\"case_id\":\"feedback_2\"}\n");
    }

    @Test
    void exportCandidatesRejectsBadLimit() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(get("/eval/candidates/export?limit=abc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }
}
