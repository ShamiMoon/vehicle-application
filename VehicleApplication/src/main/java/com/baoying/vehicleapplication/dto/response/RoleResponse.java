package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoleResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private Integer userCount;      // 使用该角色的账号数
    private Integer deptCount;      // 关联该角色的部门数
}