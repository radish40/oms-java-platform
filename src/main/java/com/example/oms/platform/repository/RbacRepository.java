package com.example.oms.platform.repository;

import com.example.oms.platform.security.AuthUser;
import com.example.oms.platform.security.PasswordHasher;
import com.example.oms.platform.security.SecurityProperties;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RbacRepository {
    public static final Map<String, RoleDefinition> ROLES = Map.of(
            "admin", new RoleDefinition("Administrator", List.of(
                    "menu:chat",
                    "menu:knowledge",
                    "menu:batch",
                    "menu:ops",
                    "menu:evaluation",
                    "menu:admin",
                    "menu:models",
                    "eval:review",
                    "admin:rbac",
                    "admin:models",
                    "disposal:create",
                    "disposal:review",
                    "disposal:update",
                    "disposal:draft",
                    "disposal:rollback",
                    "disposal:approve")),
            "support", new RoleDefinition("Support", List.of("menu:chat", "menu:knowledge", "menu:batch")));

    public static final Map<String, PermissionDefinition> PERMISSIONS = orderedPermissions();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordHasher passwordHasher;
    private final SecurityProperties properties;

    public RbacRepository(JdbcTemplate jdbcTemplate, PasswordHasher passwordHasher, SecurityProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordHasher = passwordHasher;
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        createTables();
        seedPermissions();
        seedUsers();
    }

    public Optional<AuthUser> loadProfile(String username, boolean includePassword) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT username, password_hash, display_name, status FROM oms_ai_users WHERE username = ? LIMIT 1",
                username);
        if (users.isEmpty() || !"active".equals(String.valueOf(users.get(0).get("status")))) {
            return Optional.empty();
        }
        Map<String, Object> row = users.get(0);
        List<RoleRow> roles = jdbcTemplate.query("""
                SELECT r.code, r.label
                FROM oms_ai_user_roles ur
                JOIN oms_ai_roles r ON r.code = ur.role_code
                WHERE ur.username = ?
                ORDER BY r.code
                """, (rs, index) -> new RoleRow(rs.getString("code"), rs.getString("label")), username);
        List<String> permissions = jdbcTemplate.queryForList("""
                SELECT DISTINCT p.code
                FROM oms_ai_user_roles ur
                JOIN oms_ai_role_permissions rp ON rp.role_code = ur.role_code
                JOIN oms_ai_permissions p ON p.code = rp.permission_code
                WHERE ur.username = ?
                ORDER BY p.code
                """, String.class, username);
        RoleRow primaryRole = roles.isEmpty()
                ? new RoleRow("support", ROLES.get("support").label())
                : roles.get(0);
        return Optional.of(new AuthUser(
                String.valueOf(row.get("username")),
                String.valueOf(row.get("display_name")),
                primaryRole.code(),
                primaryRole.label(),
                permissions,
                includePassword ? String.valueOf(row.get("password_hash")) : "",
                String.valueOf(row.get("status"))));
    }

    public Map<String, Object> overview(int auditLimit) {
        int limit = Math.max(1, Math.min(auditLimit <= 0 ? 50 : auditLimit, 200));
        List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
                "SELECT username, display_name, status, created_at, updated_at FROM oms_ai_users ORDER BY username");
        List<Map<String, Object>> roleRows = jdbcTemplate.queryForList(
                "SELECT code, label, created_at FROM oms_ai_roles ORDER BY code");
        List<Map<String, Object>> permissionRows = jdbcTemplate.queryForList(
                "SELECT code, label, description, created_at FROM oms_ai_permissions ORDER BY code");
        List<Map<String, Object>> auditRows = jdbcTemplate.queryForList("""
                SELECT id, event_type, actor, permission, resource_type, resource_id, payload_json, created_at
                FROM oms_ai_audit_events
                ORDER BY id DESC
                LIMIT ?
                """, limit);
        return Map.of(
                "users", formatUsers(userRows),
                "roles", formatRoles(roleRows),
                "permissions", permissionRows,
                "audit_events", auditRows);
    }

    public void recordAuditEvent(String eventType, String actor, String permission, String resourceType, String resourceId, String payloadJson) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO oms_ai_audit_events
                        (event_type, actor, permission, resource_type, resource_id, payload_json)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    text(eventType),
                    text(actor),
                    text(permission),
                    text(resourceType),
                    text(resourceId),
                    payloadJson == null ? "{}" : payloadJson);
        } catch (RuntimeException ignored) {
            // Audit persistence must not break user-facing authentication.
        }
    }

    @Transactional
    public Map<String, Object> saveUser(String username, String displayName, String status, String password, List<String> roles, String actor) {
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        for (String role : roles) {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_roles WHERE code = ?", Integer.class, role);
            if (count == null || count == 0) {
                throw new IllegalArgumentException("Unknown role: " + role);
            }
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_users WHERE username = ?", Integer.class, username);
        if (exists != null && exists > 0) {
            if (password == null || password.isBlank()) {
                jdbcTemplate.update("UPDATE oms_ai_users SET display_name = ?, status = ? WHERE username = ?", displayName, status, username);
            } else {
                jdbcTemplate.update("UPDATE oms_ai_users SET display_name = ?, status = ?, password_hash = ? WHERE username = ?",
                        displayName, status, passwordHasher.hash(password), username);
            }
        } else {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("Password is required for new user");
            }
            jdbcTemplate.update("INSERT INTO oms_ai_users (username, password_hash, display_name, status) VALUES (?, ?, ?, ?)",
                    username, passwordHasher.hash(password), displayName, status);
        }
        jdbcTemplate.update("DELETE FROM oms_ai_user_roles WHERE username = ?", username);
        for (String role : roles) {
            jdbcTemplate.update("INSERT INTO oms_ai_user_roles (username, role_code) VALUES (?, ?)", username, role);
        }
        recordAuditEvent("admin.user.save", actor, "admin:rbac", "auth_user", username, "{\"status\":\"" + status + "\"}");
        return formatUsers(jdbcTemplate.queryForList(
                "SELECT username, display_name, status, created_at, updated_at FROM oms_ai_users WHERE username = ?",
                username)).get(0);
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_users (
                    username VARCHAR(64) PRIMARY KEY,
                    password_hash VARCHAR(255) NOT NULL,
                    display_name VARCHAR(128) NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'active',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_roles (
                    code VARCHAR(64) PRIMARY KEY,
                    label VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_permissions (
                    code VARCHAR(128) PRIMARY KEY,
                    label VARCHAR(128) NOT NULL,
                    description VARCHAR(255) DEFAULT '',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_user_roles (
                    username VARCHAR(64) NOT NULL,
                    role_code VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (username, role_code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_role_permissions (
                    role_code VARCHAR(64) NOT NULL,
                    permission_code VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (role_code, permission_code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS oms_ai_audit_events (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    event_type VARCHAR(128) NOT NULL,
                    actor VARCHAR(64) DEFAULT '',
                    permission VARCHAR(128) DEFAULT '',
                    resource_type VARCHAR(64) DEFAULT '',
                    resource_id VARCHAR(128) DEFAULT '',
                    payload_json TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private void seedPermissions() {
        ROLES.forEach((code, role) -> {
            upsert("oms_ai_roles", "code", code, Map.of("label", role.label()));
            for (String permission : role.permissions()) {
                insertRolePermissionIfAbsent(code, permission);
            }
        });
        PERMISSIONS.forEach((code, permission) ->
                upsert("oms_ai_permissions", "code", code, Map.of(
                        "label", permission.label(),
                        "description", permission.description())));
    }

    private void seedUsers() {
        seedUser(properties.defaultUser(), properties.defaultPassword(), properties.defaultDisplay(), "admin");
        seedUser(properties.supportUser(), properties.supportPassword(), properties.supportDisplay(), "support");
    }

    private void seedUser(String username, String password, String displayName, String role) {
        if (username == null || username.isBlank()) {
            return;
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_users WHERE username = ?", Integer.class, username);
        if (exists == null || exists == 0) {
            jdbcTemplate.update("INSERT INTO oms_ai_users (username, password_hash, display_name, status) VALUES (?, ?, ?, 'active')",
                    username, passwordHasher.hash(password), displayName == null || displayName.isBlank() ? username : displayName);
        }
        insertUserRoleIfAbsent(username, role);
    }

    private void upsert(String table, String keyColumn, String keyValue, Map<String, String> values) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + keyColumn + " = ?", Integer.class, keyValue);
        if (exists != null && exists > 0) {
            String assignments = String.join(",", values.keySet().stream().map(column -> column + " = ?").toList());
            List<Object> params = new ArrayList<>(values.values());
            params.add(keyValue);
            jdbcTemplate.update("UPDATE " + table + " SET " + assignments + " WHERE " + keyColumn + " = ?", params.toArray());
            return;
        }
        List<String> columns = new ArrayList<>();
        columns.add(keyColumn);
        columns.addAll(values.keySet());
        List<Object> params = new ArrayList<>();
        params.add(keyValue);
        params.addAll(values.values());
        String placeholders = String.join(",", columns.stream().map(column -> "?").toList());
        jdbcTemplate.update("INSERT INTO " + table + " (" + String.join(",", columns) + ") VALUES (" + placeholders + ")", params.toArray());
    }

    private void insertRolePermissionIfAbsent(String role, String permission) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oms_ai_role_permissions WHERE role_code = ? AND permission_code = ?",
                Integer.class,
                role,
                permission);
        if (exists == null || exists == 0) {
            jdbcTemplate.update("INSERT INTO oms_ai_role_permissions (role_code, permission_code) VALUES (?, ?)", role, permission);
        }
    }

    private void insertUserRoleIfAbsent(String username, String role) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oms_ai_user_roles WHERE username = ? AND role_code = ?",
                Integer.class,
                username,
                role);
        if (exists == null || exists == 0) {
            jdbcTemplate.update("INSERT INTO oms_ai_user_roles (username, role_code) VALUES (?, ?)", username, role);
        }
    }

    private List<Map<String, Object>> formatUsers(List<Map<String, Object>> userRows) {
        return userRows.stream().map(row -> {
            String username = String.valueOf(row.get("username"));
            List<String> roles = jdbcTemplate.queryForList(
                    "SELECT role_code FROM oms_ai_user_roles WHERE username = ? ORDER BY role_code",
                    String.class,
                    username);
            List<String> permissions = roles.stream()
                    .flatMap(role -> jdbcTemplate.queryForList(
                            "SELECT permission_code FROM oms_ai_role_permissions WHERE role_code = ? ORDER BY permission_code",
                            String.class,
                            role).stream())
                    .distinct()
                    .sorted()
                    .toList();
            return mapOf(
                    "username", username,
                    "display_name", text(row.get("display_name")),
                    "status", text(row.get("status")),
                    "roles", roles,
                    "permissions", permissions,
                    "created_at", text(row.get("created_at")),
                    "updated_at", text(row.get("updated_at")));
        }).toList();
    }

    private List<Map<String, Object>> formatRoles(List<Map<String, Object>> roleRows) {
        return roleRows.stream().map(row -> {
            String code = text(row.get("code"));
            List<Map<String, Object>> permissions = jdbcTemplate.query("""
                    SELECT p.code, p.label, p.description, p.created_at
                    FROM oms_ai_role_permissions rp
                    JOIN oms_ai_permissions p ON p.code = rp.permission_code
                    WHERE rp.role_code = ?
                    ORDER BY p.code
                    """, this::permissionRow, code);
            return mapOf(
                    "code", code,
                    "label", text(row.get("label")),
                    "permissions", permissions,
                    "created_at", text(row.get("created_at")));
        }).toList();
    }

    private Map<String, Object> permissionRow(ResultSet rs, int index) throws SQLException {
        return mapOf(
                "code", rs.getString("code"),
                "label", rs.getString("label"),
                "description", rs.getString("description"),
                "created_at", text(rs.getObject("created_at")));
    }

    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Map<String, PermissionDefinition> orderedPermissions() {
        Map<String, PermissionDefinition> map = new LinkedHashMap<>();
        map.put("menu:chat", new PermissionDefinition("Chat", "Open the diagnosis chat workspace."));
        map.put("menu:knowledge", new PermissionDefinition("Knowledge", "Open the knowledge search workspace."));
        map.put("menu:batch", new PermissionDefinition("Batch", "Open batch diagnosis tools."));
        map.put("menu:ops", new PermissionDefinition("Ops", "Open operations and debug tools."));
        map.put("menu:evaluation", new PermissionDefinition("Evaluation", "Open evaluation review tools."));
        map.put("menu:admin", new PermissionDefinition("Admin", "Open administration tools."));
        map.put("menu:models", new PermissionDefinition("Models", "Open model provider configuration."));
        map.put("eval:review", new PermissionDefinition("Evaluation Review", "Review feedback candidates and export cases."));
        map.put("admin:rbac", new PermissionDefinition("RBAC Admin", "Manage platform users, roles, and permissions."));
        map.put("admin:models", new PermissionDefinition("Model Admin", "Manage encrypted model provider configuration."));
        map.put("disposal:create", new PermissionDefinition("Disposal Create", "Create new disposal workflows for human-reviewed decisions."));
        map.put("disposal:review", new PermissionDefinition("Disposal Review", "Review and record human confirmation decisions on disposal workflows."));
        map.put("disposal:update", new PermissionDefinition("Disposal Update", "Update disposal workflow status."));
        map.put("disposal:draft", new PermissionDefinition("Disposal Draft", "Create and manage action suggestion drafts."));
        map.put("disposal:rollback", new PermissionDefinition("Disposal Rollback", "Create and manage rollback plans."));
        map.put("disposal:approve", new PermissionDefinition("Disposal Approve", "Approve rollback plans for execution."));
        return map;
    }

    @Transactional
    public Map<String, Object> updateUser(String username, String displayName, String status, String password, List<String> roles, String actor) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_users WHERE username = ?", Integer.class, username);
        if (exists == null || exists == 0) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        if (displayName != null && !displayName.isBlank()) {
            jdbcTemplate.update("UPDATE oms_ai_users SET display_name = ? WHERE username = ?", displayName, username);
        }
        if (status != null && !status.isBlank()) {
            jdbcTemplate.update("UPDATE oms_ai_users SET status = ? WHERE username = ?", status, username);
        }
        if (password != null && !password.isBlank()) {
            jdbcTemplate.update("UPDATE oms_ai_users SET password_hash = ? WHERE username = ?", passwordHasher.hash(password), username);
        }
        if (roles != null && !roles.isEmpty()) {
            for (String role : roles) {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_roles WHERE code = ?", Integer.class, role);
                if (count == null || count == 0) {
                    throw new IllegalArgumentException("Unknown role: " + role);
                }
            }
            jdbcTemplate.update("DELETE FROM oms_ai_user_roles WHERE username = ?", username);
            for (String role : roles) {
                jdbcTemplate.update("INSERT INTO oms_ai_user_roles (username, role_code) VALUES (?, ?)", username, role);
            }
        }
        recordAuditEvent("admin.user.update", actor, "admin:rbac", "auth_user", username, "{}");
        return formatUsers(jdbcTemplate.queryForList(
                "SELECT username, display_name, status, created_at, updated_at FROM oms_ai_users WHERE username = ?",
                username)).get(0);
    }

    @Transactional
    public void deleteUser(String username, String actor) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_users WHERE username = ?", Integer.class, username);
        if (exists == null || exists == 0) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        jdbcTemplate.update("DELETE FROM oms_ai_user_roles WHERE username = ?", username);
        jdbcTemplate.update("DELETE FROM oms_ai_users WHERE username = ?", username);
        recordAuditEvent("admin.user.delete", actor, "admin:rbac", "auth_user", username, "{}");
    }

    public List<Map<String, Object>> listRoles() {
        return jdbcTemplate.query("""
                SELECT r.code, r.label, r.created_at
                FROM oms_ai_roles r
                ORDER BY r.code
                """, (rs, index) -> {
            String code = rs.getString("code");
            List<Map<String, Object>> perms = jdbcTemplate.query("""
                    SELECT p.code, p.label, p.description
                    FROM oms_ai_role_permissions rp
                    JOIN oms_ai_permissions p ON p.code = rp.permission_code
                    WHERE rp.role_code = ?
                    ORDER BY p.code
                    """, (prs, pi) -> mapOf(
                    "code", prs.getString("code"),
                    "label", prs.getString("label"),
                    "description", prs.getString("description")),
                    code);
            return mapOf(
                    "code", code,
                    "label", rs.getString("label"),
                    "permissions", perms,
                    "created_at", text(rs.getObject("created_at")));
        });
    }

    @Transactional
    public Map<String, Object> saveRole(String code, String label, List<String> permissions, String actor) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Role code is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Role label is required");
        }
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_roles WHERE code = ?", Integer.class, code);
        if (exists != null && exists > 0) {
            jdbcTemplate.update("UPDATE oms_ai_roles SET label = ? WHERE code = ?", label, code);
        } else {
            jdbcTemplate.update("INSERT INTO oms_ai_roles (code, label) VALUES (?, ?)", code, label);
        }
        if (permissions != null) {
            jdbcTemplate.update("DELETE FROM oms_ai_role_permissions WHERE role_code = ?", code);
            for (String perm : permissions) {
                jdbcTemplate.update("INSERT INTO oms_ai_role_permissions (role_code, permission_code) VALUES (?, ?)", code, perm);
            }
        }
        recordAuditEvent("admin.role.save", actor, "admin:rbac", "auth_role", code, "{}");
        List<Map<String, Object>> roles = listRoles();
        return roles.stream().filter(r -> code.equals(r.get("code"))).findFirst().orElse(Map.of());
    }

    @Transactional
    public void deleteRole(String code, String actor) {
        Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_ai_roles WHERE code = ?", Integer.class, code);
        if (exists == null || exists == 0) {
            throw new IllegalArgumentException("Role not found: " + code);
        }
        jdbcTemplate.update("DELETE FROM oms_ai_role_permissions WHERE role_code = ?", code);
        jdbcTemplate.update("DELETE FROM oms_ai_user_roles WHERE role_code = ?", code);
        jdbcTemplate.update("DELETE FROM oms_ai_roles WHERE code = ?", code);
        recordAuditEvent("admin.role.delete", actor, "admin:rbac", "auth_role", code, "{}");
    }

    public List<Map<String, Object>> listPermissions() {
        return jdbcTemplate.query("""
                SELECT code, label, description, created_at
                FROM oms_ai_permissions
                ORDER BY code
                """, (rs, index) -> mapOf(
                "code", rs.getString("code"),
                "label", rs.getString("label"),
                "description", rs.getString("description"),
                "created_at", text(rs.getObject("created_at"))));
    }

    public List<Map<String, Object>> buildMenuTree(List<String> permissions) {
        List<Map<String, Object>> allMenus = List.of(
                menuItem("chat", "Chat", "menu:chat", "/chat", "message-square", permissions),
                menuItem("knowledge", "Knowledge", "menu:knowledge", "/knowledge", "book-open", permissions),
                menuItem("batch", "Batch", "menu:batch", "/batch", "layers", permissions),
                menuItem("ops", "Operations", "menu:ops", "/ops", "terminal", permissions),
                menuItem("evaluation", "Evaluation", "menu:evaluation", "/evaluation", "bar-chart-2", permissions),
                menuItem("admin", "Administration", "menu:admin", "/admin", "settings", permissions),
                menuItem("models", "Models", "menu:models", "/models", "cpu", permissions));
        return allMenus.stream()
                .filter(menu -> Boolean.TRUE.equals(menu.get("visible")))
                .toList();
    }

    private Map<String, Object> menuItem(String key, String label, String permission, String path, String icon, List<String> userPermissions) {
        boolean visible = userPermissions.contains(permission);
        return mapOf(
                "key", key,
                "label", label,
                "permission", permission,
                "path", path,
                "icon", icon,
                "visible", visible);
    }

    public record RoleDefinition(String label, List<String> permissions) {
    }

    public record PermissionDefinition(String label, String description) {
    }

    private record RoleRow(String code, String label) {
    }
}
