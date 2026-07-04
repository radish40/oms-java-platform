package com.example.oms.platform.service;

import com.example.oms.platform.dto.response.ChatMessageResponse;
import com.example.oms.platform.dto.response.DeleteSessionResponse;
import com.example.oms.platform.dto.response.SessionDetailResponse;
import com.example.oms.platform.dto.response.SessionListResponse;
import com.example.oms.platform.dto.response.SessionSummaryResponse;
import com.example.oms.platform.entity.SessionEntity;
import com.example.oms.platform.entity.SessionMessageEntity;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.repository.SessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAP_TYPE = new TypeReference<>() {
    };

    private final SessionRepository repository;
    private final ObjectMapper objectMapper;

    public SessionService(SessionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public SessionListResponse listSessions() {
        return listSessions(50, 0, "");
    }

    public SessionListResponse listSessions(int limit, int offset, String search) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 200));
        int boundedOffset = Math.max(0, offset);
        List<SessionSummaryResponse> sessions = repository.findAll(boundedLimit, boundedOffset, search).stream()
                .map(this::toSummary)
                .toList();
        int total = repository.countSessions(search);
        return new SessionListResponse(sessions, total);
    }

    public SessionDetailResponse getSession(String sessionId) {
        requireSessionId(sessionId);
        if (!repository.exists(sessionId)) {
            throw new BusinessException(404, "NOT_FOUND", "Session not found", Map.of("session_id", sessionId));
        }
        List<ChatMessageResponse> messages = repository.findMessages(sessionId).stream()
                .map(this::toMessage)
                .toList();
        return new SessionDetailResponse(sessionId, messages);
    }

    public DeleteSessionResponse deleteSession(String sessionId) {
        requireSessionId(sessionId);
        if (!repository.delete(sessionId)) {
            throw new BusinessException(404, "NOT_FOUND", "Session not found", Map.of("session_id", sessionId));
        }
        return new DeleteSessionResponse(sessionId);
    }

    private SessionSummaryResponse toSummary(SessionEntity session) {
        return new SessionSummaryResponse(
                session.id(),
                session.preview(),
                session.turns(),
                timestamp(session.createdAt()),
                timestamp(session.updatedAt()));
    }

    private ChatMessageResponse toMessage(SessionMessageEntity message) {
        return new ChatMessageResponse(
                message.role(),
                blankToNull(message.content()),
                blankToNull(message.reasoningContent()),
                blankToNull(message.toolCallId()),
                blankToNull(message.displayName()),
                blankToNull(message.name()),
                blankToNull(message.params()),
                blankToNull(message.description()),
                blankToNull(message.summary()),
                message.elapsedMs(),
                parseMap(message.interpJson()),
                parseListOfMaps(message.toolCallsJson()));
    }

    private void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(400, "BAD_REQUEST", "Session id is required");
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> parseListOfMaps(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LIST_OF_MAP_TYPE);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String timestamp(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
