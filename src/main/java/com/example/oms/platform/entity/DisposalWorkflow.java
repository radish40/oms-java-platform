package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record DisposalWorkflow(
        String workflowId,
        String orderId,
        String diagnosisRunId,
        String status,
        String assignee,
        String priority,
        String summary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt) {
}
