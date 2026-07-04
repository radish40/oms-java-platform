package com.example.oms.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.dto.response.DiagnosisRuntimeResponse;
import com.example.oms.platform.repository.DiagnosisRuntimeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = com.example.oms.platform.PlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiagnosisControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiagnosisRuntimeRepository runtimeRepository;

    @Test
    void listRunsForwardsTsQueryAndAuthorization() throws Exception {
        when(runtimeRepository.getJson(eq("/diagnosis/runs"), any(), eq("Bearer dev-token")))
                .thenReturn(json("{\"runs\":[{\"run_id\":\"run-1\",\"session_id\":\"s_1\",\"question\":\"why\",\"status\":\"done\",\"latency_ms\":100,\"error\":\"\",\"started_at\":\"2026-06-20T12:00:00\",\"ended_at\":\"2026-06-20T12:00:01\"}]}"));

        mockMvc.perform(get("/diagnosis/runs?limit=5&session_id=s_1")
                        .header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runs[0].run_id").value("run-1"));

        ArgumentCaptor<Map<String, String>> query = ArgumentCaptor.forClass(Map.class);
        verify(runtimeRepository).getJson(eq("/diagnosis/runs"), query.capture(), eq("Bearer dev-token"));
        org.assertj.core.api.Assertions.assertThat(query.getValue())
                .containsEntry("limit", "5")
                .containsEntry("session_id", "s_1");
    }

    @Test
    void getRunForwardsPathAndAuthorization() throws Exception {
        when(runtimeRepository.getJson(eq("/diagnosis/runs/run-1"), any(), eq("Bearer dev-token")))
                .thenReturn(json("{\"run\":{\"run_id\":\"run-1\"},\"steps\":[],\"summary\":{\"coverage\":[]}}"));

        mockMvc.perform(get("/diagnosis/runs/run-1").header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.run_id").value("run-1"));
    }

    @Test
    void feedbackEndpointsPreserveTsFieldsAndPayload() throws Exception {
        when(runtimeRepository.getJson(eq("/diagnosis/feedback/summary"), any(), eq("Bearer dev-token")))
                .thenReturn(json("""
                        {
                          "total": 1,
                          "runs_with_feedback": 1,
                          "ratings": {"useful": 0, "incomplete": 0, "wrong": 1},
                          "negative_count": 1,
                          "latest": [],
                          "impact_mode": "record_only",
                          "impact_applied": false,
                          "impact_note": "recorded",
                          "next_impact": "review"
                        }
                        """));
        when(runtimeRepository.getJson(eq("/diagnosis/feedback"), any(), eq("Bearer dev-token")))
                .thenReturn(json("{\"feedback\":[{\"id\":1,\"run_id\":\"run-1\",\"rating\":\"wrong\",\"root_cause_correct\":false,\"comment\":\"bad\",\"created_at\":\"2026-06-20T12:00:00\"}]}"));
        when(runtimeRepository.postJson(eq("/diagnosis/feedback"), any(), eq("Bearer dev-token")))
                .thenReturn(json("{\"feedback\":{\"id\":2,\"run_id\":\"run-1\",\"rating\":\"wrong\",\"comment\":\"bad\"}}"));

        mockMvc.perform(get("/diagnosis/feedback/summary").header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.negative_count").value(1));
        mockMvc.perform(get("/diagnosis/feedback?run_id=run-1").header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback[0].root_cause_correct").value(false));
        mockMvc.perform(post("/diagnosis/feedback")
                        .header("Authorization", "Bearer dev-token")
                        .contentType("application/json")
                        .content("{\"run_id\":\"run-1\",\"rating\":\"wrong\",\"comment\":\"bad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback.run_id").value("run-1"));

        ArgumentCaptor<DiagnosisPayloadRequest> payload = ArgumentCaptor.forClass(DiagnosisPayloadRequest.class);
        verify(runtimeRepository).postJson(eq("/diagnosis/feedback"), payload.capture(), eq("Bearer dev-token"));
        org.assertj.core.api.Assertions.assertThat(payload.getValue().body())
                .containsEntry("run_id", "run-1")
                .containsEntry("rating", "wrong");
    }

    @Test
    void judgeReportRunRequiresEvalReviewAndRecordsAudit() throws Exception {
        String token = loginToken("admin", "admin-pass");
        when(runtimeRepository.postJson(eq("/diagnosis/judge-reports/run"), any(), eq("Bearer " + token)))
                .thenReturn(json("{\"report\":{\"run_id\":\"run-1\",\"provider\":\"longcat\",\"status\":\"completed\",\"latency_ms\":1200,\"scores\":{\"context_alignment\":0.8},\"issues\":[]}}"));

        mockMvc.perform(post("/diagnosis/judge-reports/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"run_id\":\"run-1\",\"provider\":\"longcat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.status").value("completed"));

        mockMvc.perform(get("/admin/rbac?audit_limit=1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit_events[0].event_type").value("ai_judge.run"))
                .andExpect(jsonPath("$.audit_events[0].resource_id").value("run-1"));
    }

    @Test
    void judgeReportBatchRunRecordsAudit() throws Exception {
        String token = loginToken("admin", "admin-pass");
        when(runtimeRepository.postJson(eq("/diagnosis/judge-reports/batch-run"), any(), eq("Bearer " + token)))
                .thenReturn(json("{\"provider\":\"longcat\",\"limit\":5,\"dry_run\":false,\"results\":[],\"completed\":1,\"failed\":0}"));

        mockMvc.perform(post("/diagnosis/judge-reports/batch-run")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"limit\":5,\"provider\":\"longcat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(1));

        mockMvc.perform(get("/admin/rbac?audit_limit=1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit_events[0].event_type").value("ai_judge.batch_run"))
                .andExpect(jsonPath("$.audit_events[0].resource_type").value("ai_judge_report"));
    }

    @Test
    void judgeReportSummaryRejectsUserWithoutEvalReviewBeforeRuntimeCall() throws Exception {
        String supportToken = loginToken("support", "support-pass");

        mockMvc.perform(get("/diagnosis/judge-reports/summary?limit=50")
                        .header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.permission").value("eval:review"));

        verify(runtimeRepository, never()).getJson(eq("/diagnosis/judge-reports/summary"), any(), any());
    }

    @Test
    void judgeReportsListAndSummaryForwardTsQueries() throws Exception {
        String token = loginToken("admin", "admin-pass");
        when(runtimeRepository.getJson(eq("/diagnosis/judge-reports"), any(), eq("Bearer dev-token")))
                .thenReturn(json("{\"reports\":[{\"id\":1,\"run_id\":\"run-1\",\"provider\":\"longcat\",\"model\":\"LongCat-2.0-Preview\",\"status\":\"completed\",\"scores\":{\"context_alignment\":0.8},\"issues\":[\"weak evidence\"],\"error_message\":\"\",\"created_at\":\"2026-06-20T12:00:00\",\"updated_at\":\"2026-06-20T12:01:00\"}]}"));
        when(runtimeRepository.getJson(eq("/diagnosis/judge-reports/summary"), any(), eq("Bearer " + token)))
                .thenReturn(json("{\"summary\":{\"provider\":\"longcat\",\"total\":2,\"completed\":1,\"failed\":1,\"pending\":0,\"low_score_count\":1,\"clarification_needed_count\":1,\"unsafe_action_advice_count\":0,\"average_scores\":{\"context_alignment\":0.8},\"top_issues\":[{\"issue\":\"weak evidence\",\"count\":1}],\"latest\":[]}}"));

        mockMvc.perform(get("/diagnosis/judge-reports?run_id=run-1").header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports[0].provider").value("longcat"));
        mockMvc.perform(get("/diagnosis/judge-reports/summary?limit=50&provider=longcat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.total").value(2));
    }

    @Test
    private DiagnosisRuntimeResponse json(String body) throws Exception {
        return new DiagnosisRuntimeResponse(objectMapper.readTree(body));
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }
}
