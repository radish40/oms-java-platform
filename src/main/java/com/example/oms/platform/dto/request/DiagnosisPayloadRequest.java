package com.example.oms.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;

public record DiagnosisPayloadRequest(
        @Schema(description = "诊断请求载荷") Map<String, Object> body) {
    public static DiagnosisPayloadRequest from(Map<String, Object> body) {
        return new DiagnosisPayloadRequest(body == null ? Map.of() : new LinkedHashMap<>(body));
    }

    public Object field(String name) {
        return body.get(name);
    }
}
