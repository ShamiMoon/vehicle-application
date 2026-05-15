package com.baoying.vehicleapplication.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class CurrentUserUtils {

    /**
     * 获取当前请求对象
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("无法获取当前请求对象，可能不在Web上下文中");
            return null;
        }
        return attributes.getRequest();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.warn("未获取到当前登录用户信息");
            return null;
        }
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            log.warn("request中未找到userId属性");
            return null;
        }
        if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof String) {
            return Long.parseLong((String) userId);
        }
        return null;
    }

    /**
     * 获取当前登录用户部门ID
     */
    public static Integer getCurrentDeptId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        Object deptId = request.getAttribute("deptId");
        if (deptId == null) {
            return null;
        }
        if (deptId instanceof Integer) {
            return (Integer) deptId;
        } else if (deptId instanceof Long) {
            return ((Long) deptId).intValue();
        } else if (deptId instanceof String) {
            return Integer.parseInt((String) deptId);
        }
        return null;
    }

    /**
     * 获取当前登录用户角色ID
     */
    public static Integer getCurrentRoleId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        Object roleId = request.getAttribute("roleId");
        if (roleId == null) {
            return null;
        }
        if (roleId instanceof Integer) {
            return (Integer) roleId;
        } else if (roleId instanceof Long) {
            return ((Long) roleId).intValue();
        } else if (roleId instanceof String) {
            return Integer.parseInt((String) roleId);
        }
        return null;
    }
}