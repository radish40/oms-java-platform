package com.example.oms.platform.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    private final Map<String, Object> details;

    public BusinessException(int status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public BusinessException(int status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : details;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
