package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sort;
    private String description;
    private Integer status;  // 1启用 0禁用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long createBy;
}