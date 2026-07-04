package com.example.oms.platform.security;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DataMaskingService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([^@]{1,3})[^@]*(@.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+?\\d{0,3})\\d*(\\d{4})");
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "\\d{4}-\\d{4}-\\d{4}-(\\d{4})");
    private static final Pattern ID_PATTERN = Pattern.compile(
            "(\\d{1,4})\\d+(\\d{4})");

    private static final Set<String> DEFAULT_SENSITIVE_FIELD_NAMES = Set.of(
            "email", "phone", "mobile", "phone_number",
            "password", "password_hash", "secret", "token",
            "address", "full_address", "street",
            "card_number", "credit_card", "id_number",
            "ssn", "social_security", "passport",
            "bank_account", "account_number",
            "ip_address", "mac_address");

    private static final Set<String> SENSITIVE_JSON_PATHS = Set.of(
            "$.user.email",
            "$.user.phone",
            "$.user.address",
            "$.payload.customer.phone",
            "$.payload.customer.email",
            "$.order.customer_address",
            "$.order.customer_phone");

    public String maskByFieldName(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (!DEFAULT_SENSITIVE_FIELD_NAMES.contains(fieldName.toLowerCase())) {
            return value;
        }
        return applyMask(fieldName, value);
    }

    public String mask(String fieldName, String value, List<String> userPermissions) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (userPermissions != null && userPermissions.contains("admin:sensitive_data")) {
            return value;
        }
        return applyMask(fieldName, value);
    }

    public String maskByStrategy(MaskingStrategy strategy, String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return switch (strategy) {
            case FULL_MASK -> "***";
            case PARTIAL_EMAIL -> maskEmail(value);
            case PARTIAL_PHONE -> maskPhone(value);
            case PARTIAL_CARD -> maskCard(value);
            case PARTIAL_ID -> maskId(value);
            case TRUNCATE -> value.length() > 4 ? value.substring(0, 4) + "..." : value;
        };
    }

    private String applyMask(String fieldName, String value) {
        String lower = fieldName.toLowerCase();
        if (lower.contains("email")) {
            return maskEmail(value);
        }
        if (lower.contains("phone") || lower.contains("mobile")) {
            return maskPhone(value);
        }
        if (lower.contains("card") || lower.contains("credit")) {
            return maskCard(value);
        }
        if (lower.contains("id_number") || lower.contains("ssn") || lower.contains("passport")) {
            return maskId(value);
        }
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
            return "***";
        }
        return "***";
    }

    private String maskEmail(String email) {
        if (!email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domain;
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
    }

    private String maskPhone(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.length() <= 4) {
            return "***";
        }
        if (digits.length() <= 7) {
            return digits.substring(0, 1) + "***" + digits.substring(digits.length() - 2);
        }
        return digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
    }

    private String maskCard(String cardNumber) {
        String digits = cardNumber.replaceAll("[^\\d]", "");
        if (digits.length() < 8) {
            return "***";
        }
        return digits.substring(0, 4) + "-****-****-" + digits.substring(digits.length() - 4);
    }

    private String maskId(String idNumber) {
        if (idNumber.length() <= 6) {
            return "***";
        }
        return idNumber.substring(0, 3) + "****" + idNumber.substring(idNumber.length() - 3);
    }
}
