package com.example.oms.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verifiesKnownPbkdf2Sha256Hash() {
        String encoded = hasher.hash("secret", "fixed-salt", 120000);

        assertThat(hasher.verify("secret", encoded)).isTrue();
        assertThat(hasher.verify("wrong", encoded)).isFalse();
    }

    @Test
    void rejectsMalformedHash() {
        assertThat(hasher.verify("secret", "bad")).isFalse();
        assertThat(hasher.verify("secret", "")).isFalse();
    }
}
