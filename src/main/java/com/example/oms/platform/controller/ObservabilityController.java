package com.example.oms.platform.controller;

import com.example.oms.platform.client.PythonRuntimeClient;
import com.example.oms.platform.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/observability")
@Tag(name = "Observability")
public class ObservabilityController {
    private final PythonRuntimeClient runtimeClient;

    public ObservabilityController(PythonRuntimeClient runtimeClient) {
        this.runtimeClient = runtimeClient;
    }

    @GetMapping("/dashboard")
    @RequiresPermission("menu:ops")
    @Operation(summary = "Runtime observability dashboard")
    public Object dashboard(
            @RequestParam(value = "window_hours", defaultValue = "24") String windowHours,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return runtimeClient.get("/observability/dashboard?window_hours=" + windowHours, authorization);
    }
}
