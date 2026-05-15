package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.common.VehicleTypeEnum;
import com.baoying.vehicleapplication.dto.request.CarApplyQueryRequest;
import com.baoying.vehicleapplication.dto.request.CarApplySaveRequest;
import com.baoying.vehicleapplication.dto.request.CarApplySubmitRequest;
import com.baoying.vehicleapplication.dto.request.CarApplyUpdateRequest;
import com.baoying.vehicleapplication.dto.response.CarApplyDetailResponse;
import com.baoying.vehicleapplication.dto.response.CarApplyListItem;
import com.baoying.vehicleapplication.service.CarApplicationService;
import com.baoying.vehicleapplication.utils.CurrentUserUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/apply/sub")
@RequiredArgsConstructor
public class CarApplicationController {

    private final CarApplicationService carApplicationService;
    /**
     * 创建用车申请（所有登录用户）
     */
    @RequirePermission(checkRole = false)
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody CarApplySaveRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Integer deptId = CurrentUserUtils.getCurrentDeptId();
        Long applyId = carApplicationService.saveApplication(request, userId, deptId);
        return Result.success(applyId);
    }
    /**
     * 提交用车申请（所有登录用户）
     */
    @RequirePermission(checkRole = false)
    @PutMapping("/submit/{applyId}")
    public Result<Void> submit(@PathVariable Long applyId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        carApplicationService.submit(applyId, userId);
        return Result.success();
    }
    @RequirePermission(checkRole = false)
    @PostMapping("/submit-directly")
    public Result<Long> submit_d(@Valid @RequestBody CarApplySubmitRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Integer deptId = CurrentUserUtils.getCurrentDeptId();
        Long applyId = carApplicationService.submitApplication(request, userId, deptId);
        return Result.success(applyId);
    }

    /**
     * 修改用车申请（所有登录用户，仅限自己的草稿）
     */
    @RequirePermission(checkRole = false, dataScope = "self")
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody CarApplyUpdateRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        carApplicationService.updateApplication(request, userId);
        return Result.success();
    }

    /**
     * 撤销用车申请（所有登录用户，仅限自己的申请）
     */
    @RequirePermission(checkRole = false, dataScope = "self")
    @DeleteMapping("/cancel/{applyId}")
    public Result<Void> cancel(@PathVariable Long applyId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        carApplicationService.cancelApplication(applyId, userId);
        return Result.success();
    }

    /**
     * 我的申请列表（所有登录用户，分页）
     */
    @RequirePermission(checkRole = false, dataScope = "self")
    @GetMapping("/my-list")
    public Result<Page<CarApplyListItem>> myList(CarApplyQueryRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.success(carApplicationService.getMyApplicationList(request, userId));
    }

    /**
     * 全量申请列表（管理员和用车管理员，分页）
     */
    @RequirePermission(roles = {1, 2}, dataScope = "all")
    @GetMapping("/all-list")
    public Result<Page<CarApplyListItem>> allList(CarApplyQueryRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.success(carApplicationService.getAllApplicationList(request, userId));
    }

    /**
     * 待我审批列表（审批人）
     */
    @RequirePermission(checkRole = false, dataScope = "self")
    @GetMapping("/pending-list")
    public Result<List<CarApplyListItem>> pendingList() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.success(carApplicationService.getPendingList(userId));
    }

    /**
     * 申请详情（所有登录用户）
     */
    @GetMapping("/detail/{applyId}")
    public Result<CarApplyDetailResponse> detail(@PathVariable Long applyId) {
        return Result.success(carApplicationService.getApplicationDetail(applyId));
    }
    // 根据模板获取可选择的车辆类型交给前端（所有登录用户）
    @GetMapping("/vehicle-types/{templateType}")
    public Result<List<Map<String, Object>>> getAvailableVehicleTypes(@PathVariable Integer templateType) {
        List<Integer> typeCodes = VehicleTypeEnum.getAvailableTypesByTemplateType(templateType);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer code : typeCodes) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", code);
            item.put("name", VehicleTypeEnum.getNameByCode(code));
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 我审批过的申请列表（审批人，分页）
     */
    @RequirePermission(checkRole = false, dataScope = "self")
    @GetMapping("/approved-by-me")
    public Result<Page<CarApplyListItem>> approvedByMe(CarApplyQueryRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.success(carApplicationService.getApprovedByMeList(request, userId));
    }

    /**
     * 处理异常申请（仅超级管理员和用车管理员）
     * @param applyId 申请ID
     * @param action 操作：1-强制通过，2-强制驳回
     * @param reason 原因（驳回时必填）
     */
    @RequirePermission(roles = {1, 2}, dataScope = "all")
    @PostMapping("/handle-abnormal/{applyId}")
    public Result<Void> handleAbnormal(@PathVariable Long applyId,
                                       @RequestParam Integer action,
                                       @RequestParam(required = false) String reason) {
        Long operatorId = CurrentUserUtils.getCurrentUserId();
        carApplicationService.handleAbnormalApplication(applyId, action, reason, operatorId);
        return Result.success();
    }
}