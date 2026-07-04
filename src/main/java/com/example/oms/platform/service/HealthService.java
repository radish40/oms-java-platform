package com.example.oms.platform.service;

import com.example.oms.platform.client.PythonRuntimeClient;
import com.example.oms.platform.client.RuntimePing;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
    private final PythonRuntimeClient runtimeClient;
    private final JdbcTemplate jdbcTemplate;

    public HealthService(PythonRuntimeClient runtimeClient, JdbcTemplate jdbcTemplate) {
        this.runtimeClient = runtimeClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> health() {
        RuntimePing runtime = runtimeClient.ping();
        RuntimePing mysql = pingMysql();
        return Map.of(
                "status", runtime.ok() && mysql.ok() ? "UP" : "DEGRADED",
                "service", "oms-platform",
                "runtime", dependency("Runtime unavailable", runtime),
                "mysql", dependency("MySQL unavailable", mysql));
    }

    private RuntimePing pingMysql() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return RuntimePing.up();
        } catch (RuntimeException exception) {
            return RuntimePing.down(exception.getMessage());
        }
    }

    private Map<String, Object> dependency(String fallback, RuntimePing ping) {
        if (ping.ok()) {
            return Map.of("status", "UP");
        }
        return Map.of("status", "DOWN", "error", ping.error() == null || ping.error().isBlank() ? fallback : ping.error());
    }
}
