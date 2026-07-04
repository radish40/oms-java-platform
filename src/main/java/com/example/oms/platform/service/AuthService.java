package com.example.oms.platform.service;

import com.example.oms.platform.dto.response.LoginResponse;
import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.PasswordHasher;
import com.example.oms.platform.security.TokenProvider;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final RbacService rbacService;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;

    public AuthService(RbacService rbacService, PasswordHasher passwordHasher, TokenProvider tokenProvider) {
        this.rbacService = rbacService;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(400, "INVALID_LOGIN", "Username and password are required");
        }
        AuthUser user = rbacService.loginProfile(username)
                .filter(profile -> passwordHasher.verify(password, profile.passwordHash()))
                .orElseThrow(() -> new BusinessException(401, "UNAUTHORIZED", "Invalid username or password"));
        rbacService.recordAuditEvent("auth.login.success", user.username(), "", "auth_user", user.username(), "{}");
        return new LoginResponse(tokenProvider.create(user), user.toResponse());
    }

    public AuthUser currentUser(String authorization) {
        String username = tokenProvider.usernameFromBearer(authorization);
        if (username.isBlank()) {
            throw new BusinessException(401, "UNAUTHORIZED", "Authentication required");
        }
        return rbacService.currentUser(username);
    }

    public AuthUser requirePermission(String authorization, String permission) {
        AuthUser user = currentUser(authorization);
        if (!user.permissions().contains(permission)) {
            throw new BusinessException(403, "FORBIDDEN", "Missing permission: " + permission, Map.of("permission", permission));
        }
        return user;
    }
}
