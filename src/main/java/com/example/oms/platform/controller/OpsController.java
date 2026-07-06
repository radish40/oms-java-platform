package com.example.oms.platform.controller;

import com.example.oms.platform.client.PythonRuntimeClient;
import com.example.oms.platform.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
@Tag(name = "运维调试")
public class OpsController {
    private final PythonRuntimeClient runtimeClient;

    public OpsController(PythonRuntimeClient runtimeClient) {
        this.runtimeClient = runtimeClient;
    }

    @GetMapping("/debug")
    @RequiresPermission("menu:ops")
    @Operation(summary = "调试信息", description = "获取运维调试信息，需具备 menu:ops 权限")
    public Object debug(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return runtimeClient.get("/ops/debug", authorization);
    }
}
