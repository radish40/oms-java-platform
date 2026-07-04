package com.example.oms.platform.client;

public record RuntimePing(boolean ok, String error) {
    public static RuntimePing up() {
        return new RuntimePing(true, null);
    }

    public static RuntimePing down(String error) {
        return new RuntimePing(false, error == null || error.isBlank() ? "Runtime unavailable" : error);
    }
}
