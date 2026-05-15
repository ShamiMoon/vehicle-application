package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class DeptRoleAssignRequest {
    private Integer deptId;
    private Integer roleId;
    private String dataScope;  // self/dept/dept_and_sub/all
}