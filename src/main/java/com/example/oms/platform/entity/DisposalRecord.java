package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record DisposalRecord(
        long id,
        String workflowId,
        String stepType,
        String actor,
        String decision,
        String note,
        String draftActionJson,
        String beforeSnapshotJson,
        String afterSnapshotJson,
        LocalDateTime createdAt) {
}
