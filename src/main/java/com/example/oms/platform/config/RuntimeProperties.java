package com.example.oms.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oms.python-runtime")
public record RuntimeProperties(String baseUrl, int timeout) {
    public String normalizedBaseUrl() {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:18010" : baseUrl;
        return value.replaceAll("/+$", "");
    }

    public int timeoutMillis() {
        return timeout <= 0 ? 30000 : timeout;
    }
}
