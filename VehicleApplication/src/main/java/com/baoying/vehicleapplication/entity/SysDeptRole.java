package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_dept_role")
public class SysDeptRole {
    private Integer deptId;
    private Integer roleId;
    private String dataScope;  // self/dept/dept_and_sub/all
}