package com.example.oms.platform.controller;

import com.example.oms.platform.service.AuthService;
import com.example.oms.platform.service.ModelAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/model-options")
public class ModelOptionsController {
    private final AuthService authService;
    private final ModelAdminService modelAdminService;

    public ModelOptionsController(AuthService authService, ModelAdminService modelAdminService) {
        this.authService = authService;
        this.modelAdminService = modelAdminService;
    }

    @GetMapping("/chat")
    public Object chatOptions(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.currentUser(authorization);
        return modelAdminService.chatOptions(authorization);
    }
}
