package com.example.oms.platform.security;

import com.example.oms.platform.dto.response.UserResponse;
import java.util.List;

public record AuthUser(
        String username,
        String displayName,
        String role,
        String roleLabel,
        List<String> permissions,
        String passwordHash,
        String status) {
    public UserResponse toResponse() {
        return new UserResponse(username, displayName, role, roleLabel, permissions);
    }
}
