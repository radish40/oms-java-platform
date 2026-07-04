package com.example.oms.platform.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = com.example.oms.platform.PlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRbacControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rbacOverviewRequiresAdminPermission() throws Exception {
        String supportToken = loginToken("support", "support-pass");

        mockMvc.perform(get("/admin/rbac").header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.permission").value("admin:rbac"));
    }

    @Test
    void rbacOverviewReturnsSeededUsersRolesAndPermissions() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(get("/admin/rbac?audit_limit=5").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[?(@.username == 'admin')]").exists())
                .andExpect(jsonPath("$.roles[?(@.code == 'admin')]").exists())
                .andExpect(jsonPath("$.permissions[?(@.code == 'admin:rbac')]").exists());
    }

    @Test
    void adminCanCreateUser() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "agent",
                                  "display_name": "Agent User",
                                  "password": "agent-pass",
                                  "roles": ["support"],
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("agent"))
                .andExpect(jsonPath("$.user.roles[0]").value("support"));
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
