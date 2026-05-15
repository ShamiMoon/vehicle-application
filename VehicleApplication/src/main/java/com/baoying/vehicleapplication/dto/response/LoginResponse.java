package com.baoying.vehicleapplication.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private Long userId;
    private String username;
    private String realname;
    private Integer deptId;
    private String deptName;
    private Integer roleId;
    private String roleName;
    private String token;
    private Boolean needChangePassword;
}