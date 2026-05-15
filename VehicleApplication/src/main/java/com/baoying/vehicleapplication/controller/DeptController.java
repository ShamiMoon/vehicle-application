package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.DeptRequest;
import com.baoying.vehicleapplication.dto.request.DeptSortItem;
import com.baoying.vehicleapplication.dto.response.DeptDetailResponse;
import com.baoying.vehicleapplication.dto.response.DeptTreeResponse;
import com.baoying.vehicleapplication.entity.SysDept;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.service.DeptService;
import com.baoying.vehicleapplication.utils.CurrentUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/org/dept")
public class DeptController {

    @Autowired
    private DeptService deptService;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return CurrentUserUtils.getCurrentUserId();
    }

    /**
     * 新增部门（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PostMapping("/add")
    public Result<Void> add(@RequestBody DeptRequest request, HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        deptService.addDept(request, currentUserId);
        return Result.success();
    }

    /**
     * 编辑部门（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PutMapping("/update")
    public Result<Void> update(@RequestBody DeptRequest request) {
        deptService.updateDept(request);
        return Result.success();
    }

    /**
     * 删除部门（仅超级管理员）
     * @param targetDeptId 可选，不为空时将该部门的账号转移到目标部门后再删除
     */
    @RequirePermission(roles = {1})
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id,
                               @RequestParam(required = false) Integer targetDeptId) {
        deptService.deleteDept(id, targetDeptId);
        return Result.success();
    }

    /**
     * 启用/禁用部门（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PutMapping("/status/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Integer id, @PathVariable Integer status) {
        deptService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 获取部门树形列表（所有登录用户）
     */
    @GetMapping("/tree")
    public Result<List<DeptTreeResponse>> tree() {
        return Result.success(deptService.getDeptTree());
    }

    @GetMapping("/list")
    public Result<List<SysDept>> list() { return Result.success(deptService.getList());}
    /**
     * 获取部门详情（所有登录用户）
     */
    @GetMapping("/detail/{id}")
    public Result<DeptDetailResponse> detail(@PathVariable Integer id) {
        return Result.success(deptService.getDeptDetail(id));
    }

    /**
     * 获取部门下账号数量（仅超级管理员）
     */
    @GetMapping("/user-count/{id}")
    public Result<Long> userCount(@PathVariable Integer id) {
        return Result.success(deptService.getUserCountByDeptId(id));
    }
    /**
     * 获取部门下的账号列表（仅超级管理员）
     */
    @GetMapping("/users/{deptId}")
    public Result<List<SysUser>> users(@PathVariable Integer deptId) {
        return Result.success(deptService.getUsersByDeptId(deptId));
    }
    /**
     * 批量转移部门下的账号（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PutMapping("/transfer")
    public Result<Void> transfer(@RequestParam Integer sourceDeptId,
                                 @RequestParam Integer targetDeptId) {
        deptService.transferUsers(sourceDeptId, targetDeptId);
        return Result.success();
    }
    /**
     * 批量更新部门排序（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PutMapping("/sort/batch")
    public Result<Void> batchUpdateSort(@RequestBody List<DeptSortItem> items) {
        deptService.batchUpdateSort(items);
        return Result.success();
    }

}