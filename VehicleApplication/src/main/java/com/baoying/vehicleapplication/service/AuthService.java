package com.baoying.vehicleapplication.service;

import com.baoying.vehicleapplication.dto.request.LoginRequest;
import com.baoying.vehicleapplication.dto.response.LoginResponse;

public interface AuthService {
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    
    /**
     * 刷新Token
     */
    String refreshToken(String token);
    
    /**
     * 验证Token是否有效
     */
    boolean validateToken(String token);
    
    /**
     * 从Token获取用户ID
     */
    Long getUserIdFromToken(String token);
}