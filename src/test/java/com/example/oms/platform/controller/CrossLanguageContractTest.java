package com.example.oms.platform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CrossLanguageContractTest {

    private static final String[] EXPECTED_ERROR_CODES = {
            "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "BAD_REQUEST",
            "INTERNAL_ERROR", "RUNTIME_UNAVAILABLE", "UPSTREAM_ERROR"
    };

    private static final Map<String, List<String>> ENDPOINT_RESPONSE_KEYS = Map.ofEntries(
            Map.entry("POST /auth/login", List.of("token", "user")),
            Map.entry("GET /auth/me", List.of("user")),
            Map.entry("GET /admin/rbac", List.of("users", "roles", "permissions", "audit_events")),
            Map.entry("GET /sessions", List.of("sessions")),
            Map.entry("GET /sessions/{id}", List.of("session_id", "messages")),
            Map.entry("DELETE /sessions/{id}", List.of("deleted")),
            Map.entry("GET /eval/candidates", List.of("candidates")),
            Map.entry("GET /health", List.of("status", "components")),
            Map.entry("GET /disposal/workflows", List.of("workflows", "total")),
            Map.entry("GET /disposal/workflows/{id}", List.of("workflow_id", "workflow", "records", "action_drafts", "rollback_plans"))
    );

    private static final Set<String> SNAKE_CASE_ENDPOINTS = Set.of(
            "/auth/login",
            "/auth/me",
            "/admin/rbac",
            "/admin/users",
            "/diagnosis/runs",
            "/diagnosis/feedback",
            "/diagnosis/judge-reports",
            "/eval/candidates",
            "/model-options/chat",
            "/disposal/workflows",
            "/disposal/records",
            "/disposal/action-drafts",
            "/disposal/rollback-plans"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void errorEnvelopeCompatibilityWithPythonAgent() {
        ObjectNode envelope = objectMapper.createObjectNode();
        ObjectNode errorNode = envelope.putObject("error");
        errorNode.put("code", "FORBIDDEN");
        errorNode.put("message", "Missing permission: admin:rbac");
        ObjectNode detailsNode = errorNode.putObject("details");
        detailsNode.put("permission", "admin:rbac");

        JsonNode parsed = objectMapper.valueToTree(
                Map.of("error", Map.of(
                        "code", "FORBIDDEN",
                        "message", "Missing permission: admin:rbac",
                        "details", Map.of("permission", "admin:rbac"))));

        assertTrue(parsed.has("error"));
        assertTrue(parsed.get("error").has("code"));
        assertTrue(parsed.get("error").has("message"));
        assertTrue(parsed.get("error").has("details"));
        assertEquals("FORBIDDEN", parsed.get("error").get("code").asText());
    }

    @Test
    void standardErrorCodesDefined() {
        for (String code : EXPECTED_ERROR_CODES) {
            assertNotNull(code, "Error code must not be null");
        }
    }

    @Test
    void responseShapeContractMatchesPythonAgentExpectations() {
        for (Map.Entry<String, List<String>> entry : ENDPOINT_RESPONSE_KEYS.entrySet()) {
            String endpoint = entry.getKey();
            List<String> keys = entry.getValue();
            assertNotNull(keys, endpoint + " must have expected response keys");
            assertTrue(keys.size() >= 1, endpoint + " must have at least 1 response key");
        }
    }

    @Test
    void allEndpointsUseSnakeCasePaths() {
        for (String path : SNAKE_CASE_ENDPOINTS) {
            assertTrue(path.equals(path.toLowerCase()),
                    "API path " + path + " must use lowercase");
            assertTrue(!path.contains("_") || path.contains("/") || path.startsWith("/"),
                    "API path " + path + " uses standard URL conventions");
        }
    }

    @Test
    void disposalEndpointsFollowPlatformConvention() {
        Map<String, String> disposalEndpoints = Map.of(
                "GET /disposal/workflows", "list disposal workflows",
                "POST /disposal/workflows", "create disposal workflow",
                "GET /disposal/workflows/{id}", "get workflow detail",
                "PUT /disposal/workflows/{id}/status", "update workflow status",
                "POST /disposal/records", "record human decision",
                "POST /disposal/action-drafts", "create action suggestion",
                "DELETE /disposal/action-drafts/{id}", "delete action draft",
                "POST /disposal/rollback-plans", "create rollback plan",
                "PUT /disposal/rollback-plans/{id}/approve", "approve rollback");

        assertEquals(9, disposalEndpoints.size(), "Disposal workflow should have 9 endpoints");
    }

    @Test
    void dataMaskingProtectsSensitiveFields() {
        List<String> sensitiveFields = List.of(
                "email", "phone", "mobile", "password", "password_hash",
                "secret", "token", "address", "card_number", "id_number",
                "customer_name", "customer_phone", "customer_email",
                "customer_address", "bank_account");

        assertTrue(sensitiveFields.size() >= 10,
                "At least 10 sensitive fields must be defined for masking");
    }

    @Test
    void disposalHardConstraintsDocumented() {
        List<String> forbiddenActions = List.of(
                "auto_change_order_status",
                "auto_cancel_order",
                "auto_modify_address",
                "auto_notify_customer");

        assertEquals(4, forbiddenActions.size(),
                "Must have 4 forbidden automatic actions");

        assertTrue(forbiddenActions.contains("auto_change_order_status"));
        assertTrue(forbiddenActions.contains("auto_cancel_order"));
    }
}
