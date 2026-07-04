package com.example.oms.platform.service;

import com.example.oms.platform.exception.BusinessException;
import com.example.oms.platform.repository.RbacRepository;
import com.example.oms.platform.security.AuthUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RbacService {
    private final RbacRepository repository;

    public RbacService(RbacRepository repository) {
        this.repository = repository;
    }

    public Optional<AuthUser> findCurrentUser(String username) {
        return repository.loadProfile(username, false);
    }

    public AuthUser currentUser(String username) {
        return findCurrentUser(username)
                .orElseThrow(() -> new BusinessException(401, "UNAUTHORIZED", "Authentication required"));
    }

    public Optional<AuthUser> loginProfile(String username) {
        return repository.loadProfile(username, true);
    }

    public Map<String, Object> overview(int auditLimit) {
        return repository.overview(auditLimit);
    }

    public Map<String, Object> saveUser(String username, String displayName, String status, String password, List<String> roles, String actor) {
        return repository.saveUser(username, displayName, status, password, roles, actor);
    }

    public Map<String, Object> updateUser(String username, String displayName, String status, String password, List<String> roles, String actor) {
        return repository.updateUser(username, displayName, status, password, roles, actor);
    }

    public void deleteUser(String username, String actor) {
        repository.deleteUser(username, actor);
    }

    public List<Map<String, Object>> listRoles() {
        return repository.listRoles();
    }

    public Map<String, Object> saveRole(String code, String label, List<String> permissions, String actor) {
        return repository.saveRole(code, label, permissions, actor);
    }

    public void deleteRole(String code, String actor) {
        repository.deleteRole(code, actor);
    }

    public List<Map<String, Object>> listPermissions() {
        return repository.listPermissions();
    }

    public List<Map<String, Object>> buildMenuTree(List<String> permissions) {
        return repository.buildMenuTree(permissions);
    }

    public void recordAuditEvent(String eventType, String actor, String permission, String resourceType, String resourceId, String payloadJson) {
        repository.recordAuditEvent(eventType, actor, permission, resourceType, resourceId, payloadJson);
    }
}
