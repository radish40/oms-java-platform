package com.example.oms.platform.controller;

import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.AuthService;
import com.example.oms.platform.service.RbacService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "权限与用户管理")
public class AdminRbacController {
    private final AuthService authService;
    private final RbacService rbacService;

    public AdminRbacController(AuthService authService, RbacService rbacService) {
        this.authService = authService;
        this.rbacService = rbacService;
    }

    @GetMapping("/rbac")
    @RequiresPermission("admin:rbac")
    @Operation(summary = "RBAC概览", description = "获取RBAC权限管理概览信息，包含审计日志")
    public Map<String, Object> rbac(@Parameter(description = "审计日志数量限制，默认50") @RequestParam(value = "audit_limit", defaultValue = "50") int auditLimit) {
        return rbacService.overview(auditLimit);
    }

    @GetMapping("/roles")
    @RequiresPermission("admin:rbac")
    @Operation(summary = "角色列表", description = "查询所有角色列表")
    public Map<String, Object> listRoles() {
        return Map.of("roles", rbacService.listRoles());
    }

    @PostMapping("/roles")
    @RequiresPermission("admin:rbac")
    @Operation(summary = "保存角色", description = "创建或更新角色")
    public Map<String, Object> saveRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SaveRoleRequest request) {
        AuthUser actor = authService.currentUser(authorization);
        return Map.of("role", rbacService.saveRole(request.code(), request.label(), request.permissions(), actor.username()));
    }

    @DeleteMapping("/roles/{code}")
    @RequiresPermission("admin:rbac")
    @Operation(summary = "删除角色", description = "根据角色编码删除角色")
    public Map<String, Object> deleteRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "角色编码") @PathVariable String code) {
        AuthUser actor = authService.currentUser(authorization);
        rbacService.deleteRole(code, actor.username());
        return Map.of("deleted", code);
    }

    @GetMapping("/permissions")
    @RequiresPermission("admin:rbac")
    @Operation(summary = "权限列表", description = "查询所有权限列表")
    public Map<String, Object> listPermissions() {
        return Map.of("permissions", rbacService.listPermissions());
    }

    @PostMapping("/permissions")
    @RequiresPermission("admin:rbac")
    @Operation(summary = "保存权限", description = "创建或更新菜单权限、功能权限")
    public Map<String, Object> savePermission(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SavePermissionRequest request) {
        AuthUser actor = authService.currentUser(authorization);
        return Map.of("permission", rbacService.savePermission(
                request.code(),
                request.label(),
                request.description(),
                actor.username()));
    }

    @GetMapping("/menus")
    @Operation(summary = "菜单树", description = "获取当前用户的菜单树结构")
    public Map<String, Object> menus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUser user = authService.currentUser(authorization);
        return Map.of("menus", rbacService.buildMenuTree(user.permissions()));
    }

    @PostMapping("/users")
    @RequiresPermission("admin:rbac")
    @Operation(summary = "创建用户", description = "创建新用户")
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
    @Operation(summary = "更新用户", description = "更新指定用户的信息")
    public Map<String, Object> updateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "用户名") @PathVariable String username,
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
    @Operation(summary = "删除用户", description = "删除指定用户")
    public Map<String, Object> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "用户名") @PathVariable String username) {
        AuthUser actor = authService.currentUser(authorization);
        rbacService.deleteUser(username, actor.username());
        return Map.of("deleted", username);
    }

    public record SaveUserRequest(
            @Schema(description = "用户名") String username,
            @Schema(description = "显示名称") @JsonProperty("display_name") String displayName,
            @Schema(description = "状态") String status,
            @Schema(description = "密码") String password,
            @Schema(description = "角色列表") List<String> roles) {
    }

    public record UpdateUserRequest(
            @Schema(description = "显示名称") @JsonProperty("display_name") String displayName,
            @Schema(description = "状态") String status,
            @Schema(description = "密码") String password,
            @Schema(description = "角色列表") List<String> roles) {
    }

    public record SaveRoleRequest(
            @Schema(description = "角色编码") String code,
            @Schema(description = "角色标签") String label,
            @Schema(description = "权限列表") List<String> permissions) {
    }

    public record SavePermissionRequest(
            @Schema(description = "权限编码") String code,
            @Schema(description = "权限名称") String label,
            @Schema(description = "权限说明") String description) {
    }
}
