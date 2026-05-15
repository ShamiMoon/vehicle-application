package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private Long id;
    private String realname;
    private String phone;
    private String email;
    private Integer deptId;
    private Integer roleId;
    private Integer status;
    private Integer emailNotify;
}