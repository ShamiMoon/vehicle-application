package com.baoying.vehicleapplication.interceptor;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.entity.CarApplication;
import com.baoying.vehicleapplication.entity.SysDeptRole;
import com.baoying.vehicleapplication.entity.SysRole;
import com.baoying.vehicleapplication.mapper.CarApplicationMapper;
import com.baoying.vehicleapplication.mapper.DeptRoleMapper;
import com.baoying.vehicleapplication.mapper.RoleMapper;
import com.baoying.vehicleapplication.utils.JwtUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 权限拦截器
 * 检查用户是否有权限访问被 @RequirePermission 注解标记的接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {
    
    private final JwtUtils jwtUtils;
    private final DeptRoleMapper deptRoleMapper;
    private final CarApplicationMapper carApplicationMapper;
    private final RoleMapper roleMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查方法或类上是否有 @RequirePermission 注解
        RequirePermission permission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            permission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        
        // 没有注解，不需要权限控制
        if (permission == null) {
            return true;
        }
        
        // 从请求属性中获取用户信息（由JwtInterceptor设置）
        Long userId = (Long) request.getAttribute("userId");
        Integer userRoleId = (Integer) request.getAttribute("roleId");
        Integer userDeptId = (Integer) request.getAttribute("deptId");
        
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }

        // 检查用户角色是否已被禁用
        if (userRoleId != null) {
            SysRole role = roleMapper.selectById(userRoleId);
            if (role == null || role.getStatus() != 1) {
                log.warn("用户 {} 的角色 {} 已被禁用", userId, userRoleId);
                throw new BusinessException("当前角色已被禁用，请联系管理员");
            }
        }

        // 1. 检查角色权限
        if (permission.checkRole() && permission.roles().length > 0) {
            if (userRoleId == null) {
                throw new BusinessException("无权访问：未分配角色");
            }
            
            // 检查用户角色是否在允许的角色列表中
            boolean hasRole = Arrays.stream(permission.roles())
                    .anyMatch(roleId -> roleId == userRoleId);
            
            if (!hasRole) {
                log.warn("用户 {} 角色 {} 无权访问接口 {}", userId, userRoleId, request.getRequestURI());
                throw new BusinessException("无权访问：需要特定角色权限");
            }
        }
        
        // 2. 检查数据权限范围
        String dataScope = permission.dataScope();
        if (!"all".equals(dataScope) && userRoleId != null && userDeptId != null) {
            // 查询用户的部门-角色配置，获取数据权限范围
            // 使用 selectList 而不是 selectOne，避免 TooManyResultsException
            List<SysDeptRole> deptRoles = deptRoleMapper.selectList(
                new LambdaQueryWrapper<SysDeptRole>()
                    .eq(SysDeptRole::getRoleId, userRoleId)
                    .eq(SysDeptRole::getDeptId, userDeptId)
            );
            
            if (deptRoles != null && !deptRoles.isEmpty()) {
                // 取第一条记录（理论上应该只有一条）
                SysDeptRole deptRole = deptRoles.get(0);
                String userScope = deptRole.getDataScope();
                
                // 如果用户的数据权限范围小于接口要求的范围，拒绝访问
                if (isDataScopeInsufficient(userScope, dataScope)) {
                    log.warn("用户 {} 数据权限不足，要求: {}, 实际: {}", userId, dataScope, userScope);
                    throw new BusinessException("无权访问：数据权限不足");
                }
                
                // 3. 具体的数据权限校验（根据dataScope和请求参数）
                if (!validateDataPermission(request, userId, userDeptId, dataScope, userScope)) {
                    log.warn("用户 {} 无权访问该数据，接口: {}", userId, request.getRequestURI());
                    throw new BusinessException("无权访问：无数据访问权限");
                }
            }
        }
        
        log.debug("用户 {} 通过权限检查，访问接口 {}", userId, request.getRequestURI());
        return true;
    }
    
    /**
     * 判断用户的数据权限是否不足
     */
    private boolean isDataScopeInsufficient(String userScope, String requiredScope) {
        // 数据权限等级：self < dept < dept_and_sub < all
        int userLevel = getDataScopeLevel(userScope);
        int requiredLevel = getDataScopeLevel(requiredScope);
        
        return userLevel < requiredLevel;
    }
    
    /**
     * 获取数据权限等级
     */
    private int getDataScopeLevel(String dataScope) {
        if (dataScope == null) {
            return 0;
        }
        switch (dataScope) {
            case "self":
                return 1;
            case "dept":
                return 2;
            case "dept_and_sub":
                return 3;
            case "all":
                return 4;
            default:
                return 0;
        }
    }
    
    /**
     * 校验数据权限
     * @param request HTTP请求
     * @param userId 当前用户ID
     * @param userDeptId 当前用户部门ID
     * @param requiredScope 接口要求的数据权限范围
     * @param userScope 用户实际的数据权限范围
     * @return 是否有权限访问
     */
    private boolean validateDataPermission(HttpServletRequest request, Long userId, 
                                           Integer userDeptId, String requiredScope, String userScope) {
        // 如果要求的是all，且用户有all权限，直接通过
        if ("all".equals(requiredScope) && "all".equals(userScope)) {
            return true;
        }
        
        // 根据不同的dataScope进行校验
        switch (requiredScope) {
            case "self":
                return validateSelfPermission(request, userId);
            case "dept":
                return validateDeptPermission(request, userDeptId);
            case "dept_and_sub":
                return validateDeptAndSubPermission(request, userDeptId);
            case "all":
                // 需要all权限才能访问
                return "all".equals(userScope);
            default:
                return true;
        }
    }
    
    /**
     * 校验本人数据权限
     * 检查请求的数据是否属于当前用户
     */
    private boolean validateSelfPermission(HttpServletRequest request, Long userId) {
        String uri = request.getRequestURI();
        
        // 对于用车申请相关接口，检查applyId
        if (uri.contains("/apply/")) {
            Long applyId = extractApplyIdFromRequest(request);
            if (applyId != null) {
                CarApplication application = carApplicationMapper.selectById(applyId);
                if (application != null) {
                    // 检查申请人是否为当前用户
                    return userId.equals(application.getApplyBy());
                }
            }
        }
        
        // 其他情况默认通过（具体业务逻辑可在Service层进一步校验）
        return true;
    }
    
    /**
     * 校验本部门数据权限
     * 检查请求的数据是否属于当前用户所在部门
     */
    private boolean validateDeptPermission(HttpServletRequest request, Integer userDeptId) {
        String uri = request.getRequestURI();
        
        // 对于用车申请相关接口，检查deptId
        if (uri.contains("/apply/")) {
            Long applyId = extractApplyIdFromRequest(request);
            if (applyId != null) {
                CarApplication application = carApplicationMapper.selectById(applyId);
                if (application != null) {
                    // 检查申请部门是否为当前用户部门
                    return userDeptId.equals(application.getDeptId());
                }
            }
        }
        
        return true;
    }
    
    /**
     * 校验本部门及下级部门数据权限
     * TODO: 需要实现部门树查询逻辑
     */
    private boolean validateDeptAndSubPermission(HttpServletRequest request, Integer userDeptId) {
        // 暂时简化处理，只检查本部门
        // TODO: 后续需要查询所有下级部门ID，然后检查数据是否在这些部门中
        return validateDeptPermission(request, userDeptId);
    }
    
    /**
     * 从请求中提取申请ID
     */
    private Long extractApplyIdFromRequest(HttpServletRequest request) {
        // 尝试从路径变量中获取
        String uri = request.getRequestURI();
        
        // 匹配 /apply/sub/detail/{applyId} 或 /apply/app/history/{applyId} 等格式
        if (uri.matches(".*/apply/.*/\\d+$")) {
            String[] parts = uri.split("/");
            try {
                return Long.parseLong(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                log.warn("无法解析申请ID: {}", uri);
            }
        }
        
        // 尝试从请求参数中获取
        String applyIdParam = request.getParameter("applyId");
        if (applyIdParam != null) {
            try {
                return Long.parseLong(applyIdParam);
            } catch (NumberFormatException e) {
                log.warn("无法解析申请ID参数: {}", applyIdParam);
            }
        }
        
        return null;
    }
}
