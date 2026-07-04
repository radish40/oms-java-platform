package com.example.oms.platform.controller;

import com.example.oms.platform.config.SecurityConfig;
import com.example.oms.platform.exception.GlobalExceptionHandler;
import com.example.oms.platform.repository.EvaluationRepository;
import com.example.oms.platform.repository.RbacRepository;
import com.example.oms.platform.repository.SessionRepository;
import com.example.oms.platform.security.HmacAuthenticationFilter;
import com.example.oms.platform.security.PasswordHasher;
import com.example.oms.platform.security.PermissionAspect;
import com.example.oms.platform.security.TokenProvider;
import com.example.oms.platform.service.AuthService;
import com.example.oms.platform.service.EvaluationService;
import com.example.oms.platform.service.RbacService;
import com.example.oms.platform.service.SessionService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        AuthController.class,
        SessionController.class,
        EvaluationController.class,
        SessionService.class,
        EvaluationService.class,
        SessionRepository.class,
        EvaluationRepository.class,
        AuthService.class,
        RbacService.class,
        RbacRepository.class,
        PasswordHasher.class,
        TokenProvider.class,
        HmacAuthenticationFilter.class,
        PermissionAspect.class
})
class SessionEvaluationTestApplication {
}
