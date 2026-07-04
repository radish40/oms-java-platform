package com.example.oms.platform.security;

import com.example.oms.platform.service.RbacService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HmacAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final RbacService rbacService;

    public HmacAuthenticationFilter(TokenProvider tokenProvider, RbacService rbacService) {
        this.tokenProvider = tokenProvider;
        this.rbacService = rbacService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String username = tokenProvider.usernameFromBearer(request.getHeader("Authorization"));
        if (!username.isBlank()) {
            rbacService.findCurrentUser(username).ifPresent(user -> {
                List<SimpleGrantedAuthority> authorities = user.permissions().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
