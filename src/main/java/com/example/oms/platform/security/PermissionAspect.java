package com.example.oms.platform.security;

import com.example.oms.platform.exception.BusinessException;
import java.util.Map;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {
    @Before("@annotation(requiresPermission)")
    public void require(RequiresPermission requiresPermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw new BusinessException(401, "UNAUTHORIZED", "Authentication required");
        }
        if (!user.permissions().contains(requiresPermission.value())) {
            throw new BusinessException(403, "FORBIDDEN", "Missing permission: " + requiresPermission.value(),
                    Map.of("permission", requiresPermission.value()));
        }
    }
}
