package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realname;
    private String phone;
    private String email;
    private Integer deptId;
    private Integer roleId;
    private Integer status;  // 1启用 0禁用
    private Integer emailNotify;  // 1开启 0关闭

    private Integer isTempPassword;      // 0否 1是
    private LocalDateTime tempPasswordExpire;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}