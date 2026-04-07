package com.example.ocrjizhang.backend.config;

import com.example.ocrjizhang.backend.auth.AuthInterceptor;
import com.example.ocrjizhang.backend.manage.ManageAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final ManageAuthInterceptor manageAuthInterceptor;

    public WebConfig(
        AuthInterceptor authInterceptor,
        ManageAuthInterceptor manageAuthInterceptor
    ) {
        this.authInterceptor = authInterceptor;
        this.manageAuthInterceptor = manageAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**");
        registry.addInterceptor(manageAuthInterceptor)
            .addPathPatterns("/manage/**")
            .excludePathPatterns("/manage/login", "/manage/manage.css");
    }
}
