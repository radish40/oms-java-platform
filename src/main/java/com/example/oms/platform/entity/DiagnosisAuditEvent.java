package com.example.oms.platform.entity;

import java.util.Map;

public record DiagnosisAuditEvent(
        String eventType,
        String actor,
        String permission,
        String resourceType,
        String resourceId,
        Map<String, Object> payload) {
}
