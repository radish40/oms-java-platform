package com.example.oms.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.oms.platform.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

class ModelRuntimeClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getParsesJsonAndForwardsAuthorization() throws Exception {
        try (TestRuntime runtime = TestRuntime.start(200, "{\"configs\":[{\"id\":\"chat\"}]}")) {
            ModelRuntimeClient client = client(runtime.baseUrl());

            Object response = client.get("/admin/model-configs", "Bearer token");

            assertThat(response).isInstanceOf(Map.class);
            assertThat(runtime.recorded().get()).contains("GET /admin/model-configs Bearer token");
        }
    }

    @Test
    void postRejectsInvalidJsonBeforeCallingRuntime() throws Exception {
        try (TestRuntime runtime = TestRuntime.start(200, "{}")) {
            ModelRuntimeClient client = client(runtime.baseUrl());

            assertThatThrownBy(() -> client.post("/admin/model-configs", "{", "Bearer token"))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        assertThat(exception.getStatus()).isEqualTo(400);
                        assertThat(exception.getCode()).isEqualTo("BAD_REQUEST");
                    });

            assertThat(runtime.recorded().get()).isNull();
        }
    }

    @Test
    void structuredRuntimeErrorPreservesCodeMessageAndDetails() throws Exception {
        try (TestRuntime runtime = TestRuntime.start(404,
                "{\"error\":{\"code\":\"MODEL_CONFIG_NOT_FOUND\",\"message\":\"Missing config\",\"details\":{\"id\":\"chat\"}}}")) {
            ModelRuntimeClient client = client(runtime.baseUrl());

            assertThatThrownBy(() -> client.get("/admin/model-configs/chat", "Bearer token"))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        assertThat(exception.getStatus()).isEqualTo(404);
                        assertThat(exception.getCode()).isEqualTo("MODEL_CONFIG_NOT_FOUND");
                        assertThat(exception.getMessage()).isEqualTo("Missing config");
                        assertThat(exception.getDetails()).containsEntry("id", "chat");
                    });
        }
    }

    @Test
    void legacyRuntimeErrorUsesUpstreamErrorCode() throws Exception {
        try (TestRuntime runtime = TestRuntime.start(502, "{\"error\":\"runtime down\"}")) {
            ModelRuntimeClient client = client(runtime.baseUrl());

            assertThatThrownBy(() -> client.get("/admin/model-configs", "Bearer token"))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        assertThat(exception.getStatus()).isEqualTo(502);
                        assertThat(exception.getCode()).isEqualTo("UPSTREAM_ERROR");
                        assertThat(exception.getMessage()).isEqualTo("runtime down");
                    });
        }
    }

    @Test
    void invalidRuntimeJsonBecomesBadGateway() throws Exception {
        try (TestRuntime runtime = TestRuntime.start(200, "not json")) {
            ModelRuntimeClient client = client(runtime.baseUrl());

            assertThatThrownBy(() -> client.get("/admin/model-configs", "Bearer token"))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        assertThat(exception.getStatus()).isEqualTo(502);
                        assertThat(exception.getCode()).isEqualTo("RUNTIME_INVALID_JSON");
                        assertThat(exception.getDetails()).containsEntry("status", 200);
                    });
        }
    }

    @Test
    void unavailableRuntimeBecomesBadGateway() throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        ModelRuntimeClient client = client("http://127.0.0.1:" + unusedPort);

        assertThatThrownBy(() -> client.get("/admin/model-configs", "Bearer token"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(502);
                    assertThat(exception.getCode()).isEqualTo("RUNTIME_UNAVAILABLE");
                    assertThat(exception.getDetails().get("url").toString()).contains("/admin/model-configs");
                });
    }

    private ModelRuntimeClient client(String baseUrl) {
        return new ModelRuntimeClient(baseUrl, 500, objectMapper, new OkHttpClient());
    }

    private static final class TestRuntime implements AutoCloseable {
        private final HttpServer server;
        private final AtomicReference<String> recorded;

        private TestRuntime(HttpServer server, AtomicReference<String> recorded) {
            this.server = server;
            this.recorded = recorded;
        }

        static TestRuntime start(int status, String body) throws IOException {
            AtomicReference<String> recorded = new AtomicReference<>();
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> handle(exchange, status, body, recorded));
            server.start();
            return new TestRuntime(server, recorded);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        AtomicReference<String> recorded() {
            return recorded;
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private static void handle(
                HttpExchange exchange,
                int status,
                String body,
                AtomicReference<String> recorded) throws IOException {
            exchange.getRequestBody().readAllBytes();
            recorded.set(exchange.getRequestMethod()
                    + " " + exchange.getRequestURI().getRawPath()
                    + " " + exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }
}
