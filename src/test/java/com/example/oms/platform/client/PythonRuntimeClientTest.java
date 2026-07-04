package com.example.oms.platform.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.oms.platform.config.RuntimeProperties;
import com.example.oms.platform.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PythonRuntimeClientTest {
    private MockWebServer server;
    private PythonRuntimeClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new PythonRuntimeClient(
                new RuntimeProperties(server.url("/runtime/").toString(), 1000),
                new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void getForwardsAuthorizationAndParsesJson() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"ok\",\"checks\":[]}"));

        Object response = client.get("/ops/debug", "Bearer dev-token");

        assertThat(response).isInstanceOf(Map.class);
        assertThat(asMap(response)).containsEntry("status", "ok");
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/runtime/ops/debug");
        assertThat(request.getHeader("authorization")).isEqualTo("Bearer dev-token");
    }

    @Test
    void postSerializesJsonBody() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"config\":{\"id\":\"chat\"}}"));

        client.post("/admin/model-configs", Map.of("id", "chat", "model", "model-a"), "Bearer dev-token");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/runtime/admin/model-configs");
        assertThat(request.getBody().readUtf8()).contains("\"id\":\"chat\"", "\"model\":\"model-a\"");
        assertThat(request.getHeader("content-type")).contains("application/json");
    }

    @Test
    void preservesStructuredRuntimeError() {
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setBody("""
                        {"error":{"code":"MODEL_CONFIG_INVALID","message":"bad config","details":{"field":"model"}}}
                        """));

        assertThatThrownBy(() -> client.get("/admin/model-configs", "Bearer dev-token"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(409);
                    assertThat(exception.getCode()).isEqualTo("MODEL_CONFIG_INVALID");
                    assertThat(exception.getMessage()).isEqualTo("bad config");
                    assertThat(exception.getDetails()).containsEntry("field", "model");
                });
    }

    @Test
    void invalidRuntimeJsonBecomesBadGateway() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("not-json"));

        assertThatThrownBy(() -> client.get("/health", null))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(502);
                    assertThat(exception.getCode()).isEqualTo("RUNTIME_INVALID_JSON");
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }
}
