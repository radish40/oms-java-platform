package com.example.oms.platform.entity;

public record SessionMessageEntity(
        long id,
        String sessionId,
        int seq,
        String role,
        String content,
        String reasoningContent,
        String toolCallId,
        String displayName,
        String name,
        String params,
        String description,
        String summary,
        Integer elapsedMs,
        String interpJson,
        String toolCallsJson) {
}
