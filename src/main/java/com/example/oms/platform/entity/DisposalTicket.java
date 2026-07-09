package com.example.oms.platform.entity;

import java.time.LocalDateTime;

public record DisposalTicket(
        long id,
        String workflowId,
        String ticketId,
        String ticketSource,
        String status,
        String title,
        String createdBy,
        LocalDateTime createdAt) {
}
