package com.baoying.vehicleapplication.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baoying.vehicleapplication.dto.request.DeptRoleAssignRequest;
import com.baoying.vehicleapplication.dto.request.DeptRoleRemoveRequest;
import com.baoying.vehicleapplication.dto.request.RoleRequest;
import com.baoying.vehicleapplication.dto.response.DeptRoleResponse;
import com.baoying.vehicleapplication.dto.response.RoleResponse;
import com.baoying.vehicleapplication.entity.SysRole;

import java.util.List;

public interface RoleService extends IService<SysRole> {
    
    /** 新增角色 */
    void addRole(RoleRequest request);
    
    /** 编辑角色 */
    void updateRole(RoleRequest request);
    
    /** 删除角色（检查是否被部门或账号使用） */
    void deleteRole(Integer id);
    
    /** 启用/禁用角色 */
    void updateStatus(Integer id, Integer status);
    
    /** 角色列表（含使用统计） */
    List<RoleResponse> listRoles(String name, Integer status);
    
    /** 角色详情 */
    RoleResponse getRoleDetail(Integer id);
    
    /** 为部门分配角色 */
    void assignRoleToDept(DeptRoleAssignRequest request);
    
    /** 移除部门的角色 */
    void removeRoleFromDept(DeptRoleRemoveRequest request);

    /** 更新部门角色的数据范围 */
    void updateDeptRoleDataScope(Integer deptId, Integer roleId, String dataScope);

    /** 获取部门已关联的角色列表 */
    List<DeptRoleResponse> getDeptRoles(Integer deptId);
    
    /** 获取部门可选择的角色列表（全局角色中未被该部门关联的） */
    List<SysRole> getAvailableRolesForDept(Integer deptId);
}