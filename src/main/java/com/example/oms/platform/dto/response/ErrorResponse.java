package com.example.oms.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "错误响应")
public record ErrorResponse(
        @Schema(description = "错误信息") ErrorBody error) {
    public static ErrorResponse of(String code, String message, Map<String, Object> details) {
        return new ErrorResponse(new ErrorBody(code, message, details == null ? Map.of() : details));
    }

    @Schema(description = "错误体")
    public record ErrorBody(
            @Schema(description = "错误编码") String code,
            @Schema(description = "错误消息") String message,
            @Schema(description = "错误详情") Map<String, Object> details) {
    }
}
