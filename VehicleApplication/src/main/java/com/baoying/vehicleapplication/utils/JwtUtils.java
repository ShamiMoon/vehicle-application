package com.baoying.vehicleapplication.utils;

import com.baoying.vehicleapplication.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtConfig jwtConfig;

    /**
     * 生成 SecretKey
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token（存储用户ID、部门ID、角色ID）
     * @param userId 账号ID
     * @param deptId 部门ID
     * @param roleId 角色ID
     */
    public String generateToken(Long userId, Integer deptId, Integer roleId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("deptId", deptId);
        claims.put("roleId", roleId);
        
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 从 Token 中获取 Claims
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("解析Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return false;
        }
        // 检查是否过期
        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.before(new Date())) {
            log.warn("Token已过期");
            return false;
        }
        return true;
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId == null) {
            // 兼容从 subject 获取
            String subject = claims.getSubject();
            if (subject != null) {
                return Long.parseLong(subject);
            }
            return null;
        }
        return ((Number) userId).longValue();
    }

    /**
     * 从 Token 中获取部门ID
     */
    public Integer getDeptIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Object deptId = claims.get("deptId");
        if (deptId == null) {
            return null;
        }
        return ((Number) deptId).intValue();
    }

    /**
     * 从 Token 中获取角色ID
     */
    public Integer getRoleIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Object roleId = claims.get("roleId");
        if (roleId == null) {
            return null;
        }
        return ((Number) roleId).intValue();
    }

    /**
     * 刷新 Token（重新生成）
     */
    public String refreshToken(String token) {
        Long userId = getUserIdFromToken(token);
        Integer deptId = getDeptIdFromToken(token);
        Integer roleId = getRoleIdFromToken(token);
        if (userId == null) {
            return null;
        }
        return generateToken(userId, deptId, roleId);
    }
}