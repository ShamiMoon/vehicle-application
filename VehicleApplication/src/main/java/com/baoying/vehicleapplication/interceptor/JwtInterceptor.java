package com.baoying.vehicleapplication.interceptor;

import com.baoying.vehicleapplication.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\",\"data\":null}");
            return false;
        }

        if (!jwtUtils.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期，请重新登录\",\"data\":null}");
            return false;
        }

        // 将用户信息存入请求属性，供后续使用
        Long userId = jwtUtils.getUserIdFromToken(token);
        Integer deptId = jwtUtils.getDeptIdFromToken(token);
        Integer roleId = jwtUtils.getRoleIdFromToken(token);
        log.debug("Token解析结果 - userId: {}, deptId: {}, roleId: {}", userId, deptId, roleId);
        request.setAttribute("userId", userId);
        request.setAttribute("deptId", deptId);
        request.setAttribute("roleId", roleId);

        return true;
    }

    private String extractToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }
}