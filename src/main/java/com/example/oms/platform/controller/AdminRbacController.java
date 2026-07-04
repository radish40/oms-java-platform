package com.example.oms.platform.controller;

import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.AuthService;
import com.example.oms.platform.service.RbacService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminRbacController {
    private final AuthService authService;
    private final RbacService rbacService;

    public AdminRbacController(AuthService authService, RbacService rbacService) {
        this.authService = authService;
        this.rbacService = rbacService;
    }

    @GetMapping("/rbac")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> rbac(@RequestParam(value = "audit_limit", defaultValue = "50") int auditLimit) {
        return rbacService.overview(auditLimit);
    }

    @GetMapping("/roles")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> listRoles() {
        return Map.of("roles", rbacService.listRoles());
    }

    @PostMapping("/roles")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> saveRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SaveRoleRequest request) {
        AuthUser actor = authService.currentUser(authorization);
        return Map.of("role", rbacService.saveRole(request.code(), request.label(), request.permissions(), actor.username()));
    }

    @DeleteMapping("/roles/{code}")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> deleteRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String code) {
        AuthUser actor = authService.currentUser(authorization);
        rbacService.deleteRole(code, actor.username());
        return Map.of("deleted", code);
    }

    @GetMapping("/permissions")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> listPermissions() {
        return Map.of("permissions", rbacService.listPermissions());
    }

    @GetMapping("/menus")
    public Map<String, Object> menus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser user = authService.currentUser(authorization);
        return Map.of("menus", rbacService.buildMenuTree(user.permissions()));
    }

    @PostMapping("/users")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> saveUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SaveUserRequest request) {
        AuthUser actor = authService.currentUser(authorization);
        Map<String, Object> user = rbacService.saveUser(
                request.username(),
                request.displayName() == null || request.displayName().isBlank() ? request.username() : request.displayName(),
                request.status() == null || request.status().isBlank() ? "active" : request.status(),
                request.password(),
                request.roles() == null ? List.of() : request.roles(),
                actor.username());
        return Map.of("user", user);
    }

    @PutMapping("/users/{username}")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> updateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username,
            @RequestBody UpdateUserRequest request) {
        AuthUser actor = authService.currentUser(authorization);
        Map<String, Object> user = rbacService.updateUser(
                username,
                request.displayName(),
                request.status(),
                request.password(),
                request.roles(),
                actor.username());
        return Map.of("user", user);
    }

    @DeleteMapping("/users/{username}")
    @RequiresPermission("admin:rbac")
    public Map<String, Object> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String username) {
        AuthUser actor = authService.currentUser(authorization);
        rbacService.deleteUser(username, actor.username());
        return Map.of("deleted", username);
    }

    public record SaveUserRequest(
            String username,
            @JsonProperty("display_name") String displayName,
            String status,
            String password,
            List<String> roles) {
    }

    public record UpdateUserRequest(
            @JsonProperty("display_name") String displayName,
            String status,
            String password,
            List<String> roles) {
    }

    public record SaveRoleRequest(
            String code,
            String label,
            List<String> permissions) {
    }
}
