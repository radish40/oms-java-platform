package com.example.oms.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeleteSessionResponse(
        @Schema(description = "已删除的会话ID") String deleted) {
}
