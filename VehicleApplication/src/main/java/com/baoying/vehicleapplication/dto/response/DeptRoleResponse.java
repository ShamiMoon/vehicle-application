package com.baoying.vehicleapplication.dto.response;

import lombok.Data;

@Data
public class DeptRoleResponse {
    private Integer roleId;
    private String roleName;
    private String dataScope;
    private Integer userCount;      // 该部门下使用此角色的账号数
}