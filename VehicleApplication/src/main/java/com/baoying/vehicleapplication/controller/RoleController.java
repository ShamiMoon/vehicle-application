package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.DeptRoleAssignRequest;
import com.baoying.vehicleapplication.dto.request.DeptRoleRemoveRequest;
import com.baoying.vehicleapplication.dto.request.RoleRequest;
import com.baoying.vehicleapplication.dto.response.DeptRoleResponse;
import com.baoying.vehicleapplication.dto.response.RoleResponse;
import com.baoying.vehicleapplication.entity.SysRole;
import com.baoying.vehicleapplication.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/org/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    // ==================== 角色管理 ====================
    @RequirePermission(roles = {1})
    @PostMapping("/add")
    public Result<Void> add(@RequestBody RoleRequest request) {
        roleService.addRole(request);
        return Result.success();
    }
    @RequirePermission(roles = {1})
    @PutMapping("/update")
    public Result<Void> update(@RequestBody RoleRequest request) {
        roleService.updateRole(request);
        return Result.success();
    }
    @RequirePermission(roles = {1})
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return Result.success();
    }
    @RequirePermission(roles = {1})
    @PutMapping("/status/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Integer id, @PathVariable Integer status) {
        roleService.updateStatus(id, status);
        return Result.success();
    }
    
    @GetMapping("/list")
    public Result<List<RoleResponse>> list(@RequestParam(required = false) String name,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(roleService.listRoles(name, status));
    }
    
    @GetMapping("/detail/{id}")
    public Result<RoleResponse> detail(@PathVariable Integer id) {
        return Result.success(roleService.getRoleDetail(id));
    }
    
    // ==================== 部门角色关联 ====================
    @RequirePermission(roles = {1})
    @PostMapping("/dept/assign")
    public Result<Void> assignToDept(@RequestBody DeptRoleAssignRequest request) {
        roleService.assignRoleToDept(request);
        return Result.success();
    }
    @RequirePermission(roles = {1})
    @DeleteMapping("/dept/remove")
    public Result<Void> removeFromDept(@RequestBody DeptRoleRemoveRequest request) {
        roleService.removeRoleFromDept(request);
        return Result.success();
    }
    
    @RequirePermission(roles = {1})
    @PutMapping("/dept/data-scope")
    public Result<Void> updateDataScope(@RequestParam Integer deptId,
                                        @RequestParam Integer roleId,
                                        @RequestParam String dataScope) {
        roleService.updateDeptRoleDataScope(deptId, roleId, dataScope);
        return Result.success();
    }

    @GetMapping("/dept/roles/{deptId}")
    public Result<List<DeptRoleResponse>> getDeptRoles(@PathVariable Integer deptId) {
        return Result.success(roleService.getDeptRoles(deptId));
    }
    
    @GetMapping("/dept/available/{deptId}")
    public Result<List<SysRole>> getAvailableRoles(@PathVariable Integer deptId) {
        return Result.success(roleService.getAvailableRolesForDept(deptId));
    }
}