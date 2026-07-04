package com.example.oms.platform.repository;

import com.example.oms.platform.dto.request.DiagnosisPayloadRequest;
import com.example.oms.platform.dto.response.DiagnosisRuntimeResponse;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.service.DiagnosisRuntimeProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Repository;

@Repository
public class DiagnosisRuntimeRepository {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;
    private final OkHttpClient client;
    private final String baseUrl;

    public DiagnosisRuntimeRepository(ObjectMapper objectMapper, DiagnosisRuntimeProperties properties) {
        this.objectMapper = objectMapper;
        this.baseUrl = stripTrailingSlash(properties.getBaseUrl());
        Duration timeout = Duration.ofMillis(Math.max(1, properties.getTimeout()));
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .build();
    }

    public DiagnosisRuntimeResponse getJson(String path, Map<String, String> query, String authorization) {
        Request request = requestBuilder(path, query, authorization).get().build();
        return new DiagnosisRuntimeResponse(executeJson(request));
    }

    public DiagnosisRuntimeResponse postJson(String path, DiagnosisPayloadRequest payload, String authorization) {
        try {
            RequestBody body = RequestBody.create(objectMapper.writeValueAsBytes(payload.body()), JSON);
            Request request = requestBuilder(path, Map.of(), authorization).post(body).build();
            return new DiagnosisRuntimeResponse(executeJson(request));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "BAD_REQUEST", "Request body is not valid JSON");
        }
    }

    public String getText(String path, Map<String, String> query, String authorization) {
        Request request = requestBuilder(path, query, authorization).get().build();
        try (Response response = client.newCall(request).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw toHttpError(response.code(), parseMaybeJson(text));
            }
            return text;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw runtimeUnavailable(exception, request.url().toString());
        }
    }

    private JsonNode executeJson(Request request) {
        try (Response response = client.newCall(request).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            JsonNode body = parseJson(text, response.code());
            if (!response.isSuccessful()) {
                throw toHttpError(response.code(), body);
            }
            return body;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw runtimeUnavailable(exception, request.url().toString());
        }
    }

    private Request.Builder requestBuilder(String path, Map<String, String> query, String authorization) {
        HttpUrl.Builder urlBuilder = url(path).newBuilder();
        query.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                urlBuilder.addQueryParameter(key, value);
            }
        });
        Request.Builder builder = new Request.Builder().url(urlBuilder.build());
        if (authorization != null && !authorization.isBlank()) {
            builder.header("Authorization", authorization);
        }
        return builder;
    }

    private HttpUrl url(String path) {
        HttpUrl url = HttpUrl.parse(baseUrl + (path.startsWith("/") ? path : "/" + path));
        if (url == null) {
            throw new BusinessException(500, "RUNTIME_URL_INVALID", "Model runtime URL is invalid",
                    Map.of("url", baseUrl));
        }
        return url;
    }

    private JsonNode parseJson(String text, int status) {
        if (text == null || text.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(text);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(502, "RUNTIME_INVALID_JSON", "Model runtime returned invalid JSON",
                    Map.of("status", status));
        }
    }

    private JsonNode parseMaybeJson(String text) {
        if (text == null || text.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(text);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode().put("error", text);
        }
    }

    private BusinessException toHttpError(int status, JsonNode body) {
        JsonNode error = body.path("error");
        if (error.isObject() && error.path("code").isTextual() && error.path("message").isTextual()) {
            return new BusinessException(status, error.path("code").asText(), error.path("message").asText(),
                    objectMapper.convertValue(error.path("details"), new TypeReference<Map<String, Object>>() {
                    }));
        }
        if (error.isTextual()) {
            return new BusinessException(status, "UPSTREAM_ERROR", error.asText());
        }
        return new BusinessException(status, "UPSTREAM_ERROR", "Model runtime returned HTTP " + status);
    }

    private BusinessException runtimeUnavailable(IOException exception, String url) {
        String message = exception.getMessage() == null ? "Runtime unavailable" : exception.getMessage();
        return new BusinessException(502, "RUNTIME_UNAVAILABLE", message, Map.of("url", url));
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:18010";
        }
        return value.replaceFirst("/+$", "");
    }
}
