package com.example.oms.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.oms.platform.client.PythonRuntimeClient;
import com.example.oms.platform.client.RuntimePing;
import com.example.oms.platform.exception.GlobalExceptionHandler;
import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.PermissionAspect;
import com.example.oms.platform.service.HealthService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RuntimeApiControllerTest {
    private PythonRuntimeClient runtimeClient;
    private JdbcTemplate jdbcTemplate;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        runtimeClient = mock(PythonRuntimeClient.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        OpsController opsController = proxiedOpsController();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new HealthController(new HealthService(runtimeClient, jdbcTemplate)),
                        opsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void healthReturnsCompatibleUpShape() throws Exception {
        when(runtimeClient.ping()).thenReturn(RuntimePing.up());
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("oms-platform"))
                .andExpect(jsonPath("$.runtime.status").value("UP"))
                .andExpect(jsonPath("$.mysql.status").value("UP"));
    }

    @Test
    void healthReturnsDegradedWhenRuntimeIsDown() throws Exception {
        when(runtimeClient.ping()).thenReturn(RuntimePing.down("runtime down"));
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.runtime.status").value("DOWN"))
                .andExpect(jsonPath("$.runtime.error").value("runtime down"))
                .andExpect(jsonPath("$.mysql.status").value("UP"));
    }

    @Test
    void opsDebugRequiresPermissionAndProxiesRuntime() throws Exception {
        setUser("admin", List.of("menu:ops"));
        when(runtimeClient.get(eq("/ops/debug"), any())).thenReturn(Map.of(
                "status", "ok",
                "generated_at", "2026-07-04T00:00:00Z",
                "summary", Map.of("total", 1, "ok", 1, "warn", 0, "fail", 0),
                "checks", List.of()));

        mockMvc.perform(get("/ops/debug").header("Authorization", "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.summary.total").value(1))
                .andExpect(jsonPath("$.checks").isArray());

        verify(runtimeClient).get("/ops/debug", "Bearer dev-token");
    }

    @Test
    void opsDebugRejectsUserWithoutPermissionBeforeProxying() throws Exception {
        setUser("support", List.of("menu:chat"));

        mockMvc.perform(get("/ops/debug").header("Authorization", "Bearer support-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.permission").value("menu:ops"));

        verify(runtimeClient, never()).get(eq("/ops/debug"), any());
    }

    @Test
    void opsDebugRejectsAnonymousRequestBeforeProxying() throws Exception {
        mockMvc.perform(get("/ops/debug"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verify(runtimeClient, never()).get(eq("/ops/debug"), any());
    }

    private OpsController proxiedOpsController() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new OpsController(runtimeClient));
        factory.addAspect(new PermissionAspect());
        return factory.getProxy();
    }

    private void setUser(String username, List<String> permissions) {
        AuthUser user = new AuthUser(username, username, "admin", "Administrator", permissions, "", "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
