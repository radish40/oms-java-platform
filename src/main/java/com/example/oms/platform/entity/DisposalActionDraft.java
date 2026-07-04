package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record DisposalActionDraft(
        long id,
        String workflowId,
        String actionType,
        String description,
        String riskLevel,
        boolean requiresApproval,
        int sortOrder,
        LocalDateTime createdAt) {
}
