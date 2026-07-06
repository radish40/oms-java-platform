package com.example.oms.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(description = "用户名") String username,
        @Schema(description = "密码") String password) {
}
