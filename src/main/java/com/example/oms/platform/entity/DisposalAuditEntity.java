package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record DisposalAuditEntity(
        long id,
        String workflowId,
        String eventType,
        String actor,
        String permission,
        String detailJson,
        LocalDateTime createdAt) {
}
