package com.example.oms.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenProviderTest {
    private final TokenProvider provider = new TokenProvider(
            new SecurityProperties("test-secret", "admin", "admin-pass", "Admin", "support", "support-pass", "Support"),
            new ObjectMapper());

    @Test
    void createsAndVerifiesHmacToken() {
        AuthUser user = new AuthUser("admin", "Admin", "admin", "Administrator", List.of("admin:rbac"), "", "active");

        String token = provider.create(user);

        assertThat(token).contains(".");
        assertThat(provider.usernameFromBearer("Bearer " + token)).isEqualTo("admin");
    }

    @Test
    void rejectsInvalidToken() {
        assertThat(provider.usernameFromBearer("Bearer invalid.token")).isEmpty();
        assertThat(provider.usernameFromBearer("")).isEmpty();
    }
}
