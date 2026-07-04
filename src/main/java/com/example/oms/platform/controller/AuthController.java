package com.example.oms.platform.controller;

import com.example.oms.platform.dto.request.LoginRequest;
import com.example.oms.platform.dto.response.LoginResponse;
import com.example.oms.platform.dto.response.UserResponse;
import com.example.oms.platform.service.AuthService;
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
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/me")
    public UserEnvelope me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return new UserEnvelope(authService.currentUser(authorization).toResponse());
    }

    public record UserEnvelope(UserResponse user) {
    }
}
