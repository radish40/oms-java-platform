package com.example.oms.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "认证令牌") String token,
        @Schema(description = "用户信息") UserResponse user) {
}
