package com.example.oms.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oms.security")
public record SecurityProperties(
        String authSecret,
        String defaultUser,
        String defaultPassword,
        String defaultDisplay,
        String supportUser,
        String supportPassword,
        String supportDisplay) {
}
