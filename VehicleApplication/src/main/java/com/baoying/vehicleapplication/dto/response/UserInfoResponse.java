package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserInfoResponse {
    private Long id;
    private String username;
    private String realname;
    private String phone;
    private String email;
    private Integer deptId;
    private String deptName;
    private Integer roleId;
    private String roleName;
    private Integer status;
    private Integer emailNotify;
    private String dataScope;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}