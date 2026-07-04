package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record RollbackPlan(
        long id,
        String workflowId,
        String planName,
        String description,
        String stepsJson,
        String triggersJson,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
