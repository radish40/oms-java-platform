package com.example.oms.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.oms.platform.repository")
public class PlatformApplication {

    public static void main(String[] args) {
        // 启用虚拟线程
        System.setProperty("spring.threads.virtual.enabled", "true");
        SpringApplication.run(PlatformApplication.class, args);
    }
}
