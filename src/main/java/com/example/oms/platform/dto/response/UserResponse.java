package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UserResponse(
        String username,
        @JsonProperty("display_name") String displayName,
        String role,
        @JsonProperty("role_label") String roleLabel,
        List<String> permissions) {
}
