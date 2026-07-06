package com.example.oms.platform.controller;

import com.example.oms.platform.dto.response.DeleteSessionResponse;
import com.example.oms.platform.dto.response.SessionDetailResponse;
import com.example.oms.platform.dto.response.SessionListResponse;
import com.example.oms.platform.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
@Tag(name = "会话管理")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    @Operation(summary = "会话列表", description = "分页查询会话列表，支持搜索和分页参数")
    public SessionListResponse listSessions(
            @Parameter(description = "每页数量，默认50") @RequestParam(value = "limit", defaultValue = "50") int limit,
            @Parameter(description = "偏移量，默认0") @RequestParam(value = "offset", defaultValue = "0") int offset,
            @Parameter(description = "搜索关键词，默认空") @RequestParam(value = "search", defaultValue = "") String search) {
        return sessionService.listSessions(limit, offset, search);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "会话详情", description = "获取指定会话的详细信息，包含聊天消息")
    public SessionDetailResponse getSession(@Parameter(description = "会话ID") @PathVariable String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定的会话记录")
    public DeleteSessionResponse deleteSession(@Parameter(description = "会话ID") @PathVariable String sessionId) {
        return sessionService.deleteSession(sessionId);
    }
}
