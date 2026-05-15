package com.baoying.vehicleapplication.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baoying.vehicleapplication.dto.request.*;
import com.baoying.vehicleapplication.dto.response.LoginResponse;
import com.baoying.vehicleapplication.dto.response.UserInfoResponse;
import com.baoying.vehicleapplication.entity.SysUser;

import java.util.List;

public interface UserService extends IService<SysUser> {


    // 新增账号
    void createUser(UserCreateRequest request);

    // 编辑账号
    void updateUser(UserUpdateRequest request);

    // 删除账号
    void deleteUser(Long id);

    // 启用/禁用账号
    void updateStatus(Long id, Integer status);

    // 管理员重置密码（生成临时密码）
    void resetPasswordByAdmin(UserResetPwdRequest request);

    // 用户修改密码（强制修改或主动修改）
    void changePassword(UserChangePwdRequest request);

    // 获取用户详情
    UserInfoResponse getUserDetail(Long id);

    // 获取用户列表（分页+多条件搜索）
    Page<UserInfoResponse> getUserList(UserQueryRequest request);

    // 用户修改自己的个人信息
    void updateProfile(Long userId, UserProfileRequest request);

    // 忘记密码（验证身份后通知管理员）
    void forgotPassword(ForgotPasswordRequest request);
}