package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UserResponse(
        @Schema(description = "用户名") String username,
        @Schema(description = "显示名称") @JsonProperty("display_name") String displayName,
        @Schema(description = "角色编码") String role,
        @Schema(description = "角色显示标签") @JsonProperty("role_label") String roleLabel,
        @Schema(description = "权限列表") List<String> permissions) {
}
