package com.example.oms.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@SpringBootTest(classes = com.example.oms.platform.PlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DisposalControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginToken("admin", "admin-pass");
        jdbcTemplate.update("DELETE FROM oms_disposal_audit_events");
        jdbcTemplate.update("DELETE FROM oms_disposal_records");
        jdbcTemplate.update("DELETE FROM oms_disposal_action_drafts");
        jdbcTemplate.update("DELETE FROM oms_disposal_rollback_plans");
        jdbcTemplate.update("DELETE FROM oms_disposal_workflows");
    }

    @Test
    void createAndListWorkflows() throws Exception {
        mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-001\",\"diagnosis_run_id\":\"run-1\",\"priority\":\"high\",\"summary\":\"Order anomaly detected\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflow.order_id").value("ORD-001"))
                .andExpect(jsonPath("$.workflow.status").value("pending"));

        mockMvc.perform(get("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflows[0].order_id").value("ORD-001"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void supportUserCannotCreateWorkflow() throws Exception {
        String supportToken = loginToken("support", "support-pass");

        mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + supportToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-001\",\"summary\":\"test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void recordHumanConfirmationDecision() throws Exception {
        String workflowJson = mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-002\",\"priority\":\"medium\",\"summary\":\"Check order detail\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String workflowId = objectMapper.readTree(workflowJson).get("workflow").get("workflow_id").asText();

        mockMvc.perform(post("/disposal/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow_id\":\"" + workflowId + "\",\"step_type\":\"manual_confirmation\",\"decision\":\"approved\",\"note\":\"Reviewed by admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step_type").value("manual_confirmation"))
                .andExpect(jsonPath("$.decision").value("approved"));
    }

    @Test
    void createActionDraft() throws Exception {
        String workflowJson = mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-003\",\"summary\":\"Action draft test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String workflowId = objectMapper.readTree(workflowJson).get("workflow").get("workflow_id").asText();

        mockMvc.perform(post("/disposal/action-drafts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow_id\":\"" + workflowId + "\",\"action_type\":\"verify_inventory\",\"description\":\"Check stock levels\",\"risk_level\":\"low\",\"requires_approval\":false,\"sort_order\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action_type").value("verify_inventory"));
    }

    @Test
    void deleteActionDraft() throws Exception {
        String workflowJson = mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-004\",\"summary\":\"Delete draft test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String workflowId = objectMapper.readTree(workflowJson).get("workflow").get("workflow_id").asText();

        String draftJson = mockMvc.perform(post("/disposal/action-drafts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow_id\":\"" + workflowId + "\",\"action_type\":\"temp_action\",\"description\":\"Temporary\",\"risk_level\":\"low\",\"requires_approval\":false,\"sort_order\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long draftId = objectMapper.readTree(draftJson).get("id").asLong();

        mockMvc.perform(delete("/disposal/action-drafts/" + draftId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(draftId));
    }

    @Test
    void createRollbackPlan() throws Exception {
        String workflowJson = mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-005\",\"summary\":\"Rollback test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String workflowId = objectMapper.readTree(workflowJson).get("workflow").get("workflow_id").asText();

        mockMvc.perform(post("/disposal/rollback-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow_id\":\"" + workflowId + "\",\"plan_name\":\"Rollback Plan A\",\"description\":\"Revert inventory change\",\"steps\":[{\"step\":1,\"action\":\"revert_stock\"}],\"triggers\":[{\"condition\":\"error_rate > 10%\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan_name").value("Rollback Plan A"));
    }

    @Test
    void approveRollbackPlan() throws Exception {
        String workflowJson = mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-006\",\"summary\":\"Approve rollback test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String workflowId = objectMapper.readTree(workflowJson).get("workflow").get("workflow_id").asText();

        String planJson = mockMvc.perform(post("/disposal/rollback-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow_id\":\"" + workflowId + "\",\"plan_name\":\"Plan B\",\"description\":\"Approve test\",\"steps\":[],\"triggers\":[]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long planId = objectMapper.readTree(planJson).get("id").asLong();

        mockMvc.perform(put("/disposal/rollback-plans/" + planId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));
    }

    @Test
    void getWorkflowDetailWithRecords() throws Exception {
        String workflowJson = mockMvc.perform(post("/disposal/workflows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_id\":\"ORD-007\",\"summary\":\"Detail test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String workflowId = objectMapper.readTree(workflowJson).get("workflow").get("workflow_id").asText();

        mockMvc.perform(post("/disposal/records")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflow_id\":\"" + workflowId + "\",\"step_type\":\"manual_confirmation\",\"decision\":\"approved\",\"note\":\"ok\"}"));

        mockMvc.perform(get("/disposal/workflows/" + workflowId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflow_id").value(workflowId))
                .andExpect(jsonPath("$.records[0].decision").value("approved"))
                .andExpect(jsonPath("$.action_drafts").isArray())
                .andExpect(jsonPath("$.rollback_plans").isArray());
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
}
