package com.example.oms.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = com.example.oms.platform.PlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminModelConfigControllerTest {
    private static final Queue<StubResponse> STUBS = new ConcurrentLinkedQueue<>();
    private static final Queue<RecordedRequest> RECORDED = new ConcurrentLinkedQueue<>();
    private static HttpServer runtime;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void runtimeProperties(DynamicPropertyRegistry registry) {
        ensureRuntime();
        registry.add("oms.python-runtime.base-url", () -> "http://127.0.0.1:" + runtime.getAddress().getPort());
        registry.add("oms.python-runtime.timeout", () -> "2000");
    }

    @BeforeAll
    static void startRuntime() {
        ensureRuntime();
    }

    @AfterEach
    void resetRuntime() {
        STUBS.clear();
        RECORDED.clear();
    }

    @Test
    void modelConfigsRequiresModelAdminPermission() throws Exception {
        String supportToken = loginToken("support", "support-pass");

        mockMvc.perform(get("/admin/model-configs").header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.details.permission").value("admin:models"));

        assertThat(RECORDED).isEmpty();
    }

    @Test
    void modelConfigsProxyListFromRuntime() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");
        enqueue(200, "{\"configs\":[{\"id\":\"chat\",\"name\":\"Chat\",\"purpose\":\"chat\",\"model\":\"model-a\"}]}");

        mockMvc.perform(get("/admin/model-configs").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configs[0].id").value("chat"))
                .andExpect(jsonPath("$.configs[0].purpose").value("chat"));

        RecordedRequest request = RECORDED.poll();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.path()).isEqualTo("/admin/model-configs");
        assertThat(request.authorization()).isEqualTo("Bearer " + adminToken);
    }

    @Test
    void saveModelConfigForwardsUnknownSnakeCasePayload() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");
        enqueue(200, "{\"config\":{\"id\":\"chat\",\"provider\":\"longcat\",\"model\":\"model-a\"}}");

        mockMvc.perform(post("/admin/model-configs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "id": "chat",
                                  "purpose": "chat",
                                  "model_config_id": "cfg-chat",
                                  "provider": "longcat",
                                  "model": "model-a"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.model").value("model-a"));

        RecordedRequest request = RECORDED.poll();
        JsonNode body = objectMapper.readTree(request.body());
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/admin/model-configs");
        assertThat(body.path("model_config_id").asText()).isEqualTo("cfg-chat");
        assertThat(body.path("provider").asText()).isEqualTo("longcat");
    }

    @Test
    void deleteModelConfigForwardsEncodedId() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");
        enqueue(200, "{\"deleted\":true}");

        mockMvc.perform(delete(URI.create("/admin/model-configs/chat%20config"))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        RecordedRequest request = RECORDED.poll();
        assertThat(request.method()).isEqualTo("DELETE");
        assertThat(request.path()).isEqualTo("/admin/model-configs/chat%20config");
    }

    @Test
    void testModelConfigAndRefreshCacheUseTsCompatiblePaths() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");
        enqueue(200, "{\"result\":{\"ok\":true,\"message\":\"connection ok\",\"model\":\"model-a\"}}");
        enqueue(200, "{\"refreshed\":true}");

        mockMvc.perform(post("/admin/model-configs/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"model_config_id\":\"chat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.ok").value(true));

        mockMvc.perform(post("/admin/model-configs/refresh-cache")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshed").value(true));

        RecordedRequest testRequest = RECORDED.poll();
        RecordedRequest refreshRequest = RECORDED.poll();
        assertThat(testRequest.path()).isEqualTo("/admin/model-configs/test");
        assertThat(objectMapper.readTree(testRequest.body()).path("model_config_id").asText()).isEqualTo("chat");
        assertThat(refreshRequest.path()).isEqualTo("/admin/model-configs/refresh-cache");
        assertThat(objectMapper.readTree(refreshRequest.body()).isObject()).isTrue();
    }

    @Test
    void modelBindingsProxyGetAndPost() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");
        enqueue(200, "{\"bindings\":[{\"purpose\":\"chat\",\"model_config_id\":\"chat\"}]}");
        enqueue(200, "{\"binding\":{\"purpose\":\"chat\",\"model_config_id\":\"chat\"}}");

        mockMvc.perform(get("/admin/model-bindings").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bindings[0].model_config_id").value("chat"));

        mockMvc.perform(post("/admin/model-bindings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"purpose\":\"chat\",\"model_config_id\":\"chat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binding.purpose").value("chat"));

        assertThat(RECORDED.poll().path()).isEqualTo("/admin/model-bindings");
        RecordedRequest saveRequest = RECORDED.poll();
        assertThat(saveRequest.path()).isEqualTo("/admin/model-bindings");
        assertThat(objectMapper.readTree(saveRequest.body()).path("model_config_id").asText()).isEqualTo("chat");
    }

    @Test
    void chatOptionsRequiresLoginButNotModelAdminPermission() throws Exception {
        String supportToken = loginToken("support", "support-pass");
        enqueue(200, "{\"options\":[{\"provider\":\"longcat\",\"model\":\"model-a\"}]}");

        mockMvc.perform(get("/model-options/chat").header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].provider").value("longcat"));

        RecordedRequest request = RECORDED.poll();
        assertThat(request.path()).isEqualTo("/model-options/chat");
        assertThat(request.authorization()).isEqualTo("Bearer " + supportToken);
    }

    @Test
    void chatOptionsRejectsAnonymousUserWithoutCallingRuntime() throws Exception {
        mockMvc.perform(get("/model-options/chat"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        assertThat(RECORDED).isEmpty();
    }

    @Test
    void invalidJsonRequestReturnsStructuredBadRequest() throws Exception {
        String adminToken = loginToken("admin", "admin-pass");

        mockMvc.perform(post("/admin/model-configs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        assertThat(RECORDED).isEmpty();
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

    private static void enqueue(int status, String body) {
        STUBS.add(new StubResponse(status, body));
    }

    private static void ensureRuntime() {
        if (runtime != null) {
            return;
        }
        try {
            runtime = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            runtime.createContext("/", AdminModelConfigControllerTest::handleRuntimeRequest);
            runtime.start();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void handleRuntimeRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        RECORDED.add(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getRawPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                body));
        StubResponse response = STUBS.poll();
        if (response == null) {
            response = new StubResponse(500, "{\"error\":\"missing stub\"}");
        }
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record StubResponse(int status, String body) {
    }

    private record RecordedRequest(String method, String path, String authorization, String body) {
    }
}
