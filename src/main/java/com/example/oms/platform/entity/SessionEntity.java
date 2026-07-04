package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record SessionEntity(
        String id,
        String preview,
        int turns,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
