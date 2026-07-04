package com.example.oms.platform.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.service.DiagnosisRuntimeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DiagnosisRuntimeRepositoryTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String lastMethod;
    private String lastQuery;
    private String lastAuthorization;
    private String lastBody;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getJsonForwardsQueryAndAuthorization() throws Exception {
        startServer(200, "{\"reports\":[]}");
        DiagnosisRuntimeRepository repository = repository();

        var body = repository.getJson("/diagnosis/judge-reports", Map.of("run_id", "run-1"), "Bearer token").body();

        assertThat(body.path("reports").isArray()).isTrue();
        assertThat(lastMethod).isEqualTo("GET");
        assertThat(lastQuery).isEqualTo("run_id=run-1");
        assertThat(lastAuthorization).isEqualTo("Bearer token");
    }

    @Test
    void postJsonForwardsBody() throws Exception {
        startServer(200, "{\"feedback\":{\"id\":2}}");
        DiagnosisRuntimeRepository repository = repository();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("run_id", "run-1");
        payload.put("rating", "wrong");

        var body = repository.postJson("/diagnosis/feedback", DiagnosisPayloadRequest.from(payload), "Bearer token").body();

        assertThat(body.path("feedback").path("id").asInt()).isEqualTo(2);
        assertThat(lastMethod).isEqualTo("POST");
        assertThat(lastBody).contains("\"run_id\":\"run-1\"");
    }

    @Test
    void contractRuntimeErrorsKeepCodeMessageAndDetails() throws Exception {
        startServer(404, "{\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"Missing run\",\"details\":{\"run_id\":\"run-x\"}}}");
        DiagnosisRuntimeRepository repository = repository();

        assertThatThrownBy(() -> repository.getJson("/diagnosis/runs/run-x", Map.of(), ""))
                .isInstanceOf(BusinessException.class)
                .extracting("status", "code", "message")
                .containsExactly(404, "NOT_FOUND", "Missing run");
    }

    @Test
    void invalidJsonBecomesRuntimeInvalidJson() throws Exception {
        startServer(200, "not-json");
        DiagnosisRuntimeRepository repository = repository();

        assertThatThrownBy(() -> repository.getJson("/diagnosis/runs", Map.of(), ""))
                .isInstanceOf(BusinessException.class)
                .extracting("status", "code")
                .containsExactly(502, "RUNTIME_INVALID_JSON");
    }

    @Test
    void unavailableRuntimeBecomesStructuredError() {
        DiagnosisRuntimeProperties properties = new DiagnosisRuntimeProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setTimeout(100);
        DiagnosisRuntimeRepository repository = new DiagnosisRuntimeRepository(objectMapper, properties);

        assertThatThrownBy(() -> repository.getJson("/diagnosis/runs", Map.of(), ""))
                .isInstanceOf(BusinessException.class)
                .extracting("status", "code")
                .containsExactly(502, "RUNTIME_UNAVAILABLE");
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> respond(exchange, status, responseBody));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String responseBody) throws IOException {
        lastMethod = exchange.getRequestMethod();
        lastQuery = exchange.getRequestURI().getRawQuery();
        lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("content-type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private DiagnosisRuntimeRepository repository() {
        DiagnosisRuntimeProperties properties = new DiagnosisRuntimeProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setTimeout(1000);
        return new DiagnosisRuntimeRepository(objectMapper, properties);
    }
}
