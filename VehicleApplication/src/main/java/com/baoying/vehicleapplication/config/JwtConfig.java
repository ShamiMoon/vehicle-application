package com.baoying.vehicleapplication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /** JWT密钥 */
    private String secret = "vehicle-application-secret-key-2026-secure";
    /** 过期时间（毫秒），默认24小时 */
    private Long expiration = 86400000L;
    /** Token 请求头 */
    private String tokenHeader = "Authorization";
}