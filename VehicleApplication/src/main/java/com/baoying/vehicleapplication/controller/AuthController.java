package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.CarApplyQueryRequest;
import com.baoying.vehicleapplication.dto.request.ForgotPasswordRequest;
import com.baoying.vehicleapplication.dto.request.LoginRequest;
import com.baoying.vehicleapplication.dto.response.LoginResponse;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.service.AuthService;
import com.baoying.vehicleapplication.service.CarApplicationService;
import com.baoying.vehicleapplication.service.UserService;
import com.baoying.vehicleapplication.utils.CurrentUserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CarApplicationService carApplicationService;
    private final UserService userService;
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 忘记密码（验证身份后通知管理员）
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return Result.success();
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<String> refresh(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.error(401, "Token不存在");
        }
        String newToken = authService.refreshToken(token);
        if (newToken == null) {
            return Result.error(401, "Token无效或已过期");
        }
        return Result.success(newToken);
    }

    /**
     * 验证Token是否有效
     */
    @GetMapping("/validate")
    public Result<Boolean> validate(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.error("未检测到tokens");
        }
        return Result.success(authService.validateToken(token));
    }

    /**
     * 导出用车申请数据（Excel）
     */
    @RequirePermission(checkRole = false)
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportApplications(CarApplyQueryRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        byte[] excelData = carApplicationService.exportApplications(request, userId);

        String fileName = "用车申请数据_" + System.currentTimeMillis() + ".xlsx";
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (Exception e) {
            encodedFileName = fileName;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }
}