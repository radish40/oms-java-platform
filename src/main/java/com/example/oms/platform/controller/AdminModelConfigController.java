package com.example.oms.platform.controller;

import com.example.oms.platform.security.RequiresPermission;
import com.example.oms.platform.service.ModelAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "模型配置管理")
public class AdminModelConfigController {
    private final ModelAdminService modelAdminService;

    public AdminModelConfigController(ModelAdminService modelAdminService) {
        this.modelAdminService = modelAdminService;
    }

    @GetMapping("/model-configs")
    @RequiresPermission("admin:models")
    @Operation(summary = "模型配置列表", description = "查询所有模型配置列表")
    public Object modelConfigs(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return modelAdminService.modelConfigs(authorization);
    }

    @PostMapping("/model-configs")
    @RequiresPermission("admin:models")
    @Operation(summary = "保存模型配置", description = "创建或更新模型配置")
    public Object saveModelConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String payload) {
        return modelAdminService.saveModelConfig(payload, authorization);
    }

    @DeleteMapping("/model-configs/{id}")
    @RequiresPermission("admin:models")
    @Operation(summary = "删除模型配置", description = "根据ID删除模型配置")
    public Object deleteModelConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "模型配置ID") @PathVariable String id) {
        return modelAdminService.deleteModelConfig(id, authorization);
    }

    @PostMapping("/model-configs/refresh-cache")
    @RequiresPermission("admin:models")
    @Operation(summary = "刷新模型缓存", description = "刷新模型配置缓存")
    public Object refreshModelCache(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return modelAdminService.refreshModelCache(authorization);
    }

    @PostMapping("/model-configs/test")
    @RequiresPermission("admin:models")
    @Operation(summary = "测试模型配置", description = "测试模型连通性")
    public Object testModelConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String payload) {
        return modelAdminService.testModelConfig(payload, authorization);
    }

    @GetMapping("/model-bindings")
    @RequiresPermission("admin:models")
    @Operation(summary = "模型绑定列表", description = "查询所有模型绑定关系")
    public Object modelBindings(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return modelAdminService.modelBindings(authorization);
    }

    @PostMapping("/model-bindings")
    @RequiresPermission("admin:models")
    @Operation(summary = "保存模型绑定", description = "创建或更新模型绑定关系")
    public Object saveModelBinding(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String payload) {
        return modelAdminService.saveModelBinding(payload, authorization);
    }
}
