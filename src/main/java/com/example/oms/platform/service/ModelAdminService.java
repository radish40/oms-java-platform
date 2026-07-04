package com.example.oms.platform.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class ModelAdminService {
    private final ModelRuntimeClient runtimeClient;

    public ModelAdminService(ModelRuntimeClient runtimeClient) {
        this.runtimeClient = runtimeClient;
    }

    public Object modelConfigs(String authorization) {
        return runtimeClient.get("/admin/model-configs", authorization);
    }

    public Object saveModelConfig(String payload, String authorization) {
        return runtimeClient.post("/admin/model-configs", payload, authorization);
    }

    public Object deleteModelConfig(String id, String authorization) {
        return runtimeClient.delete("/admin/model-configs/" + encodePathSegment(id), authorization);
    }

    public Object refreshModelCache(String authorization) {
        return runtimeClient.post("/admin/model-configs/refresh-cache", "{}", authorization);
    }

    public Object testModelConfig(String payload, String authorization) {
        return runtimeClient.post("/admin/model-configs/test", payload, authorization);
    }

    public Object modelBindings(String authorization) {
        return runtimeClient.get("/admin/model-bindings", authorization);
    }

    public Object saveModelBinding(String payload, String authorization) {
        return runtimeClient.post("/admin/model-bindings", payload, authorization);
    }

    public Object chatOptions(String authorization) {
        return runtimeClient.get("/model-options/chat", authorization);
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
