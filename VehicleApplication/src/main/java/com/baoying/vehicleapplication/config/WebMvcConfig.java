package com.baoying.vehicleapplication.config;

import com.baoying.vehicleapplication.interceptor.JwtInterceptor;
import com.baoying.vehicleapplication.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT认证拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(
                        "/org/**",
                        "/flow/**",
                        "/apply/**",
                        "/msg/**"
                )  // 拦截所有开头的请求
                .excludePathPatterns(
                    "/api/**",
                    "/org/user/create"
                );
        
        // 权限控制拦截器（在JWT之后执行）
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns(
                        "/org/**",
                        "/flow/**",
                        "/apply/**",
                        "/msg/**"
                )
                .excludePathPatterns(
                    "/api/**",
                    "/org/user/create"
                );
    }
}