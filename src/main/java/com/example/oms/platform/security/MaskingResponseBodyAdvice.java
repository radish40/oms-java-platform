package com.example.oms.platform.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class MaskingResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "email", "phone", "mobile", "phone_number",
            "password", "password_hash", "secret", "token",
            "address", "full_address", "street", "city",
            "customer_name", "customer_phone", "customer_email",
            "customer_address", "id_number", "ssn",
            "card_number", "bank_account");

    private static final Set<MediaType> MASKABLE_TYPES = Set.of(
            MediaType.APPLICATION_JSON,
            MediaType.parseMediaType("application/x-ndjson; charset=utf-8"));

    private final DataMaskingService maskingService;
    private final ObjectMapper objectMapper;

    public MaskingResponseBodyAdvice(DataMaskingService maskingService, ObjectMapper objectMapper) {
        this.maskingService = maskingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        List<String> permissions = getUserPermissions();
        if (permissions.contains("admin:sensitive_data")) {
            return body;
        }
        if (body instanceof Map<?, ?> map) {
            return maskMap(map, permissions);
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> maskMap(Map<?, ?> input, List<String> permissions) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (SENSITIVE_FIELD_NAMES.contains(key.toLowerCase()) && value instanceof String strValue) {
                result.put(key, maskingService.mask(key, strValue, permissions));
            } else if (value instanceof Map<?, ?> nestedMap) {
                result.put(key, maskMap(nestedMap, permissions));
            } else if (value instanceof List<?> list) {
                result.put(key, maskList(list, permissions));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<?> maskList(List<?> input, List<String> permissions) {
        return input.stream().map(item -> {
            if (item instanceof Map<?, ?> map) {
                return maskMap(map, permissions);
            }
            return item;
        }).toList();
    }

    private List<String> getUserPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return user.permissions();
        }
        return Collections.emptyList();
    }
}
