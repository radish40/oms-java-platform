package com.example.oms.platform.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DataMaskingServiceTest {

    private final DataMaskingService service = new DataMaskingService();

    @Test
    void fullMaskReturnsAsterisks() {
        String result = service.maskByStrategy(MaskingStrategy.FULL_MASK, "sensitive-value");
        assertEquals("***", result);
    }

    @Test
    void partialEmailMasksLocalPart() {
        String result = service.maskByStrategy(MaskingStrategy.PARTIAL_EMAIL, "user@example.com");
        assertTrue(result.contains("@example.com"), "Domain should be preserved");
        assertNotEquals("user@example.com", result);
        assertTrue(result.contains("***"), "Masking indicator should be present");
    }

    @Test
    void partialPhoneMasksMiddleDigits() {
        String result = service.maskByStrategy(MaskingStrategy.PARTIAL_PHONE, "+8613800138000");
        assertTrue(result.contains("****"), "Phone should have masked middle digits");
        assertTrue(result.startsWith("138"), "Phone should start with first digits");
    }

    @Test
    void partialCardFormatsCorrectly() {
        String result = service.maskByStrategy(MaskingStrategy.PARTIAL_CARD, "1234-5678-9012-3456");
        assertTrue(result.contains("****"), "Card should have masked middle");
        assertTrue(result.startsWith("1234"), "Card should start with first 4");
        assertTrue(result.endsWith("3456"), "Card should end with last 4");
    }

    @Test
    void partialIdMasksMiddle() {
        String result = service.maskByStrategy(MaskingStrategy.PARTIAL_ID, "320102199001011234");
        assertTrue(result.contains("****"), "ID should have masked middle");
        assertTrue(result.length() < "320102199001011234".length() || result.contains("****"));
    }

    @Test
    void truncateShortensLongValue() {
        String result = service.maskByStrategy(MaskingStrategy.TRUNCATE, "very-long-sensitive-data");
        assertEquals("very...", result);
    }

    @Test
    void nullValueReturnsNull() {
        assertEquals(null, service.maskByStrategy(MaskingStrategy.FULL_MASK, null));
    }

    @Test
    void blankValueReturnsBlank() {
        assertEquals("", service.maskByStrategy(MaskingStrategy.FULL_MASK, ""));
    }

    @Test
    void maskByFieldNameRecognizesEmail() {
        String result = service.mask("email", "test@example.com", List.of());
        assertNotEquals("test@example.com", result);
        assertTrue(result.contains("@"));
    }

    @Test
    void maskByFieldNameRecognizesPhone() {
        String result = service.mask("phone", "13800138000", List.of());
        assertTrue(result.contains("****"));
    }

    @Test
    void maskRespectsAdminSensitiveDataPermission() {
        String result = service.mask("email", "admin@example.com", List.of("admin:sensitive_data"));
        assertEquals("admin@example.com", result);
    }

    @Test
    void maskByFieldNameRecognizesPhoneNumber() {
        String result = service.mask("phone_number", "13912345678", List.of());
        assertTrue(result.contains("****"));
    }

    @Test
    void maskPasswordsToAsterisks() {
        String result = service.mask("password", "my-secret-password", List.of());
        assertEquals("***", result);
    }

    @Test
    void maskCardNumber() {
        String result = service.mask("card_number", "4111111111111111", List.of());
        assertTrue(result.contains("****"));
    }

    @Test
    void maskIdNumber() {
        String result = service.mask("id_number", "320102199001011234", List.of());
        assertTrue(result.contains("****"));
    }

    @Test
    void nonSensitiveFieldLeftUnchanged() {
        String result = service.maskByFieldName("username", "john_doe");
        assertEquals("john_doe", result);
    }
}
