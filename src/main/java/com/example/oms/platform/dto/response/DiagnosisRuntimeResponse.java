package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "诊断运行时返回")
public record DiagnosisRuntimeResponse(
        @Schema(description = "诊断运行时返回体") JsonNode body) {
}
