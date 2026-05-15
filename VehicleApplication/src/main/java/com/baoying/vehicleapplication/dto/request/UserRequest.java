package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UserRequest {
    private Long id;
    private String username;
    private String password;
    private String realname;
    private String phone;
    private String email;
    private Integer deptId;
    private Integer status;
    private Integer emailNotify;
    private List<Integer> roleIds;  // 关联的角色ID列表
}