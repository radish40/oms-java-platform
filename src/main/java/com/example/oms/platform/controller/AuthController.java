package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.LoginRequest;
import com.example.oms.platform.dto.response.LoginResponse;
import com.example.oms.platform.dto.response.UserResponse;
import com.example.oms.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回认证令牌和用户信息")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的详细信息")
    public UserEnvelope me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return new UserEnvelope(authService.currentUser(authorization).toResponse());
    }

    public record UserEnvelope(@Schema(description = "用户信息") UserResponse user) {
    }
}
