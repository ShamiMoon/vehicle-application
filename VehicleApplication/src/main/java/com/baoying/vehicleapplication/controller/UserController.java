package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.*;
import com.baoying.vehicleapplication.dto.response.UserInfoResponse;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.service.UserService;
import com.baoying.vehicleapplication.utils.CurrentUserUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/org/user")
public class UserController {

    @Autowired
    private UserService userService;
    @GetMapping("/current")
    public Result<Long> getCurrentUserId() {
        return Result.success(CurrentUserUtils.getCurrentUserId());
    }

    // 获取真实姓名
    @RequirePermission(checkRole = false)
    @GetMapping("/getName")
    public Result<String> getName(){
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        return Result.success(user.getRealname());
    }
    /**
     * 新增账号
     */
    @PostMapping("/create")
    public Result<Void> create(@RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return Result.success();
    }

    /**
     * 编辑账号
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody UserUpdateRequest request) {
        userService.updateUser(request);
        return Result.success();
    }

    /**
     * 删除账号
     */
    @RequirePermission(roles = {1})
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 启用/禁用账号
     */
    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 管理员重置密码（生成临时密码）
     */
    @RequirePermission(roles = {1})
    @PutMapping("/reset-pwd")
    public Result<Void> resetPassword(@RequestBody UserResetPwdRequest request) {
        userService.resetPasswordByAdmin(request);
        return Result.success();
    }

    /**
     * 用户修改密码（登录后使用）
     */
    @PutMapping("/change-pwd")
    public Result<Void> changePassword(@RequestBody UserChangePwdRequest request) {
        userService.changePassword(request);
        return Result.success();
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/detail/{id}")
    public Result<UserInfoResponse> detail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    /**
     * 用户修改自己的个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserProfileRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        userService.updateProfile(userId, request);
        return Result.success();
    }
    /**
     * 获取用户列表（支持分页和多条件搜索）
     */
    @RequirePermission(checkRole = false)
    @GetMapping("/list")
    public Result<Page<UserInfoResponse>> list(UserQueryRequest request) {
        return Result.success(userService.getUserList(request));
    }
}