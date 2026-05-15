package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class DeptRoleRemoveRequest {
    private Integer deptId;
    private Integer roleId;
}