package com.example.oms.platform.service;

import com.example.oms.platform.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ModelRuntimeClient {
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    @Autowired
    public ModelRuntimeClient(
            @Value("${oms.python-runtime.base-url:http://localhost:18010}") String baseUrl,
            @Value("${oms.python-runtime.timeout:30000}") int timeoutMillis,
            ObjectMapper objectMapper) {
        this(baseUrl, timeoutMillis, objectMapper, null);
    }

    ModelRuntimeClient(String baseUrl, int timeoutMillis, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.baseUrl = trimTrailingSlash(baseUrl == null || baseUrl.isBlank() ? "http://localhost:18010" : baseUrl);
        this.objectMapper = objectMapper;
        int effectiveTimeout = Math.max(1, timeoutMillis);
        this.httpClient = httpClient == null
                ? new OkHttpClient.Builder()
                        .connectTimeout(Duration.ofMillis(effectiveTimeout))
                        .readTimeout(Duration.ofMillis(effectiveTimeout))
                        .writeTimeout(Duration.ofMillis(effectiveTimeout))
                        .build()
                : httpClient;
    }

    public Object get(String path, String authorization) {
        return request(new Request.Builder().url(url(path)).get(), authorization);
    }

    public Object delete(String path, String authorization) {
        return request(new Request.Builder().url(url(path)).delete(), authorization);
    }

    public Object post(String path, String jsonBody, String authorization) {
        String normalizedBody = jsonBody == null || jsonBody.isBlank() ? "{}" : jsonBody;
        validateJson(normalizedBody);
        return request(new Request.Builder()
                .url(url(path))
                .post(RequestBody.create(normalizedBody, JSON)), authorization);
    }

    private Object request(Request.Builder builder, String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            builder.header("Authorization", authorization);
        }
        Request request = builder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            Object parsed = parseResponseBody(response);
            if (!response.isSuccessful()) {
                throw toBusinessException(response.code(), parsed);
            }
            return parsed;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(502, "RUNTIME_UNAVAILABLE", exception.getMessage(),
                    Map.of("url", request.url().toString()));
        }
    }

    private Object parseResponseBody(Response response) throws IOException {
        ResponseBody body = response.body();
        String text = body == null ? "" : body.string();
        if (text.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(text, Object.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(502, "RUNTIME_INVALID_JSON", "Model runtime returned invalid JSON",
                    Map.of("status", response.code()));
        }
    }

    private void validateJson(String jsonBody) {
        try {
            objectMapper.readTree(jsonBody);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "BAD_REQUEST", "Invalid JSON request body");
        }
    }

    @SuppressWarnings("unchecked")
    private BusinessException toBusinessException(int status, Object body) {
        if (body instanceof Map<?, ?> map) {
            Object error = map.get("error");
            if (error instanceof Map<?, ?> errorMap
                    && errorMap.get("code") instanceof String code
                    && errorMap.get("message") instanceof String message) {
                Object details = errorMap.get("details");
                return new BusinessException(status, code, message,
                        details instanceof Map<?, ?> detailsMap ? (Map<String, Object>) detailsMap : Map.of());
            }
            if (error instanceof String message) {
                return new BusinessException(status, "UPSTREAM_ERROR", message);
            }
        }
        return new BusinessException(status, "UPSTREAM_ERROR", "Model runtime returned HTTP " + status);
    }

    private String url(String path) {
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
