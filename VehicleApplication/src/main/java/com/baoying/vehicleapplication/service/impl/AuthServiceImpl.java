package com.baoying.vehicleapplication.service.impl;

import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.LoginRequest;
import com.baoying.vehicleapplication.dto.response.LoginResponse;
import com.baoying.vehicleapplication.entity.SysDept;
import com.baoying.vehicleapplication.entity.SysRole;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.mapper.DeptMapper;
import com.baoying.vehicleapplication.mapper.RoleMapper;
import com.baoying.vehicleapplication.mapper.UserMapper;
import com.baoying.vehicleapplication.service.AuthService;
import com.baoying.vehicleapplication.utils.JwtUtils;
import com.baoying.vehicleapplication.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final JwtUtils jwtUtils;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 根据用户名查询用户
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 检查账号状态
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 3. 检查角色状态（如果用户有角色，且角色被禁用则不允许登录）
        if (user.getRoleId() != null) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            if (role == null || role.getStatus() != 1) {
                throw new BusinessException("当前角色已被禁用，请联系管理员");
            }
        }

        // 4. 验证密码
        if (!PasswordUtils.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 5. 检查临时密码是否过期
        if (user.getIsTempPassword() != null && user.getIsTempPassword() == 1) {
            if (user.getTempPasswordExpire() != null && 
                user.getTempPasswordExpire().isBefore(LocalDateTime.now())) {
                throw new BusinessException("临时密码已过期，请联系管理员重新重置");
            }
        }

        // 6. 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 7. 获取部门名称和角色名称
        String deptName = null;
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                deptName = dept.getName();
            }
        }

        String roleName = null;
        if (user.getRoleId() != null) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                roleName = role.getName();
            }
        }

        // 7. 生成Token（存储用户ID、部门ID、角色ID）
        String token = jwtUtils.generateToken(
            user.getId(), 
            user.getDeptId(), 
            user.getRoleId()
        );

        // 8. 构建响应
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealname(user.getRealname());
        response.setDeptId(user.getDeptId());
        response.setDeptName(deptName);
        response.setRoleId(user.getRoleId());
        response.setRoleName(roleName);
        response.setToken(token);
        response.setNeedChangePassword(
            user.getIsTempPassword() != null && user.getIsTempPassword() == 1
        );

        return response;
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException("Token无效或已过期");
        }
        return jwtUtils.refreshToken(token);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    @Override
    public Long getUserIdFromToken(String token) {
        return jwtUtils.getUserIdFromToken(token);
    }
}