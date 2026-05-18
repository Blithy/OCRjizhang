package com.example.ocrjizhang.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 后端启动入口。
 * 这个后端主要用于本地演示登录、同步和网页管理面板，不承担正式部署职责。
 */
@SpringBootApplication
public class DemoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBackendApplication.class, args);
    }
}
