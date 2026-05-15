package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String password;
    private String realname;
    private String phone;
    private String email;
    private Integer deptId;
    private Integer roleId;
    private Integer emailNotify;
}