package com.example.oms.platform.client;

import com.example.oms.platform.config.RuntimeProperties;
import com.example.oms.platform.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

@Component
public class PythonRuntimeClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final TypeReference<Object> JSON_VALUE = new TypeReference<>() {
    };

    private final RuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public PythonRuntimeClient(RuntimeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.timeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.timeoutMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.timeoutMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public Object get(String path, String authorization) {
        return request("GET", path, authorization, null);
    }

    public Object post(String path, Object body, String authorization) {
        return request("POST", path, authorization, body == null ? Map.of() : body);
    }

    public Object delete(String path, String authorization) {
        return request("DELETE", path, authorization, null);
    }

    public RuntimePing ping() {
        try {
            get("/health", null);
            return RuntimePing.up();
        } catch (RuntimeException exception) {
            return RuntimePing.down(exception.getMessage());
        }
    }

    private Object request(String method, String path, String authorization, Object body) {
        HttpUrl url = runtimeUrl(path);
        Request.Builder builder = new Request.Builder().url(url);
        if (authorization != null && !authorization.isBlank()) {
            builder.header("authorization", authorization);
        }
        if (body == null) {
            builder.method(method, null);
        } else {
            builder.method(method, RequestBody.create(writeJson(body), JSON));
        }

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            Object parsed = parseJson(response);
            if (!response.isSuccessful()) {
                throw toBusinessException(response.code(), parsed);
            }
            return parsed;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(502, "RUNTIME_UNAVAILABLE", exception.getMessage(), Map.of("url", url.toString()));
        }
    }

    private HttpUrl runtimeUrl(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        HttpUrl url = HttpUrl.parse(properties.normalizedBaseUrl() + normalizedPath);
        if (url == null) {
            throw new BusinessException(500, "RUNTIME_CONFIGURATION_ERROR", "Invalid model runtime URL");
        }
        return url;
    }

    private String writeJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "BAD_REQUEST", "Request body is not JSON serializable");
        }
    }

    private Object parseJson(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        String text = responseBody == null ? "" : responseBody.string();
        if (text.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(text, JSON_VALUE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(502, "RUNTIME_INVALID_JSON", "Model runtime returned invalid JSON",
                    Map.of("status", response.code()));
        }
    }

    @SuppressWarnings("unchecked")
    private BusinessException toBusinessException(int status, Object body) {
        if (body instanceof Map<?, ?> map) {
            Object error = map.get("error");
            if (error instanceof Map<?, ?> errorMap && errorMap.get("code") instanceof String code
                    && errorMap.get("message") instanceof String message) {
                Object details = errorMap.get("details");
                return new BusinessException(status, code, message, details instanceof Map<?, ?> detailsMap
                        ? new LinkedHashMap<>((Map<String, Object>) detailsMap)
                        : Map.of());
            }
            if (error instanceof String legacyError) {
                return new BusinessException(status, "UPSTREAM_ERROR", legacyError);
            }
        }
        return new BusinessException(status, "UPSTREAM_ERROR", "Model runtime returned HTTP " + status);
    }
}
