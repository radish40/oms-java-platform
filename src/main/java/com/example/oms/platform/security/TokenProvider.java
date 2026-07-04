package com.example.oms.platform.security;

import com.example.oms.platform.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {
    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;

    public TokenProvider(SecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String create(AuthUser user) {
        try {
            String payload = objectMapper.writeValueAsString(new TokenPayload(
                    user.username(),
                    user.role(),
                    System.currentTimeMillis() / 1000));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            return encoded + "." + hmac(encoded);
        } catch (Exception exception) {
            throw new BusinessException(500, "TOKEN_CREATE_FAILED", "Token creation failed");
        }
    }

    public String usernameFromBearer(String authorization) {
        String token = authorization == null ? "" : authorization.replaceFirst("(?i)^Bearer\\s+", "").trim();
        if (token.isBlank()) {
            return "";
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || !constantEquals(hmac(parts[0]), parts[1])) {
            return "";
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payload);
            return node.path("username").asText("");
        } catch (Exception exception) {
            return "";
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.authSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC unavailable", exception);
        }
    }

    private boolean constantEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private record TokenPayload(String username, String role, long iat) {
    }
}
