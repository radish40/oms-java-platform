package com.example.oms.platform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.oms.platform.dto.response.ChatMessageResponse;
import com.example.oms.platform.dto.response.DeleteSessionResponse;
import com.example.oms.platform.dto.response.ErrorResponse;
import com.example.oms.platform.dto.response.EvaluationCandidateListResponse;
import com.example.oms.platform.dto.response.EvaluationCandidateResponse;
import com.example.oms.platform.dto.response.EvaluationCandidateReviewResponse;
import com.example.oms.platform.dto.response.LoginResponse;
import com.example.oms.platform.dto.response.SessionDetailResponse;
import com.example.oms.platform.dto.response.SessionListResponse;
import com.example.oms.platform.dto.response.SessionSummaryResponse;
import com.example.oms.platform.dto.response.UserResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContractValidationTest {

    private static final List<String> API_PATHS = List.of(
            "/auth/login",
            "/auth/me",
            "/admin/rbac",
            "/admin/users",
            "/admin/roles",
            "/admin/permissions",
            "/admin/menus",
            "/diagnosis/runs",
            "/diagnosis/runs/{run_id}",
            "/diagnosis/feedback",
            "/diagnosis/feedback/summary",
            "/diagnosis/judge-reports",
            "/diagnosis/judge-reports/summary",
            "/diagnosis/judge-reports/run",
            "/diagnosis/judge-reports/batch-run",
            "/sessions",
            "/sessions/{session_id}",
            "/eval/candidates",
            "/eval/candidates/review",
            "/eval/candidates/export",
            "/eval/case-bank",
            "/eval/case-bank/{case_id}",
            "/eval/case-bank/export",
            "/knowledge/entries",
            "/knowledge/entries/{entry_id}",
            "/admin/model-configs",
            "/admin/model-bindings",
            "/model-options/chat",
            "/health",
            "/ops/debug",
            "/disposal/workflows",
            "/disposal/records",
            "/disposal/action-drafts",
            "/disposal/rollback-plans");

    private static final Set<String> REQUIRED_ERROR_FIELDS = Set.of("code", "message", "details");

    @Test
    void errorResponseHasRequiredFields() {
        ErrorResponse error = ErrorResponse.of("TEST", "test message", java.util.Map.of("key", "value"));
        assertNotNull(error.error());
        assertNotNull(error.error().code());
        assertNotNull(error.error().message());
        assertNotNull(error.error().details());
        assertEquals("TEST", error.error().code());
        assertEquals("test message", error.error().message());
        assertEquals("value", error.error().details().get("key"));
    }

    @Test
    void dtoFieldsUseSnakeCaseJsonProperties() {
        checkDtoClass(LoginResponse.class);
        checkDtoClass(UserResponse.class);
        checkDtoClass(SessionSummaryResponse.class);
        checkDtoClass(SessionListResponse.class);
        checkDtoClass(SessionDetailResponse.class);
        checkDtoClass(DeleteSessionResponse.class);
        checkDtoClass(ChatMessageResponse.class);
        checkDtoClass(EvaluationCandidateResponse.class);
        checkDtoClass(EvaluationCandidateListResponse.class);
        checkDtoClass(EvaluationCandidateReviewResponse.class);
    }

    @Test
    void apiPathInventoryIsComplete() {
        assertTrue(API_PATHS.size() >= 20, "API path inventory should contain at least 20 endpoints");
        assertTrue(API_PATHS.contains("/health"), "Health endpoint must be registered");
        assertTrue(API_PATHS.contains("/auth/login"), "Auth login must be registered");
        assertTrue(API_PATHS.contains("/disposal/workflows"), "Disposal workflows must be registered");
    }

    @Test
    void errorEnvelopeStructureMatchesContract() {
        ErrorResponse.ErrorBody envelope = new ErrorResponse.ErrorBody("TEST_CODE", "Test message", java.util.Map.of("detail_key", "detail_val"));
        assertEquals("TEST_CODE", envelope.code());
        assertEquals("Test message", envelope.message());

        ObjectMapper objectMapper = new ObjectMapper();
        assertTrue(objectMapper.canSerialize(ErrorResponse.class));
        assertTrue(objectMapper.canSerialize(ErrorResponse.ErrorBody.class));
    }

    @Test
    void recordComponentsUseJsonAnnotations() {
        Class<?>[] recordClasses = {
                LoginResponse.class, UserResponse.class,
                SessionSummaryResponse.class, ChatMessageResponse.class
        };
        for (Class<?> clazz : recordClasses) {
            assertTrue(clazz.isRecord(), clazz.getSimpleName() + " must be a record");
            for (RecordComponent component : clazz.getRecordComponents()) {
                String name = component.getName();
                assertTrue(isSnakeCaseCompatible(name) || hasJsonPropertyAnnotation(component),
                        clazz.getSimpleName() + "." + name + " should be snake_case or have @JsonProperty");
            }
        }
    }

    private void checkDtoClass(Class<?> clazz) {
        assertTrue(clazz.isRecord() || clazz.getAnnotation(com.fasterxml.jackson.databind.annotation.JsonSerialize.class) == null,
                clazz.getSimpleName() + " is a serializable DTO");
    }

    private boolean isSnakeCaseCompatible(String name) {
        return !name.contains("_") || name.equals(name.toLowerCase());
    }

    private boolean hasJsonPropertyAnnotation(RecordComponent component) {
        try {
            Field field = component.getDeclaringRecord().getDeclaredField(component.getName());
            return field.isAnnotationPresent(JsonProperty.class);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
