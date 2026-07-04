package com.example.oms.platform.controller;

import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.ModelAdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminModelConfigController {
    private final ModelAdminService modelAdminService;

    public AdminModelConfigController(ModelAdminService modelAdminService) {
        this.modelAdminService = modelAdminService;
    }

    @GetMapping("/model-configs")
    @RequiresPermission("admin:models")
    public Object modelConfigs(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return modelAdminService.modelConfigs(authorization);
    }

    @PostMapping("/model-configs")
    @RequiresPermission("admin:models")
    public Object saveModelConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String payload) {
        return modelAdminService.saveModelConfig(payload, authorization);
    }

    @DeleteMapping("/model-configs/{id}")
    @RequiresPermission("admin:models")
    public Object deleteModelConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        return modelAdminService.deleteModelConfig(id, authorization);
    }

    @PostMapping("/model-configs/refresh-cache")
    @RequiresPermission("admin:models")
    public Object refreshModelCache(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return modelAdminService.refreshModelCache(authorization);
    }

    @PostMapping("/model-configs/test")
    @RequiresPermission("admin:models")
    public Object testModelConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String payload) {
        return modelAdminService.testModelConfig(payload, authorization);
    }

    @GetMapping("/model-bindings")
    @RequiresPermission("admin:models")
    public Object modelBindings(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return modelAdminService.modelBindings(authorization);
    }

    @PostMapping("/model-bindings")
    @RequiresPermission("admin:models")
    public Object saveModelBinding(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String payload) {
        return modelAdminService.saveModelBinding(payload, authorization);
    }
}
