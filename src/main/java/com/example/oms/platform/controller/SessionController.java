package com.example.oms.platform.controller;

import com.example.oms.platform.dto.response.DeleteSessionResponse;
import com.example.oms.platform.dto.response.SessionDetailResponse;
import com.example.oms.platform.dto.response.SessionListResponse;
import com.example.oms.platform.service.SessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public SessionListResponse listSessions(
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "search", defaultValue = "") String search) {
        return sessionService.listSessions(limit, offset, search);
    }

    @GetMapping("/{sessionId}")
    public SessionDetailResponse getSession(@PathVariable String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    public DeleteSessionResponse deleteSession(@PathVariable String sessionId) {
        return sessionService.deleteSession(sessionId);
    }
}
