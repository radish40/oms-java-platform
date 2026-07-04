package com.example.oms.platform.controller;

import com.example.oms.platform.client.PythonRuntimeClient;
import com.example.oms.platform.security.RequiresPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
public class OpsController {
    private final PythonRuntimeClient runtimeClient;

    public OpsController(PythonRuntimeClient runtimeClient) {
        this.runtimeClient = runtimeClient;
    }

    @GetMapping("/debug")
    @RequiresPermission("menu:ops")
    public Object debug(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return runtimeClient.get("/ops/debug", authorization);
    }
}
